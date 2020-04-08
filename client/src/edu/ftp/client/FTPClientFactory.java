package edu.ftp.client;

import java.lang.reflect.Proxy;

public class FTPClientFactory {
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
    public static FTPClient newFTPClient(String addr, int port)
            throws ReflectiveOperationException {
        return (FTPClient) Proxy.newProxyInstance(
                FTPClientImpl.class.getClassLoader(),
                FTPClientImpl.class.getInterfaces(),
                new SimpleFTPClientHandler(FTPClientImpl.class, addr, port));
    }

    public static FTPClient newMultiThreadFTPClient(String addr, int port)
            throws ReflectiveOperationException {
        return newMultiThreadFTPClient(addr, port, 3);
    }

    public static FTPClient newMultiThreadFTPClient(String addr, int port, int poolSize)
            throws ReflectiveOperationException {
        return (FTPClient) Proxy.newProxyInstance(
                FTPClientImpl.class.getClassLoader(),
                FTPClientImpl.class.getInterfaces(),
                new MultiThreadFTPClientHandler(FTPClientImpl.class, addr, port, poolSize));
    }
}
