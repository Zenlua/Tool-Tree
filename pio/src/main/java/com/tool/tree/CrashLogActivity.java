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

        String log = getIntent().getStringExtra("crash_log");
        if (log == null) {
            log = "Không có dữ liệu log.";
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 20, 20, 20);

        Button copyBtn = new Button(this);
        copyBtn.setText("Copy log");

        Button shareBtn = new Button(this);
        shareBtn.setText("Chia sẻ log");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                );
        scrollView.setLayoutParams(scrollParams);

        TextView textView = new TextView(this);
        textView.setText(log);
        textView.setTextIsSelectable(true);
        textView.setTextSize(12);

        scrollView.addView(textView);

        root.addView(copyBtn);
        root.addView(shareBtn);
        root.addView(scrollView);

        setContentView(root);

        // COPY
        copyBtn.setOnClickListener(v -> {
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

            ClipData clip = ClipData.newPlainText("Crash Log", log);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(this, "Đã copy log", Toast.LENGTH_SHORT).show();
        });

        // SHARE
        shareBtn.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Crash Log");
            shareIntent.putExtra(Intent.EXTRA_TEXT, log);

            startActivity(Intent.createChooser(shareIntent, "Chia sẻ log qua"));
        });
    }
}