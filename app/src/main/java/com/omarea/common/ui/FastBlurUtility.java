package com.omarea.common.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FastBlurUtility {

    private static final float SCALE_FACTOR = 0.125f; // Thu nhỏ 8 lần (giống code cũ 0.10f)
    private static final int BLUR_RADIUS = 8;
    
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface BlurCallback {
        void onBlurCompleted(Bitmap bitmap);
    }

    /**
     * Phương thức chính: Chụp và làm mờ không gây lag UI
     */
    public static void getBlurBackgroundAsync(Activity activity, BlurCallback callback) {
        // 1. Chụp màn hình ở dạng THU NHỎ ngay trên UI Thread (Rất nhanh, < 16ms)
        Bitmap smallBitmap = takeSmallScreenshot(activity);
        
        if (smallBitmap == null) {
            callback.onBlurCompleted(null);
            return;
        }

        // 2. Đẩy việc xử lý nặng (Blur + Dim) vào Background Thread
        executor.execute(() -> {
            // Làm mờ trên ảnh nhỏ
            Bitmap blurred = fastBlur(smallBitmap, BLUR_RADIUS);
            
            // Làm tối ảnh (giống getDimmedBitmap của code cũ)
            Bitmap dimmed = applyDim(blurred);
            
            // Phóng to ảnh bằng Matrix (giống hàm big() của code cũ)
            Bitmap finalBitmap = upscale(dimmed);

            // 3. Trả kết quả về Main Thread
            mainHandler.post(() -> callback.onBlurCompleted(finalBitmap));
        });
    }

    /**
     * Chụp màn hình và thu nhỏ ngay lập tức để tiết kiệm RAM và CPU
     */
    private static Bitmap takeSmallScreenshot(Activity activity) {
        try {
            View view = activity.getWindow().getDecorView();
            int width = view.getWidth();
            int height = view.getHeight();
            if (width <= 0 || height <= 0) return null;

            // Tạo bitmap nhỏ ngay từ đầu
            int smallW = Math.round(width * SCALE_FACTOR);
            int smallH = Math.round(height * SCALE_FACTOR);

            Bitmap bitmap = Bitmap.createBitmap(smallW, smallH, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            
            // Scale canvas để khi view.draw nó tự thu nhỏ lại
            canvas.scale(SCALE_FACTOR, SCALE_FACTOR);
            view.draw(canvas);
            
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Làm tối ảnh (Thay thế getDimmedBitmap)
     */
    private static Bitmap applyDim(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        ColorMatrix cm = new ColorMatrix();
        float contrast = 0.80f; // Giảm độ sáng giống code cũ
        cm.setScale(contrast, contrast, contrast, 1.0f);
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        
        canvas.drawBitmap(bitmap, 0, 0, paint);
        bitmap.recycle();
        return output;
    }

    /**
     * Phóng to ảnh (Thay thế hàm big())
     */
    private static Bitmap upscale(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        // Phóng to lại 8 lần (vì lúc đầu đã thu nhỏ 0.125)
        float scaleUp = 1f / SCALE_FACTOR;
        matrix.postScale(scaleUp, scaleUp);
        
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /**
     * Thuật toán StackBlur (Giữ nguyên từ code cũ của bạn)
     */
    private static Bitmap fastBlur(Bitmap sentBitmap, int radius) {
        Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        if (radius < 1) return null;

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
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

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
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) rinsum += sir[0]; else routsum += sir[0];
                if (i > 0) ginsum += sir[1]; else goutsum += sir[1];
                if (i > 0) binsum += sir[2]; else boutsum += sir[2];
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
                sir = stack[(stackpointer) % div];
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
                sir[0] = r[yi]; sir[1] = g[yi]; sir[2] = b[2];
                rbs = r1 - Math.abs(i);
                rsum += r[yi] * rbs; gsum += g[yi] * rbs; bsum += b[yi] * rbs;
                if (i > 0) rinsum += sir[0]; else routsum += sir[0];
                if (i > 0) ginsum += sir[1]; else goutsum += sir[1];
                if (i > 0) binsum += sir[2]; else boutsum += sir[2];
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
        sentBitmap.recycle();
        return bitmap;
    }
}
