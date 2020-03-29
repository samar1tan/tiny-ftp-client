package edu.ftp.client;

import java.io.*;

/**
 * FTP client interface for modern FTP servers. Implementations on
 *  * both RFC 959 and RFC 3659 are required on the server side.
 */
public interface FTPClient {

    Boolean login(String user, String pass) throws IOException;

    void quit() throws IOException;

    void setMode(DataSocket.MODE mode);

    int getStatusCode();

    String getMessage();

    String[] rawList(String dir) throws IOException;

    FTPPath[] list() throws IOException;

    FTPPath[] list(String dir) throws IOException;

    Boolean chdir(String dir) throws IOException;

    Boolean rename(String oldName, String newName) throws IOException;
}