package com.tool.tree

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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
    private val progressBarDialog by lazy { ProgressBarDialog(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeModeState.switchTheme(this)
        binding = ActivityTextEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        loadFileContent()
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
        val horizontalScroll: HorizontalScrollView = binding.editorScrollHorizontal
        editText.setHorizontallyScrolling(!wrapEnabled)
        val params = editText.layoutParams
        params.width = if (wrapEnabled) android.view.ViewGroup.LayoutParams.MATCH_PARENT else android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        editText.layoutParams = params
        horizontalScroll.isHorizontalScrollBarEnabled = !wrapEnabled
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
