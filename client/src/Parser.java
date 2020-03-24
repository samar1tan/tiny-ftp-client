import java.io.IOException;

/**
 * Parser for response from control socket after transfer completed
 * in data socket. Generally {@link Parser} is ready after
 * {@link ControlSocket#execute} gets invoked. You should apply it
 * right after the closure of {@link DataSocket} or its underlying
 * socket. Or at least apply it before invoking another
 * {@link ControlSocket#execute}, even though that's still strongly
 * discouraged.
 */
public interface Parser {
    /**
     * Apply parser for response from control socket.
     * @throws IOException .
     */
    void apply() throws IOException;
}
