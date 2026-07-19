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

/**
 * Trang soạn thảo văn bản chung của ứng dụng.
 *
 * Có thể mở bằng [start], hoặc qua Intent với các extra:
 *  - file (bắt buộc): đường dẫn tuyệt đối tới file cần soạn thảo. Nếu file chưa tồn tại thì
 *    sẽ được tạo mới khi bấm Lưu.
 *  - title, desc (tùy chọn): hiển thị trên thanh tiêu đề.
 *  - wrap (tùy chọn, mặc định true): trạng thái ngắt dòng ban đầu.
 */
class TextEditorActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_FILE = "file"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_DESC = "desc"
        private const val EXTRA_WRAP = "wrap"
        private const val EXTRA_DIR = "dir"

        // Các đuôi file được coi là script shell, cho phép "chạy thử nghiệm"
        private val RUNNABLE_EXTENSIONS = mapOf(
            "sh" to "sh",
            "bash" to "bash"
        )

        // Số bước Undo tối đa được giữ lại, và thời gian tạm dừng gõ (ms) để "chốt" một bước
        // Undo mới — gộp các ký tự gõ liên tục thành một bước duy nhất.
        private const val UNDO_HISTORY_LIMIT = 200
        private const val UNDO_DEBOUNCE_MS = 700L

        fun start(
            context: Context,
            file: String,
            title: String? = null,
            desc: String? = null,
            wrap: Boolean = true,
            dir: String? = null
        ) {
            val intent = Intent(context, TextEditorActivity::class.java).apply {
                putExtra(EXTRA_FILE, file)
                putExtra(EXTRA_TITLE, title ?: "")
                putExtra(EXTRA_DESC, desc ?: "")
                putExtra(EXTRA_WRAP, wrap)
                putExtra(EXTRA_DIR, dir ?: "")
                if (context !is AppCompatActivity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
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

    // --- Undo / Redo ---
    // Lưu lại toàn bộ nội dung tại mỗi "đợt" chỉnh sửa (gộp các ký tự gõ liên tục thành một
    // bước, thay vì lưu theo từng ký tự) để nút Undo/Redo hoạt động tự nhiên như trình soạn
    // thảo thông thường. EditText mặc định của Android không có sẵn undo/redo công khai nên
    // phải tự quản lý 2 stack này.
    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()
    private var pendingUndoSnapshot: String? = null
    private var isApplyingHistory = false
    private var undoButton: ImageButton? = null
    private var redoButton: ImageButton? = null
    private val undoHandler = Handler(Looper.getMainLooper())
    private val commitPendingUndoRunnable = Runnable { commitPendingUndoSnapshot() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeModeState.switchTheme(this)
        binding = ActivityTextEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupKeyboardInsets()

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        // Bỏ nút back (mũi tên) trên toolbar theo yêu cầu — việc thoát trang giờ thực hiện qua
        // mục "Thoát" trong menu (xem onOptionsItemSelected). Nút back cứng/gesture của hệ
        // thống vẫn hoạt động bình thường nhờ onBackPressedDispatcher bên dưới.
        supportActionBar?.setHomeButtonEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        setupUndoRedoButtons(toolbar)

        onBackPressedDispatcher.addCallback(this) { attemptClose() }

        val extraFile = intent.getStringExtra(EXTRA_FILE)
        if (extraFile.isNullOrEmpty()) {
            Toast.makeText(this, R.string.editor_file_missing, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        filePath = extraFile
        configDir = intent.getStringExtra(EXTRA_DIR).orEmpty()
        absoluteFilePath = resolveAbsolutePath(configDir, filePath)

        val extraTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        wrapEnabled = intent.getBooleanExtra(EXTRA_WRAP, true)

        title = extraTitle.ifEmpty { File(absoluteFilePath).name }

        applyWrapState()
        setupCursorAutoScroll()
        setupUndoRedoTracking()
        loadFileContent()
    }

    override fun onDestroy() {
        syntaxHighlighter?.detach()
        syntaxHighlighter = null
        undoHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    /**
     * App đang dùng chế độ edge-to-edge (decorFitsSystemWindows = false) nên hệ thống sẽ
     * không tự đẩy layout lên khi hiện bàn phím dù có khai báo windowSoftInputMode="adjustResize"
     * trong manifest — phải tự lắng nghe inset của bàn phím (IME) rồi tự xử lý.
     *
     * Dùng translationY (chỉ dịch hình ảnh) từng gây lỗi không kéo lên đầu được, vì vùng chạm/
     * cuộn vẫn được tính theo kích thước View cũ (translation không đổi kích thước/measurement
     * thật). Ở đây đổi sang tăng bottomMargin thật của vùng nội dung — tương đương cách
     * adjustResize thật sự hoạt động — nên kích thước, vùng cuộn và vùng chạm đều được tính lại
     * chính xác theo không gian còn trống phía trên bàn phím. Có thêm một khoảng đệm dư ra để
     * văn bản/con trỏ không dính sát vào mép bàn phím.
     */
    private fun setupKeyboardInsets() {
        val mainListParams = binding.mainList.layoutParams as ViewGroup.MarginLayoutParams
        mainListBaseBottomMargin = mainListParams.bottomMargin
        val extraGapPx = (24 * resources.displayMetrics.density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val imeInset = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val systemBarsInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val keyboardHeight = (imeInset - systemBarsInset).coerceAtLeast(0)
            val keyboardVisible = keyboardHeight > 0
            val extraGap = if (keyboardVisible) extraGapPx else 0

            val mlp = binding.mainList.layoutParams as ViewGroup.MarginLayoutParams
            mlp.bottomMargin = mainListBaseBottomMargin + keyboardHeight + extraGap
            binding.mainList.layoutParams = mlp

            if (keyboardVisible) {
                binding.mainList.post { scrollToCursor() }
            }

            insets
        }
    }

    /**
     * Đảm bảo con trỏ luôn được cuộn vào vùng nhìn thấy mỗi khi gõ chữ/xuống dòng, kể cả khi
     * bàn phím đang chiếm một phần màn hình.
     */
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
            lineBottom > visibleBottom -> scrollView.smoothScrollTo(
                0,
                lineBottom - scrollView.height + scrollView.paddingBottom
            )
            lineTop < visibleTop -> scrollView.smoothScrollTo(0, lineTop)
        }
    }

    private fun resolveAbsolutePath(dir: String, file: String): String {
        if (file.startsWith("/")) return file
        val base = dir.ifEmpty { FileWrite.getPrivateFileDir(applicationContext) }
        return if (base.endsWith("/")) base + file else "$base/$file"
    }

    private fun fileExtension(): String {
        val name = File(absoluteFilePath).name
        val dotIndex = name.lastIndexOf(".")
        return if (dotIndex >= 0 && dotIndex < name.length - 1) {
            name.substring(dotIndex + 1).lowercase()
        } else {
            ""
        }
    }

    private fun runnableInterpreter(): String? = RUNNABLE_EXTENSIONS[fileExtension()]

    private fun setupSyntaxHighlighting() {
        syntaxHighlighter?.detach()
        syntaxHighlighter = SyntaxHighlighterFactory.createForPath(absoluteFilePath, binding.editorContent)
        syntaxHighlighter?.attach()
    }

    /**
     * Tạo 2 nút Undo/Redo và gắn vào bên trái của toolbar (thay cho vị trí nút back đã bỏ).
     */
    private fun setupUndoRedoButtons(toolbar: Toolbar) {
        val tint = ColorStateList.valueOf(resolveThemeColor(android.R.attr.textColorPrimary))
        val buttonSize = (48 * resources.displayMetrics.density).toInt()
        val iconPadding = (12 * resources.displayMetrics.density).toInt()

        fun makeButton(iconRes: Int, descRes: Int): ImageButton {
            return ImageButton(this).apply {
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
                ImageViewCompat.setImageTintList(this, tint)
                layoutParams = Toolbar.LayoutParams(buttonSize, buttonSize).apply {
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                }
            }
        }

        undoButton = makeButton(R.drawable.ic_editor_undo, R.string.editor_undo).also {
            it.setOnClickListener { performUndo() }
            toolbar.addView(it)
        }
        redoButton = makeButton(R.drawable.ic_editor_redo, R.string.editor_redo).also {
            it.setOnClickListener { performRedo() }
            toolbar.addView(it)
        }
        refreshUndoRedoButtons()
    }

    private fun resolveThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    /**
     * Theo dõi nội dung để phục vụ Undo/Redo. Các thay đổi gõ liên tục (không dừng quá
     * [UNDO_DEBOUNCE_MS]) được gộp thành một bước, để Undo/Redo hoạt động theo "đợt chỉnh sửa"
     * thay vì theo từng ký tự.
     */
    private fun setupUndoRedoTracking() {
        binding.editorContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (isApplyingHistory) return
                if (pendingUndoSnapshot == null) {
                    pendingUndoSnapshot = s?.toString().orEmpty()
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
        if (snapshot == current) return

        if (undoStack.size >= UNDO_HISTORY_LIMIT) {
            undoStack.removeFirst()
        }
        undoStack.addLast(snapshot)
        refreshUndoRedoButtons()
    }

    private fun performUndo() {
        undoHandler.removeCallbacks(commitPendingUndoRunnable)
        val current = binding.editorContent.text?.toString().orEmpty()

        val target: String
        val pending = pendingUndoSnapshot
        if (pending != null) {
            // Đang có một đợt gõ chưa kịp "chốt" vào undoStack — hoàn tác thẳng về mốc đó.
            target = pending
            pendingUndoSnapshot = null
        } else {
            if (undoStack.isEmpty()) return
            target = undoStack.removeLast()
        }

        redoStack.addLast(current)
        applyHistoryText(target)
        refreshUndoRedoButtons()
    }

    private fun performRedo() {
        if (redoStack.isEmpty()) return
        undoHandler.removeCallbacks(commitPendingUndoRunnable)
        pendingUndoSnapshot = null

        val current = binding.editorContent.text?.toString().orEmpty()
        val target = redoStack.removeLast()

        if (undoStack.size >= UNDO_HISTORY_LIMIT) {
            undoStack.removeFirst()
        }
        undoStack.addLast(current)
        applyHistoryText(target)
        refreshUndoRedoButtons()
    }

    private fun applyHistoryText(text: String) {
        isApplyingHistory = true
        val editText = binding.editorContent
        editText.setText(text)
        editText.setSelection(text.length.coerceAtLeast(0))
        isApplyingHistory = false
    }

    private fun refreshUndoRedoButtons() {
        val canUndo = undoStack.isNotEmpty() || pendingUndoSnapshot != null
        val canRedo = redoStack.isNotEmpty()
        undoButton?.isEnabled = canUndo
        undoButton?.isClickable = canUndo
        undoButton?.alpha = if (canUndo) 1f else 0.3f
        redoButton?.isEnabled = canRedo
        redoButton?.isClickable = canRedo
        redoButton?.alpha = if (canRedo) 1f else 0.3f
    }

    private fun loadFileContent() {
        progressBarDialog.showDialog(getString(R.string.please_wait))
        lifecycleScope.launch(Dispatchers.IO) {
            var content = ""
            var newFile = true
            try {
                val stream = PathAnalysis(applicationContext, configDir).parsePath(filePath)
                if (stream != null) {
                    newFile = false
                    content = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                }
            } catch (ex: Exception) {
                content = ""
            }

            withContext(Dispatchers.Main) {
                progressBarDialog.hideDialog()
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
                binding.editorContent.hint = if (newFile) {
                    getString(R.string.editor_hint_new_file)
                } else {
                    getString(R.string.editor_hint_empty)
                }

                setupSyntaxHighlighting()
            }
        }
    }

    private fun applyWrapState() {
        val editText = binding.editorContent
        val scrollView = binding.mainList

        // Lưu lại vị trí con trỏ và nội dung hiện tại trước khi đổi chế độ
        val currentText = editText.text?.toString().orEmpty()
        val cursorStart = editText.selectionStart.coerceIn(0, currentText.length)
        val cursorEnd = editText.selectionEnd.coerceIn(0, currentText.length)

        // Gỡ EditText ra khỏi cha hiện tại (có thể là ScrollView hoặc HorizontalScrollView)
        (editText.parent as? ViewGroup)?.removeView(editText)
        noWrapContainer?.let { scrollView.removeView(it) }

        editText.setHorizontallyScrolling(!wrapEnabled)

        if (wrapEnabled) {
            // Ngắt dòng: EditText là con trực tiếp của ScrollView (dọc) -> chiều rộng bị giới hạn
            // theo màn hình nên văn bản thực sự được ngắt dòng, chiều cao tự do để cuộn dọc.
            editText.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            scrollView.addView(editText)
        } else {
            // Không ngắt dòng: bọc EditText trong một HorizontalScrollView -> chiều rộng không bị
            // giới hạn nên các dòng dài sẽ kéo dài ra và cuộn ngang được, còn HorizontalScrollView
            // đó vẫn là con trực tiếp của ScrollView (dọc) nên vẫn cuộn dọc bình thường.
            val hsv = noWrapContainer ?: HorizontalScrollView(this).also {
                it.isFillViewport = false
                it.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                noWrapContainer = it
            }
            editText.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            hsv.removeAllViews()
            hsv.addView(editText)
            hsv.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            hsv.scrollTo(0, 0)
            scrollView.addView(hsv)
        }

        // Chỉ đổi setHorizontallyScrolling()/layoutParams thôi thì TextView không tự dựng lại
        // Layout nội bộ theo chiều rộng mới (văn bản đã dàn trang trước đó bị giữ nguyên), nên
        // phải set lại text để buộc nó dựng lại layout rồi khôi phục vị trí con trỏ.
        // Đây không phải là một thao tác chỉnh sửa nội dung thật sự nên không được tính vào
        // lịch sử Undo/Redo.
        isApplyingHistory = true
        editText.setText(currentText)
        isApplyingHistory = false
        editText.setSelection(
            cursorStart.coerceAtMost(currentText.length),
            cursorEnd.coerceAtMost(currentText.length)
        )
    }

    private fun hasUnsavedChanges(): Boolean {
        return binding.editorContent.text?.toString().orEmpty() != savedContent
    }

    private fun attemptClose() {
        if (isSaving) return
        if (!hasUnsavedChanges()) {
            finish()
            return
        }
        DialogHelper.confirm(
            this,
            title = getString(R.string.editor_unsaved_title),
            message = getString(R.string.editor_unsaved_message),
            onConfirm = DialogHelper.DialogButton(
                getString(R.string.editor_save),
                onClick = Runnable {
                    saveFile { success -> if (success) finish() }
                }
            ),
            onCancel = DialogHelper.DialogButton(
                getString(R.string.editor_discard),
                onClick = Runnable {
                    finish()
                }
            )
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_text_editor, menu)
        menu.findItem(R.id.editor_menu_wrap)?.isChecked = wrapEnabled
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
            R.id.editor_menu_exit -> {
                attemptClose()
                true
            }
            android.R.id.home -> {
                attemptClose()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Lưu nội dung hiện tại xuống file.
     *
     * showProgress = false: không hiện màn hình "vui lòng chờ"
     * showToast = false: không hiện toast thành công/thất bại
     */
    private fun saveFile(
        showProgress: Boolean = true,
        showToast: Boolean = true,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        if (isSaving) return
        isSaving = true

        val content = binding.editorContent.text?.toString().orEmpty()
        if (showProgress) {
            progressBarDialog.showDialog(getString(R.string.editor_saving))
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val success = writeFileContent(absoluteFilePath, content)

            withContext(Dispatchers.Main) {
                if (showProgress) {
                    progressBarDialog.hideDialog()
                }
                isSaving = false

                if (success) {
                    savedContent = content
                    isNewFile = false
                    if (showToast) {
                        Toast.makeText(
                            this@TextEditorActivity,
                            R.string.editor_save_success,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    if (showToast) {
                        Toast.makeText(
                            this@TextEditorActivity,
                            R.string.editor_save_fail,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                onResult?.invoke(success)
            }
        }
    }

    private fun writeFileContent(path: String, content: String): Boolean {
        // 1. Thử ghi trực tiếp (không cần root)
        try {
            val file = File(path)
            val parent = file.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            if (parent != null && (parent.canWrite() || (file.exists() && file.canWrite()))) {
                file.writeText(content, Charsets.UTF_8)
                return true
            }
        } catch (_: Exception) {
        }

        // 2. Ghi qua quyền root: lưu ra file cache riêng của ứng dụng rồi copy đè bằng shell root
        if (!KeepShellPublic.checkRoot()) {
            return false
        }

        val cacheName = "editor_cache/tmp_${System.currentTimeMillis()}.tmp"
        val cachePath = FileWrite.getPrivateFilePath(this, cacheName)
        return try {
            val cacheFile = File(cachePath)
            cacheFile.parentFile?.mkdirs()
            cacheFile.writeText(content, Charsets.UTF_8)

            val parentDir = File(path).parent ?: "/"
            val command = """
                mkdir -p "$parentDir"
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
        // Lưu im lặng rồi chạy ngay, không hiện progress/toast lưu file
        saveFile(
            showProgress = false,
            showToast = false
        ) { success ->
            if (success) {
                runScript(interpreter)
            } else {
                Toast.makeText(
                    this,
                    R.string.editor_save_fail,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun runScript(interpreter: String) {
        val runNode = ActionNode(absoluteFilePath).apply {
            title = File(absoluteFilePath).name
            interruptable = true
            shell = RunnableNode.shellModeDefault
        }

        val script = "chmod 755 \"$absoluteFilePath\" 2>/dev/null\n$interpreter \"$absoluteFilePath\"\n"

        val dialog = DialogLogFragment.create(
            runNode,
            Runnable {},
            Runnable {},
            script,
            null,
            ThemeModeState.isDarkMode()
        )
        dialog.isCancelable = false
        dialog.show(supportFragmentManager, "editor-run-test")
    }
}
