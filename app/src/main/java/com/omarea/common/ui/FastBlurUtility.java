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

    // Tỉ lệ thu nhỏ ảnh để xử lý nhanh (1/10 giúp giảm 100 lần số pixel cần tính toán)
    private static final float SCALE_FACTOR = 0.10f;
    private static final int BLUR_RADIUS = 8;

    /**
     * Chụp màn hình và làm mờ (Dùng làm phương án dự phòng khi không lấy được Wallpaper)
     */
    public static Bitmap getBlurBackgroundDrawer(Activity activity) {
        Bitmap bmp = takeScreenShot(activity);
        if (bmp == null) return null;

        Bitmap blurredBmp = startBlurBackground(bmp);

        // Giải phóng bitmap gốc sau khi đã blur xong
        if (bmp != blurredBmp && !bmp.isRecycled()) {
            bmp.recycle();
        }
        return blurredBmp;
    }

    /**
     * Quy trình xử lý: Thu nhỏ -> Làm mờ -> Phóng to & Nhuộm tối (Dim)
     * Đảm bảo mượt mà và an toàn bộ nhớ từ SDK 23 trở lên.
     */
    public static Bitmap startBlurBackground(Bitmap bkg) {
        if (bkg == null || bkg.isRecycled()) return null;

        int width = Math.round(bkg.getWidth() * SCALE_FACTOR);
        int height = Math.round(bkg.getHeight() * SCALE_FACTOR);

        // Đảm bảo kích thước tối thiểu để không làm hỏng thuật toán
        if (width < 2 || height < 2) return null;

        // 1. Thu nhỏ ảnh (Sử dụng bộ lọc Bilinear để ảnh mượt hơn)
        Bitmap smallBitmap = Bitmap.createScaledBitmap(bkg, width, height, true);

        // 2. Làm mờ bằng thuật toán StackBlur
        Bitmap blurred = fastBlur(smallBitmap, BLUR_RADIUS);

        // Giải phóng smallBitmap tạm thời tránh Memory Leak
        if (smallBitmap != null && smallBitmap != blurred && !smallBitmap.isRecycled()) {
            smallBitmap.recycle();
        }

        if (blurred == null) return null;

        // 3. Phóng to về kích thước gốc và áp dụng bộ lọc màu tối (scaleAndDim sẽ tự recycle 'blurred')
        return scaleAndDim(blurred, bkg.getWidth(), bkg.getHeight());
    }

    /**
     * Chụp ảnh màn hình an toàn trên SDK 23+
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
     * Phóng to ảnh và áp dụng ColorMatrix để làm tối nền (Dim)
     */
    private static Bitmap scaleAndDim(Bitmap bitmap, int targetW, int targetH) {
        if (bitmap == null || bitmap.isRecycled()) return null;

        Bitmap output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        // Paint với bộ lọc chống răng cưa và lọc bitmap khi scale
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);

        // Tạo bộ lọc màu để giảm độ sáng (contrast 0.80f ~ giảm 20% độ sáng)
        ColorMatrix cm = new ColorMatrix();
        float contrast = 0.80f;
        cm.set(new float[]{
                contrast, 0, 0, 0, 0,
                0, contrast, 0, 0, 0,
                0, 0, contrast, 0, 0,
                0, 0, 0, 1, 0});
        paint.setColorFilter(new ColorMatrixColorFilter(cm));

        // Vẽ ảnh từ vùng nguồn (nhỏ) ra vùng đích (toàn màn hình)
        Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        Rect dst = new Rect(0, 0, targetW, targetH);
        canvas.drawBitmap(bitmap, src, dst, paint);

        // Giải phóng bitmap tạm sau khi đã vẽ xong
        if (!bitmap.isRecycled()) {
            bitmap.recycle();
        }

        return output;
    }

    /**
     * Thuật toán StackBlur (Multi-pass box blur) - Đã sửa toàn bộ lỗi OutOfBoundsException & Leak
     */
    private static Bitmap fastBlur(Bitmap sentBitmap, int radius) {
        if (sentBitmap == null || sentBitmap.isRecycled()) return null;

        Bitmap.Config config = sentBitmap.getConfig();
        if (config == null) {
            config = Bitmap.Config.ARGB_8888;
        }

        Bitmap bitmap = sentBitmap.copy(config, true);
        if (bitmap == null) return null;

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w < 1 || h < 1) return null;

        // Giới hạn bán kính làm mờ không vượt quá kích thước ảnh
        int safeRadius = Math.min(radius, Math.min(w, h) - 1);
        if (safeRadius < 1) safeRadius = 1;

        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = safeRadius + safeRadius + 1;

        int[] r = new int[wh];
        int[] g = new int[wh];
        int[] b = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int[] vmin = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dvLimit = 256 * divsum;
        int[] dv = new int[dvLimit];
        for (i = 0; i < dvLimit; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;
        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = safeRadius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -safeRadius; i <= safeRadius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + safeRadius];
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
            stackpointer = safeRadius;

            for (x = 0; x < w; x++) {
                r[yi] = dv[clamp(rsum, 0, dvLimit - 1)];
                g[yi] = dv[clamp(gsum, 0, dvLimit - 1)];
                b[yi] = dv[clamp(bsum, 0, dvLimit - 1)];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - safeRadius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) vmin[x] = Math.min(x + safeRadius + 1, wm);
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

        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -safeRadius * w;
            for (i = -safeRadius; i <= safeRadius; i++) {
                yi = Math.min(wh - 1, Math.max(0, yp) + x);
                sir = stack[i + safeRadius];
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
                if (i < hm) yp += w;
            }
            yi = x;
            stackpointer = safeRadius;
            for (y = 0; y < h; y++) {
                int rVal = dv[clamp(rsum, 0, dvLimit - 1)];
                int gVal = dv[clamp(gsum, 0, dvLimit - 1)];
                int bVal = dv[clamp(bsum, 0, dvLimit - 1)];

                pix[yi] = (0xff000000 & pix[yi]) | (rVal << 16) | (gVal << 8) | bVal;

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - safeRadius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) vmin[y] = Math.min(y + r1, hm) * w;
                p = Math.min(wh - 1, x + vmin[y]);

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

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(val, max));
    }
}
