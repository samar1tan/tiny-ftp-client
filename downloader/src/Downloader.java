import java.io.FileOutputStream;
import java.io.IOException;

public class Downloader {
    private ControlSocket controlSocket;
    private DataSocket dataSocket;
    private DownloadingStates downloadingStates;

    public Downloader(ControlSocket controlSocket, DataSocket dataSocket) {
        this.controlSocket = controlSocket;
        this.dataSocket = dataSocket;
        this.downloadingStates = new DownloadingStates();
    }

    public static void main(String[] args) {
        System.out.println("This is the downloading module of FTP client");
    }

    // RETR+REST
    public void downloadFile(FTPPath serverPath, FTPPath savePath) throws DownloadException, IOException {
        // TODO IBM
        checkServerFilePermission(serverPath, Permission.FILE, Permission.READABLE);
        FileOutputStream temp_file;
        if (downloadingStates.isExist(savePath)) {
            temp_file = new FileOutputStream(savePath.getPath(), true);
            setServerFileStartByte(downloadingStates.getDownloadingState(savePath).bytePointer);
        } else {
            temp_file = new FileOutputStream(savePath.getPath());
        }
    }

    public void downloadDirectory(FTPPath serverPath, FTPPath savePath) throws DownloadException {
    }

    public DownloadingState getDownloadingState(FTPPath savePath) {
        return downloadingStates.getDownloadingState(savePath);
    }

    private void checkServerFilePermission(FTPPath path, Permission... expectedPerms) throws PermissionDownloadExpection {
        for (Permission perm : expectedPerms) {
            switch (perm) {
                case DIRECTORY:
                    if (!path.isDirectory()) {
                        throw new PermissionDownloadExpection(path, perm);
                    }
                    break;
                case FILE:
                    if (path.isDirectory()) {
                        throw new PermissionDownloadExpection(path, perm);
                    }
                    break;
                case READABLE:
                    if (!path.isReadable()) {
                        throw new PermissionDownloadExpection(path, perm);
                    }
                    break;
                case WRITABLE:
                    if (!path.isWritable()) {
                        throw new PermissionDownloadExpection(path, perm);
                    }
            }
        }
    }

    private void setServerFileStartByte(int bytePointer) {

    }
}

