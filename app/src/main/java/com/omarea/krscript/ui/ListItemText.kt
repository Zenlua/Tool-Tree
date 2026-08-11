package com.omarea.krscript.ui

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import com.tool.tree.R
import com.omarea.krscript.model.TextNode

class ListItemText(private val context: Context,
                   layoutId: Int,
                   config: TextNode) : ListItemView(context, layoutId, config) {

    private val rowsView = layout.findViewById<TextView?>(R.id.kr_rows)
    protected var extraIconView = layout.findViewById<ImageView?>(R.id.kr_extra_icon_text)

    init {
        RowsRenderHelper.bind(context, rowsView, extraIconView, config.rows, config)
    }
}
