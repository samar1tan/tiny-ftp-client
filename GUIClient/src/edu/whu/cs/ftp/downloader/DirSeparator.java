package edu.whu.cs.ftp.downloader;

import java.io.File;

public class DirSeparator {
    private String separator;

    public DirSeparator(DirSeparatorModes mode) {
        switch (mode) {
            case FTP:
            case Unix:
                separator = "/";
                break;
            case Windows:
                separator = "\\";
                break;
            case LocalMachine:
                separator = File.separator;
        }
    }

    @Override
    public String toString() { // real representation
        return separator;
    }

    public String getSeparator() {
        return separator;
    }

    public String getSeparatorForRegex() {
        return separator.equals("\\") ? "\\\\" : separator;
    }
}
