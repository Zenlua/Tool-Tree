package com.omarea.krscript.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.tool.tree.R
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.NodeInfoBase

open class ListItemView(private val context: Context,
                        layoutId: Int,
                        private val config: NodeInfoBase) {
    protected var layout = LayoutInflater.from(context).inflate(layoutId, null)

    protected var descView = layout.findViewById<TextView?>(R.id.kr_desc)
    protected var summaryView: TextView? = layout.findViewById(R.id.kr_summary)
    protected var titleView = layout.findViewById<TextView?>(R.id.kr_title)


    val key: String
        get() {
            return config.key
        }

    var title: String
        get() {
            return titleView?.text.toString()
        }
        set(value) {
            if (value.isEmpty()) {
                titleView?.visibility = View.GONE
            } else {
                titleView?.text = value
                titleView?.visibility = View.VISIBLE
            }
        }

    var desc: String
        get() {
            return descView?.text.toString()
        }
        set(value) {
            if (value.isEmpty()) {
                descView?.visibility = View.GONE
            } else {
                descView?.text = value
                descView?.visibility = View.VISIBLE
            }
        }

    var summary: String
        get() {
            return summaryView?.text.toString()
        }
        set(value) {
            if (value.isEmpty()) {
                summaryView?.visibility = View.GONE
            } else {
                summaryView?.text = value
                summaryView?.visibility = View.VISIBLE
            }
        }

    val index: String
        get() {
            return config.index
        }

    // hide = true (xem NodeInfoBase.hide): mục này bị ẩn theo mặc định, chỉ hiện tạm bằng cử
    // chỉ 2 ngón vuốt xuống - xem ListItemGroup.setHiddenItemsVisible/ActionListFragment.
    val isHiddenItem: Boolean
        get() = config.hide

    // animate = true: có hiệu ứng mờ dần (fade) khi hiện/ẩn thay vì đổi trạng thái đột ngột.
    // Không có tác dụng gì nếu mục này không phải loại hide=true (isHiddenItem = false).
    open fun setHiddenItemVisible(visible: Boolean, animate: Boolean = true) {
        if (!isHiddenItem) return
        layout.animate().cancel()
        if (visible) {
            if (layout.visibility != View.VISIBLE) {
                layout.alpha = if (animate) 0f else 1f
                layout.visibility = View.VISIBLE
                if (animate) {
                    layout.animate().alpha(1f).setDuration(220).start()
                }
            }
        } else {
            if (layout.visibility == View.VISIBLE) {
                if (animate) {
                    layout.animate().alpha(0f).setDuration(180).withEndAction {
                        layout.visibility = View.GONE
                        layout.alpha = 1f
                    }.start()
                } else {
                    layout.visibility = View.GONE
                    layout.alpha = 1f
                }
            }
        }
    }

    open fun updateViewByShell() {
        if (config.titleSh.isNotEmpty()) {
            config.title = ScriptEnvironmen.executeResultRoot(context, config.titleSh, config)
            title = config.title
        }

        if (config.descSh.isNotEmpty()) {
            config.desc = ScriptEnvironmen.executeResultRoot(context, config.descSh, config)
            desc = config.desc
        }

        if (config.summarySh.isNotEmpty()) {
            config.summary = ScriptEnvironmen.executeResultRoot(context, config.summarySh, config)
            summary = config.summary
        }
    }

    fun getView(): View {
        return layout
    }

    init {
        title = config.title
        desc = config.desc
        summary = config.summary
        // Ẩn sẵn ngay từ đầu nếu mục khai báo hide=true - chỉ hiện lại tạm thời bằng cử chỉ 2
        // ngón vuốt xuống (không animate lúc khởi tạo, tránh hiệu ứng thừa lúc mới dựng trang).
        if (config.hide) {
            layout.visibility = View.GONE
        }
    }
}
