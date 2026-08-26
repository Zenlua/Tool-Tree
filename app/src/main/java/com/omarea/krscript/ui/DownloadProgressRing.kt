package com.omarea.krscript.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.LinearInterpolator

// Vòng tròn tiến trình gọn nhẹ - dùng để THAY CHỖ icon (kr_widget, 35dp) của item [[download]]
// trong lúc đang tải/đang chạy script, thay vì 1 thanh ngang riêng bên dưới desc (xem
// ListItemDownload.markBusy()/updateDownloadProgress()/finishBusy()). Không vẽ chữ/% bên trong
// (đã có desc cạnh bên đảm nhiệm phần đó) - chỉ 1 vòng nền mờ + 1 cung tiến trình.
//  - setIndeterminate(true): chưa biết tổng dung lượng (hoặc đang chạy script) - 1 cung ngắn cố
//    định tự xoay vòng liên tục, giống spinner.
//  - setIndeterminate(false) + setProgress(percent): cung vẽ từ đỉnh (-90°), quét theo %.
class DownloadProgressRing @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val rect = RectF()

    private var progressPercent = 0f // 0f..100f
    private var isIndeterminateMode = true
    private var spinAngle = 0f
    private var spinAnimator: ValueAnimator? = null

    init {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
        trackPaint.color = typedValue.data
        trackPaint.alpha = 60
        progressPaint.color = typedValue.data
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val strokeWidthPx = w.coerceAtMost(h) * 0.14f
        trackPaint.strokeWidth = strokeWidthPx
        progressPaint.strokeWidth = strokeWidthPx
        val inset = strokeWidthPx / 2f
        rect.set(inset, inset, w - inset, h - inset)
    }

    fun setIndeterminate(indeterminate: Boolean) {
        if (isIndeterminateMode == indeterminate) return
        isIndeterminateMode = indeterminate
        if (indeterminate) startSpin() else stopSpin()
        invalidate()
    }

    fun setProgress(percent: Float) {
        progressPercent = percent.coerceIn(0f, 100f)
        if (!isIndeterminateMode) invalidate()
    }

    private fun startSpin() {
        stopSpin()
        spinAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                spinAngle = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopSpin() {
        spinAnimator?.cancel()
        spinAnimator = null
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        // Chỉ tốn CPU quay animation khi thực sự đang hiển thị (item bận) - ẩn đi thì dừng luôn.
        if (visibility == VISIBLE && isIndeterminateMode) {
            startSpin()
        } else {
            stopSpin()
        }
    }

    override fun onDetachedFromWindow() {
        stopSpin()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawArc(rect, 0f, 360f, false, trackPaint)
        if (isIndeterminateMode) {
            canvas.drawArc(rect, spinAngle, 90f, false, progressPaint)
        } else {
            canvas.drawArc(rect, -90f, progressPercent * 3.6f, false, progressPaint)
        }
    }
}
