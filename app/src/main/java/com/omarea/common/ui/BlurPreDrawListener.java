package com.omarea.common.ui;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewTreeObserver;

public class BlurPreDrawListener implements ViewTreeObserver.OnPreDrawListener {
    // Giới hạn tần suất vẽ lại tối đa (~30fps) để giảm tải cho máy cấu hình thấp
    // khi view đang thực sự di chuyển (cuộn/animate) liên tục.
    private static final long MIN_INTERVAL_MS = 32L;

    private View targetView;
    private BlurEngine engine;

    // Lưu lại trạng thái của lần invalidate gần nhất để phát hiện "có gì thay đổi
    // thật sự hay không" trước khi quyết định vẽ lại.
    private Bitmap lastBitmap;
    private int lastX = Integer.MIN_VALUE;
    private int lastY = Integer.MIN_VALUE;
    private long lastInvalidateAt = 0L;
    private final int[] location = new int[2];

    public BlurPreDrawListener(BlurEngine engine, View view) {
        this.engine = engine;
        this.targetView = view;
    }

    @Override
    public boolean onPreDraw() {
        if (!targetView.isShown() || BlurEngine.isPaused || BlurEngine.blurBitmap == null) {
            return true;
        }

        // QUAN TRỌNG: onPreDraw() được đăng ký trên ViewTreeObserver của chính
        // targetView, nên gọi invalidate() vô điều kiện ở đây sẽ tự lên lịch một
        // pass vẽ mới, pass đó lại kích hoạt onPreDraw lần nữa -> vòng lặp vẽ lại
        // vô hạn ở tốc độ tối đa dù màn hình hoàn toàn tĩnh. Vì bitmap nền
        // (BlurEngine.blurBitmap) trên thực tế chỉ đổi khi wallpaper/theme đổi
        // (xem BlurController.captureAndBlur), ta chỉ cần invalidate khi:
        //   1) bitmap nền vừa được cập nhật, hoặc
        //   2) vị trí của targetView trên màn hình vừa đổi (đang cuộn/animate).
        // Nếu không có gì đổi thì dừng vòng lặp tại đây, không invalidate nữa.

        Bitmap currentBitmap = BlurEngine.blurBitmap;
        boolean bitmapChanged = currentBitmap != lastBitmap;

        targetView.getLocationOnScreen(location);
        boolean locationChanged = location[0] != lastX || location[1] != lastY;

        if (!bitmapChanged && !locationChanged) {
            return true;
        }

        long now = System.currentTimeMillis();
        if (!bitmapChanged && now - lastInvalidateAt < MIN_INTERVAL_MS) {
            // Đang trong lúc cuộn/animate: throttle để không vẽ lại dày hơn ~30fps
            return true;
        }

        lastBitmap = currentBitmap;
        lastX = location[0];
        lastY = location[1];
        lastInvalidateAt = now;

        targetView.invalidate();
        return true;
    }
}
