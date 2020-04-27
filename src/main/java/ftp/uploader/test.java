package ftp.uploader;

import ftp.client.FTPPath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

public class test {
    public static void main(String[] args) throws SQLException, IOException {
        ConnectMySQL sql = new ConnectMySQL();
        Path local_path = Paths.get("C:\\ftp_client\\s.txt");
        File localfile = local_path.toFile();
        FTPPath server_path = new FTPPath("/", "");
        sql.refresh();
//        sql.add(localfile.getPath(), server_path.getPath(), String.valueOf(localfile.length()));
//        if(sql.check(localfile.getPath(), server_path.getPath(), String.valueOf(localfile.length())))
//        {
//            System.out.println("----------------cunzai");
//        }
//        sql.delete("2", "2", String.valueOf(2));
        sql.close();
    }

}
