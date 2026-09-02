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
 * Cách sửa ở đây: đưa ảnh blur vào LÀM NỀN CỐ ĐỊNH của 1 outer FrameLayout đứng yên (không bị
 * DialogSwipeBackHelper translate), còn nội dung dialog thật (realRoot) được bọc trong 1 inner
 * FrameLayout riêng - CHỈ inner này mới được truyền làm swipeTarget/contentView cho
 * DialogSwipeBackHelper. Nhờ vậy lúc kéo, ảnh blur đứng yên tại chỗ như 1 phông nền cố định,
 * còn khối nội dung dialog trượt ngay bên trên nó (giống hệt cách ActionPage/SwipeBackHelper xử
 * lý ảnh preview - xem swipe_back_preview_blur/sharp trong activity_action_page.xml: 2 lớp đó
 * cũng đứng yên, chỉ swipe_foreground phía trên di chuyển).
 *
 * (Trước đây từng thử để ảnh blur trượt CÙNG khối nội dung, để phần lộ ra lúc kéo là chính cửa
 * sổ Activity thật phía sau - nhưng cách đó khiến ảnh blur bị "kéo đi theo" ngón tay, không còn
 * cảm giác là 1 phông nền cố định nữa. Giờ quay lại để ảnh blur đứng yên như mô tả ở trên.)
 *
 * QUAN TRỌNG (bài học từ lần sửa trước bị lỗi bố cục dialog nhỏ + ô nhập văn bản): wrapper PHẢI
 * được chèn vào android.R.id.content (khung nội dung gốc của CHÍNH window đó) - KHÔNG được chèn
 * vào cha trực tiếp của contentView. Với dialog dựng qua AlertDialog (isLongList=false, tức
 * <=4 mục, theme custom_alert_dialog có windowIsFloating=false + layout_gravity=center_vertical),
 * view mình truyền vào (kr_dialog_params_small...) nằm LỒNG SÂU bên trong khung chrome riêng của
 * AlertDialog (topPanel/contentPanel/customPanel/buttonPanel...) - cha trực tiếp của nó CHỈ LÀ 1
 * panel con, tự nó cũng đang wrap_content (để ôm khít khối alert nhỏ, canh giữa màn hình), KHÔNG
 * phải toàn bộ window. Nếu chèn wrapper (chứa ảnh blur to bằng cả màn hình) vào ngay cha trực
 * tiếp đó, FrameLayout đo wrap_content của panel cha (và mọi panel tổ tiên bên trên nó) đều bị
 * ảnh blur (kích thước intrinsic ~ bằng cả màn hình) kéo phình to gần bằng full-screen - vỡ bố
 * cục (dialog nhỏ hiện to/lệch tâm bất thường, ô nhập văn bản minLines/maxLines bị đo trong 1
 * khoảng không gian lớn hơn hẳn thật nên luôn hiện đủ số dòng tối đa thay vì co theo nội dung).
 *
 * android.R.id.content thì NGƯỢC LẠI - luôn đúng là khung nội dung TOÀN BỘ window (match_parent
 * thật sự theo định nghĩa của Window, không phụ thuộc theme/chrome bên trong), có ĐÚNG 1 view
 * con duy nhất dù là Dialog thường (DialogFullScreen - view con đó chính là content thật) hay
 * AlertDialog (view con đó là TOÀN BỘ khung chrome của AlertDialog, ví dụ parentPanel). Cách sửa:
 * lấy nguyên View con DUY NHẤT đó ra (giữ nguyên layoutParams/gravity gốc của nó, không đụng
 * vào), bọc nó vào 1 inner FrameLayout match_parent (đây mới là phần bị kéo trượt), rồi đặt
 * inner đó cùng ảnh blur vào chung 1 outer FrameLayout match_parent, gắn thẳng outer vào
 * android.R.id.content - khung chrome AlertDialog/DialogFullScreen bên trong vẫn tự đo/căn giữa
 * y hệt lúc trước, không bị ảnh hưởng gì bởi ảnh blur nằm cùng cấp với inner.
 *
 * @return inner FrameLayout (đã re-parent view con của android.R.id.content vào bên trong) để
 * dùng làm swipeTarget khi gọi DialogSwipeBackHelper.bind(dialog, wrapper, ...) - kéo view này
 * nghĩa là chỉ kéo nội dung dialog (kể cả khung chrome AlertDialog nếu có), ảnh blur ở outer
 * phía dưới đứng yên không di chuyển theo. Trả về null nếu không bọc được (không lấy được ảnh
 * blur - ví dụ DialogHelper.disableBlurBg = true, hoặc OOM, hoặc android.R.id.content chưa có
 * view con nào - xem view.post() ở nơi gọi) - bên gọi nên tự fallback lại
 * DialogHelper.setWindowBlurBg() + bind thẳng view gốc như hành vi cũ.
 */
object DialogSwipeBackBlurWrapper {
    fun wrap(activity: Activity, window: Window): View? {
        val contentRoot = window.findViewById<ViewGroup>(android.R.id.content) ?: return null
        if (contentRoot.childCount == 0) return null
        // android.R.id.content luôn chỉ có ĐÚNG 1 view con - dù là content thật (Dialog thường)
        // hay toàn bộ khung chrome AlertDialog (parentPanel...). Lấy index 0 là đủ, không cần dò.
        val realRoot = contentRoot.getChildAt(0)

        val blurBitmap = if (DialogHelper.disableBlurBg) {
            null
        } else {
            FastBlurUtility.getDialogBlurBackground(activity)
        } ?: return null

        val originalLayoutParams = realRoot.layoutParams

        // Window tự nó phải trong suốt - phần trống lộ ra lúc kéo phải là ảnh blur (đứng yên),
        // không phải 1 màu nền cố định nào khác.
        window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        contentRoot.removeView(realRoot)

        val blurImage = ImageView(activity).apply {
            setImageBitmap(blurBitmap)
            scaleType = ImageView.ScaleType.FIT_XY
        }

        // inner: CHỈ chứa nội dung dialog thật - đây là view duy nhất sẽ bị
        // DialogSwipeBackHelper translate lúc kéo.
        val inner = FrameLayout(activity)
        // Giữ NGUYÊN originalLayoutParams (kích thước + gravity gốc - ví dụ wrap_content +
        // gravity=center_vertical của khung AlertDialog nhỏ) cho realRoot bên trong inner - chỉ
        // đổi PARENT của nó, không đụng vào cách nó tự đo/căn giữa (xem giải thích ở doc comment
        // của class).
        inner.addView(realRoot, originalLayoutParams)

        // outer: đứng yên tuyệt đối, không được truyền cho DialogSwipeBackHelper - chỉ làm phông
        // nền cố định (ảnh blur) + khung chứa inner đang trượt phía trên.
        val outer = FrameLayout(activity)
        outer.addView(blurImage, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        outer.addView(inner, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        // outer thì LUÔN match_parent khi gắn vào android.R.id.content - vì contentRoot ở đây
        // CHẮC CHẮN là match_parent thật (khung nội dung gốc của window), không như cha trực tiếp
        // cũ của contentView (có thể là 1 panel wrap_content lồng sâu bên trong AlertDialog).
        contentRoot.addView(outer, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        return inner
    }
}