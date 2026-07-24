package com.tool.tree

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import java.util.regex.Pattern

object AnsiColorParser {

    // CSI: ESC [ params intermediate finalByte   (SGR dùng finalByte = 'm', ngoài ra còn
    // cursor move/clear line/hide cursor... với finalByte khác -> bị bỏ qua không render)
    private val CSI_PATTERN = Pattern.compile(
        "\\u001B\\[([0-9;:?]*)([ -/]*)([@-~])"
    )

    // OSC: ESC ] body (BEL | ESC \)  — dùng cho hyperlink (8;;URL) và các OSC khác (vd 0;title)
    private val OSC_PATTERN = Pattern.compile(
        "\\u001B\\]([^\\u0007\\u001B]*)(?:\\u0007|\\u001B\\\\)"
    )

    // Escape đơn dạng ESC + 1 ký tự không phải '[' hoặc ']' (vd chọn charset ESC(B, ESC)0...)
    private val SIMPLE_ESC_PATTERN = Pattern.compile(
        "\\u001B[()#][A-Za-z0-9]|\\u001B[=>]"
    )

    private const val DEFAULT_FOREGROUND = Color.LTGRAY
    private const val DEFAULT_BACKGROUND = Color.TRANSPARENT

    // Trạng thái định dạng hiện tại của luồng log (giữ nguyên xuyên suốt các lần parse)
    private var currentFgColor = DEFAULT_FOREGROUND
    private var currentBgColor = DEFAULT_BACKGROUND
    private var isBold = false
    private var isItalic = false
    private var isUnderline = false
    private var isStrikethrough = false
    private var isReverse = false
    private var isDim = false

    private val ANSI_16_COLORS = intArrayOf(
        Color.BLACK,
        Color.RED,
        Color.GREEN,
        Color.rgb(200, 150, 0),
        Color.BLUE,
        Color.MAGENTA,
        Color.CYAN,
        Color.WHITE,
        Color.DKGRAY,
        Color.rgb(255, 85, 85),
        Color.rgb(85, 255, 85),
        Color.rgb(255, 255, 85),
        Color.rgb(85, 85, 255),
        Color.rgb(255, 85, 255),
        Color.rgb(85, 255, 255),
        Color.WHITE
    )

