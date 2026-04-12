package com.tool.tree;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;  // ← Thêm khai báo này

    public CrashHandler(Context context) {
        this.context = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();  // ← Lưu default handler
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        try {
            String stackTrace = Log.getStackTraceString(ex);
            Log.e("CrashHandler", "Uncaught exception in thread: " + thread.getName(), ex);

            // (Tùy chọn) Lưu log vào file nếu bạn có implement
            // CrashFileWriter.write(context, stackTrace);

            Intent intent = new Intent(context, CrashLogActivity.class);
            intent.putExtra("crash_log", stackTrace);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            context.startActivity(intent);

        } catch (Throwable t) {
            Log.e("CrashHandler", "Failed to handle crash gracefully", t);
        }

        // Luôn gọi default handler ở đây (ngoài try-catch) để:
        // - Đảm bảo logcat có stack trace chuẩn (tag AndroidRuntime)
        // - Process được kill đúng cách
        // - Dialog "App đã dừng" xuất hiện nếu startActivity fail hoặc bạn muốn fallback
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, ex);
        }
    }

    // Phương thức tiện lợi để cài đặt (gọi 1 lần trong Application.onCreate)
    public static void install(Context context) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(context));
    }
}