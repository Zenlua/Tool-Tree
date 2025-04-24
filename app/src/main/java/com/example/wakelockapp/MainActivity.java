package com.example.wakelockapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "WakeLockApp";
    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "WakeLockApp::MyWakeLockTag"
        );

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String state = intent.getStringExtra("state");
        if ("on".equals(state)) {
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
                Log.i(TAG, "WakeLock acquired via command.");
            }
        } else if ("off".equals(state)) {
            if (wakeLock.isHeld()) {
                wakeLock.release();
                Log.i(TAG, "WakeLock released via command.");
            }
        } else {
            Log.i(TAG, "No state extra: " + state);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
}
