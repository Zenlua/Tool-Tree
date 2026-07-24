package com.omarea.common.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

public class FastBlurUtility {

    private static final float SCALE_FACTOR = 0.10f;
    private static final int BLUR_RADIUS = 8;

    /**
     * Chụp màn hình và làm mờ (Dùng làm phương án dự phòng khi không lấy được Wallpaper)
     */
    public static Bitmap getBlurBackgroundDrawer(Activity activity) {
        Bitmap bmp = takeScreenShot(activity);
        if (bmp == null) return null;

        Bitmap blurredBmp = startBlurBackground(bmp);

        if (bmp != blurredBmp && !bmp.isRecycled()) {
            bmp.recycle();
        }
        return blurredBmp;
    }

    /**
     * Quy trình xử lý: Thu nhỏ -> Làm mờ -> Phóng to & Dim
     */
    public static Bitmap startBlurBackground(Bitmap bkg) {
        if (bkg == null || bkg.isRecycled()) return null;

        // Đảm bảo kích thước tối thiểu 16px để tránh ảnh bị quá nhỏ gây mờ đục hoặc hỏng layout
        int width = Math.max(Math.round(bkg.getWidth() * SCALE_FACTOR), 16);
        int height = Math.max(Math.round(bkg.getHeight() * SCALE_FACTOR), 16);

        Bitmap smallBitmap = Bitmap.createScaledBitmap(bkg, width, height, true);

        Bitmap blurred = fastBlur(smallBitmap, BLUR_RADIUS);

        if (smallBitmap != null && smallBitmap != blurred && !smallBitmap.isRecycled()) {
            smallBitmap.recycle();
        }

        if (blurred == null) return null;

        return scaleAndDim(blurred, bkg.getWidth(), bkg.getHeight());
    }

    /**
     * Chụp ảnh màn hình an toàn
     */
    private static Bitmap takeScreenShot(Activity activity) {
        if (activity == null || activity.isFinishing()) return null;
        try {
            View view = activity.getWindow().getDecorView();
            if (view.getWidth() <= 0 || view.getHeight() <= 0) return null;

            Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            view.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Phóng to ảnh và áp dụng ColorMatrix làm tối (Dim)
     */
    private static Bitmap scaleAndDim(Bitmap bitmap, int targetW, int targetH) {
        if (bitmap == null || bitmap.isRecycled()) return null;

        Bitmap output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);

        ColorMatrix cm = new ColorMatrix();
        float contrast = 0.80f; 
        cm.set(new float[]{
                contrast, 0, 0, 0, 0,
                0, contrast, 0, 0, 0,
                0, 0, contrast, 0, 0,
                0, 0, 0, 1, 0});
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
     * Thuật toán StackBlur đã sửa lỗi hổng chỉ số mảng & giữ nguyên 100% chất lượng Blur gốc
     */
    private static Bitmap fastBlur(Bitmap sentBitmap, int radius) {
        if (sentBitmap == null || sentBitmap.isRecycled()) return null;

        Bitmap.Config config = sentBitmap.getConfig();
        if (config == null) {
            config = Bitmap.Config.ARGB_8888;
        }

        Bitmap bitmap = sentBitmap.copy(config, true);
        if (bitmap == null) return null;

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
        int rsum, gsum, bsum, x, y, i, p, yi, yw;
        int[] vmin = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int maxDv = 256 * divsum;
        int[] dv = new int[maxDv];
        for (i = 0; i < maxDv; i++) {
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

        // --- LƯỢT 1: LÀM MỜ CHIỀU NGANG ---
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
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {
                r[yi] = dv[Math.min(Math.max(rsum, 0), maxDv - 1)];
                g[yi] = dv[Math.min(Math.max(gsum, 0), maxDv - 1)];
                b[yi] = dv[Math.min(Math.max(bsum, 0), maxDv - 1)];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) vmin[x] = Math.min(x + radius + 1, wm);
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }

        // --- LƯỢT 2: LÀM MỜ CHIỀU DỌC ---
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;

            // ĐÃ SỬA LỖI: Khởi tạo stack chuẩn xác bằng Math.min(hm, Math.max(i, 0)) * w + x
            for (i = -radius; i <= radius; i++) {
                yi = Math.min(hm, Math.max(i, 0)) * w + x;
                sir = stack[i + radius];
                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);
                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }

            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                pix[yi] = (0xff000000 & pix[yi])
                        | (dv[Math.min(Math.max(rsum, 0), maxDv - 1)] << 16)
                        | (dv[Math.min(Math.max(gsum, 0), maxDv - 1)] << 8)
                        | dv[Math.min(Math.max(bsum, 0), maxDv - 1)];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) vmin[y] = Math.min(y + r1, hm) * w;
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h);
        return bitmap;
    }
}
