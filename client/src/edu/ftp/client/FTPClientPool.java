package edu.ftp.client;

import edu.ftp.client.logging.StreamLogging;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FTPClientPool extends LinkedBlockingQueue<FTPClient>
        implements StreamLogging {
    private final int capacity;
    private final AtomicInteger initialized = new AtomicInteger(0);
    private final ScheduledThreadPoolExecutor threadPool =
            (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);

    public FTPClientPool(int capacity) {
        super(capacity);
        this.capacity = capacity;
        threadPool.scheduleWithFixedDelay(() -> {
            while (initialized.get() != 0 && 2 * size() > initialized.get()) {
                try {
                    Objects.requireNonNull(poll(1, TimeUnit.SECONDS)).quit();
                    initialized.getAndDecrement();
                    logger.info(String.format("Shrinking ftp client pool: %d/%d", initialized.get(), capacity));
                } catch (InterruptedException | NullPointerException e) {
                    logger.warning(e.getMessage());
                    break;
                } catch (IOException e) {
                    logger.warning(e.getCause().getMessage());
                }
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    /**
     * Take {@link FTPClient} from {@link FTPClientPool} if available,
     * or generate one if not violating capacity restrictions.
     *
     * @return {@link FTPClient} instance or null if anything goes wrong.
     * @throws InterruptedException .
     */
    public FTPClient takeOrGenerate() throws InterruptedException {
        FTPClient result = null;
        if (size() > 0) {
            result = poll(1, TimeUnit.SECONDS);
        } else if (initialized.get() < capacity) {
            initialized.getAndIncrement();
            try {
                result = MultiThreadFTPClientHandler.FTPClientBuilder.newInstance();
                logger.info(String.format("Generating new threads: %d/%d", initialized.get(), capacity));
            } catch (ReflectiveOperationException e) {
                initialized.getAndDecrement();
                logger.warning(e.getCause().getMessage());
            }
        }
        return result;
    }

    public void shutThreadPoolNow() {
        threadPool.shutdownNow();
        while(!threadPool.isTerminated());
        for (FTPClient ftpClient : this) {
            try {
                ftpClient.quit();
            } catch (IOException e) {
                e.printStackTrace();
            }
            logger.info(String.format("Killing thread: %d/%d", initialized.get(), capacity));
        }
    }
}
