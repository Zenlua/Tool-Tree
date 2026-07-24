package com.tool.tree;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.HorizontalScrollView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.activity.OnBackPressedCallback;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CrashLogActivity extends AppCompatActivity {

    private static final String TAG = "CrashLogActivity";

    // Giới hạn số ký tự hiển thị trực tiếp trên màn hình để tránh TextView/Canvas
    // ném RuntimeException ("Canvas: trying to draw too large bitmap") hoặc OOM
    // khi log quá dài (vd stacktrace lặp vô hạn). Log đầy đủ vẫn được giữ để Copy/Share.
    private static final int MAX_DISPLAY_LENGTH = 200_000;

    // Giữ log gốc đầy đủ (không bị cắt) để dùng cho Copy / Share
    private String fullLog = "No log data available.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            buildUi();
        } catch (Throwable t) {
            // Bắt cả Throwable (bao gồm OutOfMemoryError) vì đây là activity hiển thị
            // crash log - tuyệt đối không được để chính nó crash tiếp.
            Log.e(TAG, "buildUi() failed, falling back to minimal UI", t);
            showFallbackUi(t);
        }
    }

    /**
     * Dựng giao diện đầy đủ (title, nút Copy/Share, nội dung log có thể scroll).
     * Có thể ném lỗi nếu log quá lớn hoặc thiết bị hạn chế tài nguyên; lỗi sẽ được
     * onCreate() bắt lại và chuyển sang showFallbackUi().
     */
    private void buildUi() {
        String temp = null;
        try {
            if (getIntent() != null) {
                temp = getIntent().getStringExtra("crash_log");
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to read crash_log extra", t);
        }
        fullLog = (temp != null && !temp.isEmpty()) ? temp : "No log data available.";

        // Chuỗi dùng để hiển thị lên màn hình - có thể bị cắt bớt nếu quá dài,
        // nhưng fullLog vẫn giữ nguyên vẹn cho Copy/Share.
        final boolean truncated = fullLog.length() > MAX_DISPLAY_LENGTH;
        final String displayLog = truncated
                ? fullLog.substring(0, MAX_DISPLAY_LENGTH) + "\n\n... (The log is too long, I've trimmed the display portion - use Share to get the full log.)"
                : fullLog;

        // ===== ROOT LAYOUT =====
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);

        // ===== TITLE =====
        TextView title = new TextView(this);
        title.setText("Tool Tree Crash");
        title.setTextSize(25);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(14, 0, 0, 16);
        root.addView(title);

        // ===== BUTTON ROW =====
        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout.LayoutParams btnParams =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);

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
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        HorizontalScrollView horizontalScroll = new HorizontalScrollView(this);

        TextView textView = new TextView(this);
        textView.setText(displayLog);
        textView.setTextIsSelectable(true);
        textView.setTextSize(12);
        textView.setTypeface(android.graphics.Typeface.MONOSPACE);

        textView.setHorizontallyScrolling(true);
        textView.setHorizontalScrollBarEnabled(true);

        horizontalScroll.addView(textView);
        verticalScroll.addView(horizontalScroll);

        root.addView(buttonRow);
        root.addView(verticalScroll);

        if (truncated) {
            TextView notice = new TextView(this);
            notice.setText("Log đã bị cắt bớt khi hiển thị vì quá dài. Dùng Share để lấy đầy đủ.");
            notice.setTextSize(11);
            notice.setPadding(14, 8, 14, 0);
            root.addView(notice);
        }

        setContentView(root);

        // ===== COPY LOGIC =====
        copyBtn.setOnClickListener(v -> {
            try {
                ClipboardManager clipboard =
                        (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText("Crash Log", fullLog);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Copied.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Clipboard not available.", Toast.LENGTH_SHORT).show();
                }
            } catch (Throwable t) {
                Log.e(TAG, "Copy failed", t);
                Toast.makeText(this, "Failed to copy log.", Toast.LENGTH_SHORT).show();
            }
        });

        // ===== SHARE LOGIC =====
        shareBtn.setOnClickListener(v -> {
            FileOutputStream fos = null;
            try {
                String time = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String fileName = "Tool-Tree_log_" + time + ".txt";
                File file = new File(getCacheDir(), fileName);

                fos = new FileOutputStream(file);
                fos.write(fullLog.getBytes());
                fos.flush();
                fos.close();
                fos = null;

                Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(Intent.createChooser(shareIntent, "Share log file"));

            } catch (Throwable t) {
                Log.e(TAG, "Share failed", t);
                Toast.makeText(this, "Failed to share file.", Toast.LENGTH_SHORT).show();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (Throwable ignored) {
                        // đã lỗi ở trên rồi, bỏ qua lỗi khi đóng stream
                    }
                }
            }
        });

        // ===== BACK PRESS LOGIC =====
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                try {
                    Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "Back navigation failed", t);
                } finally {
                    finish();
                }
            }
        });
    }

    /**
     * Giao diện tối giản, gần như không thể ném lỗi, dùng khi buildUi() thất bại.
     * Mục tiêu duy nhất: hiển thị được điều gì đó và không crash lần nữa.
     */
    private void showFallbackUi(Throwable original) {
        try {
            ScrollView scroll = new ScrollView(this);
            TextView text = new TextView(this);
            text.setPadding(24, 24, 24, 24);
            text.setTextIsSelectable(true);

            String safeLog = fullLog != null ? fullLog : "No log data available.";
            // Cắt rất ngắn ở fallback vì đây là chế độ "cứu hộ" cuối cùng
            if (safeLog.length() > 20_000) {
                safeLog = safeLog.substring(0, 20_000) + "\n\n... (amputatedt)";
            }

            text.setText("Unable to display the full crash log due to an internal error:\n"
                    + String.valueOf(original) + "\n\n" + safeLog);

            scroll.addView(text);
            setContentView(scroll);
        } catch (Throwable t2) {
            // Ngay cả fallback cũng lỗi -> không cố hiển thị gì thêm, chỉ đóng activity
            // để tránh vòng lặp crash liên tục.
            Log.e(TAG, "Fallback UI also failed, finishing activity", t2);
            Toast.makeText(getApplicationContext(), "Crash log unavailable.", Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
