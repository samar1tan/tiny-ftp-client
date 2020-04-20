package edu.whu.cs.ftp.downloader;

import edu.whu.cs.ftp.client.*;
import edu.whu.cs.ftp.uploader.UpLoader;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Downloader implements StreamLogging {
    private final ControlSocket controlSocket;
    private final FTPClient ftpClient;
    private final StatusPublisher guiStatusPublisher;
    private DownloadExpectedStatusCodes expectedStatusCodes;
    private boolean isAborted;

    public Downloader(ControlSocket controlSocket, FTPClient ftpClient, StatusPublisher guiStatusPublisher) {
        this.controlSocket = controlSocket;
        this.ftpClient = ftpClient;
        this.guiStatusPublisher = guiStatusPublisher;
        this.expectedStatusCodes = new DownloadExpectedStatusCodes();
        this.isAborted = false;
    }

    public void downloadFileOrDirectory(FTPPath downloadFrom, String saveTo) throws DownloadException, IOException {
        if (isAborted) {
            return;
        }

        if (!downloadFrom.isDirectory()) {
            downloadFile(downloadFrom, saveTo);
        } else {
            File rootDir = new File(saveTo);
            if (!rootDir.exists()) {
                if (!rootDir.mkdir()) {
                    throw new CreateSaveDirFailed(saveTo);
                }
            }

            FTPPath[] subPaths = ftpClient.list(downloadFrom.getPath());
            for (FTPPath subPath : subPaths) {
                DirSeparator dirSeparator = new DirSeparator(DirSeparatorModes.LocalMachine);
                String subSavePath = saveTo + (saveTo.endsWith(dirSeparator.getSeparator()) ?
                        "" : dirSeparator.getSeparator()) + subPath.getName();
                downloadFileOrDirectory(subPath, subSavePath);
            }
        }
    }

    /**
     * assume in passive mode (PASV), CWD -> SIZE -> REST -> RETR
     */
    private void downloadFile(FTPPath downloadFrom, String saveTo) throws DownloadException, IOException {
        FileInfo fileInfo = new FileInfo();
        checkRemoteFile(downloadFrom, fileInfo);
        checkLocalPath(saveTo, fileInfo);

        long dataSocketOpenedTimeInMs = Calendar.getInstance().getTimeInMillis();
        fileInfo.guiStatusID = guiStatusPublisher.initialize(saveTo, downloadFrom.getPath(),
                StatusPublisher.DIRECTION.DOWNLOAD, getSize(fileInfo.serverFileByteNum));
        guiStatusPublisher.publish(fileInfo.guiStatusID, "0.00%");
        DataSocket ftpDataSocket;
        if (fileInfo.downloadedByteNum > 0) {
            // REST must be executed right before RETR
            ftpDataSocket = execFTPCommand("RETR", fileInfo.serverFileName,
                    "REST", String.valueOf(fileInfo.downloadedByteNum));
            publishGUIStatus(fileInfo, guiStatusPublisher);
        } else {
            ftpDataSocket = (DataSocket) execFTPCommand("RETR", fileInfo.serverFileName, true);
        }

        Socket dataSocket = ftpDataSocket.getDataSocket();
        File tempFilePath = new File(fileInfo.localFilePath + ".ftpdownloading");
        FileOutputStream tempFileStream;
        if (fileInfo.downloadedByteNum > 0) {
            tempFileStream = new FileOutputStream(tempFilePath, true);
        } else {
            tempFileStream = new FileOutputStream(tempFilePath);
        }
        InputStream readFromServer = dataSocket.getInputStream();
        BufferedOutputStream tempFileBufferedStream = new BufferedOutputStream(tempFileStream);
        int bytesRead;
        byte[] byteArrayBuffer = new byte[8192];
        final int ROUND_NUM_PER_PUBLISH = 10000;
        int roundNum = 0;
        while ((bytesRead = readFromServer.read(byteArrayBuffer)) > 0) {
            if (Thread.currentThread().isInterrupted()) {
                isAborted = true;
                break;
            }

            tempFileBufferedStream.write(byteArrayBuffer, 0, bytesRead);

            if (roundNum == ROUND_NUM_PER_PUBLISH) {
                publishGUIStatus(fileInfo, guiStatusPublisher);
                roundNum = 0;
            } else {
                ++roundNum;
            }
        }

        tempFileBufferedStream.flush();
        tempFileBufferedStream.close(); // as well as underlying FileOutputStream tempFileStream
        final long minDataSocketLiveTimeInMs = 1000;
        long dataSocketClosedTimeInMs = Calendar.getInstance().getTimeInMillis();
        long dataSocketLiveTime = dataSocketClosedTimeInMs - dataSocketOpenedTimeInMs;
        if (dataSocketLiveTime < minDataSocketLiveTimeInMs) {
            logger.warning(String.format(
                    "DataSocket will die too fast (%s ms). +%ss for it.",
                    dataSocketLiveTime, minDataSocketLiveTimeInMs / 1000)
            );
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ftpDataSocket.close(); // as well as associated InputStream readFromServer

        if (!isAborted) {
            Files.move(tempFilePath.toPath(), Path.of(saveTo));
        }
        guiStatusPublisher.publish(fileInfo.guiStatusID, "完成");
    }

    private void publishGUIStatus(FileInfo fileInfo, StatusPublisher guiStatusPublisher) {
        File localFilePointer = new File(fileInfo.localFilePath + ".ftpdownloading");
        if (localFilePointer.exists()) {
            fileInfo.downloadedByteNum = localFilePointer.length();
            fileInfo.completeRatio.setRatioNum((double) fileInfo.downloadedByteNum / fileInfo.serverFileByteNum);
            guiStatusPublisher.publish(fileInfo.guiStatusID, fileInfo.completeRatio.getCompleteRatio());
        }
    }

    /** DataSocket / String message, depending on parameter getDataSocket */
    private Object execFTPCommand(String cmd, String arg, boolean getDataSocket) throws DownloadException, IOException {
        String command = cmd + ' ' + arg;
        if (getDataSocket) {
            DataSocket dataSocket = controlSocket.execute(command, expectedStatusCodes.getStatusCode(cmd));
            if (dataSocket == null) {
                throw new FTPCommandFailedException(cmd, arg, null);
            }
            return dataSocket;
        } else {
            controlSocket.execute(command);
            String statusMessage = controlSocket.getMessage();
            int statusCode = Integer.parseInt(statusMessage.substring(0, 3));
            if (statusCode != expectedStatusCodes.getStatusCode(cmd)) {
                throw new FTPCommandFailedException(cmd, arg, statusMessage);
            }
            return statusMessage;
        }
    }

    /** REST must be executed right before RETR, without PASV in between */
    private DataSocket execFTPCommand(String cmd, String arg, String preSimpleCmd, String preSimpleArg)
            throws DownloadException, IOException {
        String preSimpleCommand = preSimpleCmd + ' ' + preSimpleArg;
        String command = cmd + ' ' + arg;
        DataSocket dataSocket = controlSocket.execute(command, expectedStatusCodes.getStatusCode(cmd), preSimpleCommand);
        if (dataSocket == null) {
            throw new FTPCommandFailedException(cmd, arg, null);
        }

        return dataSocket;
    }

    private String execFTPCommand(String cmd, String arg, String groupedRegex, int regexGroupIndex)
            throws DownloadException, IOException {
        String statusMessage = (String) execFTPCommand(cmd, arg, false);
        Pattern pattern = Pattern.compile(groupedRegex);
        Matcher matcher = pattern.matcher(statusMessage);
        if (matcher.matches()) {
            return matcher.group(regexGroupIndex);
        } else {
            throw new ParseStatusMessageFailed(statusMessage, groupedRegex, regexGroupIndex);
        }
    }

    private long getServerFileSize(String serverFileName, FTPPath serverPath) throws DownloadException, IOException {
        long serverFileByteNum = 0;

        String ret = execFTPCommand("SIZE", serverFileName, "(\\d+)(\\s)(\\d+)(\\s+)", 3);
        serverFileByteNum = ret.equals("0") ? -1 : Long.parseLong(ret);
        if (serverFileByteNum == -1) {
            throw new ServerFileNotExistsException(serverPath);
        }

        return serverFileByteNum;
    }

    /** check existence and get size of remote file */
    private void checkRemoteFile(FTPPath remotePath, FileInfo fileInfo)
            throws DownloadException, IOException {
        // change working dir
        if (remotePath.getName().contains(" ")) {
            fileInfo.serverFileName = "\"" + remotePath.getName() + "\"";
        } else {
            fileInfo.serverFileName = remotePath.getName();
        }
        fileInfo.serverFileDir = parseDirFromString(remotePath.getPath(), new DirSeparator(DirSeparatorModes.FTP));
        if (!ftpClient.getWorkingDirectory().equals(fileInfo.serverFileDir)) {
            ftpClient.changeWorkingDirectory(fileInfo.serverFileDir);
        }

        // check if remote file exists and get SIZE
        fileInfo.serverFileByteNum = getServerFileSize(fileInfo.serverFileName, remotePath);
    }

    /** check if local path available to save and collect related info */
    private void checkLocalPath(String localPath, FileInfo fileInfo) throws DownloadException {
        // check if local path is already occupied
        File pathOccupied = new File(localPath);
        if (pathOccupied.exists()) {
            throw new LocalPathOccupiedException(localPath);
        }

        // collect path info and check dir
        fileInfo.localFileName = parseNameFromString(localPath, new DirSeparator(DirSeparatorModes.LocalMachine));
        fileInfo.localFileDir = parseDirFromString(localPath, new DirSeparator(DirSeparatorModes.LocalMachine));
        fileInfo.localFilePath = fileInfo.localFileDir + fileInfo.localFileName;
        if (!new File(fileInfo.localFileDir).exists()) {
            throw new SaveDirNotExistsException(fileInfo.localFileDir);
        }

        // check if enough local space and downloaded before
        File downloadedFile = new File(localPath + ".ftpdownloading");
        fileInfo.downloadedByteNum = 0;
        if (downloadedFile.exists()) {
            fileInfo.downloadedByteNum = downloadedFile.length();
        }
        long restByteNum = fileInfo.serverFileByteNum - fileInfo.downloadedByteNum;

        // check if have enough space
        long availableByteNum = new File(fileInfo.localFileDir).getUsableSpace();
        if (restByteNum >= availableByteNum) {
            throw new NoEnoughSpaceException(localPath, restByteNum, availableByteNum);
        }
    }

    public static String getSize(long size) {
        String fromUploader = UpLoader.getSize(size);
        String number, unit;
        char lastNo2 = fromUploader.charAt(fromUploader.length() - 2);
        if (Character.isDigit(lastNo2)) {
            unit = "B";
            number = fromUploader.substring(0, fromUploader.length() - 1);
        } else {
            unit = fromUploader.substring(fromUploader.length() - 2);
            number = fromUploader.substring(0, fromUploader.length() - 2);
        }

        return number + ' ' + unit;
    }

    public static String parseDirFromString(String fullPath, DirSeparator separator) {
        String[] parts = fullPath.split(separator.getSeparatorForRegex());
        StringBuilder dir = new StringBuilder();
        int len = parts.length;
        for (int i = 0; i + 1 < len; ++i) {
            dir.append(parts[i]);
            dir.append(separator);
        }

        return dir.toString();
    }

    public static String parseNameFromString(String fullPath, DirSeparator separator) {
        String[] parts = fullPath.split(separator.getSeparatorForRegex());

        return parts[parts.length - 1];
    }

    public static void main(String[] args) {
    }
}

class CompleteRatio {
    private double ratioNum = 0.;

    public void setRatioNum(double ratioNum) {
        this.ratioNum = ratioNum;
    }

    public String getCompleteRatio() {
        return String.format("%.2f%%", ratioNum * 100);
    }

    @Override
    public String toString() {
        return getCompleteRatio();
    }
}

class FileInfo {
    public String serverFileName;
    public String serverFileDir;
    public String localFileName;
    public String localFileDir;
    public String localFilePath;
    public long serverFileByteNum;
    public long downloadedByteNum;
    public int guiStatusID;
    public CompleteRatio completeRatio = new CompleteRatio();
}