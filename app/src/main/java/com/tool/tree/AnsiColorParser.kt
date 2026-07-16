package com.tool.tree

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import java.util.regex.Pattern

object AnsiColorParser {
    private val ANSI_PATTERN = Pattern.compile("\\u001B\\[([\\d;]*)m")
    
    private const val DEFAULT_FOREGROUND = Color.LTGRAY 
    private const val DEFAULT_BACKGROUND = Color.TRANSPARENT // Mặc định không có màu nền

    // Cấu trúc lưu trữ trạng thái định dạng hiện tại của luồng log
    private var currentFgColor = DEFAULT_FOREGROUND
    private var currentBgColor = DEFAULT_BACKGROUND
    private var isBold = false
    private var isUnderline = false

    // Bảng 16 màu ANSI cơ bản (cho cả Foreground và Background)
    private val ANSI_16_COLORS = intArrayOf(
        Color.BLACK,              // 0 - Black
        Color.RED,                // 1 - Red
        Color.GREEN,              // 2 - Green
        Color.rgb(200, 150, 0),   // 3 - Yellow (Tối đi một chút để tránh chói mắt)
        Color.BLUE,               // 4 - Blue
        Color.MAGENTA,            // 5 - Magenta
        Color.CYAN,               // 6 - Cyan
        Color.WHITE,              // 7 - White
        // 8 - 15: Bright Colors (Màu sáng)
        Color.DKGRAY,             // 8 - Bright Black (Dark Gray)
        Color.rgb(255, 85, 85),        // 9 - Bright Red
        Color.rgb(85, 255, 85),        // 10 - Bright Green
        Color.rgb(255, 255, 85),       // 11 - Bright Yellow
        Color.rgb(85, 85, 255),        // 12 - Bright Blue
        Color.rgb(255, 85, 255),       // 13 - Bright Magenta
        Color.rgb(85, 255, 255),       // 14 - Bright Cyan
        Color.WHITE                    // 15 - Bright White
    )

    fun parse(text: String?): CharSequence {
        if (text == null) return ""
        val builder = SpannableStringBuilder()
        val matcher = ANSI_PATTERN.matcher(text)
        var lastHeight = 0

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()

            if (start > lastHeight) {
                val part = text.substring(lastHeight, start)
                appendStyledText(builder, part)
            }

            val paramsStr = matcher.group(1) ?: ""
            parseAnsiParameters(paramsStr)
            lastHeight = end
        }

        if (lastHeight < text.length) {
            val part = text.substring(lastHeight)
            appendStyledText(builder, part)
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

        // 1. Áp dụng màu chữ (Foreground)
        builder.setSpan(
            ForegroundColorSpan(currentFgColor),
            spanStart,
            spanEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // 2. Áp dụng màu nền (Background) nếu có thiết lập màu khác trong suốt
        if (currentBgColor != Color.TRANSPARENT) {
            builder.setSpan(
                BackgroundColorSpan(currentBgColor),
                spanStart,
                spanEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // 3. Áp dụng kiểu chữ in đậm (Bold)
        if (isBold) {
            builder.setSpan(
                StyleSpan(Typeface.BOLD),
                spanStart,
                spanEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // 4. Áp dụng kiểu chữ gạch chân (Underline)
        if (isUnderline) {
            builder.setSpan(
                UnderlineSpan(),
                spanStart,
                spanEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    /**
     * Parse chuỗi tham số ANSI để cập nhật trạng thái định dạng (in đậm, màu chữ, màu nền,...)
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
                // Reset định dạng về mặc định
                code == 0 -> resetToDefault()

                // Kiểu chữ (Style)
                code == 1 -> isBold = true
                code == 4 -> isUnderline = true
                code == 21 -> isBold = false // Tắt in đậm (Double underline hoặc bold off tùy terminal)
                code == 22 -> isBold = false // Tắt in đậm thông thường
                code == 24 -> isUnderline = false // Tắt gạch chân

                // Màu chữ mặc định (Default Foreground)
                code == 39 -> currentFgColor = DEFAULT_FOREGROUND

                // Màu nền mặc định (Default Background)
                code == 49 -> currentBgColor = DEFAULT_BACKGROUND

                // Màu chữ cơ bản (30 - 37) và màu chữ sáng (90 - 97)
                code in 30..37 -> currentFgColor = ANSI_16_COLORS[code - 30]
                code in 90..97 -> currentFgColor = ANSI_16_COLORS[code - 90 + 8]

                // Màu nền cơ bản (40 - 47) và màu nền sáng (100 - 107)
                code in 40..47 -> currentBgColor = ANSI_16_COLORS[code - 40]
                code in 100..107 -> currentBgColor = ANSI_16_COLORS[code - 100 + 8]

                // Chế độ màu chữ mở rộng (38;5;ID hoặc 38;2;R;G;B)
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

                // Chế độ màu nền mở rộng (48;5;ID hoặc 48;2;R;G;B)
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
        isUnderline = false
    }

    fun reset() {
        resetToDefault()
    }
}