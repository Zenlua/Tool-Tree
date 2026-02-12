package com.tool.tree;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CrashLogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    
        String temp = getIntent().getStringExtra("crash_log");
        final String log = (temp != null) ? temp : "No log data available..";
    
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
    
        // ===== BUTTON CONTAINER (1 dòng) =====
        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
    
        LinearLayout.LayoutParams btnParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );
    
        Button copyBtn = new Button(this);
        copyBtn.setText("Copy");
        copyBtn.setTextSize(14);
        copyBtn.setLayoutParams(btnParams);
    
        Button shareBtn = new Button(this);
        shareBtn.setText("Share");
        shareBtn.setTextSize(14);
        shareBtn.setLayoutParams(btnParams);
    
        buttonRow.addView(copyBtn);
        buttonRow.addView(shareBtn);
    
        // ===== SCROLL LOG =====
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));
    
        TextView textView = new TextView(this);
        textView.setText(log);
        textView.setTextIsSelectable(true);
        textView.setTextSize(12);
        textView.setVerticalScrollBarEnabled(true);
    
        scrollView.addView(textView);
    
        root.addView(buttonRow);
        root.addView(scrollView);
    
        setContentView(root);
    
        // ===== COPY =====
        copyBtn.setOnClickListener(v -> {
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Crash Log", log);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied.", Toast.LENGTH_SHORT).show();
        });
    
        // ===== SHARE =====
        shareBtn.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Crash Log");
            shareIntent.putExtra(Intent.EXTRA_TEXT, log);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });
    }
}