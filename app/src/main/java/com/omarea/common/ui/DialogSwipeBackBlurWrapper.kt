package com.omarea.common.ui

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable

/**
 * CHỈ dùng cho các dialog ĐÃ bật DialogSwipeBackHelper (xem DialogFullScreen,
 * ActionListFragment, DialogHelper.customDialog()) - các dialog không cancelable (không cho
 * đóng) thì không đụng tới, vẫn giữ nguyên window.setBackgroundDrawable() tĩnh như cũ.
 *
 * Vấn đề cũ: DialogHelper.setWindowBlurBg() gán ảnh blur làm background của WINDOW (decorView)
 * - một lớp tách biệt với contentView. DialogSwipeBackHelper chỉ translate contentView, nên khi
 * kéo, contentView trượt đi nhưng lớp blur phía dưới đứng yên tuyệt đối -> phần "lộ ra" luôn là
 * CHÍNH tấm ảnh blur tĩnh đó, không phải cửa sổ Activity thật.
 *
 * Cách sửa ở đây: đưa ảnh blur vào LÀM 1 VIEW CON (ImageView) chung 1 FrameLayout với
 * contentView, thay vì đặt làm nền Window. Window được set nền TRONG SUỐT. Khi
 * DialogSwipeBackHelper translate FrameLayout này, cả khối "blur + nội dung" trượt cùng nhau -
 * phần trống lộ ra bên trái chính là Activity thật đang render phía dưới (khác Window, do hệ
 * thống tự compositing), không phải lại thấy cùng 1 tấm ảnh blur như trước.
 *
 * @return FrameLayout mới (đã re-parent contentView vào bên trong) để dùng thay contentView khi
 * gọi DialogSwipeBackHelper.bind(dialog, wrapper, ...). Trả về null nếu không bọc được (không
 * lấy được ảnh blur - ví dụ DialogHelper.disableBlurBg = true, hoặc OOM, hoặc contentView chưa
 * có parent) - bên gọi nên tự fallback lại DialogHelper.setWindowBlurBg() + bind thẳng
 * contentView như hành vi cũ.
 */
object DialogSwipeBackBlurWrapper {
    fun wrap(activity: Activity, window: Window, contentView: View): View? {
        val parent = contentView.parent as? ViewGroup ?: return null

        val blurBitmap = if (DialogHelper.disableBlurBg) {
            null
        } else {
            FastBlurUtility.getBlurBackgroundDrawer(activity)
        } ?: return null

        val index = parent.indexOfChild(contentView)
        val originalLayoutParams = contentView.layoutParams

        // Window tự nó phải trong suốt - phần trống lộ ra lúc kéo phải là Activity thật phía
        // dưới, không phải 1 màu nền cố định nào khác.
        window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        parent.removeView(contentView)

        val blurImage = ImageView(activity).apply {
            setImageBitmap(blurBitmap)
            scaleType = ImageView.ScaleType.FIT_XY
        }

        val wrapper = FrameLayout(activity)
        wrapper.addView(blurImage, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        // QUAN TRỌNG: giữ NGUYÊN originalLayoutParams (kích thước + gravity gốc, ví dụ
        // wrap_content + gravity=center của 1 dialog nhỏ nổi giữa màn hình) cho contentView bên
        // trong wrapper - chỉ đổi PARENT của nó, không đổi cách nó tự đo kích thước. Nếu ép luôn
        // contentView thành match_parent ở đây (như code cũ), FrameLayout khi đo wrapper sẽ lấy
        // max kích thước giữa các con - mà blurImage (ảnh chụp NGUYÊN màn hình, match_parent) có
        // kích thước intrinsic ~ bằng cả màn hình, kéo cả wrapper LẪN contentView phình to gần
        // bằng full-screen dù layout gốc chỉ nhỏ bằng nội dung thật -> vỡ bố cục (dialog nhỏ hiện
        // to bất thường, ô nhập văn bản minLines/maxLines bị đo trong 1 khoảng không gian lớn hơn
        // hẳn thật nên luôn hiện đủ số dòng tối đa thay vì co theo nội dung).
        wrapper.addView(contentView, originalLayoutParams)

        // Ngược lại, bản thân wrapper thì LUÔN phải phủ kín toàn bộ window (để vừa chứa ảnh blur
        // full-screen, vừa giữ đúng vị trí/gravity của contentView bên trong nó) - dùng
        // match_parent khi gắn wrapper vào parent thật, KHÔNG dùng lại originalLayoutParams (đó
        // là kích thước dành cho contentView, không phải cho wrapper).
        parent.addView(wrapper, index, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        return wrapper
    }
}