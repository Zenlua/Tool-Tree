package com.tool.tree.ui

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * Hiệu ứng chuyển tab kiểu Material "Fade Through" thay cho hiệu ứng trượt (slide)
 * mặc định của ViewPager2/AOSP.
 *
 * Cách hoạt động:
 * - "Đóng băng" translationX (dựa theo `position * width`) để loại bỏ hoàn toàn việc
 *   trang bị kéo trượt ngang theo ngón tay/animation mặc định - trang luôn nằm cố định
 *   giữa màn hình.
 * - Trang ĐANG hiện (outgoing) sẽ mờ dần và biến mất ở NỬA ĐẦU quá trình chuyển
 *   (progress 0 -> 0.5), đồng thời thu nhỏ nhẹ 100% -> 92%.
 * - Trang SẮP hiện (incoming) sẽ mờ dần hiện ra ở NỬA SAU quá trình chuyển
 *   (progress 0.5 -> 1), đồng thời phóng to nhẹ 92% -> 100%.
 * - Kết quả: khi vuốt/bấm tab, không thấy 2 trang "trượt" cạnh nhau như AOSP, mà thấy
 *   trang cũ mờ đi rồi trang mới mờ hiện lên đúng tại chỗ.
 */
class FadeThroughPageTransformer(
    // Tỉ lệ scale nhỏ nhất khi trang đang mờ đi/mờ đến (giống Material spec ~0.92).
    // Đặt 1f nếu chỉ muốn fade thuần, không muốn scale.
    private val minScale: Float = 0.92f
) : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        val width = page.width
        if (width == 0) return

        page.apply {
            // Hủy hiệu ứng trượt ngang mặc định: giữ trang luôn ở giữa màn hình bất kể
            // vị trí cuộn hiện tại, để chỉ còn lại hiệu ứng mờ dần (fade) + scale.
            translationX = -position * width

            when {
                // Trang đã cuộn ra ngoài hẳn hai bên -> ẩn hoàn toàn, tránh tốn vẽ thừa.
                position <= -1f || position >= 1f -> {
                    alpha = 0f
                    scaleX = minScale
                    scaleY = minScale
                }

                // Trang đang là trang "cũ" (đang dần biến mất khi vuốt sang trái, tức
                // position đi từ 0 -> -1). progress = tiến độ rời đi (0 -> 1).
                position <= 0f -> {
                    val progress = -position // 0 -> 1
                    alpha = (1f - (progress / 0.5f)).coerceIn(0f, 1f)
                    val scale = 1f - (1f - minScale) * progress.coerceIn(0f, 1f)
                    scaleX = scale
                    scaleY = scale
                }

                // Trang đang là trang "mới" (đang dần hiện ra khi vuốt tới, position đi
                // từ 1 -> 0). progress = tiến độ xuất hiện (0 -> 1).
                else -> {
                    val progress = 1f - position // 0 -> 1
                    alpha = (((progress - 0.5f) / 0.5f)).coerceIn(0f, 1f)
                    val scale = minScale + (1f - minScale) * progress.coerceIn(0f, 1f)
                    scaleX = scale
                    scaleY = scale
                }
            }
        }
    }
}
