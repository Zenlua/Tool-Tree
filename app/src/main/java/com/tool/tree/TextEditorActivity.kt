package com.tool.tree

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
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
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.krscript.config.PathAnalysis
import com.omarea.krscript.model.ActionNode
import com.omarea.krscript.model.RunnableNode
import com.omarea.krscript.ui.DialogLogFragment
import com.tool.tree.databinding.ActivityTextEditorBinding
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
            "bash" to "bash",
            "zsh" to "zsh",
            "ksh" to "ksh"
        )

        fun start(context: Context, file: String, title: String? = null, desc: String? = null, wrap: Boolean = true, dir: String? = null) {
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
    private var fabBaseBottomMargin = 0
    private val progressBarDialog by lazy { ProgressBarDialog(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeModeState.switchTheme(this)
        binding = ActivityTextEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupKeyboardInsets()

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { attemptClose() }

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
        setupFab()
        setupCursorAutoScroll()
        loadFileContent()
    }

    /**
     * App đang dùng chế độ edge-to-edge (decorFitsSystemWindows = false) nên hệ thống sẽ
     * không tự đẩy layout lên khi hiện bàn phím dù có khai báo windowSoftInputMode="adjustResize"
     * trong manifest — phải tự lắng nghe inset của bàn phím (IME) rồi tự xử lý.
     *
     * Dùng translationY (chỉ dịch hình ảnh) từng gây lỗi không kéo lên đầu được, vì vùng chạm/
     * cuộn vẫn được tính theo kích thước View cũ (translation không đổi kích thước/measurement
     * thật). Ở đây đổi sang tăng bottomMargin thật của vùng nội dung và FAB — tương đương
     * cách adjustResize thật sự hoạt động — nên kích thước, vùng cuộn và vùng chạm đều được
     * tính lại chính xác theo không gian còn trống phía trên bàn phím. Có thêm một khoảng đệm
     * dư ra để văn bản/con trỏ không dính sát vào mép bàn phím.
     */
    private fun setupKeyboardInsets() {
        val mainListParams = binding.mainList.layoutParams as ViewGroup.MarginLayoutParams
        mainListBaseBottomMargin = mainListParams.bottomMargin
        val fabParams = binding.editorFabRun.layoutParams as ViewGroup.MarginLayoutParams
        fabBaseBottomMargin = fabParams.bottomMargin
        val extraGapPx = (48 * resources.displayMetrics.density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val imeInset = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val systemBarsInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val keyboardHeight = (imeInset - systemBarsInset).coerceAtLeast(0)
            val keyboardVisible = keyboardHeight > 0
            val extraGap = if (keyboardVisible) extraGapPx else 0

            val mlp = binding.mainList.layoutParams as ViewGroup.MarginLayoutParams
            mlp.bottomMargin = mainListBaseBottomMargin + keyboardHeight + extraGap
            binding.mainList.layoutParams = mlp

            val flp = binding.editorFabRun.layoutParams as ViewGroup.MarginLayoutParams
            flp.bottomMargin = fabBaseBottomMargin + keyboardHeight + extraGap
            binding.editorFabRun.layoutParams = flp

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
            lineBottom > visibleBottom -> scrollView.smoothScrollTo(0, lineBottom - scrollView.height + scrollView.paddingBottom)
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
        return if (dotIndex >= 0 && dotIndex < name.length - 1) name.substring(dotIndex + 1).lowercase() else ""
    }

    private fun setupFab() {
        val interpreter = RUNNABLE_EXTENSIONS[fileExtension()]
        binding.editorFabRun.visibility = View.VISIBLE
        if (interpreter != null) {
            binding.editorFabRun.contentDescription = getString(R.string.editor_run_test)
            binding.editorFabRun.setImageResource(R.drawable.kr_run)
            binding.editorFabRun.setOnClickListener { saveAndRun(interpreter) }
        } else {
            binding.editorFabRun.contentDescription = getString(R.string.editor_save)
            binding.editorFabRun.setImageResource(android.R.drawable.ic_menu_save)
            binding.editorFabRun.setOnClickListener { saveFile() }
        }
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
                binding.editorContent.setText(content)
                binding.editorContent.hint = if (newFile) getString(R.string.editor_hint_new_file) else getString(R.string.editor_hint_empty)
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
            hsv.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            hsv.scrollTo(0, 0)
            scrollView.addView(hsv)
        }

        // Chỉ đổi setHorizontallyScrolling()/layoutParams thôi thì TextView không tự dựng lại
        // Layout nội bộ theo chiều rộng mới (văn bản đã dàn trang trước đó bị giữ nguyên), nên
        // phải set lại text để buộc nó dựng lại layout rồi khôi phục vị trí con trỏ.
        editText.setText(currentText)
        editText.setSelection(cursorStart.coerceAtMost(currentText.length), cursorEnd.coerceAtMost(currentText.length))
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
            onConfirm = DialogHelper.DialogButton(getString(R.string.editor_save), onClick = Runnable {
                saveFile { success -> if (success) finish() }
            }),
            onCancel = DialogHelper.DialogButton(getString(R.string.editor_discard), onClick = Runnable {
                finish()
            })
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_text_editor, menu)
        menu.findItem(R.id.editor_menu_wrap)?.isChecked = wrapEnabled
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
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
            android.R.id.home -> {
                attemptClose()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Lưu nội dung hiện tại xuống file. Thử ghi trực tiếp trước, nếu không được (thiếu quyền)
     * thì ghi ra file cache riêng của ứng dụng rồi copy đè bằng quyền root.
     */
    private fun saveFile(onResult: ((Boolean) -> Unit)? = null) {
        if (isSaving) return
        isSaving = true
        val content = binding.editorContent.text?.toString().orEmpty()
        progressBarDialog.showDialog(getString(R.string.editor_saving))

        lifecycleScope.launch(Dispatchers.IO) {
            val success = writeFileContent(absoluteFilePath, content)

            withContext(Dispatchers.Main) {
                progressBarDialog.hideDialog()
                isSaving = false
                if (success) {
                    savedContent = content
                    isNewFile = false
                    Toast.makeText(this@TextEditorActivity, R.string.editor_save_success, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@TextEditorActivity, R.string.editor_save_fail, Toast.LENGTH_LONG).show()
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
        saveFile { success ->
            if (success) {
                runScript(interpreter)
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
