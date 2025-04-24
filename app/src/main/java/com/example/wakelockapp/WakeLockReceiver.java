
package com.example.wakelockapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

public class WakeLockReceiver extends BroadcastReceiver {
    private static PowerManager.WakeLock wakeLock;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getStringExtra("state");
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        if (wakeLock == null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeLockApp::ReceiverLock");
        }

        if ("on".equals(action) && !wakeLock.isHeld()) {
            wakeLock.acquire();
            Toast.makeText(context, "Wake Lock đã BẬT", Toast.LENGTH_SHORT).show();
            Log.i("WakeLockReceiver", "WakeLock acquired via broadcast");
        } else if ("off".equals(action) && wakeLock.isHeld()) {
            wakeLock.release();
            Toast.makeText(context, "Wake Lock đã TẮT", Toast.LENGTH_SHORT).show();
            Log.i("WakeLockReceiver", "WakeLock released via broadcast");
        }
    }
}
