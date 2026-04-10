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

    // Lưu trữ trạng thái file để so sánh ở lần chạy kế tiếp
    private static long lastFileLength = -1;
    private static long lastFileModified = -1;
    
    /**
     * Chụp và làm mờ ảnh nền. 
     */
    public void captureAndBlur(Activity activity) {
        final WeakReference<Activity> activityRef = new WeakReference<>(activity);
        
        new Thread(() -> {
            Activity act = activityRef.get();
            if (act == null || act.isFinishing() || act.isDestroyed()) return;

            Bitmap source = null;
            Context context = act.getApplicationContext();

            // 1. Kiểm tra Wallpaper tùy chỉnh trong internal storage
            File customWallpaperFile = new File(act.getFilesDir(), "home/etc/wallpaper.jpg");
            
            if (customWallpaperFile.exists()) {
                long currentLength = customWallpaperFile.length();
                long currentModified = customWallpaperFile.lastModified();

                // KIỂM TRA THAY ĐỔI: Nếu size và thời gian giống hệt cũ thì bỏ qua không xử lý lại
                if (currentLength == lastFileLength && currentModified == lastFileModified) {
                    return; 
                }

                // Cập nhật dấu vết file mới
                lastFileLength = currentLength;
                lastFileModified = currentModified;

                source = BitmapFactory.decodeFile(customWallpaperFile.getAbsolutePath());
            } else {
                // Nếu file custom không tồn tại, reset dấu vết để có thể nhận diện lại nếu file xuất hiện sau này
                lastFileLength = -1;
                lastFileModified = -1;
            }

            // 2. Kế tiếp: Lấy trực tiếp Wallpaper hệ thống nếu không có file custom
            if (source == null) {
                WallpaperManager wm = WallpaperManager.getInstance(context);
                // Xóa cache cũ của WallpaperManager để đảm bảo lấy ảnh mới nhất nếu hệ thống vừa đổi
                wm.forgetLoadedWallpaper(); 
                
                if (wm.getWallpaperInfo() == null) { 
                    Drawable drawable = wm.getDrawable();
                    if (drawable instanceof BitmapDrawable) {
                        source = ((BitmapDrawable) drawable).getBitmap();
                    }
                }
            }

            // Xử lý làm mờ
            Bitmap blurredResult;
            if (source != null) {
                blurredResult = FastBlurUtility.startBlurBackground(source);
            } else {
                // Fallback: Chụp màn hình (Lưu ý: Fallback này có thể không cần check file size)
                blurredResult = FastBlurUtility.getBlurBackgroundDrawer(act);
            }

            if (blurredResult != null) {
                BlurEngine.blurBitmap = blurredResult;
                BlurEngine.isPaused = false;
                
                // Cập nhật UI
                act.runOnUiThread(() -> {
                    if (act != null && !act.isFinishing() && act.getWindow() != null) {
                        act.getWindow().getDecorView().invalidate();
                    }
                });
            }
        }).start();
    }
}
