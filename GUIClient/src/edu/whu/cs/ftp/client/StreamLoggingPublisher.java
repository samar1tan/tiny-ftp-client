package edu.whu.cs.ftp.client;

/**
 * Interface for publishing {@link StreamLogging}
 * @see StreamLogging
 */
@FunctionalInterface
public interface StreamLoggingPublisher {
    void publish(String logRecord);
}
