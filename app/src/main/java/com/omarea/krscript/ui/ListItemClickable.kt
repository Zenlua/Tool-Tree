package com.omarea.krscript.ui

import android.content.Context
import android.view.View
import android.widget.ImageView
import com.tool.tree.R
import com.omarea.krscript.config.IconPathAnalysis
import com.omarea.krscript.model.ClickableNode
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.LayerDrawable

open class ListItemClickable(context: Context,
                             layoutId: Int,
                             config: ClickableNode) : ListItemView(context, layoutId, config) {
    protected var mOnClickListener: OnClickListener? = null
    protected var mOnLongClickListener: OnLongClickListener? = null
    protected var shortcutIconView = layout.findViewById<View?>(R.id.kr_shortcut_icon)
    protected var iconView = layout.findViewById<ImageView?>(R.id.kr_icon)
    protected var extraIconView = layout.findViewById<ImageView?>(R.id.kr_extra_icon)
    protected val contentView = layout.findViewById<View>(android.R.id.content)

    fun setOnClickListener(onClickListener: OnClickListener): ListItemClickable {
        this.mOnClickListener = onClickListener

        return this
    }

    fun setOnLongClickListener(onLongClickListener: OnLongClickListener): ListItemClickable {
        this.mOnLongClickListener = onLongClickListener

        return this
    }

    fun triggerAction() {
        this.mOnClickListener?.onClick(this)
    }

    init {
        title = config.title
        desc = config.desc
        summary = config.summary

        this.layout.setOnClickListener {
            this.mOnClickListener?.onClick(this)
        }
        if (this.key.isNotEmpty() && config.allowShortcut != false) {
            this.layout.setOnLongClickListener {
                this.mOnLongClickListener?.onLongClick(this)
                true
            }
            shortcutIconView?.visibility = View.VISIBLE
        } else {
            shortcutIconView?.visibility = View.GONE
        }
        if (iconView != null) {
            iconView?.visibility = View.GONE
            if (config.iconPath.isNotEmpty()) {
                IconPathAnalysis().loadIcon(context, config)?.run {
                    iconView?.setImageDrawable(this)
                    iconView?.visibility = View.VISIBLE
                }
            }
        }
        if (extraIconView != null) {
            extraIconView?.visibility = View.GONE
            if (config.photoPath.isNotEmpty()) {
                IconPathAnalysis().loadPhoto(context, config)?.run {
                    extraIconView?.setImageDrawable(this)
                    extraIconView?.visibility = View.VISIBLE
                }
            }
        }
        // ===== Background ảnh động =====
        val bg = contentView?.background
        if (bg is RippleDrawable && config.backgroundPath.isNotEmpty()) {
            val drawable = IconPathAnalysis().loadBackground(context, config)
            drawable?.let { newBg ->
                val content = bg.getDrawable(0)
                if (content is LayerDrawable) {
                    val index = content.findIndexByLayerId(R.id.bg_image)
                    if (index != -1) {
                        content.setDrawable(index, newBg)
                    }
                }
            }
        }
    }

    interface OnClickListener {
        fun onClick(listItemView: ListItemClickable)
    }

    interface OnLongClickListener {
        fun onLongClick(listItemView: ListItemClickable)
    }
}
