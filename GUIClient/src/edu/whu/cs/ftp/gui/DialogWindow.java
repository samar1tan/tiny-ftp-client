package edu.whu.cs.ftp.gui;

import edu.whu.cs.ftp.client.*;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class DialogWindow {
    public static boolean ans;        //用户点击“确定”或者“取消”
    public static Stage st;

    public void display(String title) throws IOException {
        Stage stage = new Stage();
        st = stage;
        stage.initModality(Modality.APPLICATION_MODAL);       //设置窗口模式
        stage.setTitle(title);        //设置窗口标题

        //加载fxml文件
        Parent root = FXMLLoader.load(getClass().getResource("dialog.fxml"));

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.getIcons().add(new Image("file:icon\\symble.png"));   //设置窗口图标
        stage.setResizable(false);        //关闭窗口缩放功能
        stage.setOnCloseRequest(e->{         //点击对话框关闭按钮时将ans设置为false
            DialogWindow.ans = false;
        });
        stage.showAndWait();
    }
}
