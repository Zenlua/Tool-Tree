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
import androidx.core.content.ContextCompat;
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

            // Chỉ khi bật hẳn chế độ directbg (home/usr/log/directbg = 1, không phải trường hợp
            // fallback do Live Wallpaper không chụp được) mới vẽ thêm icon app phóng to, mờ nhạt,
            // lệch góc lên trên nền màu - để sau khi qua pipeline blur nặng bên dưới, thay vì chỉ
            // còn 1 màu phẳng lì đơn điệu thì sẽ có thêm chút mảng sáng/tối mềm mại theo hình icon,
            // nhìn có chiều sâu/đẹp hơn hẳn mà vẫn không lộ hình dạng icon rõ ràng.
            if (BlurEngine.isDirectBgMode) {
                drawIconAccent(context, canvas, width, height);
            }

            // Áp dụng contrast (giống wallpaper pipeline)
            float contrastValue;
            if (ThemeModeState.isDarkMode()) {
                contrastValue = 0.9f;
            } else {
                contrastValue = 1.2f;
            }
            Bitmap processedSource = adjustContrast(solidBitmap, contrastValue);

            // Áp dụng blur - dùng radius nhẹ hơn 1 chút so với pipeline wallpaper (16f) vì
            // canvas ở đây rất nhỏ nên cùng 1 radius sẽ blur MẠNH HƠN nhiều lần so với ảnh
            // wallpaper thật (vốn đã được scale nhỏ dần từ ảnh gốc lớn hơn hẳn) - hạ xuống 10f
            // để hình khối icon (nếu có vẽ ở trên) còn sót lại rõ sau khi blur, không bị "xoá
            // sạch" thành màu phẳng như trước.
            Bitmap blurredResult = blurBitmap(context, processedSource, 10f);

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
     * Vẽ icon app phóng to, mờ nhạt, lệch góc lên trên canvas nền màu - chỉ dùng cho pipeline
     * "directbg" (xem captureBackground()). Vì canvas ở đây rất nhỏ (15% màn hình) và còn bị
     * blur ngay sau đó (radius 10, xem captureBackground), cần cân bằng 2 yếu tố ngược nhau:
     *  - Icon không được phóng quá to (zoom quá sâu) - nếu không sẽ chỉ còn "chụp" trúng 1 mảng
     *    màu nền phẳng của icon (nhiều icon adaptive có lớp nền màu đặc, hoạ tiết glyph chỉ nằm
     *    ở vùng an toàn giữa icon) thay vì giữ được hình khối đặc trưng để blur tạo hoạ tiết.
     *  - Icon phải đủ đậm (alpha đủ cao) mới còn sót lại sau khi blur nặng, chứ không tan biến
     *    hoàn toàn vào màu nền khiến nhìn không khác gì màu phẳng như trước.
     */
    private void drawIconAccent(Context context, Canvas canvas, int width, int height) {
        try {
            Drawable icon = ContextCompat.getDrawable(context, R.mipmap.ic_launcher);
            if (icon == null) return;

            // Chỉ phóng to vừa phải (~1.4 lần cạnh dài nhất) - đủ để tràn nhẹ ra ngoài canvas
            // (tạo bố cục lệch góc tự nhiên) nhưng vẫn giữ được phần lớn hình khối/glyph đặc
            // trưng của icon trong khung hình, thay vì zoom quá sâu chỉ còn màu nền phẳng
            int iconSize = Math.round(Math.max(width, height) * 1.4f);

            // Lệch nhẹ về góc trên-phải
            int left = Math.round(width * 0.12f);
            int top = -Math.round(height * 0.12f);
            icon = icon.mutate();
            icon.setBounds(left, top, left + iconSize, top + iconSize);

            // Alpha đủ đậm (~70%) để hình khối icon còn sót lại rõ sau khi qua blur - alpha
            // quá thấp (như 43% trước đây) gần như tan biến hết, nhìn không khác gì màu nền
            // phẳng ban đầu
            icon.setAlpha(180);
            icon.draw(canvas);
        } catch (Exception ignored) {
            // Không lấy được icon (thiết bị/launcher lạ, icon adaptive vẽ lỗi...) -> bỏ qua,
            // giữ nguyên nền màu phẳng như trước đây, không làm crash cả pipeline blur
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