package edu.whu.cs.ftp.downloader;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class DownloadExpectedStatusCodes {
    public Map<String, Integer> expectedStatusCodeset;

    public DownloadExpectedStatusCodes() {
        expectedStatusCodeset = new HashMap<>();
        // add pre-defined codes here
        expectedStatusCodeset.put("SIZE", 213);
        expectedStatusCodeset.put("REST", 350);
        expectedStatusCodeset.put("RETR", 150);

    }

    public int getStatusCode(String cmd) {
        if (expectedStatusCodeset.containsKey(cmd)) {
            return expectedStatusCodeset.get(cmd);
        } else {
            System.out.println("Command '" + cmd + "' Not Found\nInput manually instead: ");
            Scanner scan = new Scanner(System.in);
            int ret = -1;
            if (scan.hasNextInt()) {
                int code = scan.nextInt();
                ret = code;
                expectedStatusCodeset.put(cmd, code);
                System.out.println("Successfully added: <" + cmd + ',' + ' ' + code + '>');
            }
            scan.close();
            return ret;
        }
    }
}
