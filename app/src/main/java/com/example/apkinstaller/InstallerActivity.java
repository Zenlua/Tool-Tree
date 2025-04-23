package com.example.apkinstaller;

import android.Manifest; import android.content.Intent; import android.content.pm.PackageManager; import android.net.Uri; import android.os.Build; import android.os.Bundle; import android.os.Environment; import android.provider.Settings; import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity; import androidx.core.content.ContextCompat; import androidx.core.content.FileProvider;

import java.io.File;

public class InstallerActivity extends AppCompatActivity { private static final int REQUEST_PERMISSIONS = 1;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (!checkPermissions()) {
        requestPermissions();
    } else {
        installApk();
    }
}

private boolean checkPermissions() {
    boolean storage = true;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        storage = Environment.isExternalStorageManager();
    }

    boolean install = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || getPackageManager().canRequestPackageInstalls();

    return storage && install;
}

private void requestPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            startActivityForResult(intent, REQUEST_PERMISSIONS);
        } catch (Exception e) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_PERMISSIONS);
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (!getPackageManager().canRequestPackageInstalls()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_PERMISSIONS);
        }
    }
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_PERMISSIONS) {
        if (checkPermissions()) {
            installApk();
        } else {
            Toast.makeText(this, "Permission denied.", Toast.LENGTH_LONG).show();
            finish();
        }
    }
}

private void installApk() {
    String apkPath = getIntent().getStringExtra("apk_path");
    if (apkPath == null) {
        Toast.makeText(this, "No APK path provided", Toast.LENGTH_LONG).show();
        finish();
        return;
    }

    File apkFile = new File(apkPath);
    if (!apkFile.exists()) {
        Toast.makeText(this, "APK file not found", Toast.LENGTH_LONG).show();
        finish();
        return;
    }

    Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", apkFile);

    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    startActivity(intent);

    finish();
}

}

