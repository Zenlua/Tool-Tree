
// MainActivity.java
package com.example.rootfilepicker;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private List<String> fileFilter = new ArrayList<>();
    private String typeUri = "path";
    private static final int PERMISSION_REQUEST_CODE = 1;
    private ListView listView;
    private File currentDirectory;
    private TextView tvCurrentPath;
    private FileListAdapter adapter;
    private final List<FileItem> fileItems = new ArrayList<>();
    private final List<FileItem> selectedItems = new ArrayList<>();
    private final String fileFilter = ""; // ví dụ: ".txt"
    private List<String> extensionFilter = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("type_uri")) {

        String extRaw = intent.getStringExtra("extension");
        if (extRaw != null) {
            for (String ext : extRaw.split(",")) {
                fileFilter.add(ext.trim().toLowerCase());
            }
        }
                typeUri = intent.getStringExtra("type_uri");

        }
        super.onCreate(savedInstanceState);
    // Lưu khi app thoát
    Runtime.getRuntime().addShutdownHook(new Thread(this::saveSelectedPaths));
    
        // Nhận danh sách đuôi file từ Intent
        extensionFilter = new ArrayList<>();
        String extRaw = getIntent().getStringExtra("extension");
        if (extRaw != null) {
            for (String ext : extRaw.split(",")) {
                extensionFilter.add(ext.trim().toLowerCase());
            }
        }
        
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listview);
        tvCurrentPath = findViewById(R.id.tv_current_path);
        Button btnSave = findViewById(R.id.btn_save_list);

        adapter = new FileListAdapter(this, fileItems, selectedItems);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            FileItem item = fileItems.get(i);
            File file = item.getFile();
            if (file.isDirectory()) {
                loadDirectory(file);
            } else {
                if (selectedItems.contains(item)) {
                    selectedItems.remove(item);
                } else {
                    selectedItems.add(item);
                }
                adapter.notifyDataSetChanged();
            }
        });

        listView.setOnItemLongClickListener((adapterView, view, i, l) -> {
            FileItem item = fileItems.get(i);
            if (item.getFile().isDirectory()) {
                if (selectedItems.contains(item)) {
                    selectedItems.remove(item);
                } else {
                    selectedItems.add(item);
                }
                adapter.notifyDataSetChanged();
                return true;
            }
            return false;
        });

        btnSave.setOnClickListener(v -> {
    saveSelectedPaths();
    finish();
});

        if (checkPermission()) {
            initFilePicker();
        } else {
            requestPermission();
        }
    }

    private void initFilePicker() {
        currentDirectory = Environment.getExternalStorageDirectory();
        loadDirectory(currentDirectory);
    }

    private void loadDirectory(File dir) {
    currentDirectory = dir;
    tvCurrentPath.setText(dir.getAbsolutePath());
    File[] files = dir.listFiles();
    fileItems.clear();
    if (files != null) {
        Arrays.sort(files, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) return -1;
            if (!f1.isDirectory() && f2.isDirectory()) return 1;
            return f1.getName().compareToIgnoreCase(f2.getName());
        });
        for (File f : files) {
            if (!fileFilter.isEmpty() && f.isFile()) {
                boolean matched = false;
                for (String ext : fileFilter) {
                    if (f.getName().toLowerCase().endsWith(ext)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) continue;
            }
            fileItems.add(new FileItem(f));
        }
    }
    adapter.notifyDataSetChanged();
}

    private void saveSelectedPaths() {
        File outFile = new File(Environment.getExternalStorageDirectory(), "list.txt");
        try (FileWriter writer = new FileWriter(outFile)) {
            for (FileItem item : selectedItems) {
                if ("uri".equals(typeUri)) {
                Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".provider",
                    item.getFile()
                );
                writer.write(uri.toString() + "\n");
            } else {
                writer.write(item.getFile().getAbsolutePath() + "\n");
            }
            }
            Toast.makeText(this, "Đã lưu tại: " + outFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Lỗi lưu file", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onBackPressed() {
        if (currentDirectory != null && !currentDirectory.getAbsolutePath().equals("/")) {
            loadDirectory(currentDirectory.getParentFile());
        } else {
            saveSelectedPaths();
        super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initFilePicker();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
