package com.tool.tree

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import java.util.regex.Pattern

object AnsiColorParser {
    // Regex chuẩn để bóc tách chính xác mã ANSI ESC [ ... m
    private val ANSI_PATTERN = Pattern.compile("\\u001B\\[([\\d;]*)m")
    
    private const val DEFAULT_FOREGROUND = Color.LTGRAY 
    private var currentFgColor = DEFAULT_FOREGROUND

    // Bảng 16 màu ANSI cơ bản và màu sáng (Bright) cho chữ
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
     * Chỉ áp dụng duy nhất ForegroundColorSpan (màu chữ) cho đoạn văn bản
     */
    private fun appendStyledText(builder: SpannableStringBuilder, text: String) {
        val spanStart = builder.length
        builder.append(text)
        val spanEnd = builder.length

        if (spanStart == spanEnd) return

        // Chỉ gán màu chữ hiện tại lên đoạn text
        builder.setSpan(
            ForegroundColorSpan(currentFgColor),
            spanStart,
            spanEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    /**
     * Parse chuỗi tham số ANSI để chỉ cập nhật màu chữ (Foreground)
     */
    private fun parseAnsiParameters(paramsStr: String) {
        if (paramsStr.isEmpty() || paramsStr == "0") {
            currentFgColor = DEFAULT_FOREGROUND
            return
        }

        val tokens = paramsStr.split(';')
        var i = 0

        while (i < tokens.size) {
            val code = tokens[i].toIntOrNull() ?: 0
            when {
                // Reset định dạng về mặc định
                code == 0 || code == 39 -> currentFgColor = DEFAULT_FOREGROUND

                // Màu chữ cơ bản (30 - 37) và màu chữ sáng (90 - 97)
                code in 30..37 -> currentFgColor = ANSI_16_COLORS[code - 30]
                code in 90..97 -> currentFgColor = ANSI_16_COLORS[code - 90 + 8]

                // Chế độ màu chữ mở rộng (38;5;ID hoặc 38;2;R;G;B)
                code == 38 -> {
                    if (i + 1 < tokens.size) {
                        val mode = tokens[i + 1].toIntOrNull() ?: 0
                        if (mode == 5 && i + 2 < tokens.size) {
                            // 256 màu: 38;5;ID
                            val colorId = tokens[i + 2].toIntOrNull() ?: 0
                            currentFgColor = get256Color(colorId)
                            i += 2
                        } else if (mode == 2 && i + 4 < tokens.size) {
                            // TrueColor RGB: 38;2;R;G;B
                            val r = tokens[i + 2].toIntOrNull() ?: 0
                            val g = tokens[i + 3].toIntOrNull() ?: 0
                            val b = tokens[i + 4].toIntOrNull() ?: 0
                            currentFgColor = Color.rgb(
                                r.coerceIn(0, 255), 
                                g.coerceIn(0, 255), 
                                b.coerceIn(0, 255)
                            )
                            i += 4
                        }
                    }
                }
                
                // Mọi mã màu nền (40-47, 48, 100-107) hay in đậm (1), gạch chân (4) 
                // đều được bỏ qua ở đây (Không xử lý)
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

    fun reset() {
        currentFgColor = DEFAULT_FOREGROUND
    }
}