import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Control Socket for FTP Client.
 */
public class ControlSocket implements StreamLogging, AutoCloseable {
    private Socket controlSocket;
    private String remoteAddr;
    private BufferedReader reader;
    private BufferedWriter writer;
    private int statusCode;
    private String message;

    /**
     * Connect to control port of FTP server. Note that {@link #reader}
     * and {@link #writer} are initialized as well.
     *
     * @param addr FTP server ip address.
     * @param port FTP server control port.
     * @throws IOException .
     */
    public ControlSocket(String addr, int port) throws IOException {
        controlSocket = new Socket(addr, port);
        reader = new BufferedReader(new InputStreamReader(
                controlSocket.getInputStream(), StandardCharsets.UTF_8));
        writer = new BufferedWriter(new OutputStreamWriter(
                controlSocket.getOutputStream(), StandardCharsets.UTF_8));
        remoteAddr = controlSocket.
                getRemoteSocketAddress().toString().split("[/|:]")[1];
        parseResponse("CONN");
    }

    /**
     * Send FTP command and parse response. To get status code
     * and response, try {@link #getStatusCode()} and
     * {@link #getMessage()}.
     *
     * @param command FTP command
     * @throws IOException .
     */
    public void execute(String command) throws IOException {
        execute(command, null);
    }

    public void execute(String command, DataSocket dataSocket) throws IOException {
        writer.write(command);
        writer.write("\r\n");
        writer.flush();
        parseResponse(command);
        if (dataSocket != null)
            dataSocket.setParser(() -> parseResponse(command));
    }

    /**
     * Parse response far control socket.
     *
     * @param command FTP command.
     * @throws IOException .
     */
    private void parseResponse(String command) throws IOException {
        StringBuilder messageBuilder = new StringBuilder();
        String ret = reader.readLine();
        logger.info(String.format("[%-4s] %s", command.split(" ")[0], ret));
        messageBuilder.append(ret).append('\n');
        statusCode = Integer.parseInt(ret.substring(0, 3));
        if (ret.charAt(3) == '-')
            do {
                ret = reader.readLine();
                logger.info(String.format("[%-4s] %s", command.split(" ")[0], ret));
                messageBuilder.append(ret).append('\n');
            } while (!ret.startsWith(statusCode + " "));
        message = messageBuilder.toString();
    }

    public String getLocalAddr() {
        return controlSocket.getLocalAddress().getHostAddress();
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public int getLocalPort() {
        return controlSocket.getLocalPort();
    }

    public BufferedReader getReader() {
        return reader;
    }

    public BufferedWriter getWriter() {
        return writer;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public void close() throws IOException {
        controlSocket.close();
    }
}