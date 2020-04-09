package edu.whu.cs.ftp.client;

/**
 * Interface for {@link StreamLoggingHandler}
 * @see StreamLogging
 */
@FunctionalInterface
public interface StreamLoggingPublisher {
    void publish(String logRecord);
}
