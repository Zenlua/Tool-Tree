package com.omarea.common.ui;

import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;

public final class BlurEngine {
    // Điều khiển chính
    public static BlurController controller = new BlurController();
    public static Bitmap blurBitmap; 
    public static boolean isPaused = true;
    
    // Thông số thẩm mỹ (Có thể điều chỉnh để tăng mờ)
    public static float cornerRadius = 30.0f;
    private static final int BLUR_TINT_COLOR = Color.parseColor("#15FFFFFF"); // Phủ trắng 10% cho hiệu ứng kính

    private View targetView;
    private int[] location = new int[2];

    public BlurEngine(View view) {
        this.targetView = view;
    }

    /**
     * Thiết lập các bộ lắng nghe và bo góc cho View
     */
    public void setup() {
        targetView.setOutlineProvider(new BlurOutlineProvider(cornerRadius));
        targetView.setClipToOutline(true);
        targetView.getViewTreeObserver().addOnPreDrawListener(new BlurPreDrawListener(this));
    }

    /**
     * Hàm tính toán cắt ảnh mờ và áp dụng lên Background
     * Được gọi liên tục bởi OnPreDrawListener
     */
    public void updateBlur() {
        // CƠ CHẾ LỌC: Dừng ngay lập tức nếu bị tạm dừng hoặc chưa có ảnh mờ
        if (isPaused || blurBitmap == null || targetView.getWidth() <= 0) {
            return;
        }

        // 1. Xác định vị trí của View trên màn hình thực tế
        targetView.getLocationInWindow(location);
        
        // 2. Tính toán tỷ lệ giữa ảnh mờ (thường là 128px hoặc 192px) và màn hình thật
        View rootView = targetView.getRootView();
        float scaleX = (float) blurBitmap.getWidth() / rootView.getWidth();
        float scaleY = (float) blurBitmap.getHeight() / rootView.getHeight();

        // 3. Tính toán vùng cần cắt (Rect) trên ảnh mờ tổng
        int x = (int) (location[0] * scaleX);
        int y = (int) (location[1] * scaleY);
        int w = (int) (targetView.getWidth() * scaleX);
        int h = (int) (targetView.getHeight() * scaleY);

        // Đảm bảo không cắt ra ngoài biên của Bitmap (tránh Crash)
        x = Math.max(0, Math.min(x, blurBitmap.getWidth() - w));
        y = Math.max(0, Math.min(y, blurBitmap.getHeight() - h));

        if (w > 0 && h > 0) {
            // 4. Cắt miếng ảnh tương ứng
            Bitmap cropped = Bitmap.createBitmap(blurBitmap, x, y, w, h);
            
            // 5. NÂNG CAO: Vẽ thêm lớp phủ màu để tăng độ rõ nét cho nội dung bên trên
            Canvas canvas = new Canvas(cropped);
            canvas.drawColor(BLUR_TINT_COLOR); 

            // 6. Cập nhật Background cho View
            targetView.setBackground(new BitmapDrawable(targetView.getResources(), cropped));
        }
    }

    /**
     * Tạo Paint để vẽ viền (Stroke) trong hàm onDraw của View
     */
    public static Paint getStrokePaint() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3.0f); // Viền mảnh
        p.setColor(Color.parseColor("#25FFFFFF")); // Màu viền trắng mờ (Glass stroke)
        return p;
    }

    public static float getCornerRadius() {
        return cornerRadius;
    }
}
