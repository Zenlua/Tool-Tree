package com.tool.tree

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.ui.DialogHelper
import com.omarea.krscript.config.PathAnalysis
import com.omarea.krscript.model.ActionNode
import com.omarea.krscript.model.RunnableNode
import com.omarea.krscript.ui.DialogLogFragment
import com.tool.tree.databinding.ActivityTextEditorBinding
import com.tool.tree.ui.SyntaxHighlighter
import com.tool.tree.ui.SyntaxHighlighterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import kotlin.math.abs

class TextEditorActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_FILE = "file"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_DESC = "desc"
        private const val EXTRA_WRAP = "wrap"
        private const val EXTRA_DIR = "dir"
        private const val UNDO_HISTORY_LIMIT = 200
        private const val UNDO_DEBOUNCE_MS = 700L
        private const val UNDO_CACHE_DEBOUNCE_MS = 400L
        private const val UNDO_CACHE_DIR = "editor_undo_cache"
    
        private const val EXTRA_PLACEHOLDER = "placeholder"
    
        private val RUNNABLE_EXTENSIONS = mapOf(
            "sh" to "sh",
            "bash" to "bash",
            "py" to "python"
        )
    
        private val FIRST_LINE_LANG_PATTERN = Regex(
            """^#!?\s*(?:/usr/bin/env\s+)?(?:/data/(?:data|user/\d+)/com\.tool\.tree/\S*/)?(python3?|py|bash|sh|shell)\b""",
            RegexOption.IGNORE_CASE
        )
    
        private fun normalizeLangToken(token: String): String? = when (token.lowercase()) {
            "python", "python3", "py" -> "python"
            "bash" -> "bash"
            "sh", "shell" -> "sh"
            else -> null
        }
    
        fun start(
            context: Context,
            file: String,
            title: String? = null,
            desc: String? = null,
            wrap: Boolean = true,
            dir: String? = null,
            placeholder: String? = null
        ) {
            val intent = Intent(context, TextEditorActivity::class.java).apply {
                putExtra(EXTRA_FILE, file)
                putExtra(EXTRA_TITLE, title ?: "")
                putExtra(EXTRA_DESC, desc ?: "")
                putExtra(EXTRA_WRAP, wrap)
                putExtra(EXTRA_DIR, dir ?: "")
                putExtra(EXTRA_PLACEHOLDER, placeholder ?: "")
                if (context !is AppCompatActivity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
    
    private lateinit var binding: ActivityTextEditorBinding
    private lateinit var filePath: String
    private lateinit var absoluteFilePath: String
    private var configDir: String = ""
    private var wrapEnabled = true
    private var monospaceEnabled = true
    private var savedContent: String = ""
    private var isSaving = false
    private var noWrapContainer: HorizontalScrollView? = null
    private var mainListBaseBottomMargin = 0
    private var syntaxHighlighter: SyntaxHighlighter? = null
    private var placeholderText: String = ""
    private var titleText: String = ""
    private var lastLanguageOverride: String? = null
    
    private data class EditorSnapshot(val text: String, val cursor: Int)
    
    private val undoStack = ArrayDeque<EditorSnapshot>()
    private val redoStack = ArrayDeque<EditorSnapshot>()
    private var pendingUndoSnapshot: EditorSnapshot? = null
    private var isApplyingHistory = false
    private var undoButton: ImageButton? = null
    private var redoButton: ImageButton? = null
    private val undoHandler = Handler(Looper.getMainLooper())
    private val commitPendingUndoRunnable = Runnable { commitPendingUndoSnapshot() }
    private val persistUndoCacheRunnable = Runnable { persistUndoCacheToDisk() }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeModeState.switchTheme(this)
        binding = ActivityTextEditorBinding.inflate(layoutInflater).also { setContentView(it.root) }
        setupKeyboardInsets()
    
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
    
        supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
    
        toolbar.setNavigationOnClickListener { attemptClose() }
        setupUndoRedoButtons(toolbar)
    
        onBackPressedDispatcher.addCallback(this) { attemptClose() }
    
        filePath = intent.getStringExtra(EXTRA_FILE) ?: ""
        if (filePath.isEmpty()) {
            Toast.makeText(this, R.string.editor_file_missing, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
    
        configDir = intent.getStringExtra(EXTRA_DIR).orEmpty()
        absoluteFilePath = resolveAbsolutePath(configDir, filePath)
        wrapEnabled = intent.getBooleanExtra(EXTRA_WRAP, true)
        placeholderText = intent.getStringExtra(EXTRA_PLACEHOLDER).orEmpty()
        titleText = intent.getStringExtra(EXTRA_TITLE).orEmpty()
    
        applyWrapState()
        applyMonospaceState()
        setupCursorAutoScroll()
        setupUndoRedoTracking()
        setupLineNumbers()
        setupSpecialCharsBar()
        setupEditorTouchAndFocus()
        loadFileContent()
    }
    
    override fun onPause() {
        commitPendingUndoSnapshot()
        persistUndoCacheToDisk()
        super.onPause()
    }
    
    override fun onDestroy() {
        syntaxHighlighter?.detach()
        undoHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
    
    private fun undoCacheFile(): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(absoluteFilePath.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val dir = File(cacheDir, UNDO_CACHE_DIR).apply { mkdirs() }
        return File(dir, "$digest.json")
    }
    
    private fun snapshotToJson(snapshot: EditorSnapshot) = JSONObject().apply {
        put("text", snapshot.text)
        put("cursor", snapshot.cursor)
    }
    
    private fun jsonToSnapshot(json: JSONObject) =
        EditorSnapshot(json.optString("text", ""), json.optInt("cursor", 0))
    
    private fun scheduleUndoCachePersist() {
        undoHandler.removeCallbacks(persistUndoCacheRunnable)
        undoHandler.postDelayed(persistUndoCacheRunnable, UNDO_CACHE_DEBOUNCE_MS)
    }
    
    private fun persistUndoCacheToDisk() {
        undoHandler.removeCallbacks(persistUndoCacheRunnable)
        if (!::binding.isInitialized) return
        val baseContent = savedContent
        val undoSnapshot = undoStack.toList()
        val redoSnapshot = redoStack.toList()
        val pending = pendingUndoSnapshot
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (undoSnapshot.isEmpty() && redoSnapshot.isEmpty() && pending == null) {
                    undoCacheFile().delete()
                    return@launch
                }
                val root = JSONObject().apply {
                    put("baseContent", baseContent)
                    put("undo", JSONArray().apply { undoSnapshot.forEach { put(snapshotToJson(it)) } })
                    put("redo", JSONArray().apply { redoSnapshot.forEach { put(snapshotToJson(it)) } })
                    pending?.let { put("pending", snapshotToJson(it)) }
                }
                undoCacheFile().writeText(root.toString())
            } catch (_: Exception) {
            }
        }
    }
    
    private fun restoreUndoCacheFromDisk() {
        try {
            val file = undoCacheFile()
            if (!file.exists()) return
            val root = JSONObject(file.readText())
            val baseContent = if (root.has("baseContent")) root.getString("baseContent") else null
            if (baseContent == null || baseContent != savedContent) {
                file.delete()
                return
            }
    
            undoStack.clear()
            redoStack.clear()
            root.optJSONArray("undo")?.let { arr ->
                for (i in 0 until arr.length()) undoStack.addLast(jsonToSnapshot(arr.getJSONObject(i)))
            }
            root.optJSONArray("redo")?.let { arr ->
                for (i in 0 until arr.length()) redoStack.addLast(jsonToSnapshot(arr.getJSONObject(i)))
            }
            pendingUndoSnapshot = root.optJSONObject("pending")?.let { jsonToSnapshot(it) }
            refreshUndoRedoButtons()
        } catch (_: Exception) {
        }
    }
    
    private fun clearUndoCache() {
        undoHandler.removeCallbacks(persistUndoCacheRunnable)
        try {
            undoCacheFile().delete()
        } catch (_: Exception) {
        }
    }
    
    private fun setupKeyboardInsets() {
        mainListBaseBottomMargin = (binding.mainList.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        val specialCharsBaseBottomMargin =
            (binding.editorSpecialCharsScroll.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
    
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val imeInset = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val sysInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            
            val targetBottomInset = if (imeInset > 0) imeInset else sysInset
    
            val charsLp = binding.editorSpecialCharsScroll.layoutParams as ViewGroup.MarginLayoutParams
            charsLp.bottomMargin = specialCharsBaseBottomMargin + targetBottomInset
            binding.editorSpecialCharsScroll.layoutParams = charsLp
    
            val mlp = binding.mainList.layoutParams as ViewGroup.MarginLayoutParams
            mlp.bottomMargin = mainListBaseBottomMargin
            binding.mainList.layoutParams = mlp
    
            if (imeInset > 0) {
                binding.mainList.post { scrollToCursor() }
            }
            insets
        }
    }
    
    private fun setupEditorTouchAndFocus() {
        val focusAndShowKeyboard = {
            binding.editorContent.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(binding.editorContent, InputMethodManager.SHOW_IMPLICIT)
        }

        binding.mainList.setOnClickListener { focusAndShowKeyboard() }
        binding.editorLineNumbers.setOnClickListener { focusAndShowKeyboard() }
        binding.editorContentContainer.setOnClickListener { focusAndShowKeyboard() }
    }
    
    private fun setupCursorAutoScroll() {
        binding.editorContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (isApplyingHistory) return
                binding.editorContent.post { scrollToCursor() }
            }
        })
    }
    
    private fun scrollToCursor() {
        val editText = binding.editorContent
        val layout = editText.layout ?: return
        val scrollView = binding.mainList
        val selection = editText.selectionEnd.coerceIn(0, editText.text?.length ?: 0)
        val line = layout.getLineForOffset(selection)
        val lineTop = layout.getLineTop(line) + editText.top + editText.paddingTop
        val lineBottom = layout.getLineBottom(line) + editText.top
    
        val extraBottomOffset = (100 * resources.displayMetrics.density).toInt()
    
        val visibleTop = scrollView.scrollY
        val visibleBottom = visibleTop + scrollView.height - scrollView.paddingBottom
    
        when {
            lineBottom + extraBottomOffset > visibleBottom -> scrollView.smoothScrollTo(
                0,
                (lineBottom + extraBottomOffset) - scrollView.height + scrollView.paddingBottom
            )
            lineTop < visibleTop -> scrollView.smoothScrollTo(0, lineTop)
        }
    }
    
    private fun setupLineNumbers() {
        binding.editorContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                updateLineNumbers()
            }
        })
        updateLineNumbers()
    }
    
    private fun updateLineNumbers() {
        val lineCount = (binding.editorContent.text?.count { it == '\n' } ?: 0) + 1
        val numbers = (1..lineCount).joinToString("\n")
        if (binding.editorLineNumbers.text.toString() != numbers) {
            binding.editorLineNumbers.text = numbers
        }
    }
    
    private fun setupSpecialCharsBar() {
        val chars = listOf(
            "Tab" to "\t",
            "|" to "|", "&" to "&",
            "\"" to "\"", "'" to "'",
            "/" to "/", "\\" to "\\",
            "$" to "$", "#" to "#",
            "{" to "{", "}" to "}",
            "(" to "(", ")" to ")",
            "[" to "[", "]" to "]",
            ";" to ";", ":" to ":",
            "<" to "<", ">" to ">",
            "@" to "@", "!" to "!",
            "=" to "=", "+" to "+",
            "-" to "-", "*" to "*",
            "~" to "~", "`" to "`",
            "_" to "_", "%" to "%"
        )
    
        val container = binding.editorSpecialCharsContainer
        container.removeAllViews()
    
        val density = resources.displayMetrics.density
        val buttonPadding = (10 * density).toInt()
        val buttonMargin = (2 * density).toInt()
        val minWidthPx = (36 * density).toInt()
    
        val outValue = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
    
        for ((label, insertText) in chars) {
            val button = TextView(this).apply {
                text = label
                typeface = Typeface.MONOSPACE
                textSize = 16f
                gravity = Gravity.CENTER
                minimumWidth = minWidthPx
                setPadding(buttonPadding, buttonPadding, buttonPadding, buttonPadding)
                setTextColor(binding.editorContent.currentTextColor)
                setBackgroundResource(outValue.resourceId)
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = buttonMargin
                    marginEnd = buttonMargin
                }
                setOnClickListener { insertAtCursor(insertText) }
            }
            container.addView(button)
        }
    }
    
    private fun insertAtCursor(text: String) {
        val editText = binding.editorContent
        val editable = editText.text ?: return
        val start = editText.selectionStart.coerceIn(0, editable.length)
        val end = editText.selectionEnd.coerceIn(0, editable.length)
        val from = minOf(start, end)
        val to = maxOf(start, end)
        editable.replace(from, to, text)
        editText.setSelection(from + text.length)
    }
    
    private fun resolveAbsolutePath(dir: String, file: String): String {
        if (file.startsWith("/")) return file
        val base = dir.ifBlank { FileWrite.getPrivateFileDir(applicationContext).orEmpty() }
        if (base.isBlank()) return file
        return File(base, file).absolutePath
    }
    
    private fun fileExtension() = File(absoluteFilePath).name.substringAfterLast('.', "").lowercase()
    
    private fun detectFirstLineLanguageOverride(): String? {
        val firstLine = binding.editorContent.text?.toString()
            ?.lineSequence()?.firstOrNull()?.trim()
            ?: return null
        if (firstLine.isEmpty()) return null
        val token = FIRST_LINE_LANG_PATTERN.find(firstLine)?.groupValues?.get(1) ?: return null
        return normalizeLangToken(token)
    }
    
    private fun runnableInterpreter(): String? =
        detectFirstLineLanguageOverride() ?: RUNNABLE_EXTENSIONS[fileExtension()]
    
    private fun setupSyntaxHighlighting() {
        syntaxHighlighter?.detach()
        val override = detectFirstLineLanguageOverride()
        lastLanguageOverride = override
        val highlightPath = when (override) {
            "python" -> "$absoluteFilePath.__lang_override.py"
            "bash" -> "$absoluteFilePath.__lang_override.bash"
            "sh" -> "$absoluteFilePath.__lang_override.sh"
            else -> absoluteFilePath
        }
        syntaxHighlighter = SyntaxHighlighterFactory.createForPath(
            highlightPath,
            binding.editorContent
        )?.apply { attach() }
    }
    
    private fun refreshLanguageOverrideIfChanged() {
        val current = detectFirstLineLanguageOverride()
        if (current != lastLanguageOverride) {
            setupSyntaxHighlighting()
            invalidateOptionsMenu()
        }
    }
    
    private fun setupUndoRedoButtons(toolbar: Toolbar) {
        toolbar.findViewWithTag<View>("undo_btn")?.let { toolbar.removeView(it) }
        toolbar.findViewWithTag<View>("redo_btn")?.let { toolbar.removeView(it) }
    
        val buttonSize = (48 * resources.displayMetrics.density).toInt()
        val iconPadding = (12 * resources.displayMetrics.density).toInt()
    
        fun makeButton(
            iconRes: Int,
            descRes: Int,
            tagStr: String,
            onClick: () -> Unit
        ): ImageButton {
            return ImageButton(this).apply {
                tag = tagStr
                setImageResource(iconRes)
                contentDescription = getString(descRes)
                setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
    
                val outValue = TypedValue()
                theme.resolveAttribute(
                    android.R.attr.selectableItemBackgroundBorderless,
                    outValue,
                    true
                )
                setBackgroundResource(outValue.resourceId)
                setOnClickListener { onClick() }
    
                layoutParams = Toolbar.LayoutParams(buttonSize, buttonSize).apply {
                    gravity = Gravity.END or Gravity.CENTER_VERTICAL
                }
            }
        }
    
        undoButton = makeButton(
            R.drawable.ic_editor_undo,
            R.string.editor_undo,
            "undo_btn"
        ) { performUndo() }
    
        redoButton = makeButton(
            R.drawable.ic_editor_redo,
            R.string.editor_redo,
            "redo_btn"
        ) { performRedo() }
    
        toolbar.addView(redoButton)
        toolbar.addView(undoButton)
        refreshUndoRedoButtons()
    }
    
    private fun setupUndoRedoTracking() {
        binding.editorContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (isApplyingHistory) return
                if (pendingUndoSnapshot == null) {
                    val cursor = binding.editorContent.selectionStart.coerceAtLeast(0)
                    pendingUndoSnapshot = EditorSnapshot(s?.toString().orEmpty(), cursor)
                }
            }
    
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
    
            override fun afterTextChanged(s: Editable?) {
                if (isApplyingHistory) return
                redoStack.clear()
                undoHandler.removeCallbacks(commitPendingUndoRunnable)
                undoHandler.postDelayed(commitPendingUndoRunnable, UNDO_DEBOUNCE_MS)
                refreshUndoRedoButtons()
                refreshLanguageOverrideIfChanged()
                scheduleUndoCachePersist()
            }
        })
    }
    
    private fun commitPendingUndoSnapshot() {
        val snapshot = pendingUndoSnapshot ?: return
        pendingUndoSnapshot = null
        val current = binding.editorContent.text?.toString().orEmpty()
        if (snapshot.text == current) return
    
        if (undoStack.size >= UNDO_HISTORY_LIMIT) undoStack.removeFirst()
        undoStack.addLast(snapshot)
        refreshUndoRedoButtons()
        scheduleUndoCachePersist()
    }
    
    private inline fun withHighlightSuspended(block: () -> Unit) {
        val highlighter = syntaxHighlighter
        highlighter?.suspendHighlighting()
        try {
            block()
        } finally {
            highlighter?.resumeHighlighting()
        }
    }
    
    private fun performUndo() {
        undoHandler.removeCallbacks(commitPendingUndoRunnable)
    
        val currentText = binding.editorContent.text?.toString().orEmpty()
        val currentCursor = binding.editorContent.selectionStart.coerceAtLeast(0)
        val target = pendingUndoSnapshot?.also { pendingUndoSnapshot = null }
            ?: if (undoStack.isEmpty()) return else undoStack.removeLast()
    
        redoStack.addLast(EditorSnapshot(currentText, currentCursor))
        applyHistorySnapshot(target)
        refreshUndoRedoButtons()
        persistUndoCacheToDisk()
    }
    
    private fun performRedo() {
        if (redoStack.isEmpty()) return
        undoHandler.removeCallbacks(commitPendingUndoRunnable)
        pendingUndoSnapshot = null
    
        val currentText = binding.editorContent.text?.toString().orEmpty()
        val currentCursor = binding.editorContent.selectionStart.coerceAtLeast(0)
        val target = redoStack.removeLast()
    
        if (undoStack.size >= UNDO_HISTORY_LIMIT) undoStack.removeFirst()
        undoStack.addLast(EditorSnapshot(currentText, currentCursor))
        applyHistorySnapshot(target)
        refreshUndoRedoButtons()
        persistUndoCacheToDisk()
    }
    
    private fun applyHistorySnapshot(snapshot: EditorSnapshot) {
        isApplyingHistory = true
        withHighlightSuspended {
            binding.editorContent.apply {
                val editable = text
                if (editable != null) {
                    editable.replace(0, editable.length, snapshot.text)
                } else {
                    setText(snapshot.text)
                }
                setSelection(snapshot.cursor.coerceIn(0, text?.length ?: 0))
            }
        }
        isApplyingHistory = false
    }
    
    private fun refreshUndoRedoButtons() {
        val canUndo = undoStack.isNotEmpty() || pendingUndoSnapshot != null
        val canRedo = redoStack.isNotEmpty()
    
        undoButton?.apply {
            isEnabled = canUndo
            isClickable = canUndo
            alpha = if (canUndo) 1f else 0.3f
        }
        redoButton?.apply {
            isEnabled = canRedo
            isClickable = canRedo
            alpha = if (canRedo) 1f else 0.3f
        }
    }
    
    private fun loadFileContent() {
        lifecycleScope.launch(Dispatchers.IO) {
            var content = ""
            var newFile = true
            try {
                PathAnalysis(applicationContext, configDir).parsePath(filePath)?.let { stream ->
                    newFile = false
                    content = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                }
            } catch (_: Exception) {
            }
    
            withContext(Dispatchers.Main) {
                savedContent = content
                isApplyingHistory = true
                withHighlightSuspended {
                    binding.editorContent.setText(content)
                    binding.editorContent.setSelection(content.length)
                }
                isApplyingHistory = false
    
                undoHandler.removeCallbacks(commitPendingUndoRunnable)
                pendingUndoSnapshot = null
                undoStack.clear()
                redoStack.clear()
    
                restoreUndoCacheFromDisk()
                refreshUndoRedoButtons()
    
                binding.editorContent.hint = when {
                    placeholderText.isNotEmpty() -> placeholderText
                    newFile -> getString(R.string.editor_hint_new_file)
                    else -> getString(R.string.editor_hint_empty)
                }
                setupSyntaxHighlighting()
            }
        }
    }
    
    private fun applyWrapState() {
        val editText = binding.editorContent
        val container = binding.editorContentContainer
        val currentText = editText.text?.toString().orEmpty()
        val cursorStart = editText.selectionStart.coerceIn(0, currentText.length)
        val cursorEnd = editText.selectionEnd.coerceIn(0, currentText.length)
    
        (editText.parent as? ViewGroup)?.removeView(editText)
        noWrapContainer?.let { container.removeView(it) }
        editText.setHorizontallyScrolling(!wrapEnabled)
    
        if (wrapEnabled) {
            editText.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            container.addView(editText)
        } else {
            val hsv = noWrapContainer ?: HorizontalScrollView(this).also {
                it.isFillViewport = true
                it.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                noWrapContainer = it
            }
            editText.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            hsv.apply {
                removeAllViews()
                addView(editText)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scrollTo(0, 0)
            }
            container.addView(hsv)
        }
    
        editText.setSelection(
            cursorStart.coerceAtMost(currentText.length),
            cursorEnd.coerceAtMost(currentText.length)
        )
    }

    private fun applyMonospaceState() {
        val targetTypeface = if (monospaceEnabled) Typeface.MONOSPACE else Typeface.DEFAULT
        binding.editorContent.typeface = targetTypeface
        binding.editorLineNumbers.typeface = targetTypeface
    }
    
    private fun hasUnsavedChanges() = binding.editorContent.text?.toString().orEmpty() != savedContent
    
    private fun attemptClose() {
        if (isSaving) return
        commitPendingUndoSnapshot()
    
        if (!hasUnsavedChanges()) {
            clearUndoCache()
            finish()
            return
        }
    
        DialogHelper.confirm(
            this,
            title = getString(R.string.editor_unsaved_title),
            message = getString(R.string.editor_unsaved_message),
            onConfirm = DialogHelper.DialogButton(getString(R.string.editor_save), Runnable {
                saveFile(showToast = true) { success ->
                    if (success && !isFinishing && !isDestroyed) finish()
                }
            }),
            onCancel = DialogHelper.DialogButton(getString(R.string.editor_discard), Runnable {
                clearUndoCache()
                finish()
            })
        )
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_text_editor, menu)
        menu.findItem(R.id.editor_menu_wrap)?.isChecked = wrapEnabled
        menu.findItem(R.id.editor_menu_monospace)?.isChecked = monospaceEnabled
        menu.findItem(R.id.editor_menu_run)?.isVisible = runnableInterpreter() != null
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.editor_menu_run -> {
                runnableInterpreter()?.let { saveAndRun(it) }
                true
            }
            R.id.editor_menu_save -> {
                saveFile()
                true
            }
            R.id.editor_menu_wrap -> {
                wrapEnabled = !wrapEnabled
                item.isChecked = wrapEnabled
                applyWrapState()
                true
            }
            R.id.editor_menu_monospace -> {
                monospaceEnabled = !monospaceEnabled
                item.isChecked = monospaceEnabled
                applyMonospaceState()
                true
            }
            android.R.id.home -> {
                attemptClose()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun saveFile(
        showToast: Boolean = true,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        if (isSaving) return
        isSaving = true
        commitPendingUndoSnapshot()
    
        val content = binding.editorContent.text?.toString().orEmpty()
    
        lifecycleScope.launch(Dispatchers.IO) {
            val success = writeFileContent(absoluteFilePath, content)
            withContext(Dispatchers.Main) {
                isSaving = false
                if (success) {
                    savedContent = content
                    persistUndoCacheToDisk()
                }
                if (showToast) {
                    Toast.makeText(
                        this@TextEditorActivity,
                        if (success) R.string.editor_save_success else R.string.editor_save_fail,
                        if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
                    ).show()
                }
                onResult?.invoke(success)
            }
        }
    }
    
    private fun writeFileContent(path: String, content: String): Boolean {
        try {
            val file = File(path)
            val parent = file.parentFile?.apply { if (!exists()) mkdirs() }
            if (parent != null && (parent.canWrite() || (file.exists() && file.canWrite()))) {
                file.writeText(content, Charsets.UTF_8)
                return true
            }
        } catch (_: Exception) {
        }
    
        if (!KeepShellPublic.checkRoot()) return false
    
        val cachePath = FileWrite.getPrivateFilePath(
            this,
            "editor_cache/tmp_${System.currentTimeMillis()}.tmp"
        )
        return try {
            val cacheFile = File(cachePath).apply {
                parentFile?.mkdirs()
                writeText(content, Charsets.UTF_8)
            }
            val command = """
                mkdir -p "${File(path).parent ?: "/"}"
                cp -f "$cachePath" "$path"
                chmod 644 "$path"
                if [ -f "$path" ]; then echo EDITOR_SAVE_OK; fi
            """.trimIndent()
            val result = KeepShellPublic.doCmdSync(command)
            cacheFile.delete()
            result.trim().contains("EDITOR_SAVE_OK")
        } catch (_: Exception) {
            false
        }
    }
    
    private fun saveAndRun(interpreter: String) {
        saveFile(showToast = false) {
            if (it) {
                runScript(interpreter)
            } else {
                Toast.makeText(this, R.string.editor_save_fail, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun runScript(interpreter: String) {
        val runNode = ActionNode(absoluteFilePath).apply {
            title = titleText.ifEmpty { File(absoluteFilePath).name }
            interruptable = true
            shell = RunnableNode.shellModeDefault
        }
        val script = "chmod 755 \"$absoluteFilePath\" 2>/dev/null\n$interpreter \"$absoluteFilePath\"\n"
        DialogLogFragment.create(
            runNode,
            {},
            {},
            script,
            null,
            ThemeModeState.isDarkMode()
        ).apply { isCancelable = false }
            .show(supportFragmentManager, "editor-run-test")
    }
}
