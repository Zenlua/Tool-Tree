
package com.example.wakelockapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.PowerManager;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeLockApp::MainLock");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Wake Lock")
               .setMessage("Bật hay tắt Wake Lock?")
               .setPositiveButton("Bật", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       if (!wakeLock.isHeld()) {
                           wakeLock.acquire();
                       }
                   }
               })
               .setNegativeButton("Tắt", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       if (wakeLock.isHeld()) {
                           wakeLock.release();
                       }
                   }
               });
        builder.create().show();
    }
}
