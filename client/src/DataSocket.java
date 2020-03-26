import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Socket for FTP Client.
 */
public class DataSocket implements StreamLogging, AutoCloseable {
    private MODE mode;
    private Socket dataSocket;
    private ServerSocket activeSocket;

    /**
     * Mode determine how data socket are created. <P>Originally
     * included in RFC 959: {@link MODE#PORT_STRICT}, {@link MODE#PASV}
     * </P> {@link MODE#PORT} is <b>NOT</b> included in RFC 959.
     * Instead of opening port N + 1 for data socket, {@link MODE#PORT}
     * randomly opens a free port for that. It should work just fine
     * as long as NAT or Firewall not in the way.
     */
    public enum MODE {PASV, PORT_STRICT, PORT}

    /**
     * @param controlSocket A valid {@link ControlSocket} must by provided.
     * @param mode          {@link MODE#PASV} is considered way better than
     *                      {@link MODE#PORT}.
     * @throws IOException Thrown when {@link ControlSocket} failed to send
     *                     or {@link DataSocket} failed to create.
     */
    public DataSocket(ControlSocket controlSocket, MODE mode) throws IOException {
        this.mode = mode;
        if (mode == MODE.PASV) {
            controlSocket.execute("PASV");
            String[] ret = controlSocket.getMessage().split("[(|)]")[1].split(",");
            int p1 = Integer.parseInt(ret[4]);
            int p2 = Integer.parseInt(ret[5]);
            int port = p1 * 256 + p2;
            dataSocket = new Socket(controlSocket.getRemoteAddr(), port);
        } else {
            int port;
            if (mode == MODE.PORT_STRICT) {
                port = controlSocket.getLocalPort() + 1;
                activeSocket = new ServerSocket(port);
            } else {
                activeSocket = new ServerSocket(0);
                port = activeSocket.getLocalPort();
            }
            int p1 = port / 256;
            int p2 = port % 256;
            String addr = controlSocket.getLocalAddr().replace('.', ',');
            controlSocket.execute(String.format("PORT %s,%d,%d", addr, p1, p2));
        }
    }

    /**
     * This method should be called before reading anything
     * out of the data socket. If you are working in {@link MODE#PASV}
     * mode, you may ignore this method since it does nothing for you.
     *
     * @throws IOException thrown when server fails to connect in
     *                     {@link MODE#PORT_STRICT} or {@link MODE#PORT} mode.
     */
    public void waitTillReady() throws IOException {
        if (mode != MODE.PASV) {
            try {
                dataSocket = activeSocket.accept();
            } finally {
                activeSocket.close();
            }
        }
    }

    /**
     * Get text (UTF-8) out of data socket. {@link #dataSocket}
     * will be closed after calling this method. This method
     * should not be used to deal with binary data.
     *
     * @return Text Response.
     * @throws IOException thrown if {@link #dataSocket} failed.
     */
    public String[] getTextResponse() throws IOException {
        waitTillReady();
        List<String> ret = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                dataSocket.getInputStream(), StandardCharsets.UTF_8))) {
            String response;
            while ((response = in.readLine()) != null)
                ret.add(response);
        }
        return ret.toArray(new String[0]);
    }

    public boolean isClosed(){
        return dataSocket.isClosed();
    }

    /**
     * Get underlying TCP Socket.
     *
     * @return underlying Socket
     */
    public Socket getDataSocket() {
        return dataSocket;
    }

    @Override
    public void close() throws IOException {
        dataSocket.close();
    }
}

