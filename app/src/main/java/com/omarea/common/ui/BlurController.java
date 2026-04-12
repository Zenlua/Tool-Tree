package com.omarea.common.ui;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import java.io.File;
import java.lang.ref.WeakReference;

public class BlurController {

    private static long lastFileLength = -1;
    private static long lastFileModified = -1;
    
    /**
     * Thuật toán làm mờ sử dụng RenderScript (Thay thế FastBlurUtility)
     */
    private Bitmap blurBitmap(Context context, Bitmap bitmap, float radius) {
        if (bitmap == null) return null;
        
        Bitmap outBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        RenderScript rs = RenderScript.create(context);
        
        try {
            Allocation input = Allocation.createFromBitmap(rs, bitmap);
            Allocation output = Allocation.createFromBitmap(rs, outBitmap);
            
            ScriptIntrinsicBlur intrinsicBlur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            intrinsicBlur.setRadius(radius); // Giá trị tối đa là 25f
            intrinsicBlur.setInput(input);
            intrinsicBlur.forEach(output);
            
            output.copyTo(outBitmap);
        } finally {
            rs.destroy();
        }
        
        return outBitmap;
    }

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

                if (currentLength == lastFileLength && currentModified == lastFileModified) {
                    if (BlurEngine.blurBitmap != null && !BlurEngine.blurBitmap.isRecycled()) {
                        return; 
                    }
                }

                lastFileLength = currentLength;
                lastFileModified = currentModified;
                source = BitmapFactory.decodeFile(customWallpaperFile.getAbsolutePath());
            } else {
                lastFileLength = -1;
                lastFileModified = -1;
            }

            // 2. Lấy Wallpaper hệ thống nếu cần
            if (source == null) {
                WallpaperManager wm = WallpaperManager.getInstance(context);
                wm.forgetLoadedWallpaper(); 
                
                if (wm.getWallpaperInfo() == null) { 
                    Drawable drawable = wm.getDrawable();
                    if (drawable instanceof BitmapDrawable) {
                        source = ((BitmapDrawable) drawable).getBitmap();
                    }
                }
            }

            // 3. Xử lý làm mờ và tối ưu RAM
            if (source != null) {
                // Tối ưu: Giảm kích thước ảnh xuống 4 lần giúp giảm 16 lần lượng RAM tiêu thụ 
                // và làm hiệu ứng mờ trông "mịn" hơn.
                int width = Math.max(source.getWidth() / 4, 1);
                int height = Math.max(source.getHeight() / 4, 1);
                Bitmap scaledSource = Bitmap.createScaledBitmap(source, width, height, false);
                
                Bitmap blurredResult = blurBitmap(context, scaledSource, 15f);

                if (blurredResult != null) {
                    BlurEngine.blurBitmap = blurredResult;
                    BlurEngine.isPaused = false;
                    
                    act.runOnUiThread(() -> {
                        if (act != null && !act.isFinishing() && act.getWindow() != null) {
                            act.getWindow().getDecorView().invalidate();
                        }
                    });
                }
            }
        }).start();
    }
}
