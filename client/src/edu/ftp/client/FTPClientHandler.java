package edu.ftp.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * FTP Client Proxy Class. Invoke methods with try-catch wrappers.
 */
public class FTPClientHandler implements InvocationHandler, StreamLogging {
    private FTPClient ftpClient;

    public FTPClientHandler(Class<FTPClientImpl> cls, String addr, int port) {
        try {
            Constructor<FTPClientImpl> constructor =
                    cls.getDeclaredConstructor(String.class, int.class);
            constructor.setAccessible(true);
            ftpClient = constructor.newInstance(addr, port);
        }catch (Exception e) {
            logger.severe(e.getCause().getMessage());
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] objects) {
        logger.entering(FTPClient.class.toString(), method.toString());
        Object ret = null;
        try {
            ret = method.invoke(ftpClient, objects);
        } catch (Exception e) {
            logger.severe(e.getMessage() == null
                    ? "Connection probably failed. Try reconnect!"
                    : e.getMessage());
        }
        logger.exiting(FTPClient.class.toString(), method.toString());
        return ret;
    }
}
