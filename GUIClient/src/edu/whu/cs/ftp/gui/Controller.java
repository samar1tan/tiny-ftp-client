package edu.whu.cs.ftp.gui;

import edu.whu.cs.ftp.client.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable, StreamLogging {
    public AnchorPane Pane;                   //面板
    public TextField TextField_IP;            //IP地址输入框
    public TextField TextField_UserName;      //用户名输入框
    public TextField TextFiled_Port;          //端口号输入框
    public Button Button_ConnectOrDisconnect; //连接或退出按钮
    public TextArea TextArea_Log;             //日志打印区
    public TextField TextField_LocalDir;      //本地路径输入框
    public TextField TextField_ServerDir;     //远程路径输入框
    public PasswordField TextField_Password;  //密码输入框
    public ListView ListView_LocalDir;        //本地目录列表
    public ListView ListView_ServerDir;       //远程目录列表
    public Button Button_Upload;              //上传按钮
    public Button Button_Download;            //下载按钮
    public TableView Table_States;            //状态信息打印区
    public MenuButton MenuButton_LocalFileOp;    //本地文件操作菜单按钮
    public MenuButton MenuButton_ServerFileOp;   //远程文件操作菜单按钮
    public String ip_address = null;          //用户输入的IP地址
    public String userName = null;            //用户输入的用户名
    public String password = null;            //用户输入的密码
    public int port = -1;                     //用户输入的端口号
    public String localPath = null;           //用户输入的本地路径
    public String serverPath = null;          //用户输入的远程路径
    public String chosenLocalFile_str = null;     //上传时，用户选择的本地文件或目录
    public String chosenLocalDir_str= null;      //下载时，用户选择的本地目录
    public String chosenServerFile_str = null;    //下载时，用户选择的远程文件或目录
    public String chosenServerDir_str = null;     //上传时，用户选择的远程目录
    public boolean isConnected = false;       //是否连接
    public String newDirName = null;          //新创建的目录名
    public static ObservableList<State> stateData = FXCollections.observableArrayList();   //存放状态信息
    public FTPClient ftp = null;              //ftp客户端
    public FTPPath[] paths = null;            //远程目录信息
    public FTPPath chosenServerFile_path = null;  //下载时，用户选择的远程文件或目录
    public State stateInfo;                           //状态信息

    //打印状态信息
    public void printState(String localFile,String direction,String serverFile,String size,String state){
        stateInfo.setLocalFile(localFile);
        stateInfo.setDirection(direction);
        stateInfo.setServerFile(serverFile);
        stateInfo.setSize(size);
        stateInfo.setState(state);
    }

    //获得IP地址
    public void getIP(){
        ip_address = TextField_IP.getText();
    }
    //获得用户名
    public void getUserName(){
        userName = TextField_UserName.getText();
    }
    //获得密码
    public void getPassword(){
        password = TextField_Password.getText();
    }
    //获得端口号
    public void getPort(){
        try {
            port = Integer.parseInt(TextFiled_Port.getText());
        }catch (Exception e){
            port = -1;
        }
    }
    //点击“连接”按钮
    public void ClickConnectOrDisconnect(){
        if(isConnected == false){              //进行连接操作
            getIP();
            getUserName();
            getPassword();
            getPort();
            try {
                ftp = FTPClientFactory.newMultiThreadFTPClient(ip_address,port,3);
            } catch (Exception e) {
                logger.warning("IP地址或端口号错误");
                return;
            }
            if(userName == null) {
                logger.warning("用户名不能为空");
                ftp = null;
                return;
            }
            if(password == null)
                password = "";
            boolean b = false;
            try {
                b = ftp.login(userName,password);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(b == true){
                if(userName == "anonymous" && password == "")
                    logger.info("匿名连接成功");
                else
                    logger.info("连接成功");
                isConnected = true;
                Button_ConnectOrDisconnect.setText("退出");
            }
            else{
                logger.info("登录失败");
                ftp = null;
            }
        }
        else{                         //进行退出操作
            boolean b = false;
            try {
                b = ftp.quit();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(b == false){
                logger.info("退出失败");
                return;
            }
            isConnected = false;
            Button_ConnectOrDisconnect.setText("连接");
            ftp = null;
            logger.info("已退出");
        }
    }
    //点击“上传”按钮
    public void ClickUpload(){
        if(isConnected == false){
            logger.warning("当前没有连接");
            return;
        }
        if(chosenLocalFile_str == null){
            logger.warning("没有选择本地文件或目录");
            return;
        }
        if(chosenServerDir_str == null){
            logger.warning("没有输入远程目录");
            return;
        }
        //上传、下载按钮变灰
        Button_Upload.setDisable(true);
        Button_Download.setDisable(true);

        stateInfo = new State("","","","","");
        stateData.add(stateInfo);

        File localFile = new File(chosenLocalFile_str);
        Path path = Paths.get(chosenLocalFile_str);
        if(localFile.isFile()){         //上传文件
            //文件上传...
            logger.info(chosenLocalFile_str + " 文件上传成功");
        }
        else{                           //上传目录
            //目录上传...
            logger.info(chosenLocalFile_str + " 目录上传成功");
        }

//        chosenLocalFile_str = null;
        //上传、下载按钮恢复
        Button_Upload.setDisable(false);
        Button_Download.setDisable(false);
        getServerDir(1);
    }
    //点击“下载”按钮
    public void ClickDownload(){
        if(isConnected == false){
            logger.warning("当前没有连接");
            return;
        }
        if(chosenLocalDir_str== null){
            logger.warning("没有输入本地目录");
            return;
        }
        if(chosenServerFile_path== null){
            logger.warning("没有选择远程文件或目录");
            return;
        }
        //上传、下载按钮变灰
        Button_Upload.setDisable(true);
        Button_Download.setDisable(true);

        stateInfo = new State("","","","","");
        stateData.add(stateInfo);

        if(!chosenServerFile_path.isDirectory()){          //下载文件
            //文件下载
            ftp.download(chosenServerFile_str);
            logger.info(chosenServerFile_str + " 文件下载成功");
        }
        else{
            //目录下载
            ftp.download(chosenServerFile_str);
            logger.info(chosenServerFile_str + " 目录下载成功");
        }

//        chosenServerFile_path= null;
//        chosenServerFile_str = null;
        //上传、下载按钮恢复
        Button_Upload.setDisable(false);
        Button_Download.setDisable(false);
        getLocalDir(1);
    }
    //获得本地目录
    public void getLocalDir(int mode){
        if(isConnected == false){
            logger.warning("当前没有连接");
            return;
        }
        List<Label> list = new ArrayList<>();
        ObservableList<Label> myObservableList = FXCollections.observableList(list);
        if(mode == 0)
            localPath = TextField_LocalDir.getText();        //获取输入的本地路径
        File file = new File(localPath);

        if(!file.exists()){                              //判断路径是否合法
            logger.info("本地路径不存在");
            ListView_LocalDir.setItems(myObservableList);
            chosenLocalDir_str= null;
            chosenLocalFile_str = null;
            return;
        }
        if(!file.isAbsolute()){                          //如果localPath是相对路径
            String cwd = System.getProperty("user.dir"); //获取工作目录
            localPath = cwd + File.separator + localPath;    //将localPath转为绝对路径
        }
        ContextMenu contextMenu = new ContextMenu();
        MenuItem localItem1 = new MenuItem("重命名");
        localItem1.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                ClickLocalRename();
            }
        });
        MenuItem localItem2 = new MenuItem("删除文件夹");
        localItem2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                ClickLocalRMD();
            }
        });
        MenuItem localItem3 = new MenuItem("删除文件");
        localItem3.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                ClickLocalDELETE();
            }
        });
        contextMenu.getItems().clear();
        contextMenu.getItems().addAll(localItem1,localItem2,localItem3);
        if(file.isFile()){                               //如果是文件
            //chosenLocalFile_str = localPath;
//            list.add(new Label(file.getName(),new ImageView(new Image("file:icon\\file.png"))));  //将文件名加入列表
            Label label = new Label(file.getName(),new ImageView(new Image("file:icon\\file.png")));
            label.setContextMenu(contextMenu);
            list.add(label);  //将文件名加入列表
            ListView_LocalDir.setItems(myObservableList);
            chosenLocalDir_str = null;
        }
        else{                                            //如果是目录
            chosenLocalDir_str= localPath;                  //设置本地目录
            File[] files = file.listFiles();             //获取目录下所有文件信息
            for(int i = 0; i < files.length; i++){       //将所有文件名加入列表
                if(files[i].isFile()) {
//                    list.add(new Label(files[i].getName(), new ImageView(new Image("file:icon\\file.png"))));
                    Label label = new Label(files[i].getName(), new ImageView(new Image("file:icon\\file.png")));
                    label.setContextMenu(contextMenu);
                    list.add(label);
                }
                else {
//                    list.add(new Label(files[i].getName(),new ImageView(new Image("file:icon\\dir.png"))));
                    Label label = new Label(files[i].getName(),new ImageView(new Image("file:icon\\dir.png")));
                    label.setContextMenu(contextMenu);
                    list.add(label);
                }
            }

            ListView_LocalDir.setItems(myObservableList);
        }
        chosenLocalFile_str = null;
    }
    //获得远程目录
    public void getServerDir(int mode){
        if(isConnected == false){                     //判断是否已连接
            logger.warning("当前没有连接");
            return;
        }
        List<Label> list = new ArrayList<>();
        ObservableList<Label> myObservableList = FXCollections.observableList(list);
        if (mode == 0) {
            serverPath = TextField_ServerDir.getText();    //获取输入的远程路径
            boolean b = false;
            try {
                b = ftp.changeWorkingDirectory(serverPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (b == false) {
                logger.info("远程目录获取失败");
                ListView_ServerDir.setItems(myObservableList);
                chosenServerFile_path = null;
                chosenServerFile_str = null;
                chosenServerDir_str = null;
                return;
            }
        }
        try {
            paths = ftp.list();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            chosenServerDir_str = ftp.getWorkingDirectory();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ContextMenu contextMenu = new ContextMenu();
        MenuItem serverItem1 = new MenuItem("重命名");
        serverItem1.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                ClickServerRename();
            }
        });
        MenuItem serverItem2 = new MenuItem("删除文件夹");
        serverItem2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                ClickServerRMD();
            }
        });
        MenuItem serverItem3 = new MenuItem("删除文件");
        serverItem3.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                ClickServerDELETE();
            }
        });
        contextMenu.getItems().clear();
        contextMenu.getItems().addAll(serverItem1,serverItem2,serverItem3);
        for(int i=0;i<paths.length;i++){
            if(paths[i].isDirectory()) {
//                list.add(new Label(paths[i].getName(), new ImageView(new Image("file:icon\\dir.png"))));
                Label label = new Label(paths[i].getName(), new ImageView(new Image("file:icon\\dir.png")));
                label.setContextMenu(contextMenu);
                list.add(label);
            }
            else {
//                list.add(new Label(paths[i].getName(), new ImageView(new Image("file:icon\\file.png"))));
                Label label = new Label(paths[i].getName(), new ImageView(new Image("file:icon\\file.png")));
                label.setContextMenu(contextMenu);
                list.add(label);
            }
        }
        ListView_ServerDir.setItems(myObservableList);

        chosenServerFile_path = null;
        chosenServerFile_str = null;
    }
    //点击远程“MKD”按钮
    public void ClickServerMKD(){
        if(isConnected == false){
            logger.warning("当前没有连接");
            return;
        }
        if(chosenServerDir_str== null){          //没有输入远程目录
            logger.warning("没有输入远程目录");
            return;
        }
        DialogWindow dw = new DialogWindow();      //创建对话框
        try {
            dw.display("Input Name");
        } catch (Exception e) {
            e.printStackTrace();
        }
        newDirName = DialogController.newDirName;     //获取用户输入的目录名
        if(DialogWindow.ans){                          //用户点击确定
            if(newDirName == null){                   //用户没有输入目录名
                logger.warning("目录名不能为空");
                return;
            }
            String c = (chosenServerDir_str.charAt(chosenServerDir_str.length()-1) == '/')? "":"/";
            boolean b = false;
            try {
                b = ftp.makeDirectory(chosenServerDir_str + c + newDirName);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(b == false)
                logger.info(newDirName + "目录创建失败");
            else {
                logger.info(newDirName + "目录创建成功");
                getServerDir(1);
            }
        }
        newDirName = null;
        DialogController.newDirName = null;
    }
    //点击远程“RMD”按钮
    public void ClickServerRMD(){
        if(isConnected == false){
            logger.warning("当前没有连接");
            return;
        }
        if(chosenServerFile_str == null){            //没有选择远程目录
            logger.warning("没有选择要删除的目录");
            return;
        }
        if(!chosenServerFile_path.isDirectory()){                   //选择的是文件
            logger.warning("请选择目录");
            return;
        }
        boolean b = false;
        try {
            b = ftp.removeDirectory(chosenServerFile_str);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(b == false)
            logger.info(chosenServerFile_str + " 目录删除失败");
        else {
            logger.info(chosenServerFile_str + " 目录删除成功");
            getServerDir(1);
        }
    }
    //点击远程“DELETE”按钮
    public void ClickServerDELETE(){
        if(isConnected == false){
            logger.warning("当前没有连接");
            return;
        }
        if(chosenServerFile_str == null){                  //没有选择远程文件
            logger.warning("没有选择要删除的文件");
            return;
        }
        if(chosenServerFile_path.isDirectory()){                      //选择的是目录
            logger.warning("请选择文件");
            return;
        }
        boolean b = false;
        try {
            b = ftp.deleteFile(chosenServerFile_str);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(b == false)
            logger.info(chosenServerFile_str + " 文件删除失败");
        else {
            logger.info(chosenServerFile_str + " 文件删除成功");
            getServerDir(1);
        }
    }
    //点击远程“Rename”按钮
    public void ClickServerRename() {
        if(isConnected == false){
            logger.warning("当前没有连接");
            return;
        }
        if(chosenServerFile_str == null){                  //没有选择远程文件或文件夹
            logger.warning("没有选择要重命名的文件或文件夹");
            return;
        }
        DialogWindow dw = new DialogWindow();      //创建对话框
        DialogController.newDirName = chosenServerFile_path.getName();
        try {
            dw.display("Input Name");
        } catch (Exception e) {
            e.printStackTrace();
        }
        newDirName = DialogController.newDirName;     //获取用户输入的新名称
        if(DialogWindow.ans){                          //用户点击确定
            if(newDirName == null){                   //用户没有输入目录名
                logger.warning("名称不能为空");
                return;
            }
            boolean b = false;
            try {
                b = ftp.rename(chosenServerFile_path.getName(),newDirName);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(b == false)
                logger.info("重命名失败");
            else {
                logger.info("重命名成功");
                getServerDir(1);
            }
        }
        newDirName = null;
        DialogController.newDirName = null;
    }

    //点击远程“Help”按钮
    public void ClickServerHelp() {
        if(isConnected == false){
            logger.warning("当前没有连接");
            return;
        }
        try {
            ftp.help();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //点击远程“getWorkingDirectory”按钮
    public void ClickServerGWD() {
        if(isConnected == false){
            logger.warning("当前没有连接");
            return;
        }
        try {
            logger.info("远端工作目录: " + ftp.getWorkingDirectory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //点击本地“MKD”按钮
    public void ClickLocalMKD() {
        if(isConnected == false){
            logger.warning("当前没有连接");
            return;
        }
        if(chosenLocalDir_str== null){          //没有输入本地目录
            logger.warning("没有输入本地目录");
            return;
        }
        DialogWindow dw = new DialogWindow();      //创建对话框
        try {
            dw.display("Input Name");
        } catch (Exception e) {
            e.printStackTrace();
        }
        newDirName = DialogController.newDirName;     //获取用户输入的目录名
        if(DialogWindow.ans){                          //用户点击确定
            if(newDirName == null){                   //用户没有输入目录名
                logger.warning("目录名不能为空");
                return;
            }
            //在chosenLocalDir_str目录中创建名为newDirName的目录
            String c = (chosenLocalDir_str.charAt(chosenLocalDir_str.length()-1) == File.separatorChar)? "":File.separator;
            File newDir = new File(chosenLocalDir_str + c + newDirName);
            boolean b = newDir.mkdir();
            if(b == false)
                logger.info(newDirName + "目录创建失败");
            else {
                logger.info(newDirName + "目录创建成功");
                getLocalDir(1);
            }
        }
        newDirName = null;
        DialogController.newDirName = null;
    }

    //迭代删除文件夹
    public void deleteDir(String dirPath)
    {
        File file = new File(dirPath);
        if(file.isFile())
        {
            boolean b = file.delete();
            if(b == false)
                logger.warning(file.getPath() + " 文件删除失败");
        }else
        {
            File[] files = file.listFiles();
            if(files == null)
            {
                boolean b = file.delete();
                if(b == false)
                    logger.warning(file.getPath() + " 目录删除失败");
            }else
            {
                for (int i = 0; i < files.length; i++)
                {
                    deleteDir(files[i].getAbsolutePath());
                }
                boolean b = file.delete();
                if(b == false)
                    logger.warning(file.getPath() + " 目录删除失败");
            }
        }
    }

    //点击本地“RMD”按钮
    public void ClickLocalRMD() {
        if(isConnected == false){
            logger.warning("当前没有连接");
            return;
        }
        if(chosenLocalFile_str == null){            //没有选择本地目录
            logger.warning("没有选择要删除的目录");
            return;
        }
        File file = new File(chosenLocalFile_str);
        if(file.isFile()){                   //选择的是文件
            logger.warning("请选择目录");
            return;
        }
        //删除chosenLocalFile_str目录
        deleteDir(chosenLocalFile_str);
        if(!file.exists()) {
            logger.info(chosenLocalFile_str + " 目录删除成功");
            getLocalDir(1);
        }
    }

    //点击本地“DELETE”按钮
    public void ClickLocalDELETE() {
        if(isConnected == false){
            logger.warning("当前没有连接");
            return;
        }
        if(chosenLocalFile_str == null){                  //没有选择本地文件
            logger.warning("没有选择要删除的文件");
            return;
        }
        File file = new File(chosenLocalFile_str);
        if(file.isDirectory()){                      //选择的是目录
            logger.warning("请选择文件");
            return;
        }
        //删除chosenLocalFile_str文件
        File file1 = new File(localPath);
        if(file1.isFile()){
            String parent = file1.getParent();
            boolean b = file.delete();
            if(b == false)
                logger.info(chosenLocalFile_str + " 文件删除失败");
            else {
                logger.info(chosenLocalFile_str + " 文件删除成功");
                TextField_LocalDir.setText(parent);
                getLocalDir(0);
            }
        }
        else {
            boolean b = file.delete();
            if (b == false)
                logger.info(chosenLocalFile_str + " 文件删除失败");
            else {
                logger.info(chosenLocalFile_str + " 文件删除成功");
                getLocalDir(1);
            }
        }
    }

    //点击本地“Rename”按钮
    public void ClickLocalRename() {
        if(isConnected == false){
            logger.warning("当前没有连接");
            return;
        }
        if(chosenLocalFile_str == null){                  //没有选择本地文件或文件夹
            logger.warning("没有选择要重命名的文件或文件夹");
            return;
        }
        File file = new File(chosenLocalFile_str);
        DialogWindow dw = new DialogWindow();      //创建对话框
        DialogController.newDirName = file.getName();
        try {
            dw.display("Input Name");
        } catch (Exception e) {
            e.printStackTrace();
        }
        newDirName = DialogController.newDirName;     //获取用户输入的新名称
        if(DialogWindow.ans){                          //用户点击确定
            if(newDirName == null){                   //用户没有输入目录名
                logger.warning("名称不能为空");
                return;
            }
            File file1 = new File(localPath);
            if(file1.isFile()){
                boolean b = file.renameTo(new File(file.getParent() + File.separator + newDirName));
                if(b == false)
                    logger.info("重命名失败");
                else {
                    logger.info("重命名成功");
                    TextField_LocalDir.setText(file.getParent() + File.separator + newDirName);
                    getLocalDir(0);
                }
            }
            else{
                boolean b = file.renameTo(new File(file.getParent() + File.separator + newDirName));
                if(b == false)
                    logger.info("重命名失败");
                else {
                    logger.info("重命名成功");
                    getLocalDir(1);
                }
            }
        }
        newDirName = null;
        DialogController.newDirName = null;
    }

    //初始化函数
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //重定向日志输出
        StreamLogging.addLogPublisher(new StreamLoggingPublisher() {
            @Override
            public void publish(String logRecord) {
                TextArea_Log.appendText(logRecord + "\n");
            }
        });

        //设置背景图片
        Pane.setBackground(new Background(new BackgroundImage(new Image("file:icon\\background.png"), BackgroundRepeat.REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,
                BackgroundSize.DEFAULT)));

        TextField_LocalDir.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                getLocalDir(0);
            }
        });
        TextField_ServerDir.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                getServerDir(0);
            }
        });

        MenuButton_LocalFileOp.setGraphic(new ImageView(new Image("file:icon\\menu.png")));
        MenuItem localItem1 = new MenuItem("重命名");
        localItem1.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                ClickLocalRename();
            }
        });
        MenuItem localItem2 = new MenuItem("创建文件夹");
        localItem2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                ClickLocalMKD();
            }
        });
        MenuItem localItem3 = new MenuItem("删除文件夹");
        localItem3.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                ClickLocalRMD();
            }
        });
        MenuItem localItem4 = new MenuItem("删除文件");
        localItem4.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                ClickLocalDELETE();
            }
        });
        MenuButton_LocalFileOp.getItems().clear();
        MenuButton_LocalFileOp.getItems().addAll(localItem1,localItem2,localItem3,localItem4);

        MenuButton_ServerFileOp.setGraphic(new ImageView(new Image("file:icon\\menu.png")));
        MenuItem serverItem0 = new MenuItem("Help");
        serverItem0.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                ClickServerHelp();
            }
        });
        MenuItem serverItem1 = new MenuItem("重命名");
        serverItem1.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                ClickServerRename();
            }
        });
        MenuItem serverItem2 = new MenuItem("创建文件夹");
        serverItem2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                ClickServerMKD();
            }
        });
        MenuItem serverItem3 = new MenuItem("删除文件夹");
        serverItem3.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                ClickServerRMD();
            }
        });
        MenuItem serverItem4 = new MenuItem("删除文件");
        serverItem4.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                ClickServerDELETE();
            }
        });
        MenuItem serverItem5 = new MenuItem("获取工作目录");
        serverItem5.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                ClickServerGWD();
            }
        });
        MenuButton_ServerFileOp.getItems().clear();
        MenuButton_ServerFileOp.getItems().addAll(serverItem0,serverItem1,serverItem2,serverItem3,serverItem4,serverItem5);

        //本地目录列表 监听单击item事件
        ListView_LocalDir.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Label>() {

            @Override
            public void changed(ObservableValue<? extends Label> observable, Label oldValue, Label newValue) {
                if(newValue != null){
                    File file = new File(localPath);
                    if(file.isFile())                      //判断是否为文件
                        chosenLocalFile_str = localPath;
                    else{
                        if(localPath.charAt(localPath.length()-1) == File.separatorChar)    //判断路径结尾是否含有分隔符
                            chosenLocalFile_str = localPath + newValue.getText();
                        else
                            chosenLocalFile_str = localPath + File.separator + newValue.getText();
                    }
                    System.out.println(chosenLocalFile_str);
                }
            }
        });

        //远端目录列表 监听单击item事件
        ListView_ServerDir.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                if(newValue != null && newValue.intValue() != -1){
                    chosenServerFile_path = paths[newValue.intValue()];
                    chosenServerFile_str = chosenServerFile_path.getPath();
                }
            }
        });

        //设置状态信息表每一列的名称，最小宽度以及绑定Model类的属性
        TableColumn<State,String> localFileColumn = new TableColumn<>("本地路径");
        localFileColumn.setMinWidth(100);
        localFileColumn.setCellValueFactory(new PropertyValueFactory<>("localFile"));
        TableColumn<State,String> directionColumn = new TableColumn<>("方向");
        directionColumn.setMinWidth(20);
        directionColumn.setStyle("-fx-alignment: CENTER;");
        directionColumn.setCellValueFactory(new PropertyValueFactory<>("direction"));
        TableColumn<State,String> serverFileColumn = new TableColumn<>("远程路径");
        serverFileColumn.setMinWidth(100);
        serverFileColumn.setCellValueFactory(new PropertyValueFactory<>("serverFile"));
        TableColumn<State,String> sizeColumn = new TableColumn<>("大小");
        sizeColumn.setMinWidth(20);
        sizeColumn.setStyle("-fx-alignment: CENTER;");
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
        TableColumn<State,String> stateColumn = new TableColumn<>("传输状态");
        stateColumn.setMinWidth(200);
        stateColumn.setStyle("-fx-alignment: CENTER;");
        stateColumn.setCellValueFactory(new PropertyValueFactory<>("state"));

        //状态信息表绑定stateData
        Table_States.setItems(stateData);
        //将定义好的列加入状态信息表
        Table_States.getColumns().addAll(localFileColumn,directionColumn,serverFileColumn,sizeColumn,stateColumn);
        //设置状态信息表的占位标志
        Table_States.setPlaceholder(new Label());
    }
    //状态信息类
    public static class State {
        private SimpleStringProperty localFile;     //本地文件或目录
        private SimpleStringProperty direction;     //方向
        private SimpleStringProperty serverFile;    //远程文件或目录
        private SimpleStringProperty size;          //大小
        private SimpleStringProperty state;         //状态

        public State(String localFile,String direction,String serverFile,String size,String state){
            this.localFile = new SimpleStringProperty(localFile==null?"":localFile);
            this.direction = new SimpleStringProperty(direction==null?"":direction);
            this.serverFile = new SimpleStringProperty(serverFile==null?"":serverFile);
            this.size = new SimpleStringProperty(size==null?"":size);
            this.state = new SimpleStringProperty(state==null?"":state);
        }

        //属性的getter和setter

        public String getLocalFile() {
            return localFile.get();
        }

        public SimpleStringProperty localFileProperty() {
            return localFile;
        }

        public void setLocalFile(String localFile) {
            this.localFile.set(localFile==null?"":localFile);
        }

        public String getDirection() {
            return direction.get();
        }

        public SimpleStringProperty directionProperty() {
            return direction;
        }

        public void setDirection(String direction) {
            this.direction.set(direction==null?"":direction);
        }

        public String getServerFile() {
            return serverFile.get();
        }

        public SimpleStringProperty serverFileProperty() {
            return serverFile;
        }

        public void setServerFile(String serverFile) {
            this.serverFile.set(serverFile==null?"":serverFile);
        }

        public String getSize() {
            return size.get();
        }

        public SimpleStringProperty sizeProperty() {
            return size;
        }

        public void setSize(String size) {
            this.size.set(size==null?"":size);
        }

        public String getState() {
            return state.get();
        }

        public SimpleStringProperty stateProperty() {
            return state;
        }

        public void setState(String state) {
            this.state.set(state==null?"":state);
        }
    }
}