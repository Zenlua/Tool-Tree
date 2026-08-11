package com.omarea.krscript.ui

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.*
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.omarea.common.ui.DialogHelper
import com.omarea.krscript.TryOpenActivity
import com.omarea.krscript.config.IconPathAnalysis
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.NodeInfoBase
import com.omarea.krscript.model.TextNode
import com.tool.tree.R

// Tách logic hiển thị "rows" (dùng chung bởi item text và item action, ...) ra một nơi duy nhất,
// tránh lặp lại code giữa ListItemText và ListItemAction.
object RowsRenderHelper {

    fun bind(
        context: Context,
        rowsView: TextView?,
        extraIconView: ImageView?,
        rows: List<TextNode.TextRow>,
        config: NodeInfoBase
    ) {
        if (rowsView == null) {
            return
        }
        if (rows.isEmpty()) {
            rowsView.visibility = View.GONE
            extraIconView?.visibility = View.GONE
            return
        }

        rowsView.text = ""
        rowsView.movementMethod = LinkMovementMethod.getInstance() // 不设置 ClickableSpan 点击没反应
        rowsView.visibility = View.VISIBLE

        for (row in rows) {
            if (row.breakRow || row.align != Layout.Alignment.ALIGN_NORMAL) {
                rowsView.append("\n")
            }
            val text = row.text
            val length = text.length
            val spannableString = SpannableString(text)

            if (extraIconView != null) {
                extraIconView.visibility = View.GONE
                if (row.photo.isNotEmpty()) {
                    IconPathAnalysis().loadtextPhoto(context, row, config.pageConfigDir)?.run {
                        extraIconView.setImageDrawable(this)
                        extraIconView.visibility = View.VISIBLE
                        applyPhotoRealSize(extraIconView, row.photoRealSize)
                        GifPlaybackHelper.bind(extraIconView, row.photoGifAutoplay, row.photoGifLoopCount)
                    }
                }
            }

            if (row.underline) {
                spannableString.setSpan(UnderlineSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            if (row.link.isNotEmpty()) {
                spannableString.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        if (row.link.isNotEmpty()) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(row.link))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (ex: Exception) {
                                Toast.makeText(context, context.getString(R.string.kr_slice_activity_fail), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        ds.color = if (row.color != 1) ds.linkColor else row.color
                        ds.isUnderlineText = row.underline
                    }
                }, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            if (row.activity.isNotEmpty()) {
                spannableString.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        TryOpenActivity(context, row.activity).tryOpen()
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        ds.color = if (row.color != 1) ds.linkColor else row.color
                        ds.isUnderlineText = row.underline
                    }
                }, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            if (row.onClickScript.isNotEmpty()) {
                spannableString.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        val result = ScriptEnvironmen.executeResultRoot(context, row.onClickScript, config)
                        if (result.trim().isNotEmpty()) {
                            DialogHelper.helpInfo(context, context.getString(R.string.kr_slice_script_result), result)
                        }
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        ds.color = if (row.color != 1) ds.linkColor else row.color
                        ds.isUnderlineText = row.underline
                    }
                }, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            if (row.color != -1) {
                spannableString.setSpan(ForegroundColorSpan(row.color), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            if (row.bgColor != -1) {
                spannableString.setSpan(BackgroundColorSpan(row.bgColor), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            if (row.bold && row.italic) {
                spannableString.setSpan(StyleSpan(Typeface.BOLD_ITALIC), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else if (row.bold) {
                spannableString.setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else if (row.italic) {
                spannableString.setSpan(StyleSpan(Typeface.ITALIC), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            if (row.size != -1) {
                spannableString.setSpan(AbsoluteSizeSpan(row.size, true), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            spannableString.setSpan(AlignmentSpan.Standard(row.align), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            rowsView.append(spannableString)
        }

        // NOTE: 修补 android.widget.Editor.touchPositionIsInSelection(Editor.java:1363) 导致的奔溃
        rowsView.setOnLongClickListener {
            true
        }
    }

    // Nếu photoRealSize = true: hiển thị ảnh trong khung vuông (chiều rộng = chiều cao = chiều ngang màn hình),
    // căn giữa theo chiều ngang; ảnh sẽ được scale vừa vặn trong khung bằng CENTER_INSIDE mà không bị méo.
    private fun applyPhotoRealSize(imageView: ImageView, realSize: Boolean) {
        val params = imageView.layoutParams ?: return
        val maxSize = imageView.context.resources.displayMetrics.widthPixels

        if (realSize) {
            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            imageView.adjustViewBounds = true
            imageView.maxWidth = maxSize
            imageView.maxHeight = maxSize

            params.width = maxSize
            params.height = maxSize

            when (params) {
                is android.widget.LinearLayout.LayoutParams -> params.gravity = android.view.Gravity.CENTER_HORIZONTAL
                is android.widget.RelativeLayout.LayoutParams -> params.addRule(android.widget.RelativeLayout.CENTER_HORIZONTAL)
            }
            imageView.layoutParams = params
        } else {
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            imageView.adjustViewBounds = true
            imageView.maxWidth = Int.MAX_VALUE
            imageView.maxHeight = Int.MAX_VALUE

            params.width = android.widget.LinearLayout.LayoutParams.MATCH_PARENT
            params.height = android.widget.LinearLayout.LayoutParams.WRAP_CONTENT

            when (params) {
                is android.widget.LinearLayout.LayoutParams -> params.gravity = android.view.Gravity.NO_GRAVITY
                is android.widget.RelativeLayout.LayoutParams -> params.addRule(android.widget.RelativeLayout.CENTER_HORIZONTAL, 0)
            }
            imageView.layoutParams = params
        }
    }
}
