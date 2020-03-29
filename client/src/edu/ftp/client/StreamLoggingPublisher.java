package edu.ftp.client;

/**
 * Interface for {@link StreamLoggingHandler}
 * @see StreamLogging
 */
public interface StreamLoggingPublisher {
    void publish(String logRecord);
}
