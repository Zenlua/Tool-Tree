package com.example.apkinstaller;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;

public class InstallerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String apkPath = getIntent().getStringExtra("apk_path");
        if (apkPath != null) {
            File apkFile = new File(apkPath);
            if (apkFile.exists()) {
                Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", apkFile);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            }
        }
        finish();
    }
}