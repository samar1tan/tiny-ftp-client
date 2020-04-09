package edu.whu.cs.ftp.client;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Socket for FTP Client.
 */
public class DataSocket implements StreamLogging, AutoCloseable {
    private final Socket dataSocket;

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
     * Constructor not to expose outside the package.
     *
     * @param dataSocket The underlying data socket.
     */
    DataSocket(Socket dataSocket) {
        this.dataSocket = dataSocket;
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
        List<String> ret = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                dataSocket.getInputStream(), StandardCharsets.UTF_8))) {
            String response;
            while ((response = in.readLine()) != null)
                ret.add(response);
        }
        return ret.toArray(new String[0]);
    }

    public boolean isClosed() {
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

