package com.omarea.common.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;

public class BlurUtils {
    public static Bitmap captureScreen(Activity activity) {
        try {
            View view = activity.getWindow().getDecorView();
            view.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);
            return bitmap;
        } catch (Exception e) { return null; }
    }

    public static Bitmap stackBlur(Bitmap bitmap, int radius) {
        if (radius < 1) return null;
        Bitmap res = bitmap.copy(bitmap.getConfig(), true);
        int w = res.getWidth();
        int h = res.getHeight();
        int[] pix = new int[w * h];
        res.getPixels(pix, 0, w, 0, 0, w, h);

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
        int stackpointer, stackstart, rbs, r1 = radius + 1;
        int routsum, goutsum, boutsum, rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                int[] sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0xff00) >> 8;
                sir[2] = (p & 0xff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) { rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]; }
                else { routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]; }
            }
            stackpointer = radius;
            for (x = 0; x < w; x++) {
                r[yi] = dv[rsum]; g[yi] = dv[gsum]; b[yi] = dv[bsum];
                rsum -= routsum; gsum -= goutsum; bsum -= boutsum;
                stackstart = stackpointer - radius + div;
                int[] sir = stack[stackstart % div];
                routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2];
                if (y == 0) vmin[x] = Math.min(x + radius + 1, wm);
                p = pix[yw + vmin[x]];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0xff00) >> 8;
                sir[2] = (p & 0xff);
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
        res.setPixels(pix, 0, w, 0, 0, w, h);
        return res;
    }
}
