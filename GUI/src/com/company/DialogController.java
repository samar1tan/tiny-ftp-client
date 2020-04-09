package com.company;

import javafx.fxml.Initializable;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class DialogController implements Initializable {
    public TextField TextField_DirName;       //新创建目录名输入框
    public static String newDirName = null;    //用户输入的目录名

    //获取新创建的目录的名字
    public void getNewDirName() {
        newDirName = TextField_DirName.getText();
    }
    //点击“确定”按钮
    public void ClickYes() {
        newDirName = TextField_DirName.getText();
        DialogWindow.ans = true;
        DialogWindow.st.close();
    }
    //点击“取消”按钮
    public void ClickNo() {
        DialogWindow.ans = false;
        DialogWindow.st.close();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if(newDirName != null)
            TextField_DirName.setText(newDirName);
    }
}
