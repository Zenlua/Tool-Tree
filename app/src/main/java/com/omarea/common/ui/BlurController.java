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
import android.view.View;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import com.tool.tree.ThemeModeState;
import com.tool.tree.R;
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

    /**
     * Chụp ảnh màn hình hiện tại rồi blur (dùng khi directbg=1).
     * Thay vì chụp wallpaper hay solid color, chụp toàn bộ nội dung màn hình
     * (tắt tạm blur panel để tránh vòng lặp), sau đó đưa qua pipeline
     * blur bình thường (scale + RenderScript blur).
     */
    public void captureBackground(Activity activity) {
        final WeakReference<Activity> activityRef = new WeakReference<>(activity);

        // Bước 1: Chụp màn hình trên UI thread (View.draw() yêu cầu UI thread)
        activity.runOnUiThread(() -> {
            Activity act = activityRef.get();
            if (act == null || act.isFinishing() || act.isDestroyed()) return;

            View decorView = act.getWindow().getDecorView();
            int w = decorView.getWidth();
            int h = decorView.getHeight();
            if (w <= 0 || h <= 0) return;

            try {
                // Tạm tắt blur để chụp màn hình không bị lặp (feedback loop)
                boolean wasPaused = BlurEngine.isPaused;
                BlurEngine.isPaused = true;

                Bitmap screenshot = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(screenshot);
                decorView.draw(canvas);

                // Khôi phục trạng thái blur
                BlurEngine.isPaused = wasPaused;

                // Bước 2: Xử lý blur trên background thread
                final Bitmap captured = screenshot;
                new Thread(() -> {
                    Activity act2 = activityRef.get();
                    if (act2 == null || act2.isFinishing() || act2.isDestroyed()) {
                        captured.recycle();
                        return;
                    }

                    Context context = act2.getApplicationContext();

                    // Scale xuống 15% (giống logic wallpaper)
                    int scaledW = Math.max(Math.round(w * 0.15f), 1);
                    int scaledH = Math.max(Math.round(h * 0.15f), 1);
                    Bitmap scaled = Bitmap.createScaledBitmap(captured, scaledW, scaledH, false);
                    captured.recycle();

                    // Áp dụng blur RenderScript
                    Bitmap blurredResult = blurBitmap(context, scaled, 16f);
                    scaled.recycle();

                    if (blurredResult != null) {
                        BlurEngine.blurBitmap = blurredResult;
                        BlurEngine.isPaused = false;

                        act2.runOnUiThread(() -> {
                            if (act2 != null && !act2.isFinishing() && act2.getWindow() != null) {
                                act2.getWindow().getDecorView().invalidate();
                            }
                        });
                    }
                }).start();
            } catch (Exception e) {
                // Fallback: nếu chụp màn hình lỗi, tạo solid color bitmap
                BlurEngine.isPaused = false;
                captureBackgroundSolid(activityRef);
            }
        });
    }

    /**
     * Fallback: tạo bitmap solid color khi chụp màn hình thất bại.
     */
    private void captureBackgroundSolid(WeakReference<Activity> activityRef) {
        new Thread(() -> {
            Activity act = activityRef.get();
            if (act == null || act.isFinishing() || act.isDestroyed()) return;

            Context context = act.getApplicationContext();
            int bgColor = BlurEngine.directBgColor;

            int screenWidth = act.getResources().getDisplayMetrics().widthPixels;
            int screenHeight = act.getResources().getDisplayMetrics().heightPixels;
            int width = Math.max(Math.round(screenWidth * 0.15f), 1);
            int height = Math.max(Math.round(screenHeight * 0.15f), 1);

            Bitmap solidBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(solidBitmap);
            canvas.drawColor(bgColor);

            Bitmap blurredResult = blurBitmap(context, solidBitmap, 16f);

            if (blurredResult != null) {
                BlurEngine.blurBitmap = blurredResult;
                BlurEngine.isPaused = false;

                act.runOnUiThread(() -> {
                    if (act != null && !act.isFinishing() && act.getWindow() != null) {
                        act.getWindow().getDecorView().invalidate();
                    }
                });
            }
            solidBitmap.recycle();
        }).start();
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
                float contrastValue;
                if (ThemeModeState.isDarkMode()) {
                    contrastValue = 0.9f;
                    // Chế độ tối: giảm tương phản, làm ảnh dịu đi
                } else {
                    contrastValue = 1.2f;
                    // Chế độ sáng: tăng tương phản, làm ảnh tươi sáng
                }

                // A. Áp dụng Contrast
                Bitmap processedSource = adjustContrast(source, contrastValue);
                int width = Math.max(Math.round(processedSource.getWidth() * 0.15f), 1);
                int height = Math.max(Math.round(processedSource.getHeight() * 0.15f), 1);
                Bitmap scaledSource = Bitmap.createScaledBitmap(processedSource, width, height, false);
                Bitmap blurredResult = blurBitmap(context, scaledSource, 16f);

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
