package com.example.fileselector;

import android.os.Build;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class FileUtils {
    public static List<FileItem> listFiles(String path, boolean useRoot) {
        List<FileItem> list = new ArrayList<>();
        try {
            if (useRoot && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "ls -a " + path});
                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    File f = new File(path, line);
                    list.add(new FileItem(f.getName(), f.getAbsolutePath(), f.isDirectory()));
                }
                in.close();
            } else {
                File dir = new File(path);
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        list.add(new FileItem(f.getName(), f.getAbsolutePath(), f.isDirectory()));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
