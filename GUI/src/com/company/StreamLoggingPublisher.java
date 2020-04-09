package com.company;

/**
 * Interface for {@link StreamLoggingHandler}
 * @see StreamLogging
 */
@FunctionalInterface
public interface StreamLoggingPublisher {
    void publish(String logRecord);
}
