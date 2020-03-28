public class DownloadException extends Exception {
    protected FTPPath wrongPath;

    public DownloadException(FTPPath wrongPath) {
        this.wrongPath = wrongPath;
    }

    public FTPPath getWrongPath() {
        return wrongPath;
    }
}

enum Permission {
    DIRECTORY,
    FILE,
    WRITABLE,
    READABLE
}

class PermissionDownloadExpection extends DownloadException {
    private Permission expectedPerm;

    public PermissionDownloadExpection(FTPPath wrongPath, Permission expectedPerm) {
        super(wrongPath);
        this.expectedPerm = expectedPerm;
    }

    @Override
    public String toString() {
        String info;
        String prefix = " is not";
        String suffix = ".";
        switch (expectedPerm) {
            case FILE:
                info = "a file";
                break;
            case DIRECTORY:
                info = "a directory";
                break;
            case WRITABLE:
                info = "writable";
                break;
            case READABLE:
                info = "readable";
                break;
            default:
                info = "of unknown permission";
        }

        return wrongPath.getPath() + prefix + info + suffix;
    }
}
