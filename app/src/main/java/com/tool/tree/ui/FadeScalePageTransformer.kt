package com.tool.tree.ui

import android.view.View
import kotlin.math.abs

/**
 * Hiệu ứng chuyển tab "Fade + Scale" kiểu Material: trang đang rời khỏi màn hình co nhỏ
 * dần + mờ dần, trang sắp hiện ra phóng to dần từ [minScale] lên 1 và hiện rõ dần.
 *
 * [minScale] - tỉ lệ thu nhỏ nhỏ nhất của trang khi nó lùi hẳn sang một bên (0..1).
 * [minAlpha] - độ mờ nhỏ nhất của trang khi nó lùi hẳn sang một bên (0..1).
 */
class FadeScalePageTransformer(
    private val minScale: Float = 0.90f,
    private val minAlpha: Float = 0.55f
) : SwipePager.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        // Chặn trong [-1, 1]: ngoài khoảng này trang đã bị ViewGroup clip (không vẽ),
        // clamp lại để công thức không cho ra scale âm hoặc alpha âm trong trường hợp
        // fling/overshoot hiếm gặp.
        val clamped = position.coerceIn(-1f, 1f)
        val factor = 1f - abs(clamped)

        val scale = minScale + (1f - minScale) * factor
        page.scaleX = scale
        page.scaleY = scale
        page.alpha = (minAlpha + (1f - minAlpha) * factor).coerceIn(0f, 1f)
    }
}
