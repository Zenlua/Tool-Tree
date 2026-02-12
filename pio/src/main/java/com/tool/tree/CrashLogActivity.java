package com.tool.tree;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.HorizontalScrollView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CrashLogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String temp = getIntent().getStringExtra("crash_log");
        final String log = (temp != null) ? temp : "No log data available.";

        // ===== ROOT LAYOUT =====
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);

        // ===== TITLE =====
        TextView title = new TextView(this);
        title.setText("LOGCAT");
        title.setTextSize(25); // chữ to
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, 16);
        root.addView(title);

        // ===== BUTTON ROW =====
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

        // ===== SCROLL VIEW (VERTICAL + HORIZONTAL) =====
        ScrollView verticalScroll = new ScrollView(this);
        verticalScroll.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));
        
        HorizontalScrollView horizontalScroll = new HorizontalScrollView(this);
        
        TextView textView = new TextView(this);
        textView.setText(log);
        textView.setTextIsSelectable(true);
        textView.setTextSize(12);
        textView.setTypeface(android.graphics.Typeface.MONOSPACE);
        
        // Không tự xuống dòng
        textView.setHorizontallyScrolling(true);
        textView.setHorizontalScrollBarEnabled(true);
        
        horizontalScroll.addView(textView);
        verticalScroll.addView(horizontalScroll);
        
        root.addView(buttonRow);
        root.addView(verticalScroll);

        setContentView(root);

        // ===== COPY BUTTON =====
        copyBtn.setOnClickListener(v -> {
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Crash Log", log);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied.", Toast.LENGTH_SHORT).show();
        });

        // ===== SHARE FILE BUTTON =====
        shareBtn.setOnClickListener(v -> {
            try {
                String time = new SimpleDateFormat(
                        "yyyyMMdd_HHmmss",
                        Locale.getDefault()
                ).format(new Date());

                String fileName = "Tool-Tree_log_" + time + ".txt";

                File file = new File(getCacheDir(), fileName);

                FileOutputStream fos = new FileOutputStream(file);
                fos.write(log.getBytes());
                fos.close();

                Uri uri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".provider",
                        file
                );

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(Intent.createChooser(shareIntent, "Share log file"));

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to share file.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}