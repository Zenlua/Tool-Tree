package com.omarea.common.ui

import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ListPopupWindow
import android.widget.ListView

/**
 * Tiện ích dùng chung cho các popup dạng Spinner bo góc (ListPopupWindow + nền bo góc
 * kr_spinner_popup_bg) trong toàn app:
 *   - Clip nội dung popup (listView) theo đúng outline bo góc, để hiệu ứng ripple khi bấm
 *     item KHÔNG tràn ra ngoài phần bo góc (mặc định selector/ripple của item vẽ full
 *     rectangle, không biết gì về bo góc của nền popup bên ngoài).
 *   - Ẩn thanh cuộn dọc của popup (chỉ ẩn thanh trượt, việc cuộn vẫn hoạt động bình thường).
 *   - Thêm dải ngăn cách mảnh (1dp) giữa các item, dùng đúng màu divider mặc định của
 *     theme hệ thống hiện tại (android:attr/listDivider) - tự đổi theo sáng/tối - và
 *     inset 2 bên bằng đúng padding ngang của text (EDGE_INSET_DP) để dải ngăn cách
 *     "đi theo chữ" như 1 gạch chân, không dính sát vào khung popup.
 *   - Tính độ rộng + vị trí popup: rộng tối thiểu theo nội dung/anchor, có thể mở rộng
 *     tối đa nhưng luôn chừa EDGE_INSET_DP hở ở cả 2 mép màn hình (không dính sát màn hình).
 *
 * Dùng chung cho: ActionPage (menu "⋮", popup FAB, dropdown spinner), ParamsSingleSelect
 * (spinner trong form param dạng combobox), MainActivity (popup chọn theme).
 */
object SpinnerPopupHelper {

    // Khớp với padding ngang của TextView trong kr_spinner_dropdown.xml - dùng chung cho
    // cả khoảng hở mép màn hình lẫn inset của dải ngăn cách, để dải ngăn cách thẳng hàng
    // đúng theo lề chữ.
    private const val EDGE_INSET_DP = 16f

    /**
     * Gọi SAU popup.show() - ListPopupWindow chỉ tạo ra listView thật sự sau khi show().
     *
     * @param radiusPx bán kính bo góc, phải khớp với bán kính đang dùng ở nền popup
     *                 (thường lấy từ R.dimen.kr_spinner_popup_radius) để clip khớp viền.
     */
    @JvmStatic
    fun applyRoundedClip(popup: ListPopupWindow, radiusPx: Float) {
        val listView = popup.listView ?: return

        listView.clipToOutline = true
        listView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
            }
        }
        listView.isVerticalScrollBarEnabled = false

        applyDivider(listView)
    }

    private fun applyDivider(listView: ListView) {
        val context = listView.context
        val attrs = context.obtainStyledAttributes(intArrayOf(android.R.attr.listDivider))
        val baseDivider = attrs.getDrawable(0)
        attrs.recycle()

        val insetPx = dpToPx(context, EDGE_INSET_DP)
        listView.divider = baseDivider?.let { InsetDrawable(it, insetPx, 0, insetPx, 0) }
        listView.dividerHeight = dpToPx(context, 1f).coerceAtLeast(1)
    }

    /**
     * Tính độ rộng popup theo nội dung (đo trước từ [itemViews]) và vị trí ngang, đảm bảo:
     *   - Rộng tối thiểu [minWidthPx].
     *   - Có thể mở rộng theo nội dung, nhưng KHÔNG bao giờ chạm mép màn hình - luôn chừa
     *     EDGE_INSET_DP (16dp) hở ở CẢ 2 bên (giới hạn maxWidth). Riêng khoảng hở 16dp hiển
     *     thị thực tế bên cạnh hộp bo góc đến từ chính inset của nền kr_spinner_popup_bg.xml
     *     (insetLeft/insetRight = activity_horizontal_margin), KHÔNG phải do hàm này cộng
     *     thêm - xem nhánh alignRight bên dưới, tránh cộng dồn 2 lần thành 32dp.
     *   - Chữ dài quá độ rộng tối đa sẽ tự xuống dòng (TextView trong kr_spinner_dropdown.xml
     *     không giới hạn số dòng), không cần xử lý riêng.
     *
     * @param alignRight true = neo sát mép phải màn hình (kiểu menu 3 chấm hệ thống, dùng cho
     *                   ActionPage); false = neo theo mép trái của anchor, chỉ lùi lại khi
     *                   tràn mép màn hình (dùng cho spinner dạng combobox).
     */
    @JvmStatic
    fun applyWidthAndPosition(
        popup: ListPopupWindow,
        anchor: View,
        itemViews: List<View>,
        background: Drawable?,
        minWidthPx: Int,
        alignRight: Boolean,
        extraTopGapPx: Int = 0,
        applyVerticalOffset: Boolean = false
    ) {
        val context = anchor.context
        val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        var maxItemWidth = 0
        for (itemView in itemViews) {
            itemView.measure(unspecified, unspecified)
            if (itemView.measuredWidth > maxItemWidth) {
                maxItemWidth = itemView.measuredWidth
            }
        }

        val bgPadding = Rect()
        background?.getPadding(bgPadding)
        val screenWidth = context.resources.displayMetrics.widthPixels
        val marginPx = dpToPx(context, EDGE_INSET_DP)
        val maxWidth = (screenWidth - marginPx * 2).coerceAtLeast(minWidthPx)
        val contentWidth = maxItemWidth + bgPadding.left + bgPadding.right
        val desiredWidth = contentWidth.coerceAtLeast(minWidthPx).coerceAtMost(maxWidth)
        popup.width = desiredWidth

        val anchorLocation = IntArray(2)
        anchor.getLocationOnScreen(anchorLocation)
        val anchorX = anchorLocation[0]

        var offset = if (alignRight) {
            // KHÔNG trừ thêm marginPx ở đây: nền kr_spinner_popup_bg.xml đã tự
            // insetLeft/insetRight = 16dp (activity_horizontal_margin) để bo góc hộp bên
            // trong khung popup - đó chính là khoảng margin 16dp hiển thị thực tế. Nếu trừ
            // thêm marginPx nữa vào vị trí khung, hộp bo góc sẽ bị đẩy vào tận 32dp
            // (16dp margin ở đây + 16dp inset của nền) thay vì đúng 16dp như ý đồ thiết kế -
            // khung chỉ cần neo sát mép phải màn hình (offset 0), phần margin để nền tự lo.
            (screenWidth - desiredWidth) - anchorX
        } else {
            val overflowRight = (anchorX + desiredWidth) - (screenWidth - marginPx)
            if (overflowRight > 0) -overflowRight else 0
        }
        // Luôn đảm bảo mép trái popup không lọt vào trong khoảng hở marginPx, dù ở chế độ nào.
        val leftEdge = anchorX + offset
        if (leftEdge < marginPx) {
            offset += (marginPx - leftEdge)
        }
        popup.horizontalOffset = offset
        if (applyVerticalOffset) {
            popup.verticalOffset = if (extraTopGapPx > 0) -extraTopGapPx else -anchor.height
        }
    }

    private fun dpToPx(context: Context, dp: Float): Int =
        (context.resources.displayMetrics.density * dp).toInt()
}