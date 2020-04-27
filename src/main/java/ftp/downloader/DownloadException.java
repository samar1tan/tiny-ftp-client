package ftp.downloader;

import ftp.client.FTPPath;

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
        return remotePath.getPath();
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

    @Override
    public String toString() {
        return String.format("Remote file '%s' doesn't exist.", getWrongPath());
    }
}

class LocalPathOccupiedException extends LocalDownloadException {
    public LocalPathOccupiedException(String localPath) {
        super(localPath);
    }

    @Override
    public String toString() {
        return String.format("Local file/directory '%s' already exists.", getWrongPath());
    }
}

class NoEnoughSpaceException extends LocalDownloadException {
    private final long expectedByteNum;
    private final long availableByteNum;

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

    @Override
    public String toString() {
        return "NoEnoughSpaceException{" +
                "expectedByteNum=" + expectedByteNum +
                ", availableByteNum=" + availableByteNum +
                '}';
    }
}

class FTPCommandFailedException extends DownloadException {
    final private String command;
    private String statusMessage = null;
    private int statusCode = 0;
    private String statusInfo = null;

    public FTPCommandFailedException(String cmd, String arg, String statusMessage) {
        this.command = cmd + ' ' + arg;
        if (statusMessage != null) {
            this.statusMessage = statusMessage;
            this.statusCode = Integer.parseInt(statusMessage.substring(0, 3));
            this.statusInfo = statusMessage.substring(4);
        }
    }

    public String getCommand() {
        return command;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusInfo() {
        return statusInfo;
    }

    @Override
    public String getWrongPath() {
        return null;
    }

    @Override
    public String toString() {
        return "FTPCommandFailedException{" +
                "command='" + command + '\'' +
                ", statusMessage='" + statusMessage + '\'' +
                ", statusCode=" + statusCode +
                ", statusInfo='" + statusInfo + '\'' +
                '}';
    }
}

class SaveDirNotExistsException extends LocalDownloadException {
    public SaveDirNotExistsException(String wrongDirPath) {
        super(wrongDirPath);
    }

    @Override
    public String toString() {
        return String.format("Saving directory %s doesn't exist.", getWrongPath());
    }
}

class ParseStatusMessageFailed extends DownloadException {
    private final String statusMessage;
    private final String regex;
    private final int regexGroupIndex;

    public ParseStatusMessageFailed(String statusMessage, String regex, int regexGroupIndex) {
        this.statusMessage = statusMessage;
        this.regex = regex;
        this.regexGroupIndex = regexGroupIndex;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getRegex() {
        return regex;
    }

    public int getRegexGroupIndex() {
        return regexGroupIndex;
    }

    @Override
    public String toString() {
        return "ParseStatusMessageFailed{" +
                "statusMessage='" + statusMessage + '\'' +
                ", regex='" + regex + '\'' +
                ", regexGroupIndex=" + regexGroupIndex +
                '}';
    }

    @Override
    public String getWrongPath() {
        return null;
    }
}

class CreateSaveDirFailed extends LocalDownloadException {
    public CreateSaveDirFailed(String failDir) {
        super(failDir);
    }

    @Override
    public String toString() {
        return String.format("Saving directory %s can't be created.", getWrongPath());
    }
}