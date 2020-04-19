package edu.whu.cs.ftp.client;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.Arrays;
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

    private Field remote;
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
        remote = master.getClass().getDeclaredField("remoteDir");
        user.setAccessible(true);
        pass.setAccessible(true);
        remote.setAccessible(true);
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
                if (!ftpConnectionPool.shutThreadPoolNow())
                    return false;
                logger.info("ftp client thread pool shut down");
                if (!master.quit())
                    return false;
                logger.info("master client shut down");
                return true;
            }
        } catch (NoSuchMethodException | IOException e) {
            logger.severe(e.getMessage());
            return false;
        }
        if (method.isAnnotationPresent(NeedSpareThread.class)) {
            try {
                String remoteDir = (String) remote.get(master);
                String username = (String) user.get(master);
                String password = (String) pass.get(master);
                threadPool.execute(() -> {
                    logger.info("Entering thread: " + Arrays.toString(objects));
                    FTPClient ftpClient = null;
                    try {
                        ftpClient = ftpConnectionPool.takeOrGenerate();
                        ftpClient.login(username, password);
                        ftpClient.changeWorkingDirectory(remoteDir);
                        method.invoke(ftpClient, objects);
                    } catch (NullPointerException | InterruptedException e) {
                        logger.warning("Failed to obtain ftp connection");
                    } catch (Exception e) {
                        logger.severe(e.getMessage() != null
                                ? e.getMessage()
                                : e.getCause().getMessage() == null
                                ? e.getCause().toString()
                                : e.getCause().getMessage());
                    } finally {
                        if (ftpClient != null) {
                            if (!ftpConnectionPool.offer(ftpClient)) {
                                logger.warning("Interrupted, quiting connection right away");
                                try {
                                    ftpClient.quit();
                                } catch (IOException ex) {
                                    logger.severe(ex.getMessage());
                                }
                            }
                        }
                    }
                    logger.info("Exiting thread: " + Arrays.toString(objects));
                });
            } catch (NullPointerException | IllegalAccessException e) {
                logger.severe("Master connection failed");
            } catch (RejectedExecutionException e) {
                logger.warning("Rejecting new task: no vacant thread");
            }
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
