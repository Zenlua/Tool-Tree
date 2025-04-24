package com.example.rootfilepicker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private RecyclerView recyclerView;
    private FileExplorerAdapter adapter;
    private EditText searchEditText;
    private Button confirmButton;

    private List<File> selectedFiles = new ArrayList<>();
    private String[] filters = null;
    private boolean returnUri = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        searchEditText = findViewById(R.id.searchEditText);
        confirmButton = findViewById(R.id.confirmButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Intent intent = getIntent();
        String extFilter = intent.getStringExtra("filter_ext");
        String typeUri = intent.getStringExtra("type_uri");

        if (extFilter != null) filters = extFilter.split(",");
        if ("uri".equalsIgnoreCase(typeUri)) returnUri = true;

        File rootDir = Environment.getExternalStorageDirectory();
        adapter = new FileExplorerAdapter(rootDir, filters, selected -> selectedFiles = selected);
        recyclerView.setAdapter(adapter);

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            adapter.filter(searchEditText.getText().toString().trim());
            return true;
        });

        confirmButton.setOnClickListener(view -> {
            ArrayList<String> results = new ArrayList<>();
            for (File file : selectedFiles) {
                if (returnUri) {
                    Uri uri = FileProvider.getUriForFile(this, "com.example.rootfilepicker.provider", file);
                    results.add(uri.toString());
                    grantUriPermission(getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } else {
                    results.add(file.getAbsolutePath());
                }
            }

            Intent result = new Intent();
            result.putStringArrayListExtra("selected", results);
            setResult(RESULT_OK, result);

            Intent broadcast = new Intent("com.yourapp.ACTION_FILES_SELECTED");
            broadcast.putStringArrayListExtra("selected", results);
            sendBroadcast(broadcast);

            finish();
        });
    }
}
