package com.tool.tree;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private final Context context;

    public CrashHandler(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
    
        String stackTrace = Log.getStackTraceString(throwable);
        Intent intent = new Intent(context, CrashLogActivity.class);
        intent.putExtra("crash_log", stackTrace);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        context.startActivity(intent);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }
}