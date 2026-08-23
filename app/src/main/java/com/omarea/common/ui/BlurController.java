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
import android.util.LruCache;
import com.tool.tree.ThemeModeState;
import com.tool.tree.R;
import java.io.File;
import java.lang.ref.WeakReference;

public class BlurController {

    private static long lastFileLength = -1;
    private static long lastFileModified = -1;

    // RenderScript dùng chung riêng cho quầng mờ icon (createIconGlow) - tránh chi phí
    // RenderScript.create()/destroy() lặp lại cho từng icon khi 1 trang có nhiều item.
    // Khởi tạo bằng applicationContext (không phải Activity context) để không giữ tham chiếu
    // Activity và không cần destroy theo vòng đời Activity.
    private static volatile RenderScript sharedIconRenderScript;

    // Cache kết quả quầng mờ theo từng icon - cùng 1 icon (cùng ConstantState, cùng size/tỉ lệ/độ
    // mờ) sẽ dùng lại bitmap đã tính thay vì blur lại mỗi lần bind view.
    private static final LruCache<String, Bitmap> iconGlowCache = new LruCache<>(80);

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
     * RenderScript dùng chung cho createIconGlow() (không destroy - sống theo vòng đời app).
     * Chỉ dùng applicationContext để không leak Activity.
     */
    private RenderScript getSharedIconRenderScript(Context context) {
        RenderScript rs = sharedIconRenderScript;
        if (rs == null) {
            synchronized (BlurController.class) {
                rs = sharedIconRenderScript;
                if (rs == null) {
                    rs = RenderScript.create(context.getApplicationContext());
                    sharedIconRenderScript = rs;
                }
            }
        }
        return rs;
    }

    /**
     * Giống blurBitmap() nhưng dùng RenderScript dùng chung (getSharedIconRenderScript) thay vì
     * tạo/huỷ RenderScript riêng mỗi lần gọi - chỉ Allocation/Script tạm được huỷ sau mỗi lần.
     */
    private Bitmap blurBitmapShared(Context context, Bitmap bitmap, float radius) {
        if (bitmap == null) return null;
        Bitmap outBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        RenderScript rs = getSharedIconRenderScript(context);
        Allocation input = null;
        Allocation output = null;
        ScriptIntrinsicBlur intrinsicBlur = null;
        try {
            input = Allocation.createFromBitmap(rs, bitmap);
            output = Allocation.createFromBitmap(rs, outBitmap);
            intrinsicBlur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            intrinsicBlur.setRadius(radius);
            intrinsicBlur.setInput(input);
            intrinsicBlur.forEach(output);
            output.copyTo(outBitmap);
        } finally {
            if (input != null) input.destroy();
            if (output != null) output.destroy();
            if (intrinsicBlur != null) intrinsicBlur.destroy();
        }
        return outBitmap;
    }

    /**
     * Khoá cache cho createIconGlow(): dựa trên identity của ConstantState (icon cùng resource/
     * cùng Drawable thường dùng chung 1 ConstantState) + các tham số kích thước/độ mờ.
     * Icon load từ file tuỳ chỉnh (mỗi lần decode ra Drawable mới) sẽ có ConstantState khác nhau
     * mỗi lần -> không cache được, chỉ mất tối ưu chứ không sai kết quả.
     */
    private String buildIconGlowCacheKey(Drawable drawable, int iconSizePx, float overflowRatio, float blurRadius) {
        Drawable.ConstantState state = drawable.getConstantState();
        Object identity = state != null ? state : drawable;
        return System.identityHashCode(identity) + "_" + iconSizePx + "_" + overflowRatio + "_" + blurRadius;
    }

