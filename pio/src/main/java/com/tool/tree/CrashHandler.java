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

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                1001,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            alarmManager.set(
                    AlarmManager.RTC,
                    System.currentTimeMillis() + 200,
                    pendingIntent
            );
        }

        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }
}