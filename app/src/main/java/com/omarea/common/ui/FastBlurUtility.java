package com.omarea.common.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.HardwareRenderer;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.hardware.HardwareBuffer;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.view.View;

public class FastBlurUtility {

    private static final float SCALE_FACTOR = 0.10f;
    private static final int BLUR_RADIUS = 8;

    public static Bitmap getBlurBackgroundDrawer(Activity activity) {
        Bitmap bmp = takeScreenShot(activity);
        if (bmp == null) return null;

        Bitmap blurredBmp = startBlurBackground(bmp);

        if (bmp != blurredBmp && !bmp.isRecycled()) {
            bmp.recycle();
        }

        return blurredBmp;
    }

    public static Bitmap startBlurBackground(Bitmap bkg) {
        if (bkg == null || bkg.isRecycled()) return null;

        try {
            // 1. Android 12+ (API 31+): Dùng GPU RenderEffect
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Bitmap gpuBlurred = blurWithRenderEffect(bkg, 20f);
                if (gpuBlurred != null) {
                    return applyDimFilter(gpuBlurred);
                }
            }

            // 2. Android 11 trở xuống (hoặc nếu GPU fail): Dùng StackBlur CPU
            int width = Math.round(bkg.getWidth() * SCALE_FACTOR);
            int height = Math.round(bkg.getHeight() * SCALE_FACTOR);

            if (width <= 0 || height <= 0) return bkg;

            Bitmap smallBitmap = Bitmap.createScaledBitmap(bkg, width, height, true);
            Bitmap blurred = fastBlur(smallBitmap, BLUR_RADIUS);

            if (smallBitmap != null && smallBitmap != blurred && !smallBitmap.isRecycled()) {
                smallBitmap.recycle();
            }

            if (blurred == null) return bkg;

            return scaleAndDim(blurred, bkg.getWidth(), bkg.getHeight());
        } catch (Throwable e) {
            // An toàn tuyệt đối: Nếu có bất kỳ lỗi toán học hay OOM nào, trả lại ảnh gốc
            return bkg;
        }
    }

    private static Bitmap blurWithRenderEffect(Bitmap input, float radius) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null;

        try {
            int width = input.getWidth();
            int height = input.getHeight();

            RenderNode node = new RenderNode("BlurEffectNode");
            node.setPosition(0, 0, width, height);
            node.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP));

            Canvas canvas = node.beginRecording(width, height);
            canvas.drawBitmap(input, 0, 0, null);
            node.endRecording();

            ImageReader imageReader = ImageReader.newInstance(
                    width, height, PixelFormat.RGBA_8888, 1, HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE | HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
            );

            HardwareRenderer renderer = new HardwareRenderer();
            renderer.setSurface(imageReader.getSurface());
            renderer.setContentRoot(node);
            renderer.createRenderRequest().syncAndDraw();

            Image image = imageReader.acquireNextImage();
            if (image == null) {
                renderer.destroy();
                imageReader.close();
                return null;
            }

            HardwareBuffer hardwareBuffer = image.getHardwareBuffer();
            Bitmap result = null;
            if (hardwareBuffer != null) {
                result = Bitmap.wrapHardwareBuffer(hardwareBuffer, null);
                hardwareBuffer.close();
            }

            image.close();
            imageReader.close();
            renderer.destroy();

            if (result != null) {
                Bitmap copy = result.copy(Bitmap.Config.ARGB_8888, true);
                result.recycle();
                return copy;
            }
        } catch (Exception e) {
            // Fallback nếu GPU không hỗ trợ
        }
        return null;
    }

    private static Bitmap applyDimFilter(Bitmap src) {
        Bitmap output = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
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

        canvas.drawBitmap(src, 0, 0, paint);
        if (!src.isRecycled()) {
            src.recycle();
        }
        return output;
    }

    public static Bitmap takeScreenShot(Activity activity) {
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

    private static Bitmap scaleAndDim(Bitmap bitmap, int targetW, int targetH) {
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

    // Hàm hỗ trợ khóa chỉ số mảng an toàn
    private static int clampIndex(int val, int maxLen) {
        if (val < 0) return 0;
        if (val >= maxLen) return maxLen - 1;
        return val;
    }

    private static Bitmap fastBlur(Bitmap sentBitmap, int radius) {
        if (sentBitmap == null || sentBitmap.isRecycled() || radius < 1) return null;

        Bitmap bitmap = sentBitmap.isMutable() ? sentBitmap : sentBitmap.copy(sentBitmap.getConfig(), true);
        if (bitmap == null) return null;

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w <= 0 || h <= 0) return null;

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
                r[yi] = dv[clampIndex(rsum, dv.length)];
                g[yi] = dv[clampIndex(gsum, dv.length)];
                b[yi] = dv[clampIndex(bsum, dv.length)];

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
                sir = stack[stackpointer % div];

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
                // ĐÃ SỬA: Kiểm tra giới hạn hàng dựa trên kích thước thực hm * w
                if (yp < (hm * w)) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                pix[yi] = (0xff000000 & pix[yi]) 
                        | (dv[clampIndex(rsum, dv.length)] << 16) 
                        | (dv[clampIndex(gsum, dv.length)] << 8) 
                        | dv[clampIndex(bsum, dv.length)];

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
                sir = stack[stackpointer % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

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
