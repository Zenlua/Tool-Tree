package com.omarea.common.ui;

import android.app.Activity;
import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import java.io.File;

public class BlurController {
    public void captureAndBlur(Activity activity) {
        new Thread(() -> {
            Bitmap source = null;
            File customWallpaperFile = new File(activity.getFilesDir(), "home/etc/wallpaper.jpg");

            // Ưu tiên 1: Lấy từ file wallpaper tùy chỉnh
            if (customWallpaperFile.exists()) {
                source = BitmapFactory.decodeFile(customWallpaperFile.getAbsolutePath());
            } 
            
            // Ưu tiên 2: Lấy từ WallpaperManager nếu không dùng Live Wallpaper
            if (source == null) {
                WallpaperManager wm = WallpaperManager.getInstance(activity);
                if (wm.getWallpaperInfo() == null) { // Chỉ lấy được nếu là ảnh tĩnh
                    Drawable drawable = wm.getDrawable();
                    if (drawable instanceof BitmapDrawable) {
                        source = ((BitmapDrawable) drawable).getBitmap();
                    }
                }
            }

            // Ưu tiên 3: Nếu là Live Wallpaper hoặc không lấy được ảnh, chụp màn hình làm fallback
            if (source == null) {
                source = BlurUtils.captureScreen(activity);
            }

            if (source != null) {
                // Resize và làm mờ
                int width = 192;
                int height = (int) (192f * source.getHeight() / source.getWidth());
                Bitmap small = Bitmap.createScaledBitmap(source, width, height, true);
                
                BlurEngine.blurBitmap = BlurUtils.stackBlur(small, 25); // Tăng radius lên 10 cho đẹp
                BlurEngine.isPaused = false;
            }
        }).start();
    }
}
