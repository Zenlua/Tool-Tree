package com.omarea.common.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

public class BlurTopBarLayout extends BlurViewLinearLayout {
    public BlurTopBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Tắt bo góc cho thanh trên (Top Bar thường là hình chữ nhật phẳng)
        this.engine.cornerRadius = 0f;
    }

    @Override
    protected void drawStroke(Canvas canvas) {
        Paint paint = BlurEngine.getStrokePaint(getContext());
        float strokeWidth = paint.getStrokeWidth();
        
        // GIẢI PHÁP: Tính toán vị trí Y sao cho đường kẻ nằm trọn bên trong View
        // Thay vì vẽ tại getHeight(), chúng ta thụt lên một nửa độ dày của viền
        float y = getHeight() - (strokeWidth / 2f);

        // Chỉ vẽ một đường kẻ ngang (Divider) ở cạnh dưới cùng
        canvas.drawLine(0, y, getWidth(), y, paint);
    }
}
