package com.omarea.krscript.ui

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import com.tool.tree.R
import com.omarea.krscript.config.IconPathAnalysis
import com.omarea.krscript.model.ClickableNode

open class ListItemClickable(context: Context,
                             layoutId: Int,
                             config: ClickableNode) : ListItemView(context, layoutId, config) {
    protected var mOnClickListener: OnClickListener? = null
    protected var mOnLongClickListener: OnLongClickListener? = null
    protected var shortcutIconView = layout.findViewById<View?>(R.id.kr_shortcut_icon)
    protected var iconView = layout.findViewById<ImageView?>(R.id.kr_icon)
    protected var extraIconView = layout.findViewById<ImageView?>(R.id.kr_extra_icon)
    protected var extraBgView = layout.findViewById<ImageView?>(R.id.kr_extra_bg)

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
                    startIfAnimated(iconView)
                }
            }
        }
        if (extraIconView != null) {
            extraIconView?.visibility = View.GONE
            if (config.photoPath.isNotEmpty()) {
                IconPathAnalysis().loadPhoto(context, config)?.run {
                    extraIconView?.setImageDrawable(this)
                    extraIconView?.visibility = View.VISIBLE
                    applyPhotoRealSize(extraIconView, config.photoRealSize)
                    startIfAnimated(extraIconView)
                }
            }
        }
        if (extraBgView != null) {
            extraBgView?.visibility = View.GONE
            if (config.bgPath.isNotEmpty()) {
                IconPathAnalysis().loadBg(context, config)?.run {
                    extraBgView?.setImageDrawable(this)
                    extraBgView?.visibility = View.VISIBLE
                }
            }
        }
    }

    // Nếu photoRealSize = true: hiện ảnh đúng kích thước thật (không phóng to full chiều ngang),
    // căn giữa theo chiều ngang; ảnh lớn hơn khung chứa sẽ được thu nhỏ vừa khung (không bị tràn/méo).
    private fun applyPhotoRealSize(imageView: ImageView?, realSize: Boolean) {
        imageView ?: return
        val params = imageView.layoutParams
        if (realSize) {
            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            imageView.adjustViewBounds = true
            val maxSize = imageView.context.resources.displayMetrics.widthPixels
            imageView.maxWidth = maxSize
            imageView.maxHeight = maxSize
            if (params != null) {
                params.width = RelativeLayout.LayoutParams.WRAP_CONTENT
                if (params is RelativeLayout.LayoutParams) {
                    params.addRule(RelativeLayout.CENTER_HORIZONTAL)
                }
                imageView.layoutParams = params
            }
        } else {
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            imageView.adjustViewBounds = true
            imageView.maxWidth = Int.MAX_VALUE
            imageView.maxHeight = Int.MAX_VALUE
            if (params != null) {
                params.width = RelativeLayout.LayoutParams.MATCH_PARENT
                if (params is RelativeLayout.LayoutParams) {
                    params.addRule(RelativeLayout.CENTER_HORIZONTAL, 0)
                }
                imageView.layoutParams = params
            }
        }
    }

    // Bắt đầu chạy hoạt ảnh (kiểu GIF) nếu drawable hiện tại của ImageView là AnimationDrawable.
    // Phải post() để đảm bảo View đã attach vào window trước khi start().
    private fun startIfAnimated(imageView: ImageView?) {
        val drawable = imageView?.drawable
        if (drawable is AnimationDrawable) {
            imageView.post {
                if (imageView.drawable === drawable) {
                    drawable.start()
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
