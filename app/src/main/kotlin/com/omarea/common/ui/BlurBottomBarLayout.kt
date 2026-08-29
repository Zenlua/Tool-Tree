package com.omarea.common.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet

class BlurBottomBarLayout(context: Context, attrs: AttributeSet?) : BlurViewLinearLayout(context, attrs) {
    init {
        // Thanh dưới thường trải dài hết màn hình nên tắt bo góc để khớp với cạnh máy
        this.engine.cornerRadius = 0f
    }

    override fun drawStroke(canvas: Canvas) {
        // Lấy Paint tĩnh từ BlurEngine để đảm bảo màu sắc thay đổi theo Dark/Light Mode
        val paint = BlurEngine.getStrokePaint(context)
        val strokeWidth = paint.strokeWidth

        // GIẢI PHÁP: Vẽ đường kẻ ở cạnh trên cùng (Top Edge) của Bottom Bar
        // Thụt xuống một nửa độ dày viền (strokeWidth / 2f) để nét vẽ nằm trọn bên trong View
        val y = strokeWidth / 2f

        canvas.drawLine(0f, y, width.toFloat(), y, paint)
    }
}
