package edu.whu.cs.ftp.uploader;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.NumberFormat;

import edu.whu.cs.ftp.client.*;

public class UpLoader implements StreamLogging {
    private ControlSocket controlSocket;
    private DataSocket dataSocket;
    private FTPClient ftpClient;
    public ConnectMySQL db = new ConnectMySQL();
    private int id;
    private StatusPublisher publisher;

    NumberFormat nt = NumberFormat.getPercentInstance();

    public UpLoader(FTPClient ftpClient, ControlSocket controlSocket, StatusPublisher publisher) throws IOException, SQLException {
        this.controlSocket = controlSocket;
        this.ftpClient = ftpClient;
        this.publisher = publisher;
    }

    /*
    上传文件
     */
    public UploadStatus UpLoadFile(Path local_path, FTPPath server_path, String serverFileName) throws IOException, SQLException {

        UploadStatus result;
        File localFile = local_path.toFile();
//        String serverFileName = localFile.getName();


        logger.info("UploadNewFileStart:" + localFile.getPath() + "-->" + server_path.getPath() + serverFileName);

        //处理远程目录
        if(server_path.isDirectory())
        {
            if(!ftpClient.changeWorkingDirectory(server_path.getPath())) {
                logger.info("Wrong Server Path");
                return UploadStatus.SeverPathWrong;
            }
        }

        //检查远程是否存在文件
        FTPPath[] files = ftpClient.list(serverFileName);
        if(files == null)
        {
            db.add(localFile.getPath(), server_path.getPath() + serverFileName, String.valueOf(localFile.length()));

            //初始化状态信息
            id = publisher.initialize(localFile.getPath(), server_path.getPath(), StatusPublisher.DIRECTION.UPLOAD, getSize(localFile.length()));
            result = Start(serverFileName, localFile);
            if(result == UploadStatus.UploadNewFileSuccess)
            {
                db.delete(localFile.getPath(), server_path.getPath() + serverFileName, String.valueOf(localFile.length()));
            }
        }
        else
        {
            long serverFileSize = files[0].getSize();
            long localFileSize = localFile.length();
            //以前不存在上传记录，说明存在同名文件
            if(!db.check(localFile.getPath(), server_path.getPath() + serverFileName, String.valueOf(localFile.length())))
            {

                result = UploadStatus.FileExits;
                logger.info("FileExits");
                return result;
            }
            else
            {
                //移动文件内读取指针，实现断点续传
                id = publisher.initialize(localFile.getPath(), server_path.getPath(), StatusPublisher.DIRECTION.UPLOAD, getSize(localFile.length()));
                result = Continue(serverFileName, localFile, serverFileSize);

                //断点续传失败，重新上传
                if(result == UploadStatus.UploadFromBreakFail)
                {
                    logger.info("UploadFromBreakFail,TryAgain");
                    if(!ftpClient.deleteFile(serverFileName))
                    {
                        result = UploadStatus.DeleteServerFileFail;
                        logger.info("DeleteServerFail");
                        return result;
                    }

                    db.delete(localFile.getPath(), server_path.getPath() + serverFileName, String.valueOf(localFile.length()));

                    id = publisher.initialize(localFile.getPath(), server_path.getPath(), StatusPublisher.DIRECTION.UPLOAD, getSize(localFile.length()));

                    result = Start(serverFileName, localFile);
                }
                else{
                    db.delete(localFile.getPath(), server_path.getPath() + serverFileName, String.valueOf(localFile.length()));

                }
            }

        }

        return result;
    }


    /*
    上传整个目录
    */
    public UploadStatus UpLoadDirectory(Path local_path, FTPPath server_path, String serverDirectoryName) throws IOException, SQLException {
        File fs = local_path.toFile();

        logger.info("UpLoadDirectory:" + fs.getPath() + "-->" + server_path.getPath());

        if(fs.isFile())
        {
           return UpLoadFile(local_path, server_path, fs.getName());
        }
        else
        {
            server_path = new FTPPath(server_path.getPath(), serverDirectoryName);
//            ftpClient.makeDirectory(server_path.getPath());
            if(!ftpClient.makeDirectory(server_path.getPath())) {
                logger.info("CreateDirectoryFail");
                return UploadStatus.CreateDirectoryFail;
            }

            File[] fi = fs.listFiles();
            for(File f : fi)
            {
                if(f.isFile())
                {
                    UpLoadFile(Paths.get(local_path.toString(), "/" , f.getName()), server_path, f.getName());
                }
                else
                {
                    UpLoadDirectory(Paths.get(local_path.toString(), "/" , f.getName()), server_path, f.getName());
                }

            }

            logger.info("UpLoadDirectoryFinish");
            return UploadStatus.UpLoadDirectoryFinish;
        }
    }

