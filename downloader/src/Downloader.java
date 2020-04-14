import edu.whu.cs.ftp.client.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Downloader implements StreamLogging {
    private ControlSocket controlSocket;
    private FTPClient ftpClient;
    private DownloadExpectedStatusCodes expectedStatusCodes;
//    private DownloadingStates downloadingStates; // TODO: thread to refresh

    public Downloader(ControlSocket controlSocket, FTPClient ftpClient) {
        this.controlSocket = controlSocket;
        this.ftpClient = ftpClient;
    }

    // assume in passive mode (PASV):
    // client:N & N+1 (N > 1024) -> client:N--control socket--server:21
    // -> client send PASV -> server:P (P>1024)
    // -> client get "227 entering passive mode (h1,h2,h3,h4,p1,p2)" -> h1.h2.h3.h4:p1*256+p2
    // -> client:N+1--data socket--server:P (already in this step when get DataSocket)
    // CWD -> SIZE -> REST -> RETR
    public void downloadFile(FTPPath downloadFrom, String saveTo) throws DownloadException, IOException {
        FileInfo fileInfo = checkFilesBeforeDataSocket(downloadFrom, saveTo);


        DataSocket dataSocket = controlSocket.execute("MLSD " + dir, 150);
        if (dataSocket == null)
            return null;
        FTPPath[] paths = FTPPath.parseFromMLSD(
                dir, dataSocket.getTextResponse());
        if (controlSocket.getStatusCode() != 226)
            return null;
        String[] res = rawList(dir);
        if (res == null)
            return null;
        for (int i = 0; i < res.length; i++)
            paths[i].addPermission(res[i]);
        return paths;

        FileOutputStream temp_file;
        if (downloadingStates.isExist(saveTo)) {
            temp_file = new FileOutputStream(saveTo.getPath(), true);
            setServerFileStartByte(downloadingStates.getDownloadingState(saveTo).bytePointer);
        } else {
            temp_file = new FileOutputStream(saveTo.getPath());
        }

        temp_file.close();
    }

    public void downloadDirectory(FTPPath serverPath, FTPPath savePath) throws DownloadException {
    }

    private void setServerFileStartByte(int bytePointer) {

    }

    private static String parseDirFromString(String fullPath, dirSeparator separator) {
        String[] parts = fullPath.split(String.valueOf(separator.getCodingSeparator()));
        StringBuilder dir = new StringBuilder();
        int len = parts.length;
        for (int i = 0; i + 1 < len; ++i) {
            dir.append(parts[i]);
            dir.append(separator);
        }

        return dir.toString();
    }

    private static String parseNameFromString(String fullPath, dirSeparator separator) {
        String[] parts = fullPath.split(separator.getCodingSeparator());

        return parts[parts.length - 1];
    }

    private boolean execCommand(String cmd, String arg) throws IOException {
        controlSocket.execute(cmd + ' ' + arg);
        return controlSocket.getStatusCode() == expectedStatusCodes.getStatusCode(cmd);
    }

    private Object execCommand(String cmd, String arg, int expectedStatusCode, boolean getDataSocket)
            throws IOException {
        DataSocket dataSocket = controlSocket.execute("MLSD " + dir, 150);
        controlSocket.execute(cmd + ' ' + arg);
        return controlSocket.getStatusCode() == expectedStatusCode;
    }

    private String execCommand(String cmd, String arg, String groupedRegex, int regexGroupIndex)
            throws IOException {
        if (execCommand(cmd, arg)) {
            Pattern pattern = Pattern.compile(groupedRegex);
            Matcher matcher = pattern.matcher(controlSocket.getMessage());
            return matcher.group(regexGroupIndex);
        } else {
            return null;
        }
    }

    private long getServerFileSize(String serverFileName, FTPPath serverPath) throws DownloadException, IOException {
        long serverFileByteNum = 0;

        String ret = execCommand("SIZE", serverFileName, "(\\d+)(\\s)(\\d+)", 3);
        serverFileByteNum = Boolean.getBoolean(ret) ? Long.getLong(ret) : -1;
        if (serverFileByteNum == -1) {
            throw new ServerFileNotExistsException(serverPath);
        }

        return serverFileByteNum;
    }

    private FileInfo checkFilesBeforeDataSocket(FTPPath remotePath, String localPath)
            throws DownloadException, IOException {
        FileInfo fileInfo = new FileInfo();

        // check if local path is already occupied
        File pathOccupied = new File(localPath);
        if (pathOccupied.exists()) {
            throw new LocalPathOccupiedException(localPath);
        }

        // Change Working Directory and collect path infos
        fileInfo.serverFileName = remotePath.getName();
        fileInfo.serverFileDir = parseDirFromString(remotePath.getPath(), new dirSeparator(dirSeparatorModes.FTP));
        if (!ftpClient.getWorkingDirectory().equals(fileInfo.serverFileDir)) {
            ftpClient.changeWorkingDirectory(fileInfo.serverFileDir);
        }
        fileInfo.localFileName = parseNameFromString(localPath, new dirSeparator(dirSeparatorModes.LocalMachine));
        fileInfo.localFileDir = parseDirFromString(localPath, new dirSeparator(dirSeparatorModes.LocalMachine));

        // check if remote file exists and get SIZE
        fileInfo.serverFileByteNum = getServerFileSize(fileInfo.serverFileName, remotePath);

        // check if enough local space and downloaded before
        File downloadedFile = new File(localPath + ".ftpdownloading");
        fileInfo.downloadedByteNum = 0;
        if (downloadedFile.exists()) {
            fileInfo.downloadedByteNum = downloadedFile.length();
        }
        long restByteNum = new File(localPath).getUsableSpace() - fileInfo.downloadedByteNum;

        // check if have enough space
        if (fileInfo.serverFileByteNum >= restByteNum) {
            throw new NoEnoughSpaceException(localPath, fileInfo.serverFileByteNum, restByteNum);
        }

        return fileInfo;
    }

    public static void main(String[] args) {
        System.out.println("Unit testing: the downloading module of FTP client");
    }
}

class FileInfo {
    public String serverFileName;
    public String serverFileDir;
    public String localFileName;
    public String localFileDir;
    public long serverFileByteNum;
    public long downloadedByteNum;
}

enum dirSeparatorModes {
    FTP,
    Unix,
    Windows,
    LocalMachine,
}

class dirSeparator {
    private String separator;

    public dirSeparator(dirSeparatorModes mode) {
        switch (mode) {
            case FTP:
            case Unix:
                separator = "/";
                break;
            case Windows:
                separator = "\\\\"; // code-level representation
                break;
            case LocalMachine:
                separator = File.separator.equals("\\") ? "\\\\" : File.separator;
        }
    }

    public String getCodingSeparator() {
        return separator;
    }

    @Override
    public String toString() { // real representation
        return separator.equals("\\\\") ? "\\" : separator;
    }
}
