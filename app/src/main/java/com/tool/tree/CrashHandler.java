package com.tool.tree;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    public CrashHandler(Context context) {
        this.context = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        try {
            String stackTrace = Log.getStackTraceString(ex);
            Log.e("CrashHandler", "Uncaught exception in thread: " + thread.getName(), ex);

            Intent intent = new Intent(context, CrashLogActivity.class);
            intent.putExtra("crash_log", stackTrace);
            // Quan trọng: Phải có FLAG_ACTIVITY_NEW_TASK vì gọi từ thread không phải UI
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            context.startActivity(intent);

        } catch (Throwable t) {
            Log.e("CrashHandler", "Failed to handle crash gracefully", t);
        } finally {
            // Kết thúc process cũ để CrashLogActivity có thể chạy trên một môi trường sạch
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        }
    }

    public static void install(Context context) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(context));
    }
}
