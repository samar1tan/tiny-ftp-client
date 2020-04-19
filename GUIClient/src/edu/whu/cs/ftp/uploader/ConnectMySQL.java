package edu.whu.cs.ftp.uploader;

import java.sql.*;

public class ConnectMySQL {
    private Connection con;
    String driver = "com.mysql.jdbc.Driver";
    String url = "jdbc:mysql://localhost:3306/upload";
    String user = "root";
    String password = "password";

    public ConnectMySQL() throws SQLException {
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
        }catch (ClassNotFoundException cne){
            cne.printStackTrace();
        }

        con=DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/upload" + "?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=UTC","root","password");

        if(con.isClosed())
        {
            System.out.println("Failed connecting to the Database!");
        }

    }

    public ConnectMySQL(String driver, String url, String user, String password) throws SQLException {
        try{
            Class.forName(driver);
        }catch (ClassNotFoundException cne){
            cne.printStackTrace();
        }

        con=DriverManager.getConnection(url, user, password);

        if(con.isClosed())
        {
            System.out.println("Failed connecting to the Database!");
        }

    }

    //关闭数据库连接
    public void close() throws SQLException {
        if(con != null) {

            try {
                con.close();  //关闭数据库连接
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    //检查数据库中是否存在对应记录
    public boolean check(String localpath, String serverpath, String size) throws SQLException {
        boolean result = false;
        PreparedStatement pstmt=con.prepareStatement("select * from upload where localpath=? and serverpath=? and size=?");
        pstmt.setString(1, localpath);
        pstmt.setString(2, serverpath);
        pstmt.setString(3, size);
        ResultSet rs=pstmt.executeQuery();

        if(rs.next())
        {
            result = true;
        }
        pstmt.close();
        rs.close();

        return result;
    }

    //向数据库中添加相关记录
    public boolean add(String localpath, String serverpath, String size) throws SQLException {
        boolean result = true;
        PreparedStatement pstmt=con.prepareStatement("insert into upload (localpath,serverpath,size)" + "values(?,?,?)");
        pstmt.setString(1, localpath);
        pstmt.setString(2, serverpath);
        pstmt.setString(3, size);

        if(pstmt.executeUpdate() == 0) {
            result = false;
        }
        pstmt.close();
        return result;
    }

    //从数据库中删除相关记录
    public boolean delete(String localpath, String serverpath, String size) throws SQLException {
        boolean result = true;
        PreparedStatement pstmt=con.prepareStatement("delete from upload where localpath=? and serverpath=? and size=?");
        pstmt.setString(1, localpath);
        pstmt.setString(2, serverpath);
        pstmt.setString(3, size);

        if(pstmt.executeUpdate() == 0) {
            result = false;
        }
        pstmt.close();
        return result;
    }

}
