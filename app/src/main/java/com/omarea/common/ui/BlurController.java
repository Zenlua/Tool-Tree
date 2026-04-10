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

    // Sử dụng biến static để ghi nhớ trạng thái file giữa các lần gọi
    private static long lastFileLength = -1;
    private static long lastFileModified = -1;
    
    public void captureAndBlur(Activity activity) {
        final WeakReference<Activity> activityRef = new WeakReference<>(activity);
        
        new Thread(() -> {
            Activity act = activityRef.get();
            if (act == null || act.isFinishing() || act.isDestroyed()) return;

            Bitmap source = null;
            Context context = act.getApplicationContext();

            // 1. Kiểm tra Wallpaper tùy chỉnh trong /files/home/etc/wallpaper.jpg
            File customWallpaperFile = new File(act.getFilesDir(), "home/etc/wallpaper.jpg");
            
            if (customWallpaperFile.exists()) {
                long currentLength = customWallpaperFile.length();
                long currentModified = customWallpaperFile.lastModified();

                // KIỂM TRA THAY ĐỔI: Nếu dung lượng và thời gian cũ giống hệt thì thoát
                if (currentLength == lastFileLength && currentModified == lastFileModified) {
                    if (BlurEngine.blurBitmap != null && !BlurEngine.blurBitmap.isRecycled()) {
                        return; // Không có gì thay đổi, không cần làm mờ lại
                    }
                }

                // Cập nhật dấu vết mới
                lastFileLength = currentLength;
                lastFileModified = currentModified;
                source = BitmapFactory.decodeFile(customWallpaperFile.getAbsolutePath());
            } else {
                // Reset dấu vết nếu file custom bị xóa
                lastFileLength = -1;
                lastFileModified = -1;
            }

            // 2. Lấy Wallpaper hệ thống nếu không có file custom
            if (source == null) {
                WallpaperManager wm = WallpaperManager.getInstance(context);
                // XÓA CACHE: Buộc hệ thống đọc lại ảnh nền mới nhất từ Launcher
                wm.forgetLoadedWallpaper(); 
                
                if (wm.getWallpaperInfo() == null) { 
                    Drawable drawable = wm.getDrawable();
                    if (drawable instanceof BitmapDrawable) {
                        source = ((BitmapDrawable) drawable).getBitmap();
                    }
                }
            }

            // 3. Xử lý làm mờ
            Bitmap blurredResult;
            if (source != null) {
                blurredResult = FastBlurUtility.startBlurBackground(source);
            } else {
                blurredResult = FastBlurUtility.getBlurBackgroundDrawer(act);
            }

            if (blurredResult != null) {
                BlurEngine.blurBitmap = blurredResult;
                BlurEngine.isPaused = false;
                
                // Ép UI vẽ lại ngay lập tức
                act.runOnUiThread(() -> {
                    if (act != null && !act.isFinishing() && act.getWindow() != null) {
                        act.getWindow().getDecorView().invalidate();
                    }
                });
            }
        }).start();
    }
}
