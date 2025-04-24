package com.example.fileselector;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private EditText searchEdit, extEdit;
    private RecyclerView recyclerView;
    private Button confirmButton;
    private FileAdapter adapter;
    private List<FileItem> fileList, selectedFiles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchEdit = findViewById(R.id.searchEdit);
        extEdit = findViewById(R.id.extEdit);
        recyclerView = findViewById(R.id.recyclerView);
        confirmButton = findViewById(R.id.confirmButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        fileList = FileUtils.listFiles("/", true);
        adapter = new FileAdapter(fileList, this, selected -> {
            selectedFiles.clear();
            for (FileItem item : adapter.getItems()) {
                if (item.isSelected()) selectedFiles.add(item);
            }
        });
        recyclerView.setAdapter(adapter);

        searchEdit.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(String text) {
                filter();
            }
        });
        extEdit.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(String text) {
                filter();
            }
        });

        confirmButton.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            String typeUri = getIntent().getStringExtra("type_uri");
            boolean returnUri = "uri".equalsIgnoreCase(typeUri);
            ArrayList<String> result = new ArrayList<>();
            for (FileItem item : selectedFiles) {
                File file = new File(item.getPath());
                if (returnUri) {
                    Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
                    result.add(uri.toString());
                } else {
                    result.add(file.getAbsolutePath());
                }
            }
            // setResult
            resultIntent.putStringArrayListExtra("selected_files", result);
            setResult(RESULT_OK, resultIntent);

            // broadcast
            Intent broadcast = new Intent("com.yourapp.ACTION_FILES_SELECTED");
            broadcast.putStringArrayListExtra("selected_files", result);
            sendBroadcast(broadcast);

            finish();
        });
    }

    private void filter() {
        String query = searchEdit.getText().toString();
        String ext = extEdit.getText().toString();
        adapter.filter(query, ext);
    }
}
