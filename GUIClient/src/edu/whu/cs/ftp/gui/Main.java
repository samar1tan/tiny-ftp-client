package edu.whu.cs.ftp.gui;

import edu.whu.cs.ftp.client.*;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


public class Main extends Application{

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        //加载fxml文件
        Parent root = FXMLLoader.load(getClass().getResource("page.fxml"));

        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.setTitle("FTP Client");      //设置主窗口标题
        stage.getIcons().add(new Image("file:icon\\symble.png"));   //设置窗口图标
        stage.setResizable(false);        //关闭主窗口缩放功能
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                System.exit(0);
            }
        });
        stage.show();
    }
}
