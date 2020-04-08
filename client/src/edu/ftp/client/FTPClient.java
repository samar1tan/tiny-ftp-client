package edu.ftp.client;

import java.io.IOException;

/**
 * FTP client interface for modern FTP servers. Implementations
 * on both RFC 959 and RFC 3659 are required on the server side.
 */
public interface FTPClient {

    void setMode(DataSocket.MODE mode);

    void setKeepAliveInterval(long mSeconds);

    int getStatusCode();

    String getMessage();

    void help() throws IOException;

    Boolean login(String user, String pass) throws IOException;

    Boolean quit() throws IOException;

    String[] rawList(String dir) throws IOException;

    FTPPath[] list() throws IOException;

    FTPPath[] list(String dir) throws IOException;

    Boolean changeWorkingDirectory(String dir) throws IOException;

    String getWorkingDirectory() throws IOException;

    Boolean rename(String oldName, String newName) throws IOException;

    Boolean deleteFile(String path) throws IOException;

    Boolean removeDirectory(String path) throws IOException;

    Boolean makeDirectory(String path) throws IOException;

    void abort() throws IOException;

    @NeedSpareThread
    void download(String path);
}