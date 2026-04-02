package com.tool.tree.ui

import android.app.Activity
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.tool.tree.R

class TabIconHelper(
    private val activity: Activity
) {

    private val views = ArrayList<View>()

    fun createTabView(text: String, drawable: Drawable, isFirst: Boolean): View {
        val layout = View.inflate(activity, R.layout.list_item_tab, null)

        val imageView = layout.findViewById<ImageView>(R.id.ItemIcon)
        val textView = layout.findViewById<TextView>(R.id.ItemTitle)

        textView.text = text
        imageView.setImageDrawable(drawable)

        // 👉 resize icon nhỏ giống TabHost
        val size = (20 * activity.resources.displayMetrics.density).toInt()
        drawable.setBounds(0, 0, size, size)
        imageView.layoutParams.width = size
        imageView.layoutParams.height = size

        // 👉 set alpha và background giống TabHost
        val selected = isFirst
        layout.alpha = if (selected) 1f else 0.7f
        layout.setBackgroundColor(getTabBackgroundColor(selected))

        views.add(layout)
        return layout
    }

    fun updateHighlight(position: Int) {
        for (i in views.indices) {
            val selected = i == position
            views[i].alpha = if (selected) 1f else 0.7f
            views[i].setBackgroundColor(getTabBackgroundColor(selected))
        }
    }

    private fun getTabBackgroundColor(isSelected: Boolean): Int {
        // Lấy màu nền navigationBarColor từ theme
        val typedValue = TypedValue()
        activity.theme.resolveAttribute(android.R.attr.navigationBarColor, typedValue, true)
        val baseColor = typedValue.data

        return if (isSelected) {
            baseColor // tab chọn full màu
        } else {
            // tab chưa chọn, làm mờ 30%
            ColorUtils.setAlphaComponent(baseColor, 80) // 0..255
        }
    }
}