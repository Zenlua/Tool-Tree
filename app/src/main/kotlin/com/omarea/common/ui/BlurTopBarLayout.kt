package com.omarea.common.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet

class BlurTopBarLayout(context: Context, attrs: AttributeSet?) : BlurViewLinearLayout(context, attrs) {
    init {
        // Tắt bo góc cho thanh trên (Top Bar thường là hình chữ nhật phẳng)
        this.engine.cornerRadius = 0f
    }

    override fun drawStroke(canvas: Canvas) {
        val paint = BlurEngine.getStrokePaint(context)
        val strokeWidth = paint.strokeWidth

        // GIẢI PHÁP: Tính toán vị trí Y sao cho đường kẻ nằm trọn bên trong View
        // Thay vì vẽ tại getHeight(), chúng ta thụt lên một nửa độ dày của viền
        val y = height - (strokeWidth / 2f)

        // Chỉ vẽ một đường kẻ ngang (Divider) ở cạnh dưới cùng
        canvas.drawLine(0f, y, width.toFloat(), y, paint)
    }
}
