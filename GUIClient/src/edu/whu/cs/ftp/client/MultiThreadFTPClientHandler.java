package edu.whu.cs.ftp.client;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.concurrent.*;

/**
 * Handling {@link FTPClient} invocation with multi-threads.
 */
public class MultiThreadFTPClientHandler implements InvocationHandler, StreamLogging {
    private FTPClient master;
    private FTPConnectionPool ftpConnectionPool;
    private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
        Configuration.ExecutorPoolConf.corePoolSize,
        Configuration.ExecutorPoolConf.maxPoolSize,
        Configuration.ExecutorPoolConf.threadKeepAliveTime,
        TimeUnit.MILLISECONDS, new SynchronousQueue<>());

    // login credential source
    private Field user;
    private Field pass;

    public MultiThreadFTPClientHandler(Class<FTPClientImpl> cls, String addr, int port, int poolSize)
        throws ReflectiveOperationException {
        if (poolSize < 2)
            throw new IllegalArgumentException("Pool size should be greater than 1");
        FTPClientBuilder.initialize(cls, addr, port);
        master = FTPClientBuilder.newInstance();
        ftpConnectionPool = new FTPConnectionPool(poolSize - 1);
        // login credential source for other thread
        user = master.getClass().getDeclaredField("username");
        pass = master.getClass().getDeclaredField("password");
        user.setAccessible(true);
        pass.setAccessible(true);
    }

    static class FTPClientBuilder {
        private static String addr;
        private static int port;
        private static Constructor<FTPClientImpl> constructor;

        static void initialize(Class<FTPClientImpl> cls, String addr, int port) throws NoSuchMethodException {
            constructor = cls.getDeclaredConstructor(String.class, int.class);
            constructor.setAccessible(true);
            FTPClientBuilder.addr = addr;
            FTPClientBuilder.port = port;
        }

        static FTPClient newInstance() throws ReflectiveOperationException {
            return constructor.newInstance(addr, port);
        }

    }

    @Override
    public Object invoke(Object o, Method method, Object[] objects) {
        try {
            if (method.equals(FTPClient.class.getMethod("quit"))) {
                logger.info("Start threads termination");
                threadPool.shutdownNow();
                while (!threadPool.isTerminated()) ;
                logger.info("Thread pool shut down");
                ftpConnectionPool.shutThreadPoolNow();
                logger.info("ftp client thread pool shut down");
                master.quit();
                logger.info("master client shut down");
                return null;
            }
        } catch (NoSuchMethodException | IOException e) {
            logger.severe(e.getMessage());
            return null;
        }
        if (method.isAnnotationPresent(NeedSpareThread.class)) {
            threadPool.execute(() -> {
                logger.info("Entering spare thread");
                FTPClient ftpClient = null;
                try {
                    ftpClient = ftpConnectionPool.takeOrGenerate();
                    ftpClient.login((String) user.get(master), (String) pass.get(master));
                    ftpClient.changeWorkingDirectory(master.getWorkingDirectory());
                    method.invoke(ftpClient, objects);
                } catch (NullPointerException ignored) {
                    logger.warning("Failed to obtain ftp connection");
                } catch (IOException | IllegalAccessException e) {
                    logger.severe(e.getMessage());
                } catch (InvocationTargetException e) {
                    logger.severe(e.getCause().getMessage());
                } catch (InterruptedException e) {
                    logger.warning("Time out for obtaining connection");
                } finally {
                    if (ftpClient != null) {
                        try {
                            ftpConnectionPool.put(ftpClient);
                        } catch (InterruptedException e) {
                            logger.severe(e.getMessage());
                        }
                    }
                }
                logger.info("Exiting spare thread");
            });
        } else {
            try {
                return method.invoke(master, objects);
            } catch (ReflectiveOperationException e) {
                logger.severe(e.getCause().getMessage());
            }
        }
        return null;
    }
}
