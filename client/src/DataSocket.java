package com.ftp.client;

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
    private static MODE mode = MODE.PASV;
    private Socket dataSocket;

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
     * @throws IOException Thrown when {@link ControlSocket} failed to send
     *                     or {@link DataSocket} failed to create.
     */
    public DataSocket(ControlSocket controlSocket) throws IOException {
        if (mode == MODE.PASV) {
            controlSocket.execute("PASV");
            String[] ret = controlSocket.getMessage().split("[(|)]")[1].split(",");
            int p1 = Integer.parseInt(ret[4]);
            int p2 = Integer.parseInt(ret[5]);
            int port = p1 * 256 + p2;
            dataSocket = new Socket(controlSocket.getRemoteAddr(), port);
        } else {
            int port;
            ServerSocket activeSocket;
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
            try {
                dataSocket = activeSocket.accept();
            } finally {
                activeSocket.close();
            }
        }
    }

    /**
     * @param mode {@link MODE#PASV} is considered way better than
     *             {@link MODE#PORT}.
     */
    public static void setMode(MODE mode) {
        DataSocket.mode = mode;
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

