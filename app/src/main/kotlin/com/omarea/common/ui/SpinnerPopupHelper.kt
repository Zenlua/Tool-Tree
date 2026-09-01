package com.omarea.common.ui

import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ListPopupWindow
import android.widget.ListView

/**
 * Tiện ích dùng chung cho các popup dạng Spinner bo góc (ListPopupWindow + nền bo góc
 * kr_spinner_popup_bg) trong toàn app:
 *   - Clip nội dung popup (listView) theo đúng outline bo góc, để hiệu ứng ripple khi bấm
 *     item KHÔNG tràn ra ngoài phần bo góc.
 *   - Ẩn thanh cuộn dọc của popup.
 *   - Thêm dải ngăn cách mảnh (1dp) giữa các item.
 *   - Tính độ rộng + chiều cao chuẩn + vị trí popup không dính sát mép màn hình.
 */
object SpinnerPopupHelper {

    private const val EDGE_INSET_DP = 16f
    private const val MAX_VISIBLE_ROWS = 6

    private fun dividerHeightPx(context: Context): Int = dpToPx(context, 1f).coerceAtLeast(1)

    /**
     * Gọi SAU popup.show() - ListPopupWindow chỉ tạo ra listView thật sự sau khi show().
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
        listView.dividerHeight = dividerHeightPx(context)
    }

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

        // Đo lại chiều cao từng dòng ĐÚNG theo độ rộng thật sự sẽ hiển thị
        val rowWidthPx = View.MeasureSpec.makeMeasureSpec(
            (desiredWidth - bgPadding.left - bgPadding.right).coerceAtLeast(0),
            View.MeasureSpec.EXACTLY
        )
        val dividerPx = dividerHeightPx(context)
        
        // Lấy tối đa MAX_VISIBLE_ROWS dòng để tính chiều cao
        val visibleCount = itemViews.size.coerceAtMost(MAX_VISIBLE_ROWS)
        var contentHeight = 0

        for (i in 0 until visibleCount) {
            val itemView = itemViews[i]
            itemView.measure(rowWidthPx, unspecified)
            contentHeight += itemView.measuredHeight
            
            // Cộng chiều cao divider giữa các dòng hiển thị
            if (i < visibleCount - 1) {
                contentHeight += dividerPx
            }
        }

        // Bổ sung bgPadding.top + bgPadding.bottom để ListView không bị chèn ép diện tích
        popup.height = contentHeight + bgPadding.top + bgPadding.bottom

        val anchorLocation = IntArray(2)
        anchor.getLocationOnScreen(anchorLocation)
        val anchorX = anchorLocation[0]

        var offset = if (alignRight) {
            (screenWidth - desiredWidth) - anchorX
        } else {
            val overflowRight = (anchorX + desiredWidth) - (screenWidth - marginPx)
            if (overflowRight > 0) -overflowRight else 0
        }
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
