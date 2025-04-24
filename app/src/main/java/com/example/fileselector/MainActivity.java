package com.example.fileselector;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_PERMISSION = 100;
    private static final String AUTHORITY = "com.example.fileselector.provider";
    public static final String ACTION_FILES_SELECTED = "com.example.fileselector.ACTION_FILES_SELECTED";

    private EditText searchEdit, extEdit;
    private RecyclerView recyclerView;
    private Button confirmButton;
    private FileAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchEdit    = findViewById(R.id.searchEdit);
        extEdit       = findViewById(R.id.extEdit);
        recyclerView  = findViewById(R.id.recyclerView);
        confirmButton = findViewById(R.id.confirmButton);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                REQ_PERMISSION);
        } else {
            initList();
        }

        searchEdit.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(String text) {
                adapter.filter(text, extEdit.getText().toString().trim());
            }
        });
        extEdit.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(String text) {
                adapter.filter(searchEdit.getText().toString().trim(), text);
            }
        });

        confirmButton.setOnClickListener(v -> {
            List<String> result = new ArrayList<>();
            boolean useUri = false;
            String typeUri = getIntent().getStringExtra("type_uri");
            if (typeUri != null) useUri = Boolean.parseBoolean(typeUri);

            for (FileItem item : adapter.getSelectedItems()) {
                File f = new File(item.getPath());
                if (useUri) {
                    Uri uri = FileProvider.getUriForFile(this, AUTHORITY, f);
                    result.add(uri.toString());
                } else {
                    result.add(f.getAbsolutePath());
                }
            }

            Intent broadcast = new Intent(ACTION_FILES_SELECTED);
            broadcast.putStringArrayListExtra("selected_files", new ArrayList<>(result));
            sendBroadcast(broadcast);
            finish();
        });
    }

    private void initList() {
        String startPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        List<FileItem> items = FileUtils.listFiles(startPath);

        adapter = new FileAdapter(this, items);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSION && grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initList();
        } else {
            finish();
        }
    }
}
