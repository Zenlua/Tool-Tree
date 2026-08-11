package com.omarea.krscript.ui

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.*
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.omarea.common.ui.BlurEngine
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
        // LinkMovementMethod mặc định: bấm bất kỳ đâu trên dòng (kể cả vùng trống do canh lề)
        // cũng tính là bấm trúng ClickableSpan của dòng đó. Dùng bản tuỳ chỉnh bên dưới để chỉ
        // nhận chạm trong vùng chữ/icon thực sự được vẽ.
        rowsView.movementMethod = BoundedLinkMovementMethod.instance // 不设置 ClickableSpan 点击没反应
        rowsView.visibility = View.VISIBLE

        // BoundedLinkMovementMethod chỉ "nuốt" (consume) sự kiện chạm khi trúng đúng ClickableSpan
        // (link/toggle/activity/script). Chạm vào phần còn lại của rowsView (chữ thường không có
        // action, hoặc khoảng trống) sẽ KHÔNG được tiêu thụ -> Android coi như rowsView "không xử
        // lý" và để sự kiện rơi xuống cho View cha (thường là toàn bộ item list) xử lý click, gây
        // bấm nhầm vào action/kênh của item. Đặt rowsView clickable + listener rỗng để nó tự nuốt
        // hết các lượt chạm còn lại, không rơi xuống item cha.
        rowsView.isClickable = true
        rowsView.setOnClickListener { }

        // bind() thường được gọi lúc RecyclerView/ListView bind item - tức TRƯỚC khi view được
        // đo/layout lần đầu (rowsView.width == 0 với view mới inflate). Tính spacer canh giữa/phải
        // cho row toggle cần width thật nên sẽ không tính được ở lần bind đầu này -> đánh dấu để
        // sau khi layout xong thì bind lại 1 lần.
        var needsRebindAfterLayout = false
        // Chỉ chèn "\n" trước 1 row nếu đã có nội dung trước đó - tránh trường hợp row ĐẦU TIÊN
        // có breakRow/align != normal khiến "\n" bị chèn vào lúc rowsView còn rỗng, tạo ra 1 dòng
        // trống thừa ở trên cùng (canh phải/giữa mới bị vì mới có "\n" chèn cho cả row đầu).
        var hasContent = false
        // BUG CŨ: AlignmentSpan/LeadingMarginSpan chỉ áp dụng được cho CẢ PARAGRAPH (dòng), không
        // áp dụng riêng cho 1 đoạn ký tự. Vì vậy khi 2 row toggle (checkbox/switch) canh lề khác
        // nhau (VD: 1 trái, 1 phải) cùng nằm chung 1 dòng (không breakRow), 2 span cấp-paragraph
        // này tranh chấp nhau -> canh lề phải/giữa bị sai (đây là lỗi user báo).
        // FIX: với row toggle có align != normal, không dùng AlignmentSpan/LeadingMarginSpan nữa,
        // mà chèn 1 "spacer" vô hình (ImageSpan với Drawable rỗng, đo đúng bề rộng cần đệm) NGAY
        // TRƯỚC nội dung row đó để đẩy nó sang phải/giữa. Spacer là span cấp-KÝ TỰ nên nhiều row
        // canh lề khác nhau có thể cùng tồn tại trên 1 dòng mà không xung đột - cho phép 2 nút
        // toggle (VD: 1 trái, 1 phải) hiển thị chung 1 dòng.
        // lineContentWidth: tổng bề rộng (px) nội dung đã có trên dòng HIỆN TẠI (reset về 0 mỗi
        // khi xuống dòng thật sự - "\n"), dùng để tính phần trống còn lại cần đệm cho row tiếp theo.
        var lineContentWidth = 0f

        for (row in rows) {
            val isToggle = row.toggle == "checkbox" || row.toggle == "switch"

            // row.line = true: chèn 1 dòng chỉ chứa đường kẻ mảnh (full chiều rộng) NGAY TRƯỚC
            // nội dung row này, dùng để tách riêng phần rows (hoặc tách nhóm row) trực quan.
            var skipLeadingBreak = false
            if (row.line) {
                if (hasContent) {
                    rowsView.append("\n")
                }
                val dividerLine = SpannableString(" ")
                dividerLine.setSpan(DividerSpan(context), 0, dividerLine.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                rowsView.append(dividerLine)
                rowsView.append("\n")
                hasContent = true
                skipLeadingBreak = true
                lineContentWidth = 0f
            }

            // CODE MỚI: Chỉ xuống dòng khi row chủ động bật breakRow = true
            if (!skipLeadingBreak && hasContent && row.breakRow) {
                rowsView.append("\n")
                lineContentWidth = 0f
            }

            // Nếu có khai báo "sh": lấy nội dung dòng bằng cách chạy lệnh shell, thay vì dùng "text" tĩnh
            val label = if (row.dynamicTextSh.isNotEmpty()) {
                ScriptEnvironmen.executeResultRoot(context, row.dynamicTextSh, config)
            } else {
                row.text
            }

            // Row dạng toggle (checkbox/switch nhỏ): chèn thêm 1 ký tự placeholder ở cuối (sau
            // label) để vẽ icon lên bằng ImageSpan - icon nằm ngay sau chữ. Muốn canh trái/giữa/
            // phải cho cả label+icon thì dùng field "align" ("normal"/"center"/"opposite") và
            // "break" giống hệt row text thường - không có cơ chế canh riêng cho icon. Toàn bộ
            // (label + icon) dùng chung 1 ClickableSpan để bấm đâu cũng đổi trạng thái được,
            // không dùng link/activity/script click thường.
            val text = if (isToggle) "$label \u2002 " else label
            val length = text.length
            val spannableString = SpannableString(text)

            var toggleDrawable: Drawable? = null
            // Bề rộng (px) thực tế của label + icon toggle - dùng để tính khoảng đệm còn trống
            // trên dòng khi cần canh phải/giữa. Chỉ có giá trị khi isToggle.
            var toggleContentWidth = 0f
            if (isToggle) {
                // Vị trí ký tự placeholder: ngay trước khoảng trắng cuối cùng vừa thêm
                val iconIndex = length - 2
                toggleDrawable = buildToggleDrawable(context, row)
                spannableString.setSpan(VerticalCenterImageSpan(toggleDrawable), iconIndex, iconIndex + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                val beforeIcon = text.substring(0, iconIndex)
                val afterIcon = text.substring(iconIndex + 1)
                toggleContentWidth = rowsView.paint.measureText(beforeIcon) + toggleDrawable.bounds.width() + rowsView.paint.measureText(afterIcon)
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
                            val result = ScriptEnvironmen.executeResultRoot(context, row.onChangeSh, config, object : HashMap<String, String>() {
                                init { put("state", if (row.checked) "1" else "0") }
                            })
                            if (result.trim().isNotEmpty()) {
                                DialogHelper.helpInfo(context, context.getString(R.string.kr_slice_script_result), result)
                            }
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

            if (isToggle) {
                // Row toggle canh giữa/phải: KHÔNG dùng AlignmentSpan/LeadingMarginSpan (span cấp-
                // paragraph, xem giải thích ở đầu hàm) - thay vào đó chèn 1 spacer vô hình ngay
                // trước nội dung row để đẩy nó sang phải/giữa PHẦN CÒN TRỐNG của dòng hiện tại.
                // Cách này tính đến cả nội dung đã có sẵn trên dòng (lineContentWidth), nên nhiều
                // row toggle canh lề khác nhau có thể cùng nằm 1 dòng mà không tranh chấp nhau.
                if (row.align != Layout.Alignment.ALIGN_NORMAL) {
                    if (rowsView.width == 0) {
                        // Chưa layout xong nên chưa đo được bề rộng thật -> bind lại sau khi layout.
                        needsRebindAfterLayout = true
                    } else {
                        val available = rowsView.width - rowsView.paddingLeft - rowsView.paddingRight
                        val remaining = available - lineContentWidth - toggleContentWidth
                        val leadingGap = when (row.align) {
                            Layout.Alignment.ALIGN_OPPOSITE -> remaining
                            Layout.Alignment.ALIGN_CENTER -> remaining / 2f
                            else -> 0f
                        }
                        if (leadingGap > 0f) {
                            appendSpacer(rowsView, leadingGap.toInt())
                            lineContentWidth += leadingGap
                        }
                    }
                }
                rowsView.append(spannableString)
                lineContentWidth += toggleContentWidth
            } else {
                spannableString.setSpan(AlignmentSpan.Standard(row.align), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                rowsView.append(spannableString)
                lineContentWidth += rowsView.paint.measureText(text)
            }
            hasContent = true
        }

        // Chặn crash Editor.touchPositionIsInSelection khi long-press vào vùng text
        // (đã bỏ tính năng copy nội dung khi long-press). Tắt luôn haptic feedback vì Android tự
        // rung khi performLongClick() được gọi, bất kể listener trả về true hay false.
        rowsView.isHapticFeedbackEnabled = false
        rowsView.setOnLongClickListener {
            true
        }

        if (needsRebindAfterLayout) {
            rowsView.post {
                if (rowsView.width > 0) {
                    bind(context, rowsView, extraIconView, rows, config)
                }
            }
        }
    }

    // Vẽ 1 đường kẻ mảnh ngang qua hết chiều rộng dòng - dùng cho row có "line = true". Cài qua
    // LeadingMarginSpan (không chiếm margin - getLeadingMargin trả 0) để không ảnh hưởng layout
    // ký tự, chỉ tận dụng callback drawLeadingMargin để vẽ tự do trên toàn bộ chiều rộng layout.
    private class DividerSpan(private val context: Context) : LeadingMarginSpan {
        override fun getLeadingMargin(first: Boolean): Int = 0

        override fun drawLeadingMargin(canvas: Canvas, paint: Paint, x: Int, dir: Int, top: Int, baseline: Int, bottom: Int, text: CharSequence?, start: Int, end: Int, first: Boolean, layout: Layout?) {
            val strokePaint = BlurEngine.getStrokePaint(context)
            val y = (top + bottom) / 2f
            val right = (layout?.width ?: 0).toFloat()
            canvas.drawLine(0f, y, right, y, strokePaint)
        }
    }

    // LinkMovementMethod gốc quy đổi toạ độ chạm sang offset ký tự gần nhất trong dòng
    // (Layout.getOffsetForHorizontal) mà không kiểm tra toạ độ đó có thực sự nằm trong vùng chữ
    // được vẽ hay không - nên bấm vào khoảng trống do canh lề/margin cũng bị tính là bấm trúng
    // ClickableSpan của dòng đó. Lớp này chặn trước: nếu x nằm ngoài [getLineLeft, getLineRight]
    // của dòng (tức ngoài vùng chữ/icon thực tế), bỏ qua sự kiện thay vì chuyển cho lớp cha xử lý.
    private class BoundedLinkMovementMethod : LinkMovementMethod() {
        companion object {
            val instance = BoundedLinkMovementMethod()
        }

        override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_UP) {
                val layout = widget.layout
                if (layout != null) {
                    val x = event.x.toInt() - widget.totalPaddingLeft + widget.scrollX
                    val y = event.y.toInt() - widget.totalPaddingTop + widget.scrollY
                    val line = layout.getLineForVertical(y)
                    if (x < layout.getLineLeft(line) || x > layout.getLineRight(line)) {
                        return false
                    }
                }
            }
            return super.onTouchEvent(widget, buffer, event)
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

    // Drawable "vô hình" - không vẽ gì cả, chỉ chiếm đúng 1 khoảng bề rộng cho trước. Dùng làm
    // spacer linh hoạt để đẩy nội dung row toggle sang phải/giữa dòng (xem appendSpacer bên dưới).
    private class SpaceDrawable(width: Int, height: Int) : Drawable() {
        init {
            setBounds(0, 0, width.coerceAtLeast(0), height.coerceAtLeast(1))
        }

        override fun draw(canvas: Canvas) {}
        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
        @Suppress("DEPRECATION")
        override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSPARENT
    }

    // Chèn 1 khoảng đệm vô hình rộng "widthPx" vào cuối rowsView, dùng để đẩy row TIẾP THEO
    // (thường là row toggle canh giữa/phải) sang đúng vị trí, mà không cần AlignmentSpan/
    // LeadingMarginSpan cấp-paragraph - nhờ vậy nhiều row canh lề khác nhau có thể chung 1 dòng.
    private fun appendSpacer(rowsView: TextView, widthPx: Int) {
        if (widthPx <= 0) {
            return
        }
        val spacer = SpannableString("\u2002")
        spacer.setSpan(ImageSpan(SpaceDrawable(widthPx, 1)), 0, spacer.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        rowsView.append(spacer)
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