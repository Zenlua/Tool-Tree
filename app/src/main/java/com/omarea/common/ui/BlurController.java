package com.omarea.common.ui;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
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
     * Tăng độ sáng cho Bitmap sử dụng ColorMatrix
     * @param brightness: 0 là bình thường, dương là sáng hơn (ví dụ: 40f)
     */
    private Bitmap adjustBrightness(Bitmap bitmap, float brightness) {
        if (bitmap == null) return null;
        
        Bitmap newBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(newBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        // Ma trận điều chỉnh độ sáng: Cộng thêm giá trị vào các kênh R, G, B
        ColorMatrix cm = new ColorMatrix(new float[] {
                1, 0, 0, 0, brightness,
                0, 1, 0, 0, brightness,
                0, 0, 1, 0, brightness,
                0, 0, 0, 1, 0
        });
        
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return newBitmap;
    }

    private Bitmap blurBitmap(Context context, Bitmap bitmap, float radius) {
        if (bitmap == null) return null;
        Bitmap outBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        RenderScript rs = RenderScript.create(context);
        try {
            Allocation input = Allocation.createFromBitmap(rs, bitmap);
            Allocation output = Allocation.createFromBitmap(rs, outBitmap);
            ScriptIntrinsicBlur intrinsicBlur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            intrinsicBlur.setRadius(radius);
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

            // 1. Lấy Wallpaper (Tùy chỉnh hoặc Hệ thống)
            File customWallpaperFile = new File(act.getFilesDir(), "home/etc/wallpaper.jpg");
            if (customWallpaperFile.exists()) {
                long currentLength = customWallpaperFile.length();
                long currentModified = customWallpaperFile.lastModified();

                if (currentLength == lastFileLength && currentModified == lastFileModified) {
                    if (BlurEngine.blurBitmap != null && !BlurEngine.blurBitmap.isRecycled()) return;
                }

                lastFileLength = currentLength;
                lastFileModified = currentModified;
                source = BitmapFactory.decodeFile(customWallpaperFile.getAbsolutePath());
            } else {
                WallpaperManager wm = WallpaperManager.getInstance(context);
                wm.forgetLoadedWallpaper(); 
                Drawable drawable = wm.getDrawable();
                if (drawable instanceof BitmapDrawable) {
                    source = ((BitmapDrawable) drawable).getBitmap();
                }
            }

            // 2. Xử lý làm sáng và làm mờ
            if (source != null) {
                // Bước A: Tăng độ sáng (Cộng 45 đơn vị để lớp mờ trông rực rỡ hơn)
                Bitmap brightSource = adjustBrightness(source, 45f);

                // Bước B: Scale nhỏ để tối ưu RAM
                int width = Math.max(brightSource.getWidth() / 4, 1);
                int height = Math.max(brightSource.getHeight() / 4, 1);
                Bitmap scaledSource = Bitmap.createScaledBitmap(brightSource, width, height, false);
                
                // Bước C: Blur bằng RenderScript
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
                
                // Giải phóng bitmap tạm để tránh tràn RAM
                if (brightSource != source) brightSource.recycle();
            }
        }).start();
    }
}
