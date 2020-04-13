package UpLoader;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;

import UpLoader.Controller.State;

public class UpLoader implements StreamLogging{
    private ControlSocket controlSocket;
    private DataSocket dataSocket;
    private FTPClient ftpClient;


    public UpLoader(FTPClient ftpClient, ControlSocket controlSocket) throws IOException {
        this.controlSocket = controlSocket;
        this.ftpClient = ftpClient;
    }

    NumberFormat nt = NumberFormat.getPercentInstance();

    /*
    上传文件
     */
    public UploadStatus UpLoadFile(Path local_path, FTPPath server_path) throws IOException {
        UploadStatus result;
        File serverFile = local_path.toFile();
        String serverFileName = serverFile.getName();

        //处理远程目录
        if(server_path.isDirectory())
        {
            //服务器目录创建失败
//            if(CreateDirectory(server_path, ftpClient) == UploadStatus.CreateDirectoryFail)
//            {
//                result = UploadStatus.CreateDirectoryFail;
//                logger.info("CreateDirectoryFail");
//                return result;
//            }
            ftpClient.changeWorkingDirectory(server_path.getPath());
        }

        //检查远程是否存在文件
        FTPPath[] files = ftpClient.list(serverFileName);
        if(files == null)
        {
            result = Start(serverFileName, serverFile, ftpClient);
        }
        else
        {
            long serverFileSize = files[0].getSize();
            long localFileSize = serverFile.length();
            if(serverFileSize == localFileSize)
            {
                result = UploadStatus.FileExits;
                logger.info("FileExits");
                return result;
            }
            else if(serverFileSize > localFileSize)
            {
                result = UploadStatus.ServerBiggerLocal;
                logger.info("ServerBiggerLocal");
                return result;
            }
            else
            {
                //移动文件内读取指针，实现断点续传
                result = Continue(serverFileName, serverFile, ftpClient, serverFileSize);

                //断点续传失败，重新上传
                if(result == UploadStatus.UploadFromBreakFail)
                {
                    if(!ftpClient.deleteFile(serverFileName))
                    {
                        result = UploadStatus.DeleteServerFail;
                        logger.info("DeleteServerFail");
                        return result;
                    }
                    result = Start(serverFileName, serverFile, ftpClient);
                }
            }

        }

        return result;
    }


    /*
    上传整个目录
    */
    public UploadStatus UpLoadDirectory(Path local_path, FTPPath server_path) throws IOException {
        File fs = local_path.toFile();

        if(fs.isFile())
        {
           return UpLoadFile(local_path, server_path);
        }
        else
        {
            server_path = new FTPPath(server_path.getPath(), fs.getName());
            if(!ftpClient.makeDirectory(server_path.getPath())) {
                return UploadStatus.CreateDirectoryFail;
            }
            File[] fi = fs.listFiles();
            for(File f : fi)
            {
                UpLoadDirectory(Paths.get(local_path.toString(), "/" , f.getName()), server_path);
            }

            logger.info("UpLoadDirectoryFinish");
            return UploadStatus.UpLoadDirectoryFinish;
        }
    }

//    /*
//    创建远程目录
//     */
//    private UploadStatus CreateDirectory(FTPPath server_path, FTPClient ftpClient) throws IOException {
//        UploadStatus result = UploadStatus.CreateDirectorySuccess;
//        if(!server_path.isDirectory())
//        {
//            result = UploadStatus.CreateDirectoryFail;
//            logger.info("CreateDirectoryFail");
//            return result;
//        }
//
//        String dir = server_path.getPath();
//
//        //远程目录不存在，则进行创建
//        if(!ftpClient.changeWorkingDirectory(dir)){
//            int start = 0;
//            int end = 0;
//            if(dir.startsWith("/"))
//            {
//                start = 1;
//            }
//            else
//            {
//                start = 0;
//            }
//
//            end = dir.indexOf("/", start);
//
//            while(true)
//            {
//                String subDir = dir.substring(start, end);
//                if(!ftpClient.changeWorkingDirectory(subDir))
//                {
//                    if(ftpClient.makeDirectory(subDir))
//                    {
//                        ftpClient.changeWorkingDirectory(subDir);
//                    }
//                    else
//                    {
//                        System.out.println("创建目录失败");
//                        logger.info("CreateDirectoryFail");
//                        return result=UploadStatus.CreateDirectoryFail;
//                    }
//                }
//
//                start = end + 1;
//                end = dir.indexOf("/", start);
//
//                if(end <= start)
//                {
//                    break;
//                }
//            }
//        }
//
//        return result;
//    }

    /*
    断点续传
     */
    private UploadStatus Continue(String serverFile, File localFile, FTPClient ftpClient, long serverSize) throws IOException {
        UploadStatus status;

        long step = localFile.length();
        float process = 0;
        float localRead = 0;



        dataSocket = controlSocket.execute("APPE " + localFile.getName(), 150);

        RandomAccessFile raf = new RandomAccessFile(localFile, "r");
        BufferedOutputStream out = new BufferedOutputStream(dataSocket.getDataSocket().getOutputStream());

        if(serverSize > 0)
        {
            process = serverSize / step;
            raf.seek(serverSize);
            localRead = serverSize;
        }

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = raf.read(buffer)) != -1)
        {
            out.write(buffer, 0, bytesRead);
            localRead += bytesRead;
            if(localRead / step != process)
            {
                process = localRead / step;
                new State(localFile.getName(), "==>", serverFile, String.valueOf(serverSize), "1", String.valueOf(process*100) + "%");
                nt.setMaximumFractionDigits(2);
                logger.info("上传进度:" + nt.format(process));
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
        }


        status = result ? UploadStatus.UploadFromBreakSuccess : UploadStatus.UploadFromBreakFail;

        logger.info(status.toString());
        return status;
    }


    /*
    首次上传
     */
    private UploadStatus Start(String serverFile, File localFile, FTPClient ftpClient) throws IOException {
        UploadStatus status;

        long step = localFile.length();
        float process = 0;
        float localRead = 0;

        dataSocket = controlSocket.execute("STOR " + localFile.getName(), 150);

        RandomAccessFile raf = new RandomAccessFile(localFile, "r");
        BufferedOutputStream out = new BufferedOutputStream(dataSocket.getDataSocket().getOutputStream());

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = raf.read(buffer)) != -1)
        {
            out.write(buffer, 0, bytesRead);
            localRead += bytesRead;
            if(localRead / step != process)
            {
                process = localRead / step;
                new State(localFile.getName(), "==>", serverFile, String.valueOf(localFile.length()), "normal", String.valueOf(process*100) + '%');
                nt.setMaximumFractionDigits(2);
                logger.info("上传进度:" + nt.format(process));
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
        }

        status = result ? UploadStatus.UploadNewFileSuccess : UploadStatus.UploadNewFileFail;

        logger.info(status.toString());
        return status;
    }
}
