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
    
    /**
     * Chụp và làm mờ ảnh nền. 
     * Đã lược bỏ kiểm tra quyền vì đã được xử lý ở logic bên ngoài.
     */
    public void captureAndBlur(Activity activity) {
        final WeakReference<Activity> activityRef = new WeakReference<>(activity);
        
        new Thread(() -> {
            Activity act = activityRef.get();
            // Kiểm tra an toàn trước khi xử lý
            if (act == null || act.isFinishing() || act.isDestroyed()) return;

            Bitmap source = null;
            Context context = act.getApplicationContext();

            // 1. Ưu tiên: Wallpaper tùy chỉnh từ thư mục riêng của app
            File customWallpaperFile = new File(act.getFilesDir(), "home/etc/wallpaper.jpg");
            if (customWallpaperFile.exists()) {
                source = BitmapFactory.decodeFile(customWallpaperFile.getAbsolutePath());
            }

            // 2. Kế tiếp: Lấy trực tiếp Wallpaper hệ thống
            if (source == null) {
                WallpaperManager wm = WallpaperManager.getInstance(context);
                // wallpaperInfo == null xác nhận đây là ảnh tĩnh (không phải Live Wallpaper)
                if (wm.getWallpaperInfo() == null) { 
                    Drawable drawable = wm.getDrawable();
                    if (drawable instanceof BitmapDrawable) {
                        source = ((BitmapDrawable) drawable).getBitmap();
                    }
                }
            }

            // Xử lý kết quả mờ
            Bitmap blurredResult;
            if (source != null) {
                // Làm mờ từ nguồn ảnh wallpaper (nhanh và sạch hơn)
                blurredResult = FastBlurUtility.startBlurBackground(source);
            } else {
                // Cuối cùng: Chụp màn hình nếu không lấy được ảnh nền (Fallback)
                blurredResult = FastBlurUtility.getBlurBackgroundDrawer(act);
            }

            // Trong file BlurController.java
            if (blurredResult != null) {
                // THAY ĐỔI: Không gọi recycle() ở đây nữa để tránh xung đột với luồng UI đang vẽ
                // Việc gán tham chiếu mới sẽ giúp tham chiếu cũ được GC tự động dọn dẹp an toàn hơn
                BlurEngine.blurBitmap = blurredResult;
                BlurEngine.isPaused = false;
                act.runOnUiThread(() -> {
                    if (act.getWindow() != null) {
                        act.getWindow().getDecorView().invalidate();
                    }
                });
            }
        }).start();
    }
}
