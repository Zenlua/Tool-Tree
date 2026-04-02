package com.tool.tree.ui

import android.app.Activity
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.tabs.TabLayout
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
        imageView.layoutParams.width = size
        imageView.layoutParams.height = size

        // 👉 alpha giống code cũ
        layout.alpha = if (isFirst) 1f else 0.3f

        views.add(layout)

        return layout
    }

    fun updateHighlight(position: Int) {
        for (i in views.indices) {
            views[i].alpha = if (i == position) 1f else 0.3f
        }
    }
}