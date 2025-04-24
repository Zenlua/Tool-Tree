package com.example.fileselector;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private EditText searchEdit, extEdit;
    private RecyclerView recyclerView;
    private Button confirmButton;
    private FileAdapter adapter;
    private List<FileItem> selectedFiles = new ArrayList<>();

    private final ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                // Not used here, custom browsing instead
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchEdit = findViewById(R.id.searchEdit);
        extEdit = findViewById(R.id.extEdit);
        recyclerView = findViewById(R.id.recyclerView);
        confirmButton = findViewById(R.id.confirmButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FileAdapter(FileUtils.listFiles("/", true), this, this::onSelectionChanged);
        recyclerView.setAdapter(adapter);

        searchEdit.addTextChangedListener(new SimpleTextWatcher(text -> filter()));
        extEdit.addTextChangedListener(new SimpleTextWatcher(text -> filter()));

        confirmButton.setOnClickListener(v -> onConfirm());
    }

    private void filter() {
        String query = searchEdit.getText().toString();
        String ext = extEdit.getText().toString();
        adapter.filter(query, ext);
    }

    private void onSelectionChanged(List<FileItem> list) {
        selectedFiles.clear();
        selectedFiles.addAll(list);
    }

    private void onConfirm() {
        Intent resultIntent = new Intent();
        String typeUri = getIntent().getStringExtra("type_uri");
        boolean returnUri = "uri".equalsIgnoreCase(typeUri);
        ArrayList<String> result = new ArrayList<>();

        for (FileItem item : selectedFiles) {
            File file = new File(item.getPath());
            if (returnUri) {
                Uri uri = FileProvider.getUriForFile(this, getPackageName()+".provider", file);
                result.add(uri.toString());
            } else {
                result.add(file.getAbsolutePath());
            }
        }

        resultIntent.putStringArrayListExtra("selected_files", result);
        setResult(RESULT_OK, resultIntent);

        Intent broadcast = new Intent("com.yourapp.ACTION_FILES_SELECTED");
        broadcast.putStringArrayListExtra("selected_files", result);
        sendBroadcast(broadcast);

        Toast.makeText(this, "Selected "+result.size()+" items", Toast.LENGTH_SHORT).show();
        finish();
    }
}
