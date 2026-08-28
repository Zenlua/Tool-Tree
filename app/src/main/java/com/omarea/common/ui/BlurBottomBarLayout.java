package com.omarea.common.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

public class BlurBottomBarLayout extends BlurViewLinearLayout {
    public BlurBottomBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Thanh dưới thường trải dài hết màn hình nên tắt bo góc để khớp với cạnh máy
        if (this.engine != null) {
            this.engine.cornerRadius = 0f;
        }
    }

    @Override
    protected void drawStroke(Canvas canvas) {
        // Lấy Paint tĩnh từ BlurEngine để đảm bảo màu sắc thay đổi theo Dark/Light Mode
        Paint paint = BlurEngine.getStrokePaint(getContext());
        float strokeWidth = paint.getStrokeWidth();
        
        // GIẢI PHÁP: Vẽ đường kẻ ở cạnh trên cùng (Top Edge) của Bottom Bar
        // Thụt xuống một nửa độ dày viền (strokeWidth / 2f) để nét vẽ nằm trọn bên trong View
        float y = strokeWidth / 2f;

        canvas.drawLine(0, y, getWidth(), y, paint);
    }
}
