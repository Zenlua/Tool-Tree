package com.tool.tree.ui

import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.EditText
import androidx.annotation.ColorInt
import java.lang.ref.WeakReference
import java.util.Locale

/**
 * Lightweight syntax highlighter for plain EditText.
 * Fully optimized using Debounce mechanism and Combined Named-Group Regex.
 */
object SyntaxHighlighterFactory {
    fun create(extension: String?, editText: EditText): SyntaxHighlighter? {
        val ext = extension.orEmpty().lowercase(Locale.ROOT)
        return when (ext) {
            "sh", "bash", "zsh", "ksh" -> ShellSyntaxHighlighter(editText)
            "xml" -> XmlSyntaxHighlighter(editText)
            "prop", "properties" -> PropSyntaxHighlighter(editText)
            else -> null
        }
    }

    fun createForPath(path: String?, editText: EditText): SyntaxHighlighter? {
        val ext = path.orEmpty().substringAfterLast('.', "")
        return create(ext, editText)
    }
}

interface SyntaxHighlighter {
    fun attach()
    fun detach()
    fun highlight()
}

abstract class BaseSyntaxHighlighter(
    editText: EditText,
    @ColorInt private val keywordColor: Int,
    @ColorInt private val builtinColor: Int,
    @ColorInt private val stringColor: Int,
    @ColorInt private val commentColor: Int,
    @ColorInt private val numberColor: Int,
    @ColorInt private val punctuationColor: Int,
) : SyntaxHighlighter {

    private val editTextRef = WeakReference(editText)
    private var watcher: TextWatcher? = null
    private var isHighlighting = false

    // Handler để điều phối bộ hoãn thực thi (Debounce)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val highlightRunnable = Runnable { highlight() }

    companion object {
        private const val MAX_HIGHLIGHT_LENGTH = 150_000 // Giới hạn an toàn để tránh lag UI
        private const val DEBOUNCE_DELAY_MS = 150L       // Đợi 150ms sau khi dừng gõ mới tô màu
    }

    protected val editText: EditText?
        get() = editTextRef.get()

    protected abstract fun applyHighlight(text: Editable)

    override fun attach() {
        val et = editText ?: return
        if (watcher != null) return

        watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (isHighlighting || s == null) return
                
                // Hủy lịch trình cũ, lên lịch trình mới để gom các phím gõ nhanh lại
                mainHandler.removeCallbacks(highlightRunnable)
                mainHandler.postDelayed(highlightRunnable, DEBOUNCE_DELAY_MS)
            }
        }
        et.addTextChangedListener(watcher)
        highlight() // Chạy lần đầu tiên ngay khi gắn vào
    }

    override fun detach() {
        mainHandler.removeCallbacks(highlightRunnable)
        val et = editText ?: return
        watcher?.let { et.removeTextChangedListener(it) }
        watcher = null
    }

    override fun highlight() {
        val et = editText ?: return
        val text = et.text ?: return
        if (isHighlighting) return

        if (text.length > MAX_HIGHLIGHT_LENGTH) {
            // File quá lớn: Bỏ qua để giữ hiệu năng gõ mượt mà cho người dùng
            return
        }

        isHighlighting = true
        try {
            applyHighlight(text)
        } catch (_: Throwable) {
            // Tránh crash ứng dụng nếu có lỗi phân tích cú pháp xảy ra ngoài ý muốn
        } finally {
            isHighlighting = false
        }
    }

    protected fun clearSpans(text: Editable) {
        // Chỉ bóc tách đúng các loại Spans do hệ thống tô màu này định nghĩa
        val colorSpans = text.getSpans(0, text.length, ForegroundColorSpan::class.java)
        for (span in colorSpans) {
            text.removeSpan(span)
        }
        val styleSpans = text.getSpans(0, text.length, StyleSpan::class.java)
        for (span in styleSpans) {
            if (span.style == Typeface.BOLD) {
                text.removeSpan(span)
            }
        }
    }

    protected fun color(text: Editable, start: Int, end: Int, @ColorInt color: Int) {
        if (start < 0 || end <= start || start >= text.length) return
        val safeEnd = end.coerceAtMost(text.length)
        text.setSpan(
            ForegroundColorSpan(color),
            start,
            safeEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    protected fun bold(text: Editable, start: Int, end: Int) {
        if (start < 0 || end <= start || start >= text.length) return
        val safeEnd = end.coerceAtMost(text.length)
        text.setSpan(
            StyleSpan(Typeface.BOLD),
            start,
            safeEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    protected fun keywordColor(): Int = keywordColor
    protected fun builtinColor(): Int = builtinColor
    protected fun stringColor(): Int = stringColor
    protected fun commentColor(): Int = commentColor
    protected fun numberColor(): Int = numberColor
    protected fun punctuationColor(): Int = punctuationColor
}

class ShellSyntaxHighlighter(editText: EditText) : BaseSyntaxHighlighter(
    editText = editText,
    keywordColor = 0xFF3D8BFF.toInt(),
    builtinColor = 0xFFFFA000.toInt(),
    stringColor = 0xFF2EAD4B.toInt(),
    commentColor = 0xFF8A8A8A.toInt(),
    numberColor = 0xFFAA66CC.toInt(),
    punctuationColor = 0xFF808080.toInt(),
) {

    private val keywords = setOf(
        "if", "then", "else", "elif", "fi",
        "for", "while", "until", "do", "done",
        "case", "esac", "in", "function", "select",
        "time", "coproc"
    )

    private val builtins = setOf(
        "echo", "cd", "pwd", "export", "unset", "alias", "exec",
        "source", ".", "printf", "test", "read", "shift", "set",
        "local", "return", "trap", "kill", "wait", "jobs", "fg", "bg",
        "true", "false", "type", "command", "eval", "let"
    )

    private val shellRegex = Regex(
        "(?<COMMENT>(?m)#.*$)" +
        "|(?<STRING>\"(?:\\\\.|[^\"\\\\])*\"|'(?:[^']*)'|`(?:\\\\.|[^`\\\\])*`)" +
        "|(?<COMMAND>\\$\\((?:[^()]*|\\([^()]*\\))*\\))" +
        "|(?<VARIABLE>\\$\\{[A-Za-z_][A-Za-z0-9_]*[^}]*\\}|\\$[A-Za-z_][A-Za-z0-9_]*)" +
        "|(?<NUMBER>(?<![A-Za-z0-9_])(?:0x[0-9A-Fa-f]+|[0-9]+)(?![A-Za-z0-9_]))" +
        "|(?<WORD>(?<![A-Za-z0-9_])[A-Za-z_][A-Za-z0-9_]*(?![A-Za-z0-9_]))" +
        "|(?<PUNCTUATION>&&|\\|\\||\\||;|\\(|\\)|\\{|\\}|\\[\\[|\\]\\]|<|>)"
    )

    override fun applyHighlight(text: Editable) {
        clearSpans(text)
        val s = text.toString()

        shellRegex.findAll(s).forEach { result ->
            val groups = result.groups
            val start = result.range.first
            val end = result.range.last + 1

            when {
                groups["COMMENT"] != null -> color(text, start, end, commentColor())
                groups["STRING"] != null -> color(text, start, end, stringColor())
                groups["COMMAND"] != null -> color(text, start, end, builtinColor())
                groups["VARIABLE"] != null -> color(text, start, end, numberColor())
                groups["NUMBER"] != null -> color(text, start, end, numberColor())
                groups["PUNCTUATION"] != null -> color(text, start, end, punctuationColor())
                groups["WORD"] != null -> {
                    val word = result.value
                    if (keywords.contains(word)) {
                        color(text, start, end, keywordColor())
                        bold(text, start, end)
                    } else if (builtins.contains(word)) {
                        color(text, start, end, builtinColor())
                    }
                }
            }
        }
    }
}

class XmlSyntaxHighlighter(editText: EditText) : BaseSyntaxHighlighter(
    editText = editText,
    keywordColor = 0xFF3D8BFF.toInt(),
    builtinColor = 0xFFFFA000.toInt(),
    stringColor = 0xFF2EAD4B.toInt(),
    commentColor = 0xFF8A8A8A.toInt(),
    numberColor = 0xFFAA66CC.toInt(),
    punctuationColor = 0xFF808080.toInt(),
) {
    private val xmlRegex = Regex(
        "(?<COMMENT>(?s)<!--.*?-->)" +
        "|(?<CDATA>(?s)<!\\[CDATA\\[.*?\\]\\]>)" +
        "|(?<TAG></?[A-Za-z_][A-Za-z0-9_:\\-\\.]*)" +
        "|(?<ATTR>\\b[A-Za-z_][A-Za-z0-9_:\\-\\.]*(?=\\s*=))" +
        "|(?<VALUE>\"(?:\\\\.|[^\"\\\\])*\")" +
        "|(?<ENTITY>&(?:amp|lt|gt|apos|quot|#x?[0-9A-Fa-f]+);)" +
        "|(?<PUNCTUATION></?|/>|>[^<]*)"
    )

    override fun applyHighlight(text: Editable) {
        clearSpans(text)
        val s = text.toString()

        xmlRegex.findAll(s).forEach { result ->
            val groups = result.groups
            val start = result.range.first
            val end = result.range.last + 1

            when {
                groups["COMMENT"] != null -> color(text, start, end, commentColor())
                groups["CDATA"] != null -> color(text, start, end, stringColor())
                groups["TAG"] != null -> {
                    color(text, start, end, keywordColor())
                    bold(text, start, end)
                }
                groups["ATTR"] != null -> color(text, start, end, builtinColor())
                groups["VALUE"] != null -> color(text, start, end, stringColor())
                groups["ENTITY"] != null -> color(text, start, end, numberColor())
                groups["PUNCTUATION"] != null -> {
                    // Chỉ tô màu ký tự phân tách thực tế (<, >, />, </) không bao gồm phần text trần bên ngoài
                    val rawValue = result.value
                    val punctEnd = when {
                        rawValue.startsWith("</") -> start + 2
                        rawValue.startsWith("/>") -> start + 2
                        rawValue.startsWith("<") || rawValue.startsWith(">") -> start + 1
                        else -> end
                    }
                    color(text, start, punctEnd, punctuationColor())
                }
            }
        }
    }
}

class PropSyntaxHighlighter(editText: EditText) : BaseSyntaxHighlighter(
    editText = editText,
    keywordColor = 0xFF3D8BFF.toInt(),
    builtinColor = 0xFFFFA000.toInt(),
    stringColor = 0xFF2EAD4B.toInt(),
    commentColor = 0xFF8A8A8A.toInt(),
    numberColor = 0xFFAA66CC.toInt(),
    punctuationColor = 0xFF808080.toInt(),
) {
    private val propRegex = Regex(
        "(?<COMMENT>(?m)^\\s*[#!].*$)" +
        "|(?<KEYVALUE>(?m)^\\s*([A-Za-z0-9_.-]+)(\\s*=))" +
        "|(?<STRING>\"(?:\\\\.|[^\"\\\\])*\")" +
        "|(?<NUMBER>(?<![A-Za-z0-9_])(?:0x[0-9A-Fa-f]+|[0-9]+)(?![A-Za-z0-9_]))" +
        "|(?<SEPARATOR>=)"
    )

    override fun applyHighlight(text: Editable) {
        clearSpans(text)
        val s = text.toString()

        propRegex.findAll(s).forEach { result ->
            val groups = result.groups
            val start = result.range.first
            val end = result.range.last + 1

            when {
                groups["COMMENT"] != null -> color(text, start, end, commentColor())
                groups["KEYVALUE"] != null -> {
                    val line = result.value
                    val keyEnd = line.indexOf('=')
                    if (keyEnd > 0) {
                        color(text, start, start + keyEnd, keywordColor())
                        color(text, start + keyEnd, start + keyEnd + 1, punctuationColor())
                        bold(text, start, start + keyEnd)
                    }
                }
                groups["STRING"] != null -> color(text, start, end, stringColor())
                groups["NUMBER"] != null -> color(text, start, end, numberColor())
                groups["SEPARATOR"] != null -> color(text, start, end, punctuationColor())
            }
        }
    }
}