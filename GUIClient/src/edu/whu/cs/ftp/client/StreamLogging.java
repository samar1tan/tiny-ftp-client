package edu.whu.cs.ftp.client;

import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.*;

/**
 * Stream Logger for all implemented classes.
 * Using {@link java.util.logging.ConsoleHandler} by default.
 */
public interface StreamLogging {
    Logger logger = Logger.getLogger("FTP");
    DateFormat dateFormatter = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
    Formatter logFormatter = new Formatter() {
        @Override
        public String format(LogRecord record) {
            String[] sourceClass = record.getSourceClassName().split("\\.");
            return String.format("%s [%s] <%s@%d> %s",
                    dateFormatter.format(new Date(record.getMillis())),
                    record.getLevel(),
                    sourceClass[sourceClass.length - 1],
                    record.getThreadID(),
                    record.getMessage());
        }
    };

    /**
     * Add publisher for {@link StreamLogging} interface.
     * If you simply want to log in console with prefix, try
     * <pre><code>
     * StreamLogging.addLogPublisher((String record)-{@literal >}{
     *      System.out.println("MyLog: " + record);
     * });
     * </code></pre>
     * or the old-fashioned way,
     * <pre><code>
     * StreamLogging.addLogPublisher(new StreamLoggingPublisher(){
     *      {@literal @}Override
     *      void publish(String logRecord){
     *          System.out.println("MyLog: " + record);
     *      }
     * });
     * </code></pre>
     *
     * @param streamLoggingPublisher Publisher for logs.
     * @see StreamLoggingPublisher
     */
    static void addLogPublisher(StreamLoggingPublisher streamLoggingPublisher) {
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord logRecord) {
                streamLoggingPublisher.publish(logFormatter.format(logRecord));
            }

            @Override
            public void flush() {}

            @Override
            public void close() throws SecurityException {}
        });
    }

    /**
     * Add {@link Handler} implementations for interface.
     *
     * @param stream .
     */
    static void addLogStream(OutputStream stream) {
        logger.setUseParentHandlers(false);
        logger.addHandler(new StreamHandler(stream, logFormatter));
    }
}