    /**
     * Chụp màu background solid (dùng khi directbg=1).
     * Thay vì đọc wallpaper, tạo một bitmap solid color từ BlurEngine.directBgColor,
     * rồi đưa qua pipeline blur bình thường (scale + RenderScript blur + contrast).
     */
    public void captureBackground(Activity activity) {
        final WeakReference<Activity> activityRef = new WeakReference<>(activity);

        new Thread(() -> {
            Activity act = activityRef.get();
            if (act == null || act.isFinishing() || act.isDestroyed()) return;

            Context context = act.getApplicationContext();
            int bgColor = BlurEngine.directBgColor;

            // Tạo bitmap solid color nhỏ (15% kích thước màn hình, giống logic wallpaper)
            int screenWidth = act.getResources().getDisplayMetrics().widthPixels;
            int screenHeight = act.getResources().getDisplayMetrics().heightPixels;
            int width = Math.max(Math.round(screenWidth * 0.15f), 1);
            int height = Math.max(Math.round(screenHeight * 0.15f), 1);

            Bitmap solidBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(solidBitmap);
            canvas.drawColor(bgColor);

            // Áp dụng contrast (giống wallpaper pipeline)
            float contrastValue;
            if (ThemeModeState.isDarkMode()) {
                contrastValue = 0.9f;
            } else {
                contrastValue = 1.2f;
            }
            Bitmap processedSource = adjustContrast(solidBitmap, contrastValue);

            // Áp dụng blur (giống wallpaper pipeline)
            Bitmap blurredResult = blurBitmap(context, processedSource, 16f);

            if (blurredResult != null) {
                BlurEngine.blurBitmap = blurredResult;
                BlurEngine.isPaused = false;

                act.runOnUiThread(() -> {
                    if (act != null && !act.isFinishing() && act.getWindow() != null) {
                        act.getWindow().getDecorView().invalidate();
                    }
                });
            }

            // Dọn dẹp
            if (processedSource != solidBitmap) {
                processedSource.recycle();
            }
            solidBitmap.recycle();
        }).start();
    }

    /**
     * Tạo "quầng mờ" (glow) phía sau 1 icon: vẽ icon gốc lên bitmap phóng to nhẹ (tràn ra ngoài
     * khung icon gốc), rồi làm mờ - GIỮ NGUYÊN màu thật của icon (không chỉnh contrast/tint).
     * Dùng để thay thế nền đen phẳng phía sau icon khi bật directbg=1
     * (xem com.omarea.krscript.ui.ListItemClickable).
     *
     * @param context       Context để tạo RenderScript.
     * @param iconDrawable  Drawable gốc của icon (không bị thay đổi/mutate ngoài ý muốn).
     * @param iconSizePx    Kích thước (px) của khung icon gốc (hình vuông).
     * @param overflowRatio Tỉ lệ phóng to so với icon gốc, ví dụ 1.25f = tràn ra ngoài 25%.
     * @param blurRadius    Bán kính blur, tự động giới hạn trong khoảng 0f..25f (giới hạn của RenderScript).
     * @return Bitmap quầng mờ đã blur (kích thước = round(iconSizePx * overflowRatio)), hoặc null nếu lỗi.
     */
    public Bitmap createIconGlow(Context context, Drawable iconDrawable, int iconSizePx, float overflowRatio, float blurRadius) {
        if (iconDrawable == null || iconSizePx <= 0) return null;

        String cacheKey = buildIconGlowCacheKey(iconDrawable, iconSizePx, overflowRatio, blurRadius);
        Bitmap cached = iconGlowCache.get(cacheKey);
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }

        try {
            int glowSize = Math.max(Math.round(iconSizePx * overflowRatio), 1);

            Bitmap source = Bitmap.createBitmap(glowSize, glowSize, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(source);

            // Vẽ trên 1 bản sao độc lập của drawable để không đụng tới bounds của icon đang
            // hiển thị thật trên ImageView (iconDrawable có thể đang được dùng ở nơi khác).
            Drawable drawableToDraw = iconDrawable.getConstantState() != null
                    ? iconDrawable.getConstantState().newDrawable().mutate()
                    : iconDrawable;
            drawableToDraw.setBounds(0, 0, glowSize, glowSize);
            drawableToDraw.draw(canvas);

            float radius = Math.max(0f, Math.min(blurRadius, 25f));
            Bitmap blurred = blurBitmapShared(context, source, radius);

            if (blurred != source) {
                source.recycle();
            }
            if (blurred != null) {
                iconGlowCache.put(cacheKey, blurred);
            }
            return blurred;
        } catch (Exception e) {
            return null;
        }
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