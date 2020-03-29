package edu.ftp.client;

import java.io.*;
import java.lang.reflect.Proxy;

/**
 * FTP client implementation for modern FTP servers. Implementations
 * on both RFC 959 and RFC 3659 are required on the server side.
 * Most methods will return {@code null} if any exception occurred.
 */
public class FTPClientImpl implements FTPClient, StreamLogging {
    private String remoteDir = "/";
    private ControlSocket controlSocket;

    /**
     * Connect to FTP server. Invoking this method directly is
     * not permitted, since it may throw {@link IOException}
     *
     * @param addr FTP server address
     * @param port FTP server port
     * @throws IOException exception thrown by Socket connection
     */
    private FTPClientImpl(String addr, int port) throws IOException {
        controlSocket = new ControlSocket(addr, port);
    }

    /**
     * Connect to FTP server. To validate FTPClient, you may
     * check for {@code null} pointer on return value.
     * To retrieve response, see {@link ControlSocket#getStatusCode()}
     * and {@link ControlSocket#getMessage()}.
     *
     * @param addr FTP server address
     * @param port FTP server port
     * @return {@link FTPClientImpl}
     */
    public static FTPClient getFTPClient(String addr, int port) {
        return (FTPClient) Proxy.newProxyInstance(
                FTPClientImpl.class.getClassLoader(),
                FTPClientImpl.class.getInterfaces(),
                new FTPClientHandler(FTPClientImpl.class, addr, port));
    }

    /**
     * Login for ftp client. For anonymous login, try
     * <pre>{@code
     * ftp.login("anonymous", "");
     * }</pre>
     *
     * @param user username for ftp account
     * @param pass password for ftp account
     * @return {@code true} if access granted.
     * @throws IOException thrown if {@link #controlSocket} went wrong.
     */
    @Override
    public Boolean login(String user, String pass) throws IOException {
        controlSocket.execute("USER " + user);
        controlSocket.execute("PASS " + pass);
        return controlSocket.getStatusCode() == 230;
    }

    /**
     * Simply quit and close underlying
     * {@link #controlSocket}. After this operation,
     * {@link #controlSocket} will no longer be valid.
     *
     * @throws IOException .
     */
    @Override
    public void quit() throws IOException {
        controlSocket.execute("QUIT");
        controlSocket.close();
    }

    /**
     * Set mode for underlying {@link DataSocket}.
     *
     * @param mode mode for {@link DataSocket}
     * @see DataSocket.MODE
     */
    @Override
    public void setMode(DataSocket.MODE mode) {
        DataSocket.setMode(mode);
    }

    @Override
    public int getStatusCode() {
        return controlSocket.getStatusCode();
    }

    @Override
    public String getMessage() {
        return controlSocket.getMessage();
    }

    /**
     * Raw response of LIST command. Calling this method
     * directly is strongly discouraged, though some FTP
     * server only support this command, rather than MLSD.
     *
     * @param dir remote directory to list.
     * @return raw response.
     * @throws IOException .
     */
    @Override
    public String[] rawList(String dir) throws IOException {
        DataSocket dataSocket = new DataSocket(controlSocket);
        controlSocket.execute("LIST " + dir, dataSocket);
        return dataSocket.getTextResponse();
    }

    /**
     * Current directory listing with LIST and MLSD.
     *
     * @return Array of {@link FTPPath}, or {@code null}
     * if the server side happen to not support MLSD.
     * @throws IOException .
     * @see FTPPath
     * @see #list(String dir)
     */
    @Override
    public FTPPath[] list() throws IOException {
        return list(remoteDir);
    }

    /**
     * Directory listing with LIST and MLSD. Note that some
     * ancient FTP server may not support MLSD, so good luck.
     *
     * @param dir Remote directory.
     * @return Array of {@link FTPPath}, or {@code null}
     * if the server side happen to not support MLSD.
     * @throws IOException .
     * @see FTPPath
     */
    @Override
    public FTPPath[] list(String dir) throws IOException {
        DataSocket dataSocket = new DataSocket(controlSocket);
        controlSocket.execute("MLSD " + dir, dataSocket);
        if (controlSocket.getStatusCode() != 150) {
            dataSocket.close();
            return null;
        }
        FTPPath[] paths = FTPPath.parseFromMLSD(
                dir, dataSocket.getTextResponse());
        String[] res = rawList(dir);
        for (int i = 0; i < res.length; i++)
            paths[i].addPermission(res[i]);
        return paths;
    }

    /**
     * Change current directory. Syntax like "..",
     * relative path and absolute path are supported as well.
     *
     * @param dir Remote directory.
     * @return {@code false} if directory not found else {@code true}
     * @throws IOException .
     */
    @Override
    public Boolean chdir(String dir) throws IOException {
        controlSocket.execute("CWD " + dir);
        boolean ret = controlSocket.getStatusCode() == 250;
        controlSocket.execute("PWD");
        remoteDir = controlSocket.getMessage().split("\"")[1];
        return ret;
    }

    /**
     * Rename file or directory.
     *
     * @param oldName Current name.
     * @param newName New name.
     * @return {@code false} if directory not found, permission denied,
     * or other shits happened else {@code true}.
     * @throws IOException .
     */
    @Override
    public Boolean rename(String oldName, String newName) throws IOException {
        controlSocket.execute("RNFR " + oldName);
        if (controlSocket.getStatusCode() != 350)
            return false;
        controlSocket.execute("RNTO " + newName);
        return controlSocket.getStatusCode() == 250;
    }

    public static void main(String[] args) throws Exception {
        addLogPublisher(System.out::println);

        FTPClient ftp = FTPClientImpl.getFTPClient("192.168.31.94", 21);
        ftp.login("anonymous", "");
        ftp.rename("a", "abc");
        ftp.chdir("b.txt");
        for (FTPPath f : ftp.list()) {
            System.out.println(f);
        }
        ftp.quit();
        ftp.rename("a", "abc");
        ftp.chdir("b.txt");
        ftp.quit();
    }
}