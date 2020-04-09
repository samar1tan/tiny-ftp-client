package edu.whu.cs.ftp.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * FTP Client Proxy Class. Invoke methods with try-catch wrappers.
 */
public class SimpleFTPClientHandler implements InvocationHandler, StreamLogging {
    private FTPClient ftpClient;

    public SimpleFTPClientHandler(Class<FTPClientImpl> cls, String addr, int port)
            throws ReflectiveOperationException {
        Constructor<FTPClientImpl> constructor =
                cls.getDeclaredConstructor(String.class, int.class);
        constructor.setAccessible(true);
        ftpClient = constructor.newInstance(addr, port);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] objects) {
        Object ret = null;
        try {
            ret = method.invoke(ftpClient, objects);
        } catch (ReflectiveOperationException e) {
            logger.severe(e.getCause().getMessage());
        }
        return ret;
    }
}
