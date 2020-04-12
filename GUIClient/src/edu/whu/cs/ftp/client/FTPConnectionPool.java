package edu.whu.cs.ftp.client;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FTPConnectionPool extends LinkedBlockingQueue<FTPClient>
        implements StreamLogging {
    private final int capacity;
    private final AtomicInteger count = new AtomicInteger(0);
    private final AtomicInteger initialized = new AtomicInteger(0);
    private final ScheduledThreadPoolExecutor threadPool =
            (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);

    public FTPConnectionPool(int capacity) {
        super(capacity);
        this.capacity = capacity;
        threadPool.scheduleWithFixedDelay(() -> {
                    while (initialized.get() > 1 && 2 * size() > initialized.get()) {
                        try {
                            Objects.requireNonNull(poll(
                                    Configuration.FTPConnectionPoolConf.directPollTimeOut,
                                    TimeUnit.MILLISECONDS)).quit();
                            initialized.getAndDecrement();
                            logger.info(String.format("Shrinking ftp client pool: %d/%d",
                                    initialized.get(), capacity));
                        } catch (NullPointerException ignored) {
                        } catch (InterruptedException e) {
                            logger.warning(e.getMessage());
                            break;
                        } catch (IOException e) {
                            logger.warning(e.getCause().getMessage());
                        }
                    }
                },
                Configuration.FTPConnectionPoolConf.shrinkInterval,
                Configuration.FTPConnectionPoolConf.shrinkInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * Take {@link FTPClient} from {@link FTPConnectionPool} if available,
     * or generate one if not violating capacity restrictions.
     *
     * @return {@link FTPClient} instance or null if anything goes wrong.
     * @throws InterruptedException .
     */
    public FTPClient takeOrGenerate() throws InterruptedException {
        FTPClient result = null;
        int clientCnt = 0;
        synchronized (this) {
            if (size() > 0 && (result = poll(
                    Configuration.FTPConnectionPoolConf.directPollTimeOut,
                    TimeUnit.MILLISECONDS)) != null)
                return result;
        }
        if ((clientCnt = initialized.getAndIncrement()) < capacity) {
            try {
                result = MultiThreadFTPClientHandler.FTPClientBuilder.newInstance();
                logger.info(String.format("Generating new connection: %d/%d", clientCnt + 1, capacity));
            } catch (ReflectiveOperationException e) {
                initialized.getAndDecrement();
                logger.warning(e.getCause().getMessage());
            }
        } else {
            initialized.getAndDecrement();
            result = poll(Configuration.FTPConnectionPoolConf.pendingPollTimeOut, TimeUnit.MILLISECONDS);
        }
        return result;
    }

    public boolean shutThreadPoolNow() {
        threadPool.shutdownNow();
        while (!threadPool.isTerminated()) ;
        boolean successShutDown = true;
        for (FTPClient ftpClient : this) {
            try {
                ftpClient.quit();
            } catch (IOException e) {
                successShutDown = false;
                logger.warning(e.getMessage());
            }
            logger.info(String.format("Killing connection: %d/%d", initialized.decrementAndGet(), capacity));
        }
        return successShutDown;
    }
}
