package com.omarea.common.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FastBlurUtility {

    private static final float SCALE_FACTOR = 0.10f;
    private static final int BLUR_RADIUS = 10;
    
    // Executor để xử lý các tác vụ nặng ngoài UI Thread
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface BlurCallback {
        void onBlurCompleted(Bitmap bitmap);
    }

    /**
     * Phương thức chính: Chụp và làm mờ không gây lag UI
     */
    public static void getBlurBackgroundAsync(Activity activity, BlurCallback callback) {
        // 1. Chụp màn hình phải thực hiện trên UI Thread
        Bitmap screenshot = takeScreenShot(activity);
        
        if (screenshot == null) {
            callback.onBlurCompleted(null);
            return;
        }

        // 2. Đẩy việc xử lý làm mờ vào Background Thread
        executor.execute(() -> {
            Bitmap blurred = startBlurProcess(screenshot);
            
            // 3. Trả kết quả về Main Thread qua Callback
            mainHandler.post(() -> callback.onBlurCompleted(blurred));
        });
    }

    private static Bitmap startBlurProcess(Bitmap bkg) {
        if (bkg == null || bkg.isRecycled()) return null;

        int originWidth = bkg.getWidth();
        int originHeight = bkg.getHeight();

        // Tính toán kích thước thu nhỏ
        int width = Math.round(originWidth * SCALE_FACTOR);
        int height = Math.round(originHeight * SCALE_FACTOR);
        
        if (width <= 0 || height <= 0) return bkg;

        // Thu nhỏ ảnh
        Bitmap smallBitmap = Bitmap.createScaledBitmap(bkg, width, height, true);
        
        // Giải phóng ảnh gốc ngay để tiết kiệm RAM
        if (bkg != smallBitmap) {
            bkg.recycle();
        }

        // Làm mờ bằng thuật toán StackBlur
        Bitmap blurred = fastBlur(smallBitmap, BLUR_RADIUS);

        // Phóng to và làm tối
        return scaleAndDim(blurred, originWidth, originHeight);
    }

    private static Bitmap takeScreenShot(Activity activity) {
        try {
            View view = activity.getWindow().getDecorView();
            if (view.getWidth() <= 0 || view.getHeight() <= 0) return null;

            // Dùng RGB_565 để tiết kiệm 50% bộ nhớ so với ARGB_8888 (phù hợp SDK 23+)
            Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            view.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    private static Bitmap scaleAndDim(Bitmap bitmap, int targetW, int targetH) {
        Bitmap output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);

        // Giảm độ sáng 20%
        ColorMatrix cm = new ColorMatrix();
        float contrast = 0.8f; 
        cm.setScale(contrast, contrast, contrast, 1.0f);
        paint.setColorFilter(new ColorMatrixColorFilter(cm));

        Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        Rect dst = new Rect(0, 0, targetW, targetH);
        canvas.drawBitmap(bitmap, src, dst, paint);

        if (!bitmap.isRecycled()) {
            bitmap.recycle();
        }

        return output;
    }

    /**
     * Thuật toán StackBlur tối ưu hóa
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
                if (i > 0) {
                    rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2];
                } else {
                    routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2];
                }
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
                sir[0] = r[yi]; sir[1] = g[yi]; sir[2] = b[yi];
                rbs = r1 - Math.abs(i);
                rsum += r[yi] * rbs; gsum += g[yi] * rbs; bsum += b[yi] * rbs;
                if (i > 0) {
                    rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2];
                } else {
                    routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2];
                }
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
        sentBitmap.recycle(); // Giải phóng bitmap cũ sau khi copy
        return bitmap;
    }
}
