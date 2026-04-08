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
                // Gọi hàm sau khi đã sửa thành public
                Bitmap blurredResult = FastBlurUtility.startBlurBackground(source);
                
                // Kiểm tra null trước khi gán
                if (blurredResult != null) {
                    BlurEngine.blurBitmap = blurredResult;
                    BlurEngine.isPaused = false;
                }
            } else {
                Bitmap screenshotBlur = FastBlurUtility.getBlurBackgroundDrawer(activity);
                if (screenshotBlur != null) {
                    BlurEngine.blurBitmap = screenshotBlur;
                    BlurEngine.isPaused = false;
                }
            }
        }).start();
    }
}
