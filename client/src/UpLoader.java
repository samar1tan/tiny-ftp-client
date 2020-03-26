package UpLoader;
import java.io.*;
import java.net.Socket;

enum UploadStatus {
    CreateDirectoryFail, //远程服务器相应目录创建失败
    CreateDirectorySuccess, //远程服务器闯将目录成功
    UploadNewFileSuccess, //上传新文件成功
    UploadNewFileFail, //上传新文件失败
    FileExits, //文件已经存在
    ServerBiggerLocal, //远程文件大于本地文件
    UploadFromBreakSuccess, //断点续传成功
    UploadFromBreakFail, //断点续传失败
    DeleteServerFail, //删除远程文件失败
    UpLoadDirectoryFinish; //文件夹传送完成
}

public class UpLoader {
    public UpLoader(FTPClient ftpClient) throws IOException {
        this.ftpClient = ftpClient;
    }

    FTPClient ftpClient;
    /*
    上传文件
     */
    public UploadStatus UpLoadFile(Socket socket, String local_path, FTPPath server_path) throws IOException {
        UploadStatus result;
        String serverFileName = server_path.getName();

        //处理远程目录
        if(server_path.isDirectory())
        {
            //服务器目录创建失败
            if(CreateDirectory(server_path, ftpClient) == UploadStatus.CreateDirectoryFail)
            {
                result = UploadStatus.CreateDirectoryFail;
                return result;
            }
        }

        //检查远程是否存在文件
        FTPPath[] files = ftpClient.list(serverFileName);
        if(files.length == 1)
        {
            long serverFileSize = files[0].getSize();
            File f = new File(local_path);
            long localFileSize = f.length();
            if(serverFileSize == localFileSize)
            {
                result = UploadStatus.FileExits;
                return result;
            }
            else if(serverFileSize > localFileSize)
            {
                result = UploadStatus.ServerBiggerLocal;
                return result;
            }
            else
            {
                //移动文件内读取指针，实现断点续传
                result = Continue(serverFileName, f, ftpClient, serverFileSize);

                //断点续传失败，重新上传
                if(result == UploadStatus.UploadFromBreakFail)
                {
                    if(!ftpClient.deleteFile(serverFileName))
                    {
                        result = UploadStatus.DeleteServerFail;
                        return result;
                    }
                    result = Continue(serverFileName, f, ftpClient, 0);
                }
            }
        }
        else
        {
            File f = new File(local_path);
            result = Continue(serverFileName, f, ftpClient, 0);
        }

        return result;
    }


    /*
    上传整个目录
    */
    public UploadStatus UpLoadDirectory(Socket socket, String local_path, FTPPath server_path) throws IOException {
        File fs = new File(local_path);
        if(fs.isFile())
        {
           return UpLoadFile(socket, local_path, server_path);
        }
        else
        {
            server_path = new FTPPath(server_path.getPath(), fs.getName(), 0);
            CreateDirectory(server_path, ftpClient);
            File[] fi = fs.listFiles();
            for(File f : fi)
            {
                UpLoadDirectory(socket, local_path + "/" + f.getName(), server_path);
            }

            return UploadStatus.UpLoadDirectoryFinish;
        }
    }


    /*
    创建远程目录
     */
    public UploadStatus CreateDirectory(FTPPath server_path, FTPClient ftpClient) throws IOException {
        UploadStatus result = UploadStatus.CreateDirectorySuccess;
        if(!server_path.isDirectory())
        {
            result = UploadStatus.CreateDirectoryFail;
            return result;
        }

        String dir = server_path.getPath();

        //远程目录不存在，则进行创建
        if(!ftpClient.chdir(dir)){
            int start = 0;
            int end = 0;
            if(dir.startsWith("/"))
            {
                start = 1;
            }
            else
            {
                start = 0;
            }

            end = dir.indexOf("/", start);

            while(true)
            {
                String subDir = dir.substring(start, end);
                if(!ftpClient.chdir(subDir))
                {
                    if(ftpClient.mkdir(subDir))
                    {
                        ftpClient.chdir(subDir);
                    }
                    else
                    {
                        System.out.println("创建目录失败");
                        return result=UploadStatus.CreateDirectoryFail;
                    }
                }

                start = end + 1;
                end = dir.indexOf("/", start);

                if(end <= start)
                {
                    break;
                }
            }
        }

        return result;
    }

    /*
    断点续传
     */
    public UploadStatus Continue(String serverFile, File localFile, FTPClient ftpClient, long serverSize) throws IOException {
        UploadStatus status;

        long step = localFile.length();
        long process = 0;
        long localRead = 0;

        RandomAccessFile raf = new RandomAccessFile(localFile, "r");
        OutputStream out = null;
//        OutputStream out = ftpClient.appendFileStream();  未实现

        if(serverSize > 0)
        {
//            ftpClient.setRestartOffset(); 未实现
            process = serverSize / step;
            raf.seek(serverSize);
            localRead = serverSize;
        }

        byte[] bytes = new byte[1024];
        int c;
        while ((c = raf.read(bytes)) != -1)
        {
            out.write(bytes, 0, c);
            localRead += c;
            if(localRead / step != process)
            {
                process = localRead / step;
                System.out.println("上传进度:" + process);
            }
        }
        out.flush();
        raf.close();
        out.close();

        boolean result = true;
//        result = ftpClient.completePendingCommand;  未实现
        if(serverSize > 0)
        {
            status = result ? UploadStatus.UploadFromBreakSuccess : UploadStatus.UploadFromBreakFail;
        }
        else
        {
            status = result ? UploadStatus.UploadNewFileSuccess : UploadStatus.UploadNewFileFail;
        }

        return status;
    }
}
