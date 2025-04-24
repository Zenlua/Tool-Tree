package com.example.rootfilepicker;

import java.io.File;

public class FileUtils {
    public static boolean isReadable(File file) {
        return file.exists() && file.canRead();
    }

    public static boolean isWritable(File file) {
        return file.exists() && file.canWrite();
    }

    public static String getExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return (lastDot != -1) ? name.substring(lastDot + 1) : "";
    }
}