    fun parse(text: String?): CharSequence {
        if (text == null) return ""

        val builder = SpannableStringBuilder()
        val plainBuffer = StringBuilder()

        var pendingLinkUrl: String? = null
        var pendingLinkStart = -1

        var i = 0
        val len = text.length

        fun flushPlainBuffer() {
            if (plainBuffer.isNotEmpty()) {
                appendStyledText(builder, plainBuffer.toString())
                plainBuffer.setLength(0)
            }
        }

        while (i < len) {
            val c = text[i]

            if (c != '\u001B') {
                plainBuffer.append(c)
                i++
                continue
            }

            // Gặp ESC -> flush phần text thường đã tích lũy trước khi xử lý escape
            flushPlainBuffer()

            val nextChar = if (i + 1 < len) text[i + 1] else '\u0000'

            when (nextChar) {
                '[' -> {
                    val m = CSI_PATTERN.matcher(text)
                    m.region(i, len)
                    if (m.lookingAt()) {
                        val params = m.group(1) ?: ""
                        val finalByte = m.group(3)
                        if (finalByte == "m") {
                            parseAnsiParameters(params)
                        }
                        // Các CSI khác (cursor move, clear line, hide cursor...) -> bỏ qua, không render
                        i = m.end()
                    } else {
                        i++ // escape hỏng/không đầy đủ, bỏ qua 1 ký tự để tránh vòng lặp vô hạn
                    }
                }

                ']' -> {
                    val m = OSC_PATTERN.matcher(text)
                    m.region(i, len)
                    if (m.lookingAt()) {
                        val body = m.group(1) ?: ""
                        if (body.startsWith("8;")) {
                            // OSC 8 hyperlink: "8;params;URL"
                            val url = body.substringAfterLast(';', "")
                            if (url.isEmpty()) {
                                // Thẻ đóng link
                                if (pendingLinkUrl != null && pendingLinkStart in 0..builder.length) {
                                    builder.setSpan(
                                        URLSpan(pendingLinkUrl),
                                        pendingLinkStart,
                                        builder.length,
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )
                                }
                                pendingLinkUrl = null
                                pendingLinkStart = -1
                            } else {
                                // Thẻ mở link
                                pendingLinkUrl = url
                                pendingLinkStart = builder.length
                            }
                        }
                        // Các OSC khác (đặt title cửa sổ, v.v.) -> bỏ qua, không hiển thị ra text
                        i = m.end()
                    } else {
                        i++
                    }
                }

                else -> {
                    val m = SIMPLE_ESC_PATTERN.matcher(text)
                    m.region(i, len)
                    if (m.lookingAt()) {
                        i = m.end() // bỏ qua, không có nội dung hiển thị
                    } else {
                        i++ // ESC đơn lẻ không rõ dạng -> bỏ qua chính nó
                    }
                }
            }
        }

        flushPlainBuffer()

        // Nếu chuỗi kết thúc mà link vẫn chưa đóng (log bị cắt giữa chừng), vẫn áp span cho phần đã có
        if (pendingLinkUrl != null && pendingLinkStart in 0 until builder.length) {
            builder.setSpan(
                URLSpan(pendingLinkUrl),
                pendingLinkStart,
                builder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return builder
    }

    /**
     * Thêm chuỗi văn bản và áp dụng tất cả cấu hình span đang hoạt động
     */
    private fun appendStyledText(builder: SpannableStringBuilder, text: String) {
        val spanStart = builder.length
        builder.append(text)
        val spanEnd = builder.length

        if (spanStart == spanEnd) return

        var fg = currentFgColor
        var bg = currentBgColor

        // Reverse video: hoán đổi fg/bg khi hiển thị
        if (isReverse) {
            val realBg = if (bg == Color.TRANSPARENT) Color.BLACK else bg
            val tmp = fg
            fg = realBg
            bg = tmp
        }

        // Dim/faint: giảm độ chói của màu chữ
        if (isDim) {
            fg = Color.argb(
                180,
                Color.red(fg), Color.green(fg), Color.blue(fg)
            )
        }

        builder.setSpan(
            ForegroundColorSpan(fg),
            spanStart, spanEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        if (bg != Color.TRANSPARENT) {
            builder.setSpan(
                BackgroundColorSpan(bg),
                spanStart, spanEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        if (isBold && isItalic) {
            builder.setSpan(
                StyleSpan(Typeface.BOLD_ITALIC),
                spanStart, spanEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } else if (isBold) {
            builder.setSpan(
                StyleSpan(Typeface.BOLD),
                spanStart, spanEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } else if (isItalic) {
            builder.setSpan(
                StyleSpan(Typeface.ITALIC),
                spanStart, spanEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        if (isUnderline) {
            builder.setSpan(
                UnderlineSpan(),
                spanStart, spanEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        if (isStrikethrough) {
            builder.setSpan(
                StrikethroughSpan(),
                spanStart, spanEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    /**
     * Parse chuỗi tham số SGR để cập nhật trạng thái định dạng
     */
    private fun parseAnsiParameters(paramsStr: String) {
        if (paramsStr.isEmpty() || paramsStr == "0") {
            resetToDefault()
            return
        }

        val tokens = paramsStr.split(';')
        var i = 0

        while (i < tokens.size) {
            val code = tokens[i].toIntOrNull() ?: 0
            when {
                code == 0 -> resetToDefault()

                code == 1 -> isBold = true
                code == 2 -> isDim = true
                code == 3 -> isItalic = true
                code == 4 -> isUnderline = true
                code == 7 -> isReverse = true
                code == 9 -> isStrikethrough = true

                code == 21 -> isBold = false // double underline / bold-off tùy terminal
                code == 22 -> { isBold = false; isDim = false }
                code == 23 -> isItalic = false
                code == 24 -> isUnderline = false
                code == 27 -> isReverse = false
                code == 29 -> isStrikethrough = false

                code == 39 -> currentFgColor = DEFAULT_FOREGROUND
                code == 49 -> currentBgColor = DEFAULT_BACKGROUND

                code in 30..37 -> currentFgColor = ANSI_16_COLORS[code - 30]
                code in 90..97 -> currentFgColor = ANSI_16_COLORS[code - 90 + 8]

                code in 40..47 -> currentBgColor = ANSI_16_COLORS[code - 40]
                code in 100..107 -> currentBgColor = ANSI_16_COLORS[code - 100 + 8]

                // Màu chữ mở rộng: 38;5;ID hoặc 38;2;R;G;B
                code == 38 -> {
                    if (i + 1 < tokens.size) {
                        val mode = tokens[i + 1].toIntOrNull() ?: 0
                        if (mode == 5 && i + 2 < tokens.size) {
                            val colorId = tokens[i + 2].toIntOrNull() ?: 0
                            currentFgColor = get256Color(colorId)
                            i += 2
                        } else if (mode == 2 && i + 4 < tokens.size) {
                            val r = tokens[i + 2].toIntOrNull() ?: 0
                            val g = tokens[i + 3].toIntOrNull() ?: 0
                            val b = tokens[i + 4].toIntOrNull() ?: 0
                            currentFgColor = Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
                            i += 4
                        }
                    }
                }

                // Màu nền mở rộng: 48;5;ID hoặc 48;2;R;G;B
                code == 48 -> {
                    if (i + 1 < tokens.size) {
                        val mode = tokens[i + 1].toIntOrNull() ?: 0
                        if (mode == 5 && i + 2 < tokens.size) {
                            val colorId = tokens[i + 2].toIntOrNull() ?: 0
                            currentBgColor = get256Color(colorId)
                            i += 2
                        } else if (mode == 2 && i + 4 < tokens.size) {
                            val r = tokens[i + 2].toIntOrNull() ?: 0
                            val g = tokens[i + 3].toIntOrNull() ?: 0
                            val b = tokens[i + 4].toIntOrNull() ?: 0
                            currentBgColor = Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
                            i += 4
                        }
                    }
                }
            }
            i++
        }
    }

    private fun get256Color(colorId: Int): Int {
        val id = colorId.coerceIn(0, 255)
        return when {
            id < 16 -> ANSI_16_COLORS[id]
            id in 16..231 -> {
                val offset = id - 16
                val r = (offset / 36) * 51
                val g = ((offset % 36) / 6) * 51
                val b = (offset % 6) * 51
                Color.rgb(r, g, b)
            }
            else -> {
                val grayValue = 8 + (id - 232) * 10
                Color.rgb(grayValue, grayValue, grayValue)
            }
        }
    }

    private fun resetToDefault() {
        currentFgColor = DEFAULT_FOREGROUND
        currentBgColor = DEFAULT_BACKGROUND
        isBold = false
        isItalic = false
        isUnderline = false
        isStrikethrough = false
        isReverse = false
        isDim = false
    }

    fun reset() {
        resetToDefault()
    }
}
