package com.omarea.krscript.ui

import android.content.Context
import android.view.View
import android.widget.ImageView
import com.tool.tree.R
import com.omarea.krscript.model.EditorNode

class ListItemEditor(context: Context, config: EditorNode) : ListItemClickable(context, R.layout.kr_action_list_item, config) {
    private val widgetView = layout.findViewById<ImageView?>(R.id.kr_widget)

    init {
        widgetView?.visibility = View.VISIBLE
        widgetView?.setImageDrawable(context.getDrawable(R.drawable.kr_script))
    }
}
