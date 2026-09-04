package com.tool.tree.ui

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import android.widget.ListPopupWindow
import com.omarea.common.ui.BlurPopupBackgroundDrawable
import com.omarea.common.ui.DialogHelper
import com.tool.tree.ThemeModeState

/**
 * Tiện ích dùng chung cho các popup dạng Spinner bo góc (ListPopupWindow + nền bo góc
 * kr_spinner_popup_bg) trong toàn app:
 *   - Clip nội dung popup (listView) theo đúng outline bo góc, để hiệu ứng ripple khi bấm
 *     item KHÔNG tràn ra ngoài phần bo góc.
 *   - Ẩn thanh cuộn dọc của popup.
 *   - Tính độ rộng + chiều cao chuẩn + vị trí popup không dính sát mép màn hình.
 */
object SpinnerPopupHelper {

    private const val EDGE_INSET_DP = 16f

    /**
     * Gọi SAU popup.show() - ListPopupWindow chỉ tạo ra listView thật sự sau khi show().
     */
    @JvmStatic
    fun applyRoundedClip(popup: ListPopupWindow, radiusPx: Float) {
        val listView = popup.listView ?: return
        listView.divider = null
        listView.dividerHeight = 0
        listView.clipToOutline = true
        listView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
            }
        }
        listView.isVerticalScrollBarEnabled = false
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
        val screenWidth = context.resources.displayMetrics.widthPixels
        val marginPx = dpToPx(context, EDGE_INSET_DP)
        val maxWidth = screenWidth.coerceAtLeast(minWidthPx)

        val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        var maxItemWidth = 0
        for (itemView in itemViews) {
            val naturalWidth = measureNaturalWidth(itemView, unspecified)
            if (naturalWidth > maxItemWidth) {
                maxItemWidth = naturalWidth
            }
        }

        val contentWidth = maxItemWidth + bgPadding.left + bgPadding.right
        val desiredWidth = contentWidth.coerceAtLeast(minWidthPx).coerceAtMost(maxWidth)
        popup.width = desiredWidth

        // Đo lại chiều cao từng dòng ĐÚNG theo độ rộng thật sự sẽ hiển thị
        val rowWidthPx = View.MeasureSpec.makeMeasureSpec(
            (desiredWidth - bgPadding.left - bgPadding.right).coerceAtLeast(0),
            View.MeasureSpec.EXACTLY
        )

        var contentHeight = 0
        for (itemView in itemViews) {
            itemView.measure(rowWidthPx, unspecified)
            contentHeight += itemView.measuredHeight
        }

        val screenHeight = context.resources.displayMetrics.heightPixels
        val topMarginPx = dpToPx(context, EDGE_INSET_DP)
        val maxHeight = (screenHeight - topMarginPx).coerceAtLeast(0)

        popup.height = (contentHeight + bgPadding.top + bgPadding.bottom).coerceAtMost(maxHeight)

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

    private fun measureNaturalWidth(view: View, unspecified: Int): Int {
        val relaxed = HashMap<View, Int>()
        relaxWidths(view, relaxed)
        view.measure(unspecified, unspecified)
        val naturalWidth = view.measuredWidth
        for ((relaxedView, originalWidth) in relaxed) {
            relaxedView.layoutParams = relaxedView.layoutParams.apply { width = originalWidth }
        }
        return naturalWidth
    }

    private fun relaxWidths(view: View, relaxed: MutableMap<View, Int>) {
        val lp = view.layoutParams
        val isWeighted = lp is LinearLayout.LayoutParams && lp.weight > 0f
        if (lp != null && (lp.width == ViewGroup.LayoutParams.MATCH_PARENT || isWeighted)) {
            relaxed[view] = lp.width
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                relaxWidths(view.getChildAt(i), relaxed)
            }
        }
    }

    private fun dpToPx(context: Context, dp: Float): Int =
        (context.resources.displayMetrics.density * dp).toInt()

    /**
     * Nền cho ListPopupWindow (thay cho việc gán thẳng "fallback" = kr_spinner_popup_bg):
     * nếu đang bật "chế độ ảnh nền" (ThemeModeState.isImageBackgroundMode()) VÀ blur không bị
     * tắt (DialogHelper.disableBlurBg == false) thì trả về nền kính mờ (crop cache blur
     * wallpaper toàn màn hình đúng vùng popup, xem BlurPopupBackgroundDrawable), giữ nguyên
     * viền bo góc + hiệu ứng ripple + khoảng inset như "fallback" gốc. Ngược lại (không ở chế
     * độ ảnh nền, hoặc blur bị tắt, hoặc fallback null) trả về nguyên vẹn "fallback".
     *
     * PHẢI gọi TRƯỚC popup.show() (cùng chỗ với popup.setBackgroundDrawable(...) hiện có) -
     * việc chụp/crop ảnh thật sự được hoãn tới lần draw() đầu tiên của Drawable trả về, vì
     * popup.listView + toạ độ thật trên màn hình chỉ có SAU show().
     */
    @JvmStatic
    fun buildPopupBackground(
        activity: Activity,
        popup: ListPopupWindow,
        fallback: Drawable?,
        cornerRadiusPx: Float
    ): Drawable? {
        if (fallback == null || DialogHelper.disableBlurBg || !ThemeModeState.isImageBackgroundMode()) {
            return fallback
        }

        val padding = Rect()
        fallback.getPadding(padding)

        val fallbackContent = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerRadiusPx
            setColor(readThemeColor(activity, android.R.attr.colorBackgroundFloating, Color.WHITE))
        }
        val blurContent = BlurPopupBackgroundDrawable(activity, popup, cornerRadiusPx, fallbackContent)

        val rippleMask = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerRadiusPx
            setColor(Color.WHITE)
        }
        val rippleColor = ColorStateList.valueOf(
            readThemeColor(activity, android.R.attr.colorControlHighlight, Color.LTGRAY)
        )
        val ripple = RippleDrawable(rippleColor, blurContent, rippleMask)

        return InsetDrawable(ripple, padding.left, padding.top, padding.right, padding.bottom)
    }

    private fun readThemeColor(context: Context, attr: Int, defaultColor: Int): Int {
        val typedArray = context.obtainStyledAttributes(intArrayOf(attr))
        val color = typedArray.getColor(0, defaultColor)
        typedArray.recycle()
        return color
    }

    /**
     * Dựng nút "⋮" (overflow) bằng code - style giống hệt icon toolbar mặc định
     * (actionBarItemBackground cho hiệu ứng bấm, icon ic_more_vert). Dùng làm actionView cho
     * 1 MenuItem neo (xem ActionPage.setupOverflowMenuButton / TextEditorActivity) thay vì để
     * hệ thống tự vẽ overflow mặc định - để bấm vào nó luôn mở showListPopup() (nền kính mờ,
     * bo góc, đồng bộ giao diện) thay vì popup hệ thống mặc định.
     */
    @JvmStatic
    fun buildOverflowMenuButton(activity: Activity, contentDescription: String): android.widget.ImageButton {
        val density = activity.resources.displayMetrics.density
        val sizePx = (48 * density).toInt()
        val paddingPx = (12 * density).toInt()

        val backgroundResId = android.util.TypedValue().let {
            activity.theme.resolveAttribute(android.R.attr.actionBarItemBackground, it, true)
            it.resourceId
        }

        return android.widget.ImageButton(activity).apply {
            layoutParams = ViewGroup.LayoutParams(sizePx, sizePx)
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            if (backgroundResId != 0) setBackgroundResource(backgroundResId)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            setImageResource(com.tool.tree.R.drawable.ic_more_vert)
            this.contentDescription = contentDescription
        }
    }

    /**
     * Popup List Item dùng chung cho MỌI menu "⋮" / popup chọn nhiều item trong toàn app
     * (menu "⋮" của ActionPage, popup chọn khi FAB nhiều item, menu "⋮" của TextEditorActivity...) -
     * đảm bảo tất cả có CÙNG giao diện: nền bo góc + kính mờ (buildPopupBackground), neo sát
     * mép phải màn hình, tự lật lên trên khi không đủ chỗ bên dưới.
     */
    @JvmStatic
    fun showListPopup(activity: Activity, anchor: View, rows: List<PopupMenuRow>, extraTopGapPx: Int = 0) {
        if (rows.isEmpty()) return

        val adapter = PopupMenuListAdapter(activity, rows)
        val background = androidx.core.content.ContextCompat.getDrawable(activity, com.tool.tree.R.drawable.kr_spinner_popup_bg)

        val popup = ListPopupWindow(activity)
        popup.anchorView = anchor
        popup.setAdapter(adapter)
        popup.setBackgroundDrawable(
            buildPopupBackground(activity, popup, background, activity.resources.getDimension(com.tool.tree.R.dimen.kr_spinner_popup_radius))
        )
        popup.isModal = true
        popup.setOnItemClickListener { _, _, position, _ ->
            popup.dismiss()
            rows.getOrNull(position)?.onClick?.invoke()
        }

        val parent = anchor.parent as? ViewGroup
        val itemViews = rows.indices.map { adapter.getView(it, null, parent) }
        val minWidthPx = (activity.resources.displayMetrics.density * 200).toInt()
        applyWidthAndPosition(
            popup, anchor, itemViews, background, minWidthPx, alignRight = true,
            extraTopGapPx = extraTopGapPx, applyVerticalOffset = true
        )

        popup.show()
        applyRoundedClip(popup, activity.resources.getDimension(com.tool.tree.R.dimen.kr_spinner_popup_radius))
    }
}
