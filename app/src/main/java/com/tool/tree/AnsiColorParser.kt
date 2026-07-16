package com.tool.tree

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import java.util.regex.Pattern

object AnsiColorParser {
    // Regex chuẩn để bóc tách chính xác mã ANSI ESC [ ... m
    private val ANSI_PATTERN = Pattern.compile("\\u001B\\[([\\d;]*)m")
    
    private const val DEFAULT_LOG_COLOR = Color.LTGRAY 
    private var currentColor = DEFAULT_LOG_COLOR

    // Bảng màu chuẩn 16 màu ANSI cơ bản và mở rộng (Bright)
    private val ANSI_16_COLORS = intArrayOf(
        Color.BLACK,          // 30 - Black
        Color.RED,            // 31 - Red
        Color.GREEN,          // 32 - Green
        Color.rgb(200, 150, 0), // 33 - Yellow (Dùng màu sậm hơn chút để không bị chói trên nền trắng)
        Color.BLUE,           // 34 - Blue
        Color.MAGENTA,        // 35 - Magenta
        Color.CYAN,           // 36 - Cyan
        Color.WHITE,          // 37 - White
        // 90 - 97: Các phiên bản sáng hơn (Bright/High Intensity)
        Color.DKGRAY,         // 90 - Bright Black (Dark Gray)
        Color.rgb(255, 85, 85),    // 91 - Bright Red
        Color.rgb(85, 255, 85),    // 92 - Bright Green
        Color.rgb(255, 255, 85),   // 93 - Bright Yellow
        Color.rgb(85, 85, 255),    // 94 - Bright Blue
        Color.rgb(255, 85, 255),   // 95 - Bright Magenta
        Color.rgb(85, 255, 255),   // 96 - Bright Cyan
        Color.WHITE                // 97 - Bright White
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
                val spanStart = builder.length
                builder.append(part)
                builder.setSpan(
                    ForegroundColorSpan(currentColor),
                    spanStart,
                    builder.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            // Lấy phần tham số nằm giữa '[' và 'm' (ví dụ: "1;31" hoặc "38;5;208")
            val paramsStr = matcher.group(1) ?: ""
            currentColor = parseAnsiParameters(paramsStr, currentColor)
            lastHeight = end
        }

        if (lastHeight < text.length) {
            val part = text.substring(lastHeight)
            val spanStart = builder.length
            builder.append(part)
            builder.setSpan(
                ForegroundColorSpan(currentColor),
                spanStart,
                builder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return builder
    }

    /**
     * Parse chuỗi tham số ANSI (ví dụ: "1;31;40") để tìm màu chữ phù hợp
     */
    private fun parseAnsiParameters(paramsStr: String, defaultColor: Int): Int {
        if (paramsStr.isEmpty() || paramsStr == "0") {
            return DEFAULT_LOG_COLOR
        }

        val tokens = paramsStr.split(';')
        var i = 0
        var color = defaultColor

        while (i < tokens.size) {
            val code = tokens[i].toIntOrNull() ?: 0
            when {
                // Reset về mặc định
                code == 0 || code == 39 -> {
                    color = DEFAULT_LOG_COLOR
                }
                // 16 Màu cơ bản (30 - 37) và màu sáng (90 - 97)
                code in 30..37 -> {
                    color = ANSI_16_COLORS[code - 30]
                }
                code in 90..97 -> {
                    color = ANSI_16_COLORS[code - 90 + 8]
                }
                // Hỗ trợ chế độ màu mở rộng (256 màu và TrueColor)
                code == 38 -> {
                    if (i + 1 < tokens.size) {
                        val mode = tokens[i + 1].toIntOrNull() ?: 0
                        if (mode == 5 && i + 2 < tokens.size) {
                            // 256 màu: 38;5;ID
                            val colorId = tokens[i + 2].toIntOrNull() ?: 0
                            color = get256Color(colorId)
                            i += 2
                        } else if (mode == 2 && i + 4 < tokens.size) {
                            // TrueColor 24-bit: 38;2;R;G;B
                            val r = tokens[i + 2].toIntOrNull() ?: 0
                            val g = tokens[i + 3].toIntOrNull() ?: 0
                            val b = tokens[i + 4].toIntOrNull() ?: 0
                            color = Color.rgb(
                                r.coerceIn(0, 255),
                                g.coerceIn(0, 255),
                                b.coerceIn(0, 255)
                            )
                            i += 4
                        }
                    }
                }
            }
            i++
        }
        return color
    }

    /**
     * Chuyển đổi mã màu ID (0-255) thành màu ARGB tương ứng trong Android
     */
    private fun get256Color(colorId: Int): Int {
        val id = colorId.coerceIn(0, 255)
        return when {
            // 16 màu cơ bản đầu tiên
            id < 16 -> ANSI_16_COLORS[id]
            // Khối màu 6x6x6 (Chỉ số từ 16 đến 231)
            id in 16..231 -> {
                val offset = id - 16
                val r = (offset / 36) * 51
                val g = ((offset % 36) / 6) * 51
                val b = (offset % 6) * 51
                Color.rgb(r, g, b)
            }
            // Thang đo màu xám (Chỉ số từ 232 đến 255)
            else -> {
                val grayValue = 8 + (id - 232) * 10
                Color.rgb(grayValue, grayValue, grayValue)
            }
        }
    }

    fun reset() {
        currentColor = DEFAULT_LOG_COLOR
    }
}