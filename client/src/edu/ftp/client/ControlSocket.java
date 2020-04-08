package edu.ftp.client;

import edu.ftp.client.logging.StreamLogging;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Control Socket for FTP Client.
 */
public class ControlSocket implements StreamLogging {
    private Socket controlSocket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private int statusCode;
    private String message;
    private String remoteAddr;

    private final ScheduledThreadPoolExecutor threadPool =
            (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
    private volatile long keepAliveInterval = 30000;
    private volatile long lastExecution = Calendar.getInstance().getTimeInMillis();

    private DataSocket dataSocket;
    private DataSocket.MODE mode = DataSocket.MODE.PASV;

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
        parseResponse("CONN");
        remoteAddr = addr;
        threadPool.scheduleWithFixedDelay(() -> {
            if (Calendar.getInstance().getTimeInMillis() - lastExecution > keepAliveInterval) {
                logger.info("Sending keep-alive");
                try {
                    execute("NOOP");
                } catch (IOException e) {
                    logger.severe(e.getMessage());
                }
            }
        }, 1, 5, TimeUnit.SECONDS);
    }

    public DataSocket getDataSocket() throws IOException {
        Socket dataSocket;
        if (mode == DataSocket.MODE.PASV) {
            execute("PASV");
            String[] ret = getMessage().split("[(|)]")[1].split(",");
            int p1 = Integer.parseInt(ret[4]);
            int p2 = Integer.parseInt(ret[5]);
            int port = p1 * 256 + p2;
            dataSocket = new Socket(remoteAddr, port);
        } else {
            int port;
            ServerSocket activeSocket;
            if (mode == DataSocket.MODE.PORT_STRICT) {
                port = controlSocket.getLocalPort() + 1;
                activeSocket = new ServerSocket(port);
            } else {
                activeSocket = new ServerSocket(0);
                port = activeSocket.getLocalPort();
            }
            int p1 = port / 256;
            int p2 = port % 256;
            execute(String.format("PORT %s,%d,%d",
                    controlSocket.getLocalAddress().getHostAddress()
                            .replace('.', ','), p1, p2));
            try {
                dataSocket = activeSocket.accept();
            } finally {
                activeSocket.close();
            }
        }
        logger.info(mode + " data socket created");
        return new DataSocket(dataSocket);
    }

    /**
     * Send FTP command and parse response. To get status code
     * and response, try {@link #getStatusCode()} and
     * {@link #getMessage()}.
     *
     * @param command FTP command which doesn't need data socket
     * @throws IOException .
     */
    public void execute(String command) throws IOException {
        execute(command, false);
    }

    /**
     * Send FTP command and parse response. To get status code
     * and response, try {@link #getStatusCode()} and
     * {@link #getMessage()}.
     *
     * @param command    FTP command which needs data socket
     * @param needSocket whether need a {@link DataSocket} for
     *                   data transfer
     * @return {@link DataSocket} for data transfer
     * @throws IOException .
     */
    public synchronized DataSocket execute(String command, boolean needSocket)
            throws IOException {
        waitForDataSocketClosure();
        lastExecution = Calendar.getInstance().getTimeInMillis();
        if (needSocket) {
            dataSocket = getDataSocket();
            new Thread(() -> parseAfterTransfer(command)).start();
        }
        writer.write(command);
        writer.write("\r\n");
        writer.flush();
        parseResponse(command);
        return needSocket ? dataSocket : null;
    }

    private void parseAfterTransfer(String command) {
        while (true) {
            synchronized (this) {
                if (this.dataSocket.isClosed()) {
                    try {
                        logger.info(mode + " data socket closed");
                        parseResponse(command);
                    } catch (IOException e) {
                        logger.severe(e.getMessage());
                    } finally {
                        this.dataSocket = null;
                    }
                    break;
                }
            }
        }
    }

    private void checkDataSocketState() {
        while (true)
            synchronized (this) {
                if (dataSocket == null)
                    break;
                else if (!dataSocket.isClosed())
                    break;
            }
    }

    private void waitForDataSocketClosure() {
        while (true)
            synchronized (this) {
                if (dataSocket == null) break;
            }
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

    /**
     * Set keep alive interval for control socket. Typically, server
     * will disconnect client if the socket remains idle for a period
     * of time. In order to avoid that, control socket will send a
     * dummy packet to server once in a while to stay active.
     *
     * @param keepAliveInterval the client will send a dummy packet
     *                          when control socket remain idle for
     *                          {@code keepAliveInterval}
     */
    public void setKeepAliveInterval(long keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }

    public void setMode(DataSocket.MODE mode) {
        this.mode = mode;
    }

    public int getStatusCode() {
        checkDataSocketState();
        return statusCode;
    }

    public String getMessage() {
        checkDataSocketState();
        return message;
    }

    public void close() throws IOException {
        threadPool.shutdownNow();
        logger.info("Waiting for the death of keep-alive thread");
        while (!threadPool.isTerminated()) ;
        logger.info("Keep-alive thread died gracefully");
        controlSocket.close();
    }
}