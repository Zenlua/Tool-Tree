package com.tool.tree.ui

import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator
import kotlin.math.exp

/**
 * Hiệu ứng "nảy" (rubber-band) dùng chung cho vuốt lùi toàn trang (SwipeBackHelper) và vuốt
 * đóng dialog (DialogSwipeBackHelper) khi người dùng vuốt SANG TRÁI - ngược hướng với hướng
 * "trở lại/đóng" chính (vuốt phải). Không có ý nghĩa điều hướng nào, chỉ là phản hồi kiểu
 * "đã chạm biên, không có gì ở đây" - giống hiệu ứng overscroll của iOS.
 *
 * dampen(): càng kéo xa thì càng bị "cản" nhiều hơn, tiệm cận nhưng KHÔNG BAO GIỜ vượt quá
 * maxPull dù ngón tay kéo bao xa - dùng hàm mũ e^-x nên mượt tự nhiên, không cần thêm hằng số
 * tinh chỉnh nào khác ngoài maxPull.
 */
object SwipeBounceEffect {
    // Khoảng kéo tối đa (dp) - vuốt trái không bao giờ đẩy nội dung lệch quá xa khỏi vị trí gốc
    private const val MAX_PULL_DP = 56f

    fun maxPullPx(density: Float): Float = MAX_PULL_DP * density

    /**
     * @param rawPull khoảng cách kéo thô, LUÔN LÀ SỐ DƯƠNG (truyền vào -dx khi dx đang âm)
     * @param maxPull giới hạn kéo tối đa (px), xem [maxPullPx]
     * @return khoảng cách đã bị "cản", luôn nằm trong [0, maxPull)
     */
    fun dampen(rawPull: Float, maxPull: Float): Float {
        if (maxPull <= 0f) return 0f
        return maxPull * (1f - exp(-rawPull / maxPull))
    }

    // Overshoot nhẹ khi bật lại về 0 sau khi buông tay - tạo cảm giác "nảy" thay vì chỉ
    // trượt về đều đều như animation trở-lại-trang bình thường.
    val bounceInterpolator: Interpolator = OvershootInterpolator(1.8f)
}
