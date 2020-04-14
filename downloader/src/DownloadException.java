import edu.whu.cs.ftp.client.FTPPath;

public abstract class DownloadException extends Exception {
    public abstract String getWrongPath();
}

class LocalDownloadException extends DownloadException {
    protected String localPath;

    public LocalDownloadException(String localPath) {
        this.localPath = localPath;
    }

    public String getWrongPath() {
        return localPath;
    }
}

class RemoteDownloadException extends DownloadException {
    protected FTPPath remotePath;

    public RemoteDownloadException(FTPPath remotePath) {
        this.remotePath = remotePath;
    }

    public String getWrongPath() {
        return remotePath.toString();
    }
}
//
//enum Permission {
//    DIRECTORY,
//    FILE,
//    WRITABLE,
//    READABLE
//}
//
//class PermissionDownloadExpection extends RemoteDownloadException {
//    private Permission expectedPerm;
//
//    public PermissionDownloadExpection(FTPPath remotePath, Permission expectedPerm) {
//        super(remotePath);
//        this.expectedPerm = expectedPerm;
//    }
//
//    @Override
//    public String toString() {
//        String info;
//        String prefix = " is not";
//        String suffix = ".";
//        switch (expectedPerm) {
//            case FILE:
//                info = "a file";
//                break;
//            case DIRECTORY:
//                info = "a directory";
//                break;
//            case WRITABLE:
//                info = "writable";
//                break;
//            case READABLE:
//                info = "readable";
//                break;
//            default:
//                info = "of unknown permission";
//        }
//
//        return remotePath.getPath() + prefix + info + suffix;
//    }
//}

class ServerFileNotExistsException extends RemoteDownloadException {
    public ServerFileNotExistsException(FTPPath remotePath) {
        super(remotePath);
    }
}

class LocalPathOccupiedException extends LocalDownloadException {
    public LocalPathOccupiedException(String localPath) {
        super(localPath);
    }
}

class NoEnoughSpaceException extends LocalDownloadException {
    private long expectedByteNum;
    private long availableByteNum;

    public NoEnoughSpaceException(String localPath, long expectedByteNum, long availableByteNum) {
        super(localPath);
        this.expectedByteNum = expectedByteNum;
        this.availableByteNum = availableByteNum;
    }

    public long getExpectedByteNum() {
        return expectedByteNum;
    }

    public long getAvailableByteNum() {
        return availableByteNum;
    }
}
