package com.tool.tree.ui

import android.app.Activity
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import com.tool.tree.R

// Nút "⋮" + popup List Item bo góc dùng chung ngoài ActionPage.kt (icon/kích thước/vị trí trên
// toolbar giữ nguyên như overflow mặc định, chỉ khác nền/giao diện popup dùng kr_spinner_popup_bg +
// PopupMenuListAdapter giống ActionPage.showListPopup() để đồng bộ giao diện toàn app).
object OverflowMenuPopup {

    // Dựng nút "⋮" - cùng icon/kích thước/vị trí như ActionPage.buildOverflowMenuButton().
    @JvmStatic
    fun buildButton(activity: Activity): ImageButton {
        val density = activity.resources.displayMetrics.density
        val sizePx = (48 * density).toInt()
        val paddingPx = (12 * density).toInt()

        val backgroundResId = TypedValue().let {
            activity.theme.resolveAttribute(android.R.attr.actionBarItemBackground, it, true)
            it.resourceId
        }

        return ImageButton(activity).apply {
            layoutParams = ViewGroup.LayoutParams(sizePx, sizePx)
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            if (backgroundResId != 0) setBackgroundResource(backgroundResId)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageResource(R.drawable.ic_more_vert)
            contentDescription = activity.getString(R.string.kr_more_options)
        }
    }

    // Hiện popup List Item bo góc tại anchor - cùng cơ chế với ActionPage.showListPopup().
    @JvmStatic
    fun show(activity: Activity, anchor: View, rows: List<PopupMenuRow>) {
        if (rows.isEmpty()) return

        val adapter = PopupMenuListAdapter(activity, rows)
        val background = ContextCompat.getDrawable(activity, R.drawable.kr_spinner_popup_bg)

        val popup = ListPopupWindow(activity)
        popup.anchorView = anchor
        popup.setAdapter(adapter)
        popup.setBackgroundDrawable(background)
        popup.isModal = true
        popup.setOnItemClickListener { _, _, position, _ ->
            popup.dismiss()
            rows.getOrNull(position)?.onClick?.invoke()
        }

        val parent = anchor.parent as? ViewGroup
        val itemViews = rows.indices.map { adapter.getView(it, null, parent) }
        val minWidthPx = (activity.resources.displayMetrics.density * 200).toInt()
        SpinnerPopupHelper.applyWidthAndPosition(
            popup, anchor, itemViews, background, minWidthPx, alignRight = true,
            applyVerticalOffset = true
        )

        popup.show()
        SpinnerPopupHelper.applyRoundedClip(popup, activity.resources.getDimension(R.dimen.kr_spinner_popup_radius))
    }
}
