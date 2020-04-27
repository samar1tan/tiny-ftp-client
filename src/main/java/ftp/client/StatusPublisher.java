package ftp.client;

public interface StatusPublisher {

    enum DIRECTION {
        DOWNLOAD, UPLOAD
    }

    int initialize(String localPath, String remotePath, DIRECTION direction, String size);

    void publish(int id, String status);
}
