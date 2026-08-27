package com.omarea.common.ui

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
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

        // Lưu lại kích thước/gravity GỐC của contentView trước khi originalLayoutParams bị
        // mutate thành MATCH_PARENT cho wrapper ở dưới. Với dialog nhỏ (vd kr_dialog_params_small,
        // <=4 tham số) root vốn khai wrap_content chiều cao để AlertDialog tự hiện như 1 khung nhỏ
        // giữa màn hình; dialog full-screen thì root vốn đã MATCH_PARENT/MATCH_PARENT sẵn.
        val originalWidth = originalLayoutParams.width
        val originalHeight = originalLayoutParams.height
        val originalGravity = (originalLayoutParams as? FrameLayout.LayoutParams)?.gravity ?: Gravity.CENTER

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
        // QUAN TRỌNG - đây chính là chỗ trước đây làm dialog nhỏ (<=4 tham số, root vốn chỉ
        // wrap_content) bị kéo giãn tràn kín màn hình: ép cứng contentView thành MATCH_PARENT dù
        // kích thước gốc của nó nhỏ hơn. Giờ giữ nguyên width/height gốc + gravity=CENTER, để
        // contentView vẫn hiện đúng kích thước của nó (nhỏ, căn giữa) bên trong wrapper full màn
        // hình; dialog full-screen thì originalWidth/Height đã MATCH_PARENT sẵn nên không đổi gì.
        wrapper.addView(contentView, FrameLayout.LayoutParams(originalWidth, originalHeight, originalGravity))

        // Ngược lại, wrapper (khung chứa ảnh blur + contentView) LUÔN phải MATCH_PARENT để ảnh
        // blur phủ kín toàn màn hình và phần lộ ra khi vuốt đúng là toàn bộ Activity thật phía
        // sau - bất kể contentView bên trong nhỏ hơn màn hình.
        val wrapperLp = originalLayoutParams.apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        parent.addView(wrapper, index, wrapperLp)

        return wrapper
    }
}