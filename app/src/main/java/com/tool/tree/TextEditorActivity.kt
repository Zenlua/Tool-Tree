package com.tool.tree

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.lifecycleScope
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
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
import java.io.File

class TextEditorActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_FILE = "file"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_DESC = "desc"
        private const val EXTRA_WRAP = "wrap"
        private const val EXTRA_DIR = "dir"
        private const val UNDO_HISTORY_LIMIT = 200
        private const val UNDO_DEBOUNCE_MS = 700L

        private val RUNNABLE_EXTENSIONS = mapOf("sh" to "sh", "bash" to "bash", "py" to "python")

        fun start(context: Context, file: String, title: String? = null, desc: String? = null, wrap: Boolean = true, dir: String? = null) {
            val intent = Intent(context, TextEditorActivity::class.java).apply {
                putExtra(EXTRA_FILE, file)
                putExtra(EXTRA_TITLE, title ?: "")
                putExtra(EXTRA_DESC, desc ?: "")
                putExtra(EXTRA_WRAP, wrap)
                putExtra(EXTRA_DIR, dir ?: "")
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
    private var savedContent: String = ""
    private var isNewFile = false
    private var isSaving = false
    private var noWrapContainer: HorizontalScrollView? = null
    private var mainListBaseBottomMargin = 0
    private var syntaxHighlighter: SyntaxHighlighter? = null
    private val progressBarDialog by lazy { ProgressBarDialog(this) }

    private data class EditorSnapshot(val text: String, val cursor: Int)
    private val undoStack = ArrayDeque<EditorSnapshot>()
    private val redoStack = ArrayDeque<EditorSnapshot>()
    private var pendingUndoSnapshot: EditorSnapshot? = null
    private var isApplyingHistory = false
    private var undoButton: ImageButton? = null
    private var redoButton: ImageButton? = null
    private val undoHandler = Handler(Looper.getMainLooper())
    private val commitPendingUndoRunnable = Runnable { commitPendingUndoSnapshot() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeModeState.switchTheme(this)
        binding = ActivityTextEditorBinding.inflate(layoutInflater).also { setContentView(it.root) }
        setupKeyboardInsets()

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        
        supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
        
        // Đảm bảo nút điều hướng Back trên Toolbar luôn chạy vào logic xác nhận đóng
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

        applyWrapState()
        setupCursorAutoScroll()
        setupUndoRedoTracking()
        loadFileContent()
    }

    override fun onDestroy() {
        syntaxHighlighter?.detach()
        undoHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun setupKeyboardInsets() {
        mainListBaseBottomMargin = (binding.mainList.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        val extraGapPx = (24 * resources.displayMetrics.density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val imeInset = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val systemBarsInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val keyboardHeight = (imeInset - systemBarsInset).coerceAtLeast(0)
            val keyboardVisible = keyboardHeight > 0

            val mlp = binding.mainList.layoutParams as ViewGroup.MarginLayoutParams
            mlp.bottomMargin = mainListBaseBottomMargin + keyboardHeight + if (keyboardVisible) extraGapPx else 0
            binding.mainList.layoutParams = mlp

            if (keyboardVisible) binding.mainList.post { scrollToCursor() }
            insets
        }
    }

    private fun setupCursorAutoScroll() {
        binding.editorContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
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
        val visibleTop = scrollView.scrollY
        val visibleBottom = visibleTop + scrollView.height - scrollView.paddingBottom

        when {
            lineBottom > visibleBottom -> scrollView.smoothScrollTo(0, lineBottom - scrollView.height + scrollView.paddingBottom)
            lineTop < visibleTop -> scrollView.smoothScrollTo(0, lineTop)
        }
    }

    private fun resolveAbsolutePath(dir: String, file: String): String {
        if (file.startsWith("/")) return file
        val base = dir.ifEmpty { FileWrite.getPrivateFileDir(applicationContext) }
        return if (base.endsWith("/")) base + file else "$base/$file"
    }

    private fun fileExtension() = File(absoluteFilePath).name.substringAfterLast('.', "").lowercase()

    private fun runnableInterpreter(): String? = RUNNABLE_EXTENSIONS[fileExtension()]

    private fun setupSyntaxHighlighting() {
        syntaxHighlighter?.detach()
        syntaxHighlighter = SyntaxHighlighterFactory.createForPath(absoluteFilePath, binding.editorContent)?.apply { attach() }
    }

    private fun setupUndoRedoButtons(toolbar: Toolbar) {
        // Tránh add trùng lặp View khi cấu hình Activity thay đổi
        toolbar.findViewWithTag<View>("undo_btn")?.let { toolbar.removeView(it) }
        toolbar.findViewWithTag<View>("redo_btn")?.let { toolbar.removeView(it) }

        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        val tint = ColorStateList.valueOf(typedValue.data)
        val buttonSize = (48 * resources.displayMetrics.density).toInt()
        val iconPadding = (12 * resources.displayMetrics.density).toInt()

        fun makeButton(iconRes: Int, descRes: Int, tagStr: String, onClick: () -> Unit): ImageButton {
            return ImageButton(this).apply {
                tag = tagStr
                setImageResource(iconRes)
                contentDescription = getString(descRes)
                setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
                val outValue = TypedValue()
                theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
                setBackgroundResource(outValue.resourceId)
                ImageViewCompat.setImageTintList(this, tint)
                setOnClickListener { onClick() }
                layoutParams = Toolbar.LayoutParams(buttonSize, buttonSize).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL }
            }
        }

        undoButton = makeButton(R.drawable.ic_editor_undo, R.string.editor_undo, "undo_btn") { performUndo() }
        redoButton = makeButton(R.drawable.ic_editor_redo, R.string.editor_redo, "redo_btn") { performRedo() }

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
    }

    private fun performUndo() {
        undoHandler.removeCallbacks(commitPendingUndoRunnable)
        val currentText = binding.editorContent.text?.toString().orEmpty()
        val currentCursor = binding.editorContent.selectionStart.coerceAtLeast(0)
        val target = pendingUndoSnapshot?.also { pendingUndoSnapshot = null } ?: if (undoStack.isEmpty()) return else undoStack.removeLast()

        redoStack.addLast(EditorSnapshot(currentText, currentCursor))
        applyHistorySnapshot(target)
        refreshUndoRedoButtons()
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
    }

    private fun applyHistorySnapshot(snapshot: EditorSnapshot) {
        isApplyingHistory = true
        binding.editorContent.apply {
            text?.let { it.replace(0, it.length, snapshot.text) } ?: setText(snapshot.text)
            setSelection(snapshot.cursor.coerceIn(0, text?.length ?: 0))
        }
        isApplyingHistory = false
    }

    private fun refreshUndoRedoButtons() {
        val canUndo = undoStack.isNotEmpty() || pendingUndoSnapshot != null
        val canRedo = redoStack.isNotEmpty()
        undoButton?.apply { isEnabled = canUndo; isClickable = canUndo; alpha = if (canUndo) 1f else 0.3f }
        redoButton?.apply { isEnabled = canRedo; isClickable = canRedo; alpha = if (canRedo) 1f else 0.3f }
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
            } catch (_: Exception) {}

            withContext(Dispatchers.Main) {
                isNewFile = newFile
                savedContent = content
                isApplyingHistory = true
                binding.editorContent.setText(content)
                isApplyingHistory = false
                undoHandler.removeCallbacks(commitPendingUndoRunnable)
                pendingUndoSnapshot = null
                undoStack.clear()
                redoStack.clear()
                refreshUndoRedoButtons()
                binding.editorContent.hint = getString(if (newFile) R.string.editor_hint_new_file else R.string.editor_hint_empty)
                setupSyntaxHighlighting()
            }
        }
    }

    private fun applyWrapState() {
        val editText = binding.editorContent
        val scrollView = binding.mainList
        val currentText = editText.text?.toString().orEmpty()
        val cursorStart = editText.selectionStart.coerceIn(0, currentText.length)
        val cursorEnd = editText.selectionEnd.coerceIn(0, currentText.length)

        (editText.parent as? ViewGroup)?.removeView(editText)
        noWrapContainer?.let { scrollView.removeView(it) }
        editText.setHorizontallyScrolling(!wrapEnabled)

        if (wrapEnabled) {
            editText.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            scrollView.addView(editText)
        } else {
            val hsv = noWrapContainer ?: HorizontalScrollView(this).also {
                it.isFillViewport = false
                it.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                noWrapContainer = it
            }
            editText.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            hsv.apply { removeAllViews(); addView(editText); layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); scrollTo(0, 0) }
            scrollView.addView(hsv)
        }

        isApplyingHistory = true
        editText.setText(currentText)
        isApplyingHistory = false
        editText.setSelection(cursorStart.coerceAtMost(currentText.length), cursorEnd.coerceAtMost(currentText.length))
    }

    private fun hasUnsavedChanges() = binding.editorContent.text?.toString().orEmpty() != savedContent

    private fun attemptClose() {
        if (isSaving) return
        if (!hasUnsavedChanges()) { finish(); return }
        
        // Cấu hình DialogButton truyền dạng Boolean kết hợp kèm block lắng nghe kết quả ở cuối
        DialogHelper.confirm(
            this, 
            title = getString(R.string.editor_unsaved_title), 
            message = getString(R.string.editor_unsaved_message),
            onConfirm = DialogHelper.DialogButton(getString(R.string.editor_save), true),
            onCancel = DialogHelper.DialogButton(getString(R.string.editor_discard), false)
        ) { confirmed -> // Nhận kết quả Boolean trả về khi người dùng tương tác với một trong hai nút
            if (confirmed) {
                saveFile(showProgress = false, showToast = true) { success -> 
                    if (success) finish() 
                }
            } else {
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_text_editor, menu)
        menu.findItem(R.id.editor_menu_wrap)?.isChecked = wrapEnabled
        menu.findItem(R.id.editor_menu_run)?.isVisible = runnableInterpreter() != null
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.editor_menu_run -> { runnableInterpreter()?.let { saveAndRun(it) }; true }
            R.id.editor_menu_save -> { saveFile(); true }
            R.id.editor_menu_wrap -> { wrapEnabled = !wrapEnabled; item.isChecked = wrapEnabled; applyWrapState(); true }
            android.R.id.home -> { attemptClose(); true } // Xử lý sự kiện nhấn nút Back trên thanh Menu gốc
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveFile(showProgress: Boolean = false, showToast: Boolean = true, onResult: ((Boolean) -> Unit)? = null) {
        if (isSaving) return
        isSaving = true
        val content = binding.editorContent.text?.toString().orEmpty()

        lifecycleScope.launch(Dispatchers.IO) {
            val success = writeFileContent(absoluteFilePath, content)
            withContext(Dispatchers.Main) {
                isSaving = false
                if (success) {
                    savedContent = content
                    isNewFile = false
                }
                if (showToast) Toast.makeText(this@TextEditorActivity, if (success) R.string.editor_save_success else R.string.editor_save_fail, if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
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
        } catch (_: Exception) {}

        if (!KeepShellPublic.checkRoot()) return false
        val cachePath = FileWrite.getPrivateFilePath(this, "editor_cache/tmp_${System.currentTimeMillis()}.tmp")
        return try {
            val cacheFile = File(cachePath).apply { parentFile?.mkdirs(); writeText(content, Charsets.UTF_8) }
            val command = """
                mkdir -p "${File(path).parent ?: "/"}"
                cp -f "$cachePath" "$path"
                chmod 644 "$path"
                if [ -f "$path" ]; then echo EDITOR_SAVE_OK; fi
            """.trimIndent()
            val result = KeepShellPublic.doCmdSync(command)
            cacheFile.delete()
            result.trim().contains("EDITOR_SAVE_OK")
        } catch (_: Exception) { false }
    }

    private fun saveAndRun(interpreter: String) {
        saveFile(showProgress = false, showToast = false) { if (it) runScript(interpreter) else Toast.makeText(this, R.string.editor_save_fail, Toast.LENGTH_LONG).show() }
    }

    private fun runScript(interpreter: String) {
        val runNode = ActionNode(absoluteFilePath).apply { title = File(absoluteFilePath).name; interruptable = true; shell = RunnableNode.shellModeDefault }
        val script = "chmod 755 \"$absoluteFilePath\" 2>/dev/null\n$interpreter \"$absoluteFilePath\"\n"
        DialogLogFragment.create(runNode, {}, {}, script, null, ThemeModeState.isDarkMode()).apply { isCancelable = false }.show(supportFragmentManager, "editor-run-test")
    }
}