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

            // Ưu tiên 1: Wallpaper tùy chỉnh
            if (customWallpaperFile.exists()) {
                source = BitmapFactory.decodeFile(customWallpaperFile.getAbsolutePath());
            } 
            
            // Ưu tiên 2: Wallpaper hệ thống (Ảnh tĩnh)
            if (source == null) {
                WallpaperManager wm = WallpaperManager.getInstance(activity);
                if (wm.getWallpaperInfo() == null) {
                    Drawable drawable = wm.getDrawable();
                    if (drawable instanceof BitmapDrawable) {
                        source = ((BitmapDrawable) drawable).getBitmap();
                    }
                }
            }

            // Xử lý làm mờ bằng Utility mới của bạn
            if (source != null) {
                // Dùng phương thức startBlurBackground để tự động thu nhỏ + làm mờ + làm tối
                BlurEngine.blurBitmap = FastBlurUtility.startBlurBackground(source);
                BlurEngine.isPaused = false;
            } else {
                // Fallback: Chụp màn hình nếu không lấy được ảnh nền trực tiếp
                BlurEngine.blurBitmap = FastBlurUtility.getBlurBackgroundDrawer(activity);
                BlurEngine.isPaused = false;
            }
        }).start();
    }
}
