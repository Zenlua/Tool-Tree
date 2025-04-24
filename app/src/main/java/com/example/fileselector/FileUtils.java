package com.example.fileselector;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {
    /**
     * Liệt kê tất cả file và folder trong đường dẫn path
     */
    public static List<FileItem> listFiles(String path) {
        List<FileItem> list = new ArrayList<>();
        File dir = new File(path);
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                list.add(new FileItem(
                    f.getName(),
                    f.getAbsolutePath(),
                    f.isDirectory()
                ));
            }
        }
        return list;
    }
}
