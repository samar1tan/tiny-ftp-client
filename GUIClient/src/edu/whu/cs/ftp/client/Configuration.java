package edu.whu.cs.ftp.client;

import edu.whu.cs.ftp.client.DataSocket.MODE;

public class Configuration {
    public static class ExecutorPoolConf{
        public static volatile int corePoolSize = 0;
        public static volatile int maxPoolSize = 15;
        public static volatile long threadKeepAliveTime = 30000;//30s
    }

    public static class FTPConnectionPoolConf {
        public static volatile int defaultPoolSize = 10;
        public static volatile long directPollTimeOut = 5000;//1s
        public static volatile long pendingPollTimeOut = 30000;//30s
        public static volatile long shrinkInterval = 20000;//20s
    }

    public static class ControlSocketConf {
        /**
         * Set keep alive interval for control socket. Typically, server
         * will disconnect client if the socket remains idle for a period
         * of time. In order to avoid that, control socket will send a
         * dummy packet to server once in a while to stay active.
         */
        public static volatile long sendKeepAliveInterval = 30000;//30s
        /**
         * check interval for whether send keep-alive or not.
         * <p><b>NOTE: </b>MUST be set before initializing
         * {@link FTPClient} from {@link FTPClientFactory}</p>
         */
        public static volatile long checkKeepAliveInterval = 30000;//30s
        public static volatile int serverSocketTimeOut = 5;//5s
    }

    public static class DataSocketConf {
        public static volatile MODE mode = MODE.PASV;
    }

}
