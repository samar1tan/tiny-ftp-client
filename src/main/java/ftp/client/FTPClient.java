package ftp.client;

import java.io.IOException;

/**
 * FTP client interface for modern FTP servers. Implementations
 * on both RFC 959 and RFC 3659 are required on the server side.
 */
public interface FTPClient {

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

    @NeedSpareThread
    void downloadFile(String remotePath, String localPath, StatusPublisher publisher) throws Throwable;

    @NeedSpareThread
    void downloadDirectory(String remotePath, String localPath, StatusPublisher publisher) throws Throwable;

    @NeedSpareThread
    void uploadFile(String localPath, String remotePath, StatusPublisher publisher) throws Throwable;

    @NeedSpareThread
    void uploadDirectory(String localPath, String remotePath, StatusPublisher publisher) throws Throwable;
}