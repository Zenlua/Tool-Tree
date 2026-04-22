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
import com.tool.tree.ThemeModeState;
import java.io.File;
import java.lang.ref.WeakReference;

public class BlurController {

    private static long lastFileLength = -1;
    private static long lastFileModified = -1;

    /**
     * Điều chỉnh độ tương phản (Contrast) của Bitmap
     * @param contrast 1.2f cho chế độ sáng, 0.7f cho chế độ tối
     */
    private Bitmap adjustContrast(Bitmap bitmap, float contrast) {
        if (bitmap == null) return null;

        Bitmap out = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Công thức tính offset để giữ điểm xám trung tâm không bị lệch màu quá nhiều
        // offset = (1 - contrast) * 128
        float offset = (1f - contrast) * 128f;

        ColorMatrix cm = new ColorMatrix(new float[] {
                contrast, 0, 0, 0, offset,
                0, contrast, 0, 0, offset,
                0, 0, contrast, 0, offset,
                0, 0, 0, 1, 0
        });

        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return out;
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

            // 1. Lấy Wallpaper gốc
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

            // 2. Xử lý logic Theme và Hiệu ứng
            if (source != null) {
                // SỬ DỤNG IF ĐỂ CHECK THEME
                float contrastValue;
                if (ThemeModeState.isDarkMode()) {
                    contrastValue = 0.9f; // Chế độ tối: giảm tương phản, làm ảnh dịu đi
                } else {
                    contrastValue = 1.2f; // Chế độ sáng: tăng tương phản, làm ảnh tươi sáng
                }

                // A. Áp dụng Contrast
                Bitmap processedSource = adjustContrast(source, contrastValue);

                // B. Scale nhỏ để tối ưu (giảm 4 lần kích thước = giảm 16 lần RAM)
                int width = Math.max(processedSource.getWidth() / 4, 1);
                int height = Math.max(processedSource.getHeight() / 4, 1);
                Bitmap scaledSource = Bitmap.createScaledBitmap(processedSource, width, height, false);
                
                // C. Blur bằng RenderScript (Radius 15f là mức cân bằng tốt)
                Bitmap blurredResult = blurBitmap(context, scaledSource, 10f);

                if (blurredResult != null) {
                    BlurEngine.blurBitmap = blurredResult;
                    BlurEngine.isPaused = false;
                    
                    act.runOnUiThread(() -> {
                        if (act != null && !act.isFinishing() && act.getWindow() != null) {
                            act.getWindow().getDecorView().invalidate();
                        }
                    });
                }
                
                // Dọn dẹp bitmap trung gian để tránh rò rỉ bộ nhớ
                if (processedSource != source) {
                    processedSource.recycle();
                }
            }
        }).start();
    }
}
