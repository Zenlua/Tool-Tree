package com.omarea.krscript.ui

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import com.omarea.common.ui.BlurEngine
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

        // Kích thước khung icon gốc (px), khớp với 35dp khai báo cứng trong kr_action_list_item.xml
        // / kr_switch_list_item.xml. Tính cố định từ hằng số dp (không đọc layoutParams hiện tại)
        // để không bị lệch nếu view này từng được phóng to bởi quầng mờ ở lần bind trước.
        val baseIconSizePx = (35f * context.resources.displayMetrics.density + 0.5f).toInt()

        // Quầng mờ phía sau icon (chỉ bật khi directbg=1 - xem BlurEngine.isDirectBgMode,
        // được ThemeModeState cập nhật): thay vì nền đen phẳng, dùng chính bản mờ + phóng to nhẹ
        // của icon đó làm nền, giữ nguyên màu thật của icon.
        val applyIconGlow = fun(iconDrawable: Drawable) {
            val icon = iconView ?: return
            if (!BlurEngine.isDirectBgMode) {
                icon.background = null
                val params = icon.layoutParams
                if (params != null && (params.width != baseIconSizePx || params.height != baseIconSizePx)) {
                    params.width = baseIconSizePx
                    params.height = baseIconSizePx
                    icon.layoutParams = params
                }
                icon.setPadding(0, 0, 0, 0)
                return
            }

            val glow = BlurEngine.controller.createIconGlow(context, iconDrawable, baseIconSizePx, 1.25f, 10f)
            if (glow == null) {
                icon.background = null
                return
            }

            val glowSizePx = glow.width
            val extra = (glowSizePx - baseIconSizePx).coerceAtLeast(0)
            val pad = extra / 2

            val params = icon.layoutParams
            if (params != null) {
                params.width = glowSizePx
                params.height = glowSizePx
                icon.layoutParams = params
            }
            icon.setPadding(pad, pad, pad, pad)
            icon.scaleType = ImageView.ScaleType.FIT_CENTER
            icon.background = BitmapDrawable(context.resources, glow)
        }

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
                    applyIconGlow(this)
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