import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class DownloadExpectedStatusCodes {
    public Map<String, Integer> expectedStatusCodeset;

    public DownloadExpectedStatusCodes() {
        expectedStatusCodeset = new HashMap<>();
        // add pre-defined codes here
        expectedStatusCodeset.put("SIZE", 213);
    }

    public Integer getStatusCode(String cmd) {
        if (expectedStatusCodeset.containsKey(cmd)) {
            return expectedStatusCodeset.get(cmd);
        } else {
            System.out.println("Command '" + cmd + "' Not Found\nInput manually instead: ");
            Scanner scan = new Scanner(System.in);
            Integer ret = -1;
            if (scan.hasNextInt()) {
                Integer code = scan.nextInt();
                ret = code;
                expectedStatusCodeset.put(cmd, code);
                System.out.println("Successfully added: <" + cmd + ',' + ' ' + code + '>');
            }
            scan.close();
            return ret;
        }
    }
}
