
// FileItem.java
package com.example.rootfilepicker;

import java.io.File;

public class FileItem {
    private final File file;

    public FileItem(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }
}
