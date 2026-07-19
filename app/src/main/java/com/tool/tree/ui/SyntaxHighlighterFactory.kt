package com.tool.tree.ui

import android.graphics.Typeface
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
 *
 * Supported by default:
 * - Shell scripts: .sh, .bash, .zsh, .ksh
 * - XML files: .xml
 * - build.prop / .prop / .properties
 *
 * Usage:
 *   val highlighter = SyntaxHighlighterFactory.create("sh", editText)
 *   highlighter?.attach()
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

    companion object {
        private const val MAX_HIGHLIGHT_LENGTH = 300_000
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
                highlight()
            }
        }
        et.addTextChangedListener(watcher)
        highlight()
    }

    override fun detach() {
        val et = editText ?: return
        watcher?.let { et.removeTextChangedListener(it) }
        watcher = null
    }

    override fun highlight() {
        val et = editText ?: return
        val text = et.text ?: return
        if (isHighlighting) return

        if (text.length > MAX_HIGHLIGHT_LENGTH) {
            // File quá lớn: tô màu lại toàn bộ trên mỗi lần gõ sẽ làm treo UI thread (ANR).
            // Bỏ qua tô màu cú pháp cho các file lớn, người dùng vẫn gõ/sửa bình thường.
            return
        }

        isHighlighting = true
        try {
            applyHighlight(text)
        } catch (_: Throwable) {
            // Không để một lỗi ở bước tô màu cú pháp (vd. regex gặp nội dung bất thường)
            // làm crash toàn bộ app — bỏ qua lần tô màu này là đủ, nội dung gõ vẫn giữ nguyên.
        } finally {
            isHighlighting = false
        }
    }

    protected fun clearSpans(text: Editable) {
        val spans = text.getSpans(0, text.length, Any::class.java)
        for (span in spans) {
            when (span) {
                is ForegroundColorSpan, is StyleSpan -> text.removeSpan(span)
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

    override fun applyHighlight(text: Editable) {
        clearSpans(text)
        val s = text.toString()

        // Comments
        Regex("""(?m)#.*$""").findAll(s).forEach {
            color(text, it.range.first, it.range.last + 1, commentColor())
        }

        // Strings: double quotes, single quotes, backticks
        // Lưu ý: nhánh escape (\\.) và nhánh "mọi ký tự khác" phải KHÔNG được chồng lấn nhau
        // (cả hai cùng khớp được ký tự '\') — nếu chồng lấn, một chuỗi chưa đóng (thiếu dấu
        // đóng ") kèm nhiều dấu '\' sẽ khiến regex engine backtrack theo cấp số nhân và ném
        // StackOverflowError, làm crash app ngay khi người dùng đang gõ dở. Loại bỏ '\\' khỏi
        // lớp ký tự [^"\\] để hai nhánh không còn khớp trùng nhau nữa.
        Regex(""""(?:\\.|[^"\\])*"""").findAll(s).forEach {
            color(text, it.range.first, it.range.last + 1, stringColor())
        }
        Regex("""'(?:[^']*)'""").findAll(s).forEach {
            color(text, it.range.first, it.range.last + 1, stringColor())
        }
        Regex("""`(?:\\.|[^`\\])*`""").findAll(s).forEach {
            color(text, it.range.first, it.range.last + 1, stringColor())
        }

        // Command substitution
        Regex("""\$\((?:[^()]*|\([^()]*\))*\)""").findAll(s).forEach {
            color(text, it.range.first, it.range.last + 1, builtinColor())
        }

        // Variables
        Regex("""\$\{[A-Za-z_][A-Za-z0-9_]*[^}]*}""").findAll(s).forEach {
            color(text, it.range.first, it.range.last + 1, numberColor())
        }
        Regex("""\$[A-Za-z_][A-Za-z0-9_]*""").findAll(s).forEach {
            color(text, it.range.first, it.range.last + 1, numberColor())
        }

        // Numbers
        Regex("""(?<![A-Za-z0-9_])(?:0x[0-9A-Fa-f]+|[0-9]+)(?![A-Za-z0-9_])""").findAll(s).forEach {
            color(text, it.range.first, it.range.last + 1, numberColor())
        }

        // Keywords / builtins
        Regex("""(?<![A-Za-z0-9_])([A-Za-z_][A-Za-z0-9_]*)(?![A-Za-z0-9_])""").findAll(s).forEach {
            val word = it.value
            when {
                keywords.contains(word) -> {
                    color(text, it.range.first, it.range.last + 1, keywordColor())
                    bold(text, it.range.first, it.range.last + 1)
                }
                builtins.contains(word) -> color(text, it.range.first, it.range.last + 1, builtinColor())
            }
        }

        // Punctuation/operators
        Regex("""&&|\|\||\||;|\(|\)|\{|\}|\[\[|\]\]|<|>""").findAll(s).forEach {
            color(text, it.range.first, it.range.last + 1, punctuationColor())
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
    override fun applyHighlight(text: Editable) {
        clearSpans(text)
        val s = text.toString()

        // XML comments
        Regex("""(?s)<!--.*?-->""").findAll(s).forEach {
            color(text, it.range.first, it.range.last + 1, commentColor())
        }

        // CDATA
        Regex("""(?s)<!\[CDATA\[.*?\]\]>""").findAll(s).forEach {
            color(text, it.range.first, it.range.last + 1, stringColor())
        }

        // Tags
        Regex("""</?[A-Za-z_][A-Za-z0-9_:\-\.]*""").findAll(s).forEach {
            color(text, it.range.first, it.range.last + 1, keywordColor())
            bold(text, it.range.first, it.range.last + 1)
        }

        // Attribute names (the part before =")
        Regex("""\b[A-Za-z_][A-Za-z0-9_:\-\.]*(?=\s*=)""").findAll(s).forEach {
            color(text, it.range.first, it.range.last + 1, builtinColor())
        }

        // Attribute values (xem ghi chú về backtracking ở ShellSyntaxHighlighter phía trên)
        Regex(""""(?:\\.|[^"\\])*"""").findAll(s).forEach {
            color(text, it.range.first, it.range.last + 1, stringColor())
        }

        // Entities
        Regex("""&(?:amp|lt|gt|apos|quot|#x?[0-9A-Fa-f]+);""").findAll(s).forEach {
            color(text, it.range.first, it.range.last + 1, numberColor())
        }

        // Angle brackets / punctuation
        Regex("""</?|/>|>""").findAll(s).forEach {
            color(text, it.range.first, it.range.last + 1, punctuationColor())
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
    override fun applyHighlight(text: Editable) {
        clearSpans(text)
        val s = text.toString()

        // Comments
        Regex("""(?m)^\s*[#!].*$""").findAll(s).forEach {
            color(text, it.range.first, it.range.last + 1, commentColor())
        }

        // Key = value
        Regex("""(?m)^\s*([A-Za-z0-9_.-]+)(\s*=)""").findAll(s).forEach {
            val line = it.value
            val keyEnd = line.indexOf('=')
            if (keyEnd > 0) {
                color(text, it.range.first, it.range.first + keyEnd, keywordColor())
                color(text, it.range.first + keyEnd, it.range.first + keyEnd + 1, punctuationColor())
                bold(text, it.range.first, it.range.first + keyEnd)
            }
        }

        // Quoted values (xem ghi chú về backtracking ở ShellSyntaxHighlighter phía trên)
        Regex(""""(?:\\.|[^"\\])*"""").findAll(s).forEach {
            color(text, it.range.first, it.range.last + 1, stringColor())
        }

        // Numbers
        Regex("""(?<![A-Za-z0-9_])(?:0x[0-9A-Fa-f]+|[0-9]+)(?![A-Za-z0-9_])""").findAll(s).forEach {
            color(text, it.range.first, it.range.last + 1, numberColor())
        }

        // Separators
        Regex("""=""").findAll(s).forEach {
            color(text, it.range.first, it.range.last + 1, punctuationColor())
        }
    }
}
