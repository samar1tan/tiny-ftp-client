import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    public AnchorPane Pane;                   //面板
    public TextField TextField_IP;            //IP地址输入框
    public TextField TextField_UserName;      //用户名输入框
    public TextField TextFiled_Port;          //端口号输入框
    public TextArea TextArea_Log;             //日志打印区
    public TextField TextField_LocalDir;      //本地路径输入框
    public TextField TextField_ServerDir;     //远程路径输入框
    public PasswordField TextField_Password;  //密码输入框
    public ListView ListView_LocalDir;        //本地目录列表
    public ListView ListView_ServerDir;       //远程目录列表
    public Button Button_Upload;              //上传按钮
    public Button Button_Download;            //下载按钮
    public TableView Table_States;            //状态信息打印区
    public String ip_address = null;          //用户输入的IP地址
    public String userName = null;            //用户输入的用户名
    public String password = null;            //用户输入的密码
    public int port = -1;                     //用户输入的端口号
    public String localPath = null;           //用户输入的本地路径
    public String serverPath = null;          //用户输入的远程路径
    public String chosenLocalFile = null;     //上传时，用户选择的本地文件或目录
    public String chosenLocalDir = null;      //下载时，用户选择的本地目录
    public String chosenServerFile = null;    //下载时，用户选择的远程文件或目录
    public String chosenServerDir = null;     //上传时，用户选择的远程目录
    public boolean isConnected = false;       //是否连接
    public ObservableList<State> stateData = FXCollections.observableArrayList();   //存放状态信息

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
            //TextArea_Log.appendText("Error: 端口号输入不是数字\n");
            port = -1;
        }
    }
    //点击“连接”按钮
    public void ClickConnect(){
        //判断用户名、密码是否正确
        //判断IP地址是否存在
        //判断端口号是否符合范围

        //......
    }
    //点击“上传”按钮
    public void ClickUpload(){
        if(chosenLocalFile == null){
            TextArea_Log.appendText("请选择本地文件或目录\n");
            return;
        }
        if(chosenServerDir == null){
            TextArea_Log.appendText("请选择远程目录\n");
            return;
        }
        //上传、下载按钮变灰
        Button_Upload.setDisable(true);
        Button_Download.setDisable(true);

        File localFile = new File(chosenLocalFile);
        if(localFile.isFile()){         //上传文件
            //文件上传...
        }
        else{                           //上传目录
            //目录上传...
        }

        chosenLocalFile = null;
        //上传、下载按钮恢复
        Button_Upload.setDisable(false);
        Button_Download.setDisable(false);
    }
    //点击“下载”按钮
    public void ClickDownload(){
        if(chosenLocalDir == null){
            TextArea_Log.appendText("请选择本地目录\n");
            return;
        }
        if(chosenServerFile == null){
            TextArea_Log.appendText("请选择远程文件或目录\n");
            return;
        }
        //上传、下载按钮变灰
        Button_Upload.setDisable(true);
        Button_Download.setDisable(true);
        //chosenServerFile是文件
        //文件下载
        //chosenServerFile是目录
        //目录下载

        chosenServerFile = null;
        //上传、下载按钮恢复
        Button_Upload.setDisable(false);
        Button_Download.setDisable(false);
    }
    //获得本地目录
    public void getLocalDir(){
        List<Label> list = new ArrayList<>();
        ObservableList<Label> myObservableList = FXCollections.observableList(list);
        localPath = TextField_LocalDir.getText();        //获取输入的本地路径
        File file = new File(localPath);

        if(!file.exists()){                              //判断路径是否合法
            TextArea_Log.appendText("Error: 本地路径不存在\n");
            ListView_LocalDir.setItems(myObservableList);
            chosenLocalDir = null;
            chosenLocalFile = null;
            return;
        }
        if(!file.isAbsolute()){                          //如果localPath是相对路径
            String cwd = System.getProperty("user.dir"); //获取工作目录
            localPath = cwd + File.separator + localPath;    //将localPath转为绝对路径
        }
        if(file.isFile()){                               //如果是文件
            //chosenLocalFile = localPath;
            list.add(new Label(file.getName(),new ImageView(new Image("file:icon\\file.png"))));  //将文件名加入列表
            ListView_LocalDir.setItems(myObservableList);
        }
        else{                                            //如果是目录
            chosenLocalDir = localPath;                  //设置本地目录
            File[] files = file.listFiles();             //获取目录下所有文件信息
            for(int i = 0; i < files.length; i++){       //将所有文件名加入列表
                if(files[i].isFile())
                    list.add(new Label(files[i].getName(),new ImageView(new Image("file:icon\\file.png"))));
                else
                    list.add(new Label(files[i].getName(),new ImageView(new Image("file:icon\\dir.png"))));
            }

            ListView_LocalDir.setItems(myObservableList);
        }
        chosenLocalFile = null;
    }
    //获得远程目录
    public void getServerDir(){
        if(isConnected == false){                     //判断是否已连接
            TextArea_Log.appendText("请先连接\n");
            return;
        }
        List<Label> list = new ArrayList<>();
        ObservableList<Label> myObservableList = FXCollections.observableList(list);
        serverPath = TextField_ServerDir.getText();    //获取输入的远程路径
    }
    //点击“MKD”按钮
    public void ClickMKD(){

    }
    //点击“RMD”按钮
    public void ClickRMD(){

    }
    //点击“DELETE”按钮
    public void ClickDELETE(){

    }

    //初始化函数
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //设置背景图片
        Pane.setBackground(new Background(new BackgroundImage(new Image("file:icon\\background.png"), BackgroundRepeat.REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,
                BackgroundSize.DEFAULT)));

        //本地目录列表 监听单击item事件
        ListView_LocalDir.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Label>() {

            @Override
            public void changed(ObservableValue<? extends Label> observable, Label oldValue, Label newValue) {
                if(newValue != null){
                    File file = new File(localPath);
                    if(file.isFile())                      //判断是否为文件
                        chosenLocalFile = localPath;
                    else{
                        if(localPath.charAt(localPath.length()-1) == File.separatorChar)    //判断路径结尾是否含有分隔符
                            chosenLocalFile = localPath + newValue.getText();
                        else
                            chosenLocalFile = localPath + File.separator + newValue.getText();
                    }
                }
            }
        });

        //远端目录列表 监听单击item事件
        ListView_ServerDir.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Label>() {

            @Override
            public void changed(ObservableValue<? extends Label> observable, Label oldValue, Label newValue) {
                if(newValue != null){
                    //serverPath是文件
                    chosenServerFile = serverPath;
                    //serverPath是目录

                    //获取远程系统的路径分隔符
                    char seperator;
                    if(serverPath.contains("\\"))
                        seperator = '\\';
                    else
                        seperator = '/';

                    if (serverPath.charAt(serverPath.length() - 1) == seperator)           //判断路径结尾是否含有分隔符
                        chosenServerFile = serverPath + newValue.getText();
                    else
                        chosenServerFile = serverPath + seperator + newValue.getText();
                }
            }
        });

        //设置状态信息表每一列的名称，最小宽度以及绑定Model类的属性
        TableColumn<State,String> localFileColumn = new TableColumn<>("本地文件");
        localFileColumn.setMinWidth(100);
        localFileColumn.setCellValueFactory(new PropertyValueFactory<>("localFile"));
        TableColumn<State,String> directionColumn = new TableColumn<>("方向");
        directionColumn.setMinWidth(20);
        directionColumn.setCellValueFactory(new PropertyValueFactory<>("direction"));
        TableColumn<State,String> serverFileColumn = new TableColumn<>("远程文件");
        serverFileColumn.setMinWidth(100);
        serverFileColumn.setCellValueFactory(new PropertyValueFactory<>("serverFile"));
        TableColumn<State,String> sizeColumn = new TableColumn<>("大小");
        sizeColumn.setMinWidth(30);
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
        TableColumn<State,String> priorityColumn = new TableColumn<>("优先级");
        priorityColumn.setMinWidth(30);
        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        TableColumn<State,String> stateColumn = new TableColumn<>("状态");
        stateColumn.setMinWidth(200);
        stateColumn.setCellValueFactory(new PropertyValueFactory<>("state"));

        //状态信息表绑定stateData
        Table_States.setItems(stateData);
        //将定义好的列加入状态信息表
        Table_States.getColumns().addAll(localFileColumn,directionColumn,serverFileColumn,sizeColumn,priorityColumn,stateColumn);
        //设置状态信息表的占位标志
        Table_States.setPlaceholder(new Label());
    }
    //状态信息类
    public static class State {
        private SimpleStringProperty localFile;     //本地文件或目录
        private SimpleStringProperty direction;     //方向
        private SimpleStringProperty serverFile;    //远程文件或目录
        private SimpleStringProperty size;          //大小
        private SimpleStringProperty priority;      //优先级
        private SimpleStringProperty state;         //状态

        public State(String localFile,String direction,String serverFile,String size,String priority,String state){
            this.localFile = new SimpleStringProperty(localFile==null?"":localFile);
            this.direction = new SimpleStringProperty(direction==null?"":direction);
            this.serverFile = new SimpleStringProperty(serverFile==null?"":serverFile);
            this.size = new SimpleStringProperty(size==null?"":size);
            this.priority = new SimpleStringProperty(priority==null?"":priority);
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

        public String getPriority() {
            return priority.get();
        }

        public SimpleStringProperty priorityProperty() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority.set(priority==null?"":priority);
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