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
        val bgPadding = Rect()
        background?.getPadding(bgPadding)
        val bgW = bgPadding.left + bgPadding.right
        val bgH = bgPadding.top + bgPadding.bottom
        val screenWidth = context.resources.displayMetrics.widthPixels
        val marginPx = dpToPx(context, EDGE_INSET_DP)

        // --- Đo rộng từng item (UNSPECIFIED để tự co theo nội dung) ---
        val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        var maxItemWidth = 0
        for (itemView in itemViews) {
            itemView.measure(unspecified, unspecified)
            if (itemView.measuredWidth > maxItemWidth) {
                maxItemWidth = itemView.measuredWidth
            }
        }

        // --- Tính giới hạn TOÀN BỘ cửa sổ (nội dung + nền) ---
        val maxTotalW = (screenWidth - marginPx * 2).coerceAtLeast(minWidthPx)
        // Nội dung = toàn bộ trừ nền
        val maxContentW = (maxTotalW - bgW).coerceAtLeast(0)
        val minContentW = (minWidthPx - bgW).coerceAtLeast(0)

        val desiredContentW = maxItemWidth
            .coerceAtLeast(minContentW)
            .coerceAtMost(maxContentW)

        // Set CHỈ phần nội dung; PopupWindow tự cộng bgPadding cho cửa sổ
        popup.width = desiredContentW

        // --- Đo lại chiều cao theo độ rộng nội dung thật sự ---
        val rowWidthSpec = View.MeasureSpec.makeMeasureSpec(
            desiredContentW, View.MeasureSpec.EXACTLY
        )
        val dividerPx = dividerHeightPx(context)

        val visibleCount = itemViews.size.coerceAtMost(MAX_VISIBLE_ROWS)
        var contentHeight = 0
        for (i in 0 until visibleCount) {
            val itemView = itemViews[i]
            itemView.measure(rowWidthSpec, unspecified)
            contentHeight += itemView.measuredHeight
            if (i < visibleCount - 1) {
                contentHeight += dividerPx
            }
        }

        popup.height = contentHeight

        // --- Tính vị trí (dùng tổng rộng = nội dung + nền) ---
        val totalW = desiredContentW + bgW
        val anchorLocation = IntArray(2)
        anchor.getLocationOnScreen(anchorLocation)
        val anchorX = anchorLocation[0]

        var offset = if (alignRight) {
            (screenWidth - totalW) - anchorX
        } else {
            val overflowRight = (anchorX + totalW) - (screenWidth - marginPx)
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
