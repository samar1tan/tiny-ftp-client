package edu.ftp.client;

import edu.ftp.client.logging.StreamLogging;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.*;

/**
 * Handling {@link FTPClient} invocation with multi-threads.
 */
public class MultiThreadFTPClientHandler implements InvocationHandler, StreamLogging {
    private FTPClient master;
    private FTPClientPool ftpClientPool;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    public MultiThreadFTPClientHandler(Class<FTPClientImpl> cls, String addr, int port, int poolSize)
            throws ReflectiveOperationException {
        if (poolSize < 2)
            throw new IllegalArgumentException("Pool size should be greater than 1");
        FTPClientBuilder.initialize(cls, addr, port);
        master = FTPClientBuilder.newInstance();
        ftpClientPool = new FTPClientPool(poolSize - 1);
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
                ftpClientPool.shutThreadPoolNow();
                logger.info("ftp client thread shut down");
                master.quit();
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
                    ftpClient = ftpClientPool.takeOrGenerate();
                    ftpClient.changeWorkingDirectory(master.getWorkingDirectory());
                    method.invoke(ftpClient, objects);
                } catch (NullPointerException ignored) {
                    logger.warning("Failed to obtain ftp client");
                } catch (IOException | InterruptedException e) {
                    logger.severe(e.getMessage());
                } catch (ReflectiveOperationException e) {
                    logger.severe(e.getCause().getMessage());
                } finally {
                    if (ftpClient != null) {
                        try {
                            ftpClientPool.put(ftpClient);
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
