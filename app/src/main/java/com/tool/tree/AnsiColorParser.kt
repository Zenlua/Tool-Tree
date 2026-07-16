package com.tool.tree

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import java.util.regex.Pattern

object AnsiColorParser {
    private val ANSI_PATTERN = Pattern.compile("\\u001B\\[[;\\d]*m")
    
    // Đặt ở đây để lưu lại màu của dòng log trước đó trong luồng stream
    // Mặc định ban đầu dùng màu xám sáng (Light Gray) để an toàn trên cả nền tối/sáng
    private const val DEFAULT_LOG_COLOR = Color.LTGRAY 
    private var currentColor = DEFAULT_LOG_COLOR

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

            val ansiCode = matcher.group()
            currentColor = getAnsiColor(ansiCode, currentColor)
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

    private fun getAnsiColor(ansiCode: String, defaultColor: Int): Int {
        // Nếu chứa mã reset (0 hoặc 39)
        if (ansiCode.contains("0m") || ansiCode.contains("39m")) {
            return DEFAULT_LOG_COLOR
        }
        
        return when {
            ansiCode.contains("30m") -> Color.BLACK
            ansiCode.contains("31m") -> Color.RED
            ansiCode.contains("32m") -> Color.GREEN
            ansiCode.contains("33m") -> Color.YELLOW // Hoặc chọn màu Orange/Gold nếu vàng quá chói
            ansiCode.contains("34m") -> Color.BLUE
            ansiCode.contains("35m") -> Color.MAGENTA
            ansiCode.contains("36m") -> Color.CYAN
            ansiCode.contains("37m") -> Color.WHITE
            else -> defaultColor
        }
    }

    // Hàm bổ sung: Gọi hàm này khi bắt đầu một phiên chạy script mới để xoá trạng thái màu cũ
    fun reset() {
        currentColor = DEFAULT_LOG_COLOR
    }
}