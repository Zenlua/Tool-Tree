package com.omarea.common.ui;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import java.io.File;
import java.lang.ref.WeakReference;

public class BlurController {

    // Lưu trữ thông tin file cũ để so sánh
    private static long lastFileLength = -1;
    private static long lastFileModified = -1;
    
    public void captureAndBlur(Activity activity) {
        final WeakReference<Activity> activityRef = new WeakReference<>(activity);
        
        new Thread(() -> {
            Activity act = activityRef.get();
            if (act == null || act.isFinishing() || act.isDestroyed()) return;

            Bitmap source = null;
            Context context = act.getApplicationContext();

            // 1. Kiểm tra Wallpaper tùy chỉnh
            File customWallpaperFile = new File(act.getFilesDir(), "home/etc/wallpaper.jpg");
            
            if (customWallpaperFile.exists()) {
                long currentLength = customWallpaperFile.length();
                long currentModified = customWallpaperFile.lastModified();

                // Nếu file giống hệt lần trước thì thoát, tránh xử lý thừa
                if (currentLength == lastFileLength && currentModified == lastFileModified) {
                    // Nếu đã có ảnh blur rồi thì không cần làm lại
                    if (BlurEngine.blurBitmap != null && !BlurEngine.blurBitmap.isRecycled()) {
                        return;
                    }
                }

                // Cập nhật thông tin file mới
                lastFileLength = currentLength;
                lastFileModified = currentModified;
                source = BitmapFactory.decodeFile(customWallpaperFile.getAbsolutePath());
            } else {
                // Reset nếu không dùng file custom
                lastFileLength = -1;
                lastFileModified = -1;
            }

            // 2. Lấy Wallpaper hệ thống nếu không có file hoặc file lỗi
            if (source == null) {
                WallpaperManager wm = WallpaperManager.getInstance(context);
                // Ép hệ thống xóa cache để lấy ảnh mới nhất (vừa đổi ngoài launcher)
                wm.forgetLoadedWallpaper(); 
                
                if (wm.getWallpaperInfo() == null) { 
                    Drawable drawable = wm.getDrawable();
                    if (drawable instanceof BitmapDrawable) {
                        source = ((BitmapDrawable) drawable).getBitmap();
                    }
                }
            }

            // Xử lý Blur
            Bitmap blurredResult;
            if (source != null) {
                blurredResult = FastBlurUtility.startBlurBackground(source);
            } else {
                blurredResult = FastBlurUtility.getBlurBackgroundDrawer(act);
            }

            if (blurredResult != null) {
                BlurEngine.blurBitmap = blurredResult;
                BlurEngine.isPaused = false;
                
                // Cập nhật UI ngay lập tức
                act.runOnUiThread(() -> {
                    if (act != null && !act.isFinishing() && act.getWindow() != null) {
                        act.getWindow().getDecorView().invalidate();
                    }
                });
            }
        }).start();
    }
}