    /*
    首次上传
     */
    private UploadStatus Start(String serverFileName, File localFile) throws IOException {
        UploadStatus status;

        float step = localFile.length();
        float process = 0;
        float localRead = 0;

        dataSocket = controlSocket.execute("STOR " + serverFileName, 150);

        RandomAccessFile raf = new RandomAccessFile(localFile, "r");
        BufferedOutputStream out = new BufferedOutputStream(dataSocket.getDataSocket().getOutputStream());

        byte[] buffer = new byte[1024];
        int bytesRead;

        int i = 0;//控制刷新的频率
        nt.setMaximumFractionDigits(2);
        if(step != 0)
        {
            while ((bytesRead = raf.read(buffer)) != -1)
            {
                out.write(buffer, 0, bytesRead);
                localRead += bytesRead;
                if(localRead / step != process)
                {
                    process = localRead / step;

                    i++;

                    if(i > 10000)
                    {
                        publisher.publish(id, nt.format(process));
                        i = 0;
                    }
//                    logger.info("UpLoadStatus:" + nt.format(process));
                }

            }
            out.flush();
            raf.close();
            out.close();
        }
        else
        {
            process = 1;
            nt.setMaximumFractionDigits(2);
            publisher.publish(id, nt.format(process));
            logger.info("UpLoadStatus:" + nt.format(process));
        }

        logger.info("UpLoadStatus:" + nt.format(process));

        if(!dataSocket.isClosed()) {
            dataSocket.close();
        }

        boolean result;
        if(process < 1)
        {
            result = false;
        }
        else
        {
            result = true;
            publisher.publish(id, "完成");
        }

        status = result ? UploadStatus.UploadNewFileSuccess : UploadStatus.UploadNewFileFail;

        logger.info(status.toString());
        return status;
    }

    /*
    断点续传
     */
    private UploadStatus Continue(String serverFileName, File localFile, long serverSize) throws IOException {
        UploadStatus status;

        float step = localFile.length();
        float process = 0;
        float localRead = 0;


        logger.info("UploadFromBreakStart:" + localFile.getPath());

        dataSocket = controlSocket.execute("APPE " + serverFileName, 150);

        RandomAccessFile raf = new RandomAccessFile(localFile, "r");
        BufferedOutputStream out = new BufferedOutputStream(dataSocket.getDataSocket().getOutputStream());

        if(serverSize > 0)
        {
            process = serverSize / step;
            raf.seek(serverSize);
            localRead = serverSize;
        }

        if(serverSize == step)
        {
            return UploadStatus.UploadFromBreakSuccess;
        }

        int i = 0;//控制刷新的频率
        nt.setMaximumFractionDigits(2);

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = raf.read(buffer)) != -1)
        {
            out.write(buffer, 0, bytesRead);
            localRead += bytesRead;
            if(localRead / step != process)
            {
                process = localRead / step;

                i++;

                if(i > 10000)
                {
                    publisher.publish(id, nt.format(process));
                    i = 0;
                }
//                logger.info("UpLoadStatus:" + nt.format(process));
            }
        }
        out.flush();
        raf.close();
        out.close();

        if(!dataSocket.isClosed()) {
            dataSocket.close();
        }

        boolean result;
        if(process < 1)
        {
            result = false;
        }
        else
        {
            result = true;
            publisher.publish(id, "完成");
        }

        status = result ? UploadStatus.UploadFromBreakSuccess : UploadStatus.UploadFromBreakFail;

        logger.info(status.toString());

        return status;
    }

    public static String getSize(long size) {

        //以B为单位
        if (size < 1024) {
            return String.valueOf(size) + "B";
        } else {
            size = size / 1024;
        }

        //以KB为单位
        if (size < 1024) {
            return String.valueOf(size) + "KB";
        } else {
            size = size / 1024;
        }

        if (size < 1024) {
            //以MB为单位
            size = size * 100;
            return String.valueOf((size / 100)) + "."
                    + String.valueOf((size % 100)) + "MB";
        } else {
            //以GB为单位
            size = size * 100 / 1024;
            return String.valueOf((size / 100)) + "."
                    + String.valueOf((size % 100)) + "GB";
        }
    }
}
