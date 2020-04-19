package edu.whu.cs.ftp.uploader;

enum UploadStatus {
    CreateDirectoryFail, //远程服务器相应目录创建失败
    UploadNewFileSuccess, //上传新文件成功
    UploadNewFileFail, //上传新文件失败
    FileExits, //文件已经存在
    ServerBiggerLocal, //远程文件大于本地文件
    UploadFromBreakSuccess, //断点续传成功
    UploadFromBreakFail, //断点续传失败
    DeleteServerFail, //删除远程文件失败
    UpLoadDirectoryFinish, //文件夹传送完成
    SeverPathWrong;//远程路径错误
}