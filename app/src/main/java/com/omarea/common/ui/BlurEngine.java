package com.omarea.common.ui;

import android.graphics.*;
import android.view.View;
import com.tool.tree.ThemeModeState;
import androidx.core.content.ContextCompat;
import com.tool.tree.R;
import android.content.Context;

public final class BlurEngine {
    public static BlurController controller = new BlurController();
    public static volatile Bitmap blurBitmap; 
    public static boolean isPaused = false;
    
    public static float DEFAULT_CORNER_RADIUS = 30.0f;
    public float cornerRadius = DEFAULT_CORNER_RADIUS;

    private View targetView;
    private int[] location = new int[2];
    private Bitmap cachedBitmap;
    private Canvas cachedCanvas;
    private static Paint strokePaint;

    // ========== TÍNH NĂNG MỚI: NGUỒN BLUR ĐỘNG (blur nội dung đang cuộn, không phải wallpaper) ==========
    // Mặc định (dynamicSource == null), BlurEngine hoạt động như cũ: hiện ảnh wallpaper đã
    // blur sẵn (BlurEngine.blurBitmap, static, dùng chung toàn app). Khi gán dynamicSource
    // (ví dụ ScrollContentBlurSource), engine sẽ ưu tiên lấy bitmap từ đó thay vì wallpaper -
    // dùng cho hiệu ứng "kính mờ" phản ánh ĐÚNG nội dung list đang trôi qua sau toolbar.
    public interface DynamicBlurSource {
        // Trả về bitmap đã blur sẵn (bất kỳ kích thước nào, sẽ được scale khớp targetView),
        // hoặc null nếu chưa có/chưa sẵn sàng - engine sẽ giữ nguyên frame trước đó.
        Bitmap getBlurSnapshot();
    }

    private DynamicBlurSource dynamicSource;

    public void setDynamicSource(DynamicBlurSource source) {
        this.dynamicSource = source;
    }

    public BlurEngine(View view) {
        this.targetView = view;
    }

    public void setup() {
        if (cornerRadius > 0) {
            targetView.setOutlineProvider(new BlurOutlineProvider(cornerRadius));
            targetView.setClipToOutline(true);
        } else {
            targetView.setOutlineProvider(null);
            targetView.setClipToOutline(false);
        }
        targetView.getViewTreeObserver().addOnPreDrawListener(new BlurPreDrawListener(this, targetView));
    }

    public Bitmap getUpdatedBlurBitmap() {
        if (isPaused || targetView.getWidth() <= 0 || targetView.getHeight() <= 0) {
            return null;
        }

        // ========== NHÁNH MỚI: NGUỒN BLUR ĐỘNG ==========
        // Khi có dynamicSource, BỎ QUA hoàn toàn logic wallpaper bên dưới (vốn cần cắt/dịch
        // theo tọa độ màn hình vì blurBitmap là 1 ảnh wallpaper dùng chung cho CẢ MÀN HÌNH).
        // Bitmap từ dynamicSource đã là đúng vùng nội dung cần hiện (được chụp trực tiếp từ
        // view đang cuộn, cùng hệ tọa độ với targetView), nên chỉ cần scale khớp kích thước
        // rồi tint, không cần tính toán vị trí trên màn hình.
        if (dynamicSource != null) {
            return getUpdatedFromDynamicSource();
        }

        if (blurBitmap == null || blurBitmap.isRecycled()) {
            return null;
        }

        // Lấy RootView để tính toán tỷ lệ chính xác giữa màn hình và BlurBitmap
        View rootView = targetView.getRootView();
        if (rootView == null) return null;

        targetView.getLocationOnScreen(location);
        
        float scaleX = (float) blurBitmap.getWidth() / rootView.getWidth();
        float scaleY = (float) blurBitmap.getHeight() / rootView.getHeight();

        // Kích thước thực tế của vùng Blur trên View
        int w = (int) (targetView.getWidth() * scaleX);
        int h = (int) (targetView.getHeight() * scaleY);

        // Tọa độ thực tế (cho phép giá trị âm khi vuốt ra ngoài biên)
        int x = (int) (location[0] * scaleX);
        int y = (int) (location[1] * scaleY);

        try {
            // Khởi tạo hoặc tái sử dụng cachedBitmap theo kích thước View
            if (cachedBitmap == null || cachedBitmap.getWidth() != w || cachedBitmap.getHeight() != h) {
                if (cachedBitmap != null) cachedBitmap.recycle();
                cachedBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                cachedCanvas = new Canvas(cachedBitmap);
            }
        
            // Xóa canvas cũ
            cachedCanvas.drawColor(0, PorterDuff.Mode.CLEAR); 
            
            /**
             * GIẢI PHÁP CHO VẤN ĐỀ 4:
             * Thay vì dùng srcRect cắt giới hạn trong Bitmap, ta dịch chuyển Canvas.
             * Chúng ta dịch chuyển ngược một khoảng đúng bằng tọa độ của View trên màn hình.
             * Điều này đảm bảo ảnh nền luôn khớp với vị trí của View bất kể View ở đâu.
             */
            cachedCanvas.save();
            cachedCanvas.translate(-x, -y);
            cachedCanvas.drawBitmap(blurBitmap, 0, 0, null);
            cachedCanvas.restore();

            // Phủ lớp màu (Tint) lên trên lớp blur
            cachedCanvas.drawColor(getBlurTintColor()); 
            
            return cachedBitmap;
        } catch (Exception e) {
            return null;
        }
    }

    // Vẽ bitmap đã blur sẵn từ dynamicSource ra cachedBitmap (tái dùng chung cachedBitmap/
    // cachedCanvas với nhánh wallpaper để đỡ cấp phát thêm bộ nhớ), scale khớp kích thước
    // targetView, rồi phủ tint như bình thường.
    private Bitmap getUpdatedFromDynamicSource() {
        Bitmap snapshot = dynamicSource.getBlurSnapshot();
        if (snapshot == null || snapshot.isRecycled()) {
            return null;
        }

        int w = targetView.getWidth();
        int h = targetView.getHeight();

        try {
            if (cachedBitmap == null || cachedBitmap.getWidth() != w || cachedBitmap.getHeight() != h) {
                if (cachedBitmap != null) cachedBitmap.recycle();
                cachedBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                cachedCanvas = new Canvas(cachedBitmap);
            }

            cachedCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
            Rect src = new Rect(0, 0, snapshot.getWidth(), snapshot.getHeight());
            Rect dst = new Rect(0, 0, w, h);
            cachedCanvas.drawBitmap(snapshot, src, dst, null);
            cachedCanvas.drawColor(getBlurTintColor());

            return cachedBitmap;
        } catch (Exception e) {
            return null;
        }
    }

    private int getBlurTintColor() {
        Context context = targetView.getContext();
        int colorRes = ThemeModeState.isDarkMode() ? R.color.colorBlurDark : R.color.colorBlurLight;
        return ContextCompat.getColor(context, colorRes);
    }

    public static Paint getStrokePaint(Context context) {
        if (strokePaint == null) {
            strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(3.0f); 
        }
        int colorRes = ThemeModeState.isDarkMode() ? R.color.colorPirmLight : R.color.colorPirmDark;
        int color = ContextCompat.getColor(context, colorRes);
        if (strokePaint.getColor() != color) strokePaint.setColor(color);
        return strokePaint;
    }

    public void destroy() {
        if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
            cachedBitmap.recycle();
            cachedBitmap = null;
        }
        cachedCanvas = null;
    }
}
