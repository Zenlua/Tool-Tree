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
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.tool.tree.ThemeConfig; //

import java.io.File;

public class FastBlurUtility {

    // Tỉ lệ thu nhỏ ảnh để xử lý nhanh (giúp giảm tải cho CPU)
    private static final float SCALE_FACTOR = 0.10f;
    private static final int BLUR_RADIUS = 15;

    // Lưu vết để nhận diện thay đổi file
    private static long lastFileLength = -1;
    private static long lastFileModified = -1;

    /**
     * Quy trình chính: Kiểm tra ThemeMode -> Chọn nguồn ảnh -> Làm mờ
     */
    public static Bitmap getBlurBackgroundDrawer(Activity activity) {
        ThemeConfig themeConfig = new ThemeConfig(activity);
        int mode = themeConfig.getThemeMode(); //

        Bitmap sourceBmp = null;

        // 1. Nếu Mode >= 3: Tắt chụp màn hình, ưu tiên lấy từ file hoặc Wallpaper hệ thống
        if (mode >= 3) {
            sourceBmp = getCustomWallpaper(activity);
            
            if (sourceBmp == null) {
                sourceBmp = getSystemWallpaper(activity);
            }
        } 
        
        // 2. Nếu Mode < 3 hoặc không lấy được ảnh nền: Dùng phương án chụp màn hình
        if (sourceBmp == null) {
            sourceBmp = takeScreenShot(activity);
        }

        return startBlurBackground(sourceBmp);
    }

    /**
     * Kiểm tra và lấy ảnh từ file custom: home/etc/wallpaper.jpg
     */
    private static Bitmap getCustomWallpaper(Context context) {
        File file = new File(context.getFilesDir(), "home/etc/wallpaper.jpg");
        if (file.exists()) {
            long currentLength = file.length();
            long currentModified = file.lastModified();

            // Cập nhật thông tin file để nhận diện thay đổi lần sau
            lastFileLength = currentLength;
            lastFileModified = currentModified;

            return BitmapFactory.decodeFile(file.getAbsolutePath());
        }
        return null;
    }

    /**
     * Lấy hình nền mặc định của hệ thống
     */
    private static Bitmap getSystemWallpaper(Context context) {
        try {
            WallpaperManager wm = WallpaperManager.getInstance(context);
            Drawable drawable = wm.getDrawable();
            if (drawable instanceof BitmapDrawable) {
                return ((BitmapDrawable) drawable).getBitmap();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Quy trình xử lý: Thu nhỏ -> Làm mờ -> Phóng to & Nhuộm tối
     */
    public static Bitmap startBlurBackground(Bitmap bkg) {
        if (bkg == null || bkg.isRecycled()) return null;

        int width = Math.round(bkg.getWidth() * SCALE_FACTOR);
        int height = Math.round(bkg.getHeight() * SCALE_FACTOR);
        
        if (width <= 0 || height <= 0) return bkg;

        // Thu nhỏ để tối ưu hiệu năng
        Bitmap smallBitmap = Bitmap.createScaledBitmap(bkg, width, height, true);

        // Làm mờ bằng thuật toán StackBlur
        Bitmap blurred = fastBlur(smallBitmap, BLUR_RADIUS);

        // Phóng to về kích thước gốc và áp dụng bộ lọc màu tối (Dim)
        return scaleAndDim(blurred, bkg.getWidth(), bkg.getHeight());
    }

    private static Bitmap takeScreenShot(Activity activity) {
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

    private static Bitmap scaleAndDim(Bitmap bitmap, int targetW, int targetH) {
        Bitmap output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);

        // Làm tối nền (contrast 0.80f giúp nội dung Drawer nổi bật hơn)
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

        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }

        return output;
    }

    /**
     * Thuật toán StackBlur
     */
    private static Bitmap fastBlur(Bitmap sentBitmap, int radius) {
        if (sentBitmap == null || sentBitmap.isRecycled()) return null;
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
                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];
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
                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];
                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];
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
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];
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
