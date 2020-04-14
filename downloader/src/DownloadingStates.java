import edu.whu.cs.ftp.client.FTPPath;

// FIXME: zfk
// logging database, key=path
public class DownloadingStates {
    public DownloadingStates() {
    }

    public boolean isExist(FTPPath path) {
        return false;
    }

    public DownloadingState getDownloadingState(FTPPath path) {
        return new DownloadingState();
    }
}

// entries in DownloadingStates
// 下载完成比例、已消耗时间、剩余时间、当前速度...
class DownloadingState {
    public int bytePointer = 0;

}
