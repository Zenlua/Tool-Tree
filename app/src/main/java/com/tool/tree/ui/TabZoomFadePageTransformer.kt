package com.tool.tree.ui

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs
import kotlin.math.max

/**
 * Hiệu ứng chuyển tab: vừa TRƯỢT (giữ chuyển động mặc định của ViewPager2, không "đóng băng"
 * translationX như bản trước) vừa THU NHỎ + MỜ DẦN với tab đang đi ra, và PHÓNG TO + HIỆN RÕ
 * DẦN với tab đang đi vào - giống hiệu ứng "Zoom Out" kinh điển của Google (ZoomOutPageTransformer)
 * chứ không phải fade-through đứng yên tại chỗ.
 *
 * position (do ViewPager2 cung cấp cho mỗi trang):
 * -1  = trang nằm hẳn bên trái màn hình hiện tại
 *  0  = trang đang hiển thị chính giữa màn hình
 *  1  = trang nằm hẳn bên phải màn hình hiện tại
 *
 * Trang càng lệch xa vị trí 0 (|position| tiến gần 1) thì càng nhỏ + càng mờ; khi về đúng
 * position = 0 thì full size (100%) + full alpha (100%).
 */
class TabZoomFadePageTransformer(
    // Kích thước nhỏ nhất khi trang đang ở rìa màn hình (|position| = 1).
    private val minScale: Float = 0.85f,
    // Độ mờ nhỏ nhất khi trang đang ở rìa màn hình (|position| = 1).
    private val minAlpha: Float = 0.5f
) : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        val pageWidth = page.width
        val pageHeight = page.height
        if (pageWidth == 0 || pageHeight == 0) return

        page.apply {
            when {
                // Trang đã trôi ra ngoài hẳn 2 bên -> ẩn để khỏi tốn vẽ thừa.
                position < -1f || position > 1f -> {
                    alpha = 0f
                }

                else -> {
                    // KHÔNG set translationX thủ công ở đây -> ViewPager2 vẫn tự trượt trang
                    // theo đúng chuyển động mặc định (kéo tay/animation chuyển tab), khác hẳn
                    // bản fade-through cũ (đóng băng translationX làm trang đứng im).
                    val scaleFactor = max(minScale, 1f - abs(position))
                    scaleX = scaleFactor
                    scaleY = scaleFactor

                    // Bù lại phần lệch do scale từ tâm, để trang thu nhỏ vẫn "áp" đúng theo
                    // hướng trượt thay vì bị lộ khoảng trống 2 bên khi scale nhỏ lại.
                    val vertMargin = pageHeight * (1 - scaleFactor) / 2
                    val horzMargin = pageWidth * (1 - scaleFactor) / 2
                    translationX = if (position < 0) {
                        (horzMargin - vertMargin / 2)
                    } else {
                        (-horzMargin + vertMargin / 2)
                    }

                    // Mờ dần tỉ lệ thuận theo scale: scale càng nhỏ (càng lệch xa) thì càng mờ.
                    alpha = minAlpha + (scaleFactor - minScale) / (1f - minScale) * (1f - minAlpha)
                }
            }
        }
    }
}
