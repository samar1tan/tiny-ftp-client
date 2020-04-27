package ftp.uploader;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.NumberFormat;

import ftp.client.*;

/*封奇志*/
/*UpLoader类:实现上传的相关操作*/
public class UpLoader implements StreamLogging {
    private ControlSocket controlSocket;
    private DataSocket dataSocket;
    private FTPClient ftpClient;
    private int id;
    private StatusPublisher publisher;
    private boolean isAborted;

    NumberFormat nt = NumberFormat.getPercentInstance();

    public UpLoader(FTPClient ftpClient, ControlSocket controlSocket, StatusPublisher publisher) throws IOException, SQLException {
        this.controlSocket = controlSocket;
        this.ftpClient = ftpClient;
        this.publisher = publisher;
        this.isAborted = false;
    }

    /*
    上传文件
     */
    public UpLoadStatus UpLoadFile(Path local_path, FTPPath server_path, String serverFileName) throws IOException, SQLException {

        if (isAborted) {
            return UpLoadStatus.IsAborted;
        }
        UpLoadStatus result;
        File localFile = local_path.toFile();

        logger.info("UploadNewFileStart:" + localFile.getPath() + "-->" + server_path.getPath() + serverFileName);

        //处理远程目录
        if(server_path.isDirectory())
        {
            if(!ftpClient.changeWorkingDirectory(server_path.getPath())) {
                logger.info("Wrong Server Path");
                return UpLoadStatus.SeverPathWrong;
            }
        }

        //检查远程是否存在文件
        FTPPath[] files = ftpClient.list(serverFileName);
        //存在同名文件
        if(files != null)
        {
            result = UpLoadStatus.FileExits;
            logger.info("FileExits");
            return result;
        }

        files = ftpClient.list(serverFileName + ".uploading");
        if(files == null)
        {
            //初始化状态信息
            id = publisher.initialize(localFile.getPath(), server_path.getPath(), StatusPublisher.DIRECTION.UPLOAD, getSize(localFile.length()));
            result = Start(serverFileName + ".uploading", localFile);
            if(result == UpLoadStatus.UploadNewFileSuccess)
            {
                ftpClient.rename(serverFileName + ".uploading", serverFileName);
            }
        }
        else
        {
            long serverFileSize = files[0].getSize();
            long localFileSize = localFile.length();

            //移动文件内读取指针，实现断点续传
            id = publisher.initialize(localFile.getPath(), server_path.getPath(), StatusPublisher.DIRECTION.UPLOAD, getSize(localFile.length()));
            result = Continue(serverFileName + ".uploading", localFile, serverFileSize);


            //断点续传失败，重新上传
            if(result == UpLoadStatus.UploadFromBreakFail)
            {
                logger.info("UploadFromBreakFail,TryAgain");
                if(!ftpClient.deleteFile(serverFileName + ".uploading"))
                {
                    result = UpLoadStatus.DeleteServerFileFail;
                    logger.info("DeleteServerFail");
                    return result;
                }


                id = publisher.initialize(localFile.getPath(), server_path.getPath(), StatusPublisher.DIRECTION.UPLOAD, getSize(localFile.length()));

                result = Start(serverFileName + ".uploading", localFile);
                if(result == UpLoadStatus.UploadNewFileSuccess)
                {
                    ftpClient.rename(serverFileName + ".uploading", serverFileName);
                }

            }
            else
            {
                ftpClient.rename(serverFileName + ".uploading", serverFileName);
            }
        }
        return result;
    }


    /*
    上传整个目录
    */
    public UpLoadStatus UpLoadDirectory(Path local_path, FTPPath server_path, String serverDirectoryName) throws IOException, SQLException {

        if (isAborted) {
            return UpLoadStatus.IsAborted;
        }

        File fs = local_path.toFile();

        logger.info("UpLoadDirectory:" + fs.getPath() + "-->" + server_path.getPath());

        if(fs.isFile())
        {
            return UpLoadFile(local_path, server_path, fs.getName());
        }
        else
        {
            server_path = new FTPPath(server_path.getPath(), serverDirectoryName);
            if(!ftpClient.makeDirectory(server_path.getPath())) {
                logger.info("CreateDirectoryFail");
                return UpLoadStatus.CreateDirectoryFail;
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
            return UpLoadStatus.UpLoadDirectoryFinish;
        }
    }

    /*
    首次上传
     */
    private UpLoadStatus Start(String serverFileName, File localFile) throws IOException {
        UpLoadStatus status;

        float step = localFile.length();
        float process = 0;
        float localRead = 0;
        boolean result;

        dataSocket = controlSocket.execute("STOR " + serverFileName, 150);

        RandomAccessFile raf = new RandomAccessFile(localFile, "r");
        BufferedOutputStream out = new BufferedOutputStream(dataSocket.getDataSocket().getOutputStream());

        byte[] buffer = new byte[1024];
        int bytesRead;

        int i = 0;//控制前端显示刷新的频率
        nt.setMaximumFractionDigits(2);
        if(step != 0)
        {
            while ((bytesRead = raf.read(buffer)) != -1)
            {
                if (Thread.currentThread().isInterrupted()) {
                    isAborted = true;
                    publisher.publish(id, "完成");
                    break;
                }
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

        if(process < 1)
        {
            result = false;
        }
        else
        {
            result = true;
            publisher.publish(id, "完成");
        }

        status = result ? UpLoadStatus.UploadNewFileSuccess : UpLoadStatus.UploadNewFileFail;

        logger.info(status.toString());
        return status;
    }

    /*
    断点续传
     */
    private UpLoadStatus Continue(String serverFileName, File localFile, long serverSize) throws IOException {
        UpLoadStatus status;

        float step = localFile.length();
        float process = 0;
        float localRead = 0;
        boolean result;

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
            return UpLoadStatus.UploadFromBreakSuccess;
        }

        int i = 0;//控制前端显示刷新的频率
        nt.setMaximumFractionDigits(2);

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = raf.read(buffer)) != -1)
        {
            if (Thread.currentThread().isInterrupted()) {
                isAborted = true;
                publisher.publish(id, "完成");
                break;
            }
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

            }
        }
        out.flush();
        raf.close();
        out.close();

        if(!dataSocket.isClosed()) {
            dataSocket.close();
        }


        if(process < 1)
        {
            result = false;
        }
        else
        {
            result = true;
            publisher.publish(id, "完成");
        }

        status = result ? UpLoadStatus.UploadFromBreakSuccess : UpLoadStatus.UploadFromBreakFail;

        logger.info(status.toString());

        return status;
    }

    /*
    将文件大小转换为字符串形式输出
    */
    public static String getSize(long size) {

        //以B为单位
        if (size < 1024) {
            return String.valueOf(size) + " B";
        } else {
            size = size / 1024;
        }

        //以KB为单位
        if (size < 1024) {
            return String.valueOf(size) + " KB";
        } else {
            size = size / 1024;
        }

        if (size < 1024) {
            //以MB为单位
            size = size * 100;
            return String.valueOf((size / 100)) + "."
                    + String.valueOf((size % 100)) + " MB";
        } else {
            //以GB为单位
            size = size * 100 / 1024;
            return String.valueOf((size / 100)) + "."
                    + String.valueOf((size % 100)) + " GB";
        }
    }
}
