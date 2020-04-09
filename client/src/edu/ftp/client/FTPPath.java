package edu.ftp.client;

import edu.ftp.client.logging.StreamLogging;

/**
 * Representation of file and directory.
 * Note that this is <b>NOT</b> a resource.
 */
public class FTPPath implements StreamLogging {
    private boolean isDirectory;
    private boolean readable;
    private boolean writable;
    private int size;
    private String name;
    private String dir;

    public FTPPath(String dir, String name, int size) {
        this.dir = dir;
        this.name = name;
        this.size = size;
    }

    public FTPPath(String dir, String name) {
        this(dir, name, 0);
        isDirectory = true;
    }

    public void addPermission(String perm) {
        readable = perm.charAt(1) == 'r';
        writable = perm.charAt(2) == 'w';
    }

    public static FTPPath[] parseFromMLSD(String dir, String[] infoStr) {
        FTPPath[] paths = new FTPPath[infoStr.length];
        for (int i = 0; i < infoStr.length; i++) {
            String[] res = infoStr[i].split(";");
            if (res[0].endsWith("dir")) {
                paths[i] = new FTPPath(dir, res[2].trim());
            } else {
                paths[i] = new FTPPath(dir, res[3].trim(),
                        Integer.parseInt(res[2].split("=")[1]));
            }
        }
        return paths;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public int getSize() {
        return size;
    }

    public String getName() {
        return name;
    }

    public boolean isReadable() {
        return readable;
    }

    public boolean isWritable() {
        return writable;
    }

    public String getPath() {
        return dir + (dir.endsWith("/") ? "":"/") + name;
    }

    @Override
    public String toString() {
        return "FTPPath{" +
                "isDirectory=" + isDirectory +
                ", readable=" + readable +
                ", writable=" + writable +
                ", size=" + size +
                ", name='" + name + '\'' +
                ", dir='" + dir + '\'' +
                '}';
    }
}