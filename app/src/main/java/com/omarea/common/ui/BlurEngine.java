package com.omarea.common.ui;

import android.graphics.*;
import android.view.View;
import com.tool.tree.ThemeModeState;
import androidx.core.content.ContextCompat;
import com.tool.tree.R;
import android.content.Context;

public final class BlurEngine {
    // --- Các thành phần cũ để giữ tương thích hệ thống ---
    public static BlurController controller = new BlurController();
    public static volatile Bitmap blurBitmap;
    public static boolean isPaused = false;
    public static float DEFAULT_CORNER_RADIUS = 30.0f;
    
    // --- Cấu hình cho tính năng Blur thời gian thực (Lựa chọn 2) ---
    private static final float SCALE_FACTOR = 0.15f; 
    private static final int BLUR_RADIUS = 8;

    public float cornerRadius = DEFAULT_CORNER_RADIUS;

    private View targetView;
    private int[] location = new int[2];
    private Rect srcRect = new Rect();
    private Bitmap cachedBitmap;
    private Canvas cachedCanvas;
    private static Paint strokePaint;

    public BlurEngine(View view) {
        this.targetView = view;
    }

    public void setup() {
        if (cornerRadius > 0) {
            targetView.setOutlineProvider(new BlurOutlineProvider(cornerRadius));
            targetView.setClipToOutline(true);
        } else {
            targetView.setOutlineProvider(null);
            targetView.setClipToOutline(false);
        }
        targetView.getViewTreeObserver().addOnPreDrawListener(new BlurPreDrawListener(this, targetView));
    }

    /**
     * Logic mới: Chụp nội dung phía sau trực tiếp để không bị mất blur khi cuộn dài
     */
    public Bitmap getUpdatedBlurBitmap() {
        if (isPaused || targetView.getWidth() <= 0 || targetView.getHeight() <= 0) {
            return null;
        }

        View rootView = targetView.getRootView();
        if (rootView == null) return null;

        // 1. Tính toán kích thước thu nhỏ để tối ưu hiệu suất
        int w = Math.round(targetView.getWidth() * SCALE_FACTOR);
        int h = Math.round(targetView.getHeight() * SCALE_FACTOR);

        if (w <= 0 || h <= 0) return null;

        try {
            // 2. Quản lý bộ nhớ đệm Bitmap
            if (cachedBitmap == null || cachedBitmap.getWidth() != w || cachedBitmap.getHeight() != h) {
                if (cachedBitmap != null) cachedBitmap.recycle();
                cachedBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                cachedCanvas = new Canvas(cachedBitmap);
            }
        
            // 3. Xác định tọa độ thực tế trên màn hình
            targetView.getLocationOnScreen(location);

            // 4. CHỤP NỘI DUNG NỀN (Lựa chọn 2)
            cachedCanvas.save();
            cachedCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            
            // Di chuyển và scale canvas để "soi" đúng phần diện tích phía sau targetView
            cachedCanvas.scale(SCALE_FACTOR, SCALE_FACTOR);
            cachedCanvas.translate(-location[0], -location[1]);
            
            // Tạm ẩn chính nó để tránh vòng lặp hiển thị (Recursive drawing)
            targetView.setVisibility(View.INVISIBLE);
            rootView.draw(cachedCanvas);
            targetView.setVisibility(View.VISIBLE);
            
            cachedCanvas.restore();

            // 5. Làm mờ trực tiếp trên ảnh đã thu nhỏ
            fastBlur(cachedBitmap, BLUR_RADIUS);

            // 6. Phủ màu Tint theo theme (Dark/Light)
            cachedCanvas.drawColor(getBlurTintColor());
            
            return cachedBitmap;
        } catch (Exception e) {
            return null;
        }
    }

    private int getBlurTintColor() {
        Context context = targetView.getContext();
        int colorRes = ThemeModeState.isDarkMode() ? R.color.colorBlurDark : R.color.colorBlurLight;
        return ContextCompat.getColor(context, colorRes);
    }

    public static Paint getStrokePaint(Context context) {
        if (strokePaint == null) {
            strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(3.0f);
        }
        int colorRes = ThemeModeState.isDarkMode() ? R.color.colorPirmLight : R.color.colorPirmDark;
        int color = ContextCompat.getColor(context, colorRes);
        if (strokePaint.getColor() != color) strokePaint.setColor(color);
        return strokePaint;
    }

    /**
     * Thuật toán StackBlur tích hợp (Dùng xử lý ảnh nhỏ cực nhanh)
     */
    private void fastBlur(Bitmap bitmap, int radius) {
        if (radius < 1) return;
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int[] r = new int[wh];
        int[] g = new int[wh];
        int[] b = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int[] vmin = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int[] dv = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) dv[i] = (i / divsum);

        yw = yi = 0;
        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs; gsum += sir[1] * rbs; bsum += sir[2] * rbs;
                if (i > 0) { rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]; }
                else { routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]; }
            }
            stackpointer = radius;
            for (x = 0; x < w; x++) {
                r[yi] = dv[rsum]; g[yi] = dv[gsum]; b[yi] = dv[bsum];
                rsum -= routsum; gsum -= goutsum; bsum -= boutsum;
                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];
                routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2];
                if (y == 0) vmin[x] = Math.min(x + radius + 1, wm);
                p = pix[yw + vmin[x]];
                sir[0] = (p & 0xff0000) >> 16; sir[1] = (p & 0x00ff00) >> 8; sir[2] = (p & 0x0000ff);
                rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2];
                rsum += rinsum; gsum += ginsum; bsum += binsum;
                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer % div];
                routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2];
                rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2];
                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;
                sir = stack[i + radius];
                sir[0] = r[yi]; sir[1] = g[yi]; sir[2] = b[yi];
                rbs = r1 - Math.abs(i);
                rsum += r[yi] * rbs; gsum += g[yi] * rbs; bsum += b[yi] * rbs;
                if (i > 0) { rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]; }
                else { routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]; }
                if (i < hm) yp += w;
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];
                rsum -= routsum; gsum -= goutsum; bsum -= boutsum;
                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];
                routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2];
                if (x == 0) vmin[y] = Math.min(y + r1, hm) * w;
                p = x + vmin[y];
                sir[0] = r[p]; sir[1] = g[p]; sir[2] = b[p];
                rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2];
                rsum += rinsum; gsum += ginsum; bsum += binsum;
                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];
                routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2];
                rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2];
                yi += w;
            }
        }
        bitmap.setPixels(pix, 0, w, 0, 0, w, h);
    }

    public void destroy() {
        if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
            cachedBitmap.recycle();
            cachedBitmap = null;
        }
        cachedCanvas = null;
    }
}
