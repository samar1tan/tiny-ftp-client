import java.io.OutputStream;
import java.util.logging.Formatter;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

/**
 * Stream Logger for all implemented classes.
 * Using {@link java.util.logging.ConsoleHandler} by default.
 */
public interface StreamLogging {
    Logger logger = Logger.getLogger("FTP");

    /**
     * Add handler for StreamLogging interface.
     *
     * @param outStream output stream for logger
     * @param formatter formatter for log handler,
     *                  using {@link SimpleFormatter}
     *                  if {@code null} is given.
     */
    static void addLogHandler(OutputStream outStream, Formatter formatter) {
        logger.addHandler(new StreamHandler(outStream, formatter == null ?
                new SimpleFormatter() : formatter));
    }
}
