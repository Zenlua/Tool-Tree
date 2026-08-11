package com.omarea.krscript.ui

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.*
import android.view.View
import android.view.ViewTreeObserver
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

        // Bề rộng khả dụng (trừ padding) để tính vị trí tab-stop canh icon toggle sát lề phải.
        // Nếu view chưa được layout (width = 0, ví dụ lần bind đầu tiên khi RecyclerView chưa
        // đo xong), rows vẫn hiển thị bình thường (icon nằm ngay sau label, không bị lệch/mất)
        // và tự bind lại đúng 1 lần ngay khi layout xong để canh lại icon.
        val availableWidth = rowsView.width - rowsView.paddingLeft - rowsView.paddingRight
        if (availableWidth <= 0) {
            rowsView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    rowsView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    bind(context, rowsView, extraIconView, rows, config)
                }
            })
        }

        for (row in rows) {
            val isToggle = row.toggle == "checkbox" || row.toggle == "switch"
            // Toggle luôn tự xuống dòng riêng để canh icon sát lề phải cho đúng (label bên
            // trái, icon bên phải - giống 1 hàng cài đặt thông thường).
            if (row.breakRow || row.align != Layout.Alignment.ALIGN_NORMAL || isToggle) {
                rowsView.append("\n")
            }
            // Nếu có khai báo "sh": lấy nội dung dòng bằng cách chạy lệnh shell, thay vì dùng "text" tĩnh
            val label = if (row.dynamicTextSh.isNotEmpty()) {
                ScriptEnvironmen.executeResultRoot(context, row.dynamicTextSh, config)
            } else {
                row.text
            }

            // Row dạng toggle (checkbox/switch nhỏ): chèn 1 ký tự tab rồi tới 1 ký tự placeholder
            // để vẽ icon lên bằng ImageSpan. Kèm 1 TabStopSpan để ký tự tab nhảy tới sát lề phải
            // của view (nếu đã biết bề rộng view) - nhờ đó icon luôn canh sát lề phải bất kể
            // label dài ngắn thế nào. Toàn bộ (label + icon) dùng chung 1 ClickableSpan để bấm
            // đâu trên hàng cũng đổi trạng thái được, không dùng link/activity/script click thường.
            val toggleDrawable = if (isToggle) buildToggleDrawable(context, row) else null
            val text = if (isToggle) "$label\t\u2002" else label
            val length = text.length
            val spannableString = SpannableString(text)

            if (isToggle && toggleDrawable != null) {
                val iconIndex = length - 1
                spannableString.setSpan(VerticalCenterImageSpan(toggleDrawable), iconIndex, iconIndex + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (availableWidth > 0) {
                    val tabStop = (availableWidth - toggleDrawable.bounds.width()).coerceAtLeast(0)
                    spannableString.setSpan(TabStopSpan.Standard(tabStop), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

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

            if (row.strikethrough) {
                spannableString.setSpan(StrikethroughSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            if (row.monospace) {
                @Suppress("DEPRECATION")
                spannableString.setSpan(TypefaceSpan("monospace"), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            if (isToggle) {
                spannableString.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        row.checked = !row.checked
                        if (row.onChangeSh.isNotEmpty()) {
                            ScriptEnvironmen.executeResultRoot(context, row.onChangeSh, config, object : HashMap<String, String>() {
                                init { put("state", if (row.checked) "1" else "0") }
                            })
                        }
                        // Vẽ lại toàn bộ rows để cập nhật icon vừa đổi trạng thái
                        bind(context, rowsView, extraIconView, rows, config)
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        ds.isUnderlineText = false
                    }
                }, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else if (row.link.isNotEmpty()) {
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

            if (!isToggle && row.activity.isNotEmpty()) {
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

            if (!isToggle && row.onClickScript.isNotEmpty()) {
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

        // Chặn crash Editor.touchPositionIsInSelection khi long-press vào vùng text
        // (đã bỏ tính năng copy nội dung khi long-press).
        rowsView.setOnLongClickListener {
            true
        }
    }

    // ImageSpan.ALIGN_CENTER canh icon theo giữa cả dòng (line box), nhưng dòng có thể cao hơn
    // vùng chữ thật (do line spacing, dấu, ...) khiến icon bị lệch trên/dưới so với text xung
    // quanh. Class này canh icon theo giữa vùng chữ thật (ascent/descent của Paint tại vị trí
    // vẽ) để icon luôn thẳng hàng với text.
    private class VerticalCenterImageSpan(drawable: Drawable) : ImageSpan(drawable) {
        override fun draw(canvas: Canvas, text: CharSequence?, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
            val b = drawable
            canvas.save()

            val fontMetrics = paint.fontMetricsInt
            val textCenter = y + (fontMetrics.descent + fontMetrics.ascent) / 2f
            val transY = textCenter - b.bounds.height() / 2f

            canvas.translate(x, transY)
            b.draw(canvas)
            canvas.restore()
        }
    }

    // Tạo drawable icon cho row dạng toggle (checkbox/switch), kích thước nhỏ vừa 1 dòng text,
    // chọn ảnh theo loại (checkbox/switch) và trạng thái hiện tại (checked/unchecked).
    private fun buildToggleDrawable(context: Context, row: TextNode.TextRow): Drawable {
        val density = context.resources.displayMetrics.density
        val drawableRes = if (row.toggle == "switch") {
            if (row.checked) R.drawable.kr_row_switch_on else R.drawable.kr_row_switch_off
        } else {
            if (row.checked) R.drawable.checkbox_true else R.drawable.checkbox_false
        }
        val drawable = context.getDrawable(drawableRes)!!.mutate()
        val width: Int
        val height: Int
        if (row.toggle == "switch") {
            width = (28 * density).toInt()
            height = (16 * density).toInt()
        } else {
            width = (20 * density).toInt()
            height = (20 * density).toInt()
        }
        drawable.setBounds(0, 0, width, height)
        return drawable
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