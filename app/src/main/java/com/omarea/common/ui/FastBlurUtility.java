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
    // Alias public để nơi khác (VD: DialogHelper khi tự chụp screenshot trên main thread rồi
    // đẩy phần blur sang background thread) biết chụp ở đúng tỉ lệ mà blurCapturedBitmap() cần.
    public static final float CAPTURE_SCALE = SCALE_FACTOR;
    private static final int BLUR_RADIUS = 8;
    // Bán kính blur GPU tương ứng với ảnh full-res gốc là 20f; vì giờ blur được thực hiện
    // trên ảnh đã thu nhỏ theo SCALE_FACTOR nên bán kính cũng phải thu nhỏ theo tỉ lệ tương ứng
    // để giữ hiệu ứng mờ tương đương sau khi phóng to lại (kỹ thuật downsample-blur-upsample).
    private static final float GPU_BLUR_RADIUS = 20f * SCALE_FACTOR;

    // Bảng tra cứu (lookup table) dùng trong StackBlur chỉ phụ thuộc vào bán kính blur.
    // Vì BLUR_RADIUS luôn là hằng số, bảng này được tính đúng MỘT LẦN khi class được load,
    // thay vì cấp phát mảng mới (256 * divsum phần tử, ~80KB) và tính lại mỗi lần fastBlur()
    // chạy - đây từng là một nguồn gây lag/rác GC không cần thiết.
    private static final int[] DV_LOOKUP = buildLookupTable(BLUR_RADIUS);

    private static int[] buildLookupTable(int radius) {
        int div = radius + radius + 1;
        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int[] dv = new int[256 * divsum];
        for (int i = 0; i < dv.length; i++) {
            dv[i] = i / divsum;
        }
        return dv;
    }

    public static Bitmap getBlurBackgroundDrawer(Activity activity) {
        if (activity == null || activity.isFinishing()) return null;

        View decorView;
        int targetW, targetH;
        try {
            decorView = activity.getWindow().getDecorView();
            targetW = decorView.getWidth();
            targetH = decorView.getHeight();
        } catch (Throwable e) {
            return null;
        }
        if (targetW <= 0 || targetH <= 0) return null;

        // Chụp trực tiếp ở độ phân giải đã thu nhỏ (SCALE_FACTOR) thay vì chụp screenshot
        // full-res rồi mới scale xuống như trước. Đây là nguồn gây lag lớn nhất trước đây:
        // - view.draw() vào một Bitmap full màn hình rất tốn bộ nhớ + thời gian rasterize.
        // - Với path GPU (Android 12+), phải copy toàn bộ hardware buffer kích thước màn
        //   hình gốc từ GPU sang software bitmap (result.copy(ARGB_8888, true)) - rất chậm.
        // Chụp ảnh nhỏ ngay từ đầu giúp mọi bước xử lý phía sau (vẽ, copy, blur) đều nhanh hơn
        // nhiều lần vì chỉ phải xử lý ~1% số pixel so với trước.
        Bitmap smallBmp = takeScreenShot(activity, SCALE_FACTOR);
        if (smallBmp == null) return null;

        try {
            return blurCapturedBitmap(smallBmp, targetW, targetH);
        } catch (Throwable e) {
            // An toàn tuyệt đối: nếu có lỗi toán học hay OOM, không có ảnh để trả về
            if (!smallBmp.isRecycled()) smallBmp.recycle();
            return null;
        }
    }

    /**
     * Giữ lại cho tương thích ngược: xử lý blur từ một bitmap full-res có sẵn (ví dụ do nơi
     * khác tự chụp screenshot rồi truyền vào). Nếu có thể, nên dùng getBlurBackgroundDrawer()
     * để tận dụng việc chụp ảnh trực tiếp ở kích thước nhỏ.
     */
    public static Bitmap startBlurBackground(Bitmap bkg) {
        if (bkg == null || bkg.isRecycled()) return null;

        try {
            int targetW = bkg.getWidth();
            int targetH = bkg.getHeight();

            int width = Math.max(1, Math.round(targetW * SCALE_FACTOR));
            int height = Math.max(1, Math.round(targetH * SCALE_FACTOR));

            Bitmap smallBitmap = Bitmap.createScaledBitmap(bkg, width, height, true);
            if (smallBitmap == null) return bkg;

            return blurCapturedBitmap(smallBitmap, targetW, targetH);
        } catch (Throwable e) {
            // An toàn tuyệt đối: Nếu có bất kỳ lỗi toán học hay OOM nào, trả lại ảnh gốc
            return bkg;
        }
    }

    /**
     * Thực hiện blur trên một bitmap ĐÃ được thu nhỏ sẵn (theo CAPTURE_SCALE), rồi phóng to +
     * làm tối (dim) về đúng kích thước targetW x targetH. Bitmap đầu vào sẽ bị recycle bởi
     * hàm này (hoặc bởi các hàm con của nó) sau khi dùng xong.
     *
     * Hàm này KHÔNG đụng tới View hierarchy nên an toàn để gọi trên background thread - khác
     * với takeScreenShot()/takeScreenShot(activity, scale), vốn bắt buộc phải chạy trên main
     * thread vì cần view.draw(). Cách dùng khuyến khích cho nơi cần tránh block main thread:
     *   1. Trên main thread: val small = FastBlurUtility.takeScreenShot(activity, FastBlurUtility.CAPTURE_SCALE)
     *   2. Trên background thread: val blurred = FastBlurUtility.blurCapturedBitmap(small, targetW, targetH)
     *   3. Post kết quả về main thread để set background.
     */
    public static Bitmap blurCapturedBitmap(Bitmap smallBmp, int targetW, int targetH) {
        // 1. Android 12+ (API 31+): Dùng GPU RenderEffect trên ảnh nhỏ (rất nhanh)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Bitmap gpuBlurred = blurWithRenderEffect(smallBmp, GPU_BLUR_RADIUS);
            if (gpuBlurred != null) {
                if (!smallBmp.isRecycled()) {
                    smallBmp.recycle();
                }
                return scaleAndDim(gpuBlurred, targetW, targetH);
            }
        }

        // 2. Android 11 trở xuống (hoặc nếu GPU fail): Dùng StackBlur CPU
        Bitmap blurred = fastBlur(smallBmp, BLUR_RADIUS);

        if (blurred == null) {
            // fastBlur thất bại: vẫn cần trả về đúng kích thước, dùng luôn ảnh nhỏ chưa blur
            return scaleAndDim(smallBmp, targetW, targetH);
        }

        return scaleAndDim(blurred, targetW, targetH);
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

    public static Bitmap takeScreenShot(Activity activity) {
        return takeScreenShot(activity, 1f);
    }

    /**
     * Chụp screenshot của decor view, có thể vẽ trực tiếp ở tỉ lệ thu nhỏ (scale < 1f) để
     * tránh phải cấp phát + rasterize một Bitmap full màn hình rồi mới scale xuống sau đó.
     */
    public static Bitmap takeScreenShot(Activity activity, float scale) {
        if (activity == null || activity.isFinishing()) return null;
        try {
            View view = activity.getWindow().getDecorView();
            int viewWidth = view.getWidth();
            int viewHeight = view.getHeight();
            if (viewWidth <= 0 || viewHeight <= 0) return null;

            int width = Math.max(1, Math.round(viewWidth * scale));
            int height = Math.max(1, Math.round(viewHeight * scale));

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            if (scale != 1f) {
                canvas.scale(scale, scale);
            }
            view.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    private static void applyDimPaint(Paint paint) {
        ColorMatrix cm = new ColorMatrix();
        float contrast = 0.80f;
        cm.set(new float[]{
                contrast, 0, 0, 0, 0,
                0, contrast, 0, 0, 0,
                0, 0, contrast, 0, 0,
                0, 0, 0, 1, 0});
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
    }

    private static Bitmap scaleAndDim(Bitmap bitmap, int targetW, int targetH) {
        Bitmap output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        applyDimPaint(paint);

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

        // Dùng bảng tra cứu đã cache sẵn khi radius trùng với BLUR_RADIUS (trường hợp luôn
        // xảy ra trong thực tế); chỉ build lại nếu có ai đó gọi fastBlur với radius khác.
        int[] dv = (radius == BLUR_RADIUS) ? DV_LOOKUP : buildLookupTable(radius);

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
                // Kiểm tra giới hạn hàng dựa trên kích thước thực hm * w
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
