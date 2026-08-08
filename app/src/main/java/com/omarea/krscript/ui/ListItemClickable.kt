package com.omarea.krscript.ui

import android.content.Context
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
                    GifPlaybackHelper.bind(iconView, config.iconGifAutoplay, config.iconGifLoopCount)
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
                    GifPlaybackHelper.bind(extraIconView, config.photoGifAutoplay, config.photoGifLoopCount)
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
        val params = imageView.layoutParams ?: return
        val maxSize = imageView.context.resources.displayMetrics.widthPixels
    
        if (realSize) {
            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            imageView.adjustViewBounds = true
            imageView.maxWidth = maxSize
            imageView.maxHeight = maxSize
            
            // Cố định chiều rộng và chiều cao bằng nhau (khung vuông bằng chiều rộng màn hình)
            params.width = maxSize
            params.height = maxSize
            
            if (params is RelativeLayout.LayoutParams) {
                params.addRule(RelativeLayout.CENTER_HORIZONTAL)
            }
            imageView.layoutParams = params
        } else {
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            imageView.adjustViewBounds = true
            imageView.maxWidth = Int.MAX_VALUE
            imageView.maxHeight = Int.MAX_VALUE
            
            params.width = RelativeLayout.LayoutParams.MATCH_PARENT
            params.height = RelativeLayout.LayoutParams.WRAP_CONTENT
            
            if (params is RelativeLayout.LayoutParams) {
                params.addRule(RelativeLayout.CENTER_HORIZONTAL, 0)
            }
            imageView.layoutParams = params
        }
    }

    interface OnClickListener {
        fun onClick(listItemView: ListItemClickable)
    }

    interface OnLongClickListener {
        fun onLongClick(listItemView: ListItemClickable)
    }
}
