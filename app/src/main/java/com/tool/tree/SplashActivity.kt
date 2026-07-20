package com.tool.tree

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.TypedValue
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.ShellExecutor
import com.omarea.common.ui.DialogHelper
import com.omarea.krscript.config.StringResRef
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.SilentShellOutputHandler
import com.tool.tree.databinding.ActivitySplashBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.util.Locale
import com.tool.tree.databinding.ActivityMainBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val REQUEST_CODE_PERMISSIONS = 1001
    private val REQUEST_CODE_MANAGE_ALL_FILES = 1002

    private var hasRoot = false
    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        LanguageManager.init(this)
        super.onCreate(savedInstanceState)
        ThemeModeState.switchTheme(this)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Kiểm tra nếu script đã chạy hoặc đang chạy thì vào thẳng Home
        if (ScriptEnvironmen.isInited() && isTaskRoot &&
            !intent.getBooleanExtra("force_reset", false)) {
            gotoHome()
            return
        }

        // 2. Logic khởi đầu: Tách biệt "Đồng ý điều khoản" và "Quyền hệ thống"
        if (!hasAgreed()) {
            // Nếu chưa từng đồng ý điều khoản, hiện Dialog đầu tiên
            showAgreementDialog()
        } else {
            // Đã đồng ý điều khoản rồi, chỉ kiểm tra quyền Android
            if (hasRequiredPermissions()) {
                checkPermissionsNextStep()
            } else {
                requestRequiredPermissions()
            }
        }

        binding.startLogoXml.postDelayed({
            val animation = AnimationUtils.loadAnimation(this, R.anim.ic_settings_rotate)
            binding.startLogoXml.startAnimation(animation)
        }, 2000)

    }

    // =================== LOGIC XỬ LÝ QUYỀN ===================

    private fun showAgreementDialog() {
        DialogHelper.warning(
            this,
            getString(R.string.permission_dialog_title),
            getString(R.string.permission_dialog_message),
            Runnable {
                // Quan trọng: Lưu trạng thái đồng ý ngay khi nhấn nút
                saveAgreement()
                // Sau đó mới đi xin quyền hệ thống
                requestRequiredPermissions()
            },
            Runnable { finish() }
        ).setCancelable(false)
    }

    private fun hasRequiredPermissions(): Boolean {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_CODE_PERMISSIONS)
    }

    private fun checkPermissionsNextStep() {
        // Nếu là Android 11+ và chưa có quyền "All Files Access"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            requestManageAllFilesPermission()
        } else {
            checkRootAndStart()
        }
    }

    private fun requestManageAllFilesPermission() {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivityForResult(intent, REQUEST_CODE_MANAGE_ALL_FILES)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivityForResult(intent, REQUEST_CODE_MANAGE_ALL_FILES)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                checkPermissionsNextStep()
            } else finish()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_MANAGE_ALL_FILES) {
            checkRootAndStart()
        }
    }

    // =================== LOGIC ROOT & KHỞI CHẠY ===================

    @Synchronized
    private fun checkRootAndStart() {
        if (started) return
        started = true

        lifecycleScope.launch(Dispatchers.IO) {
            hasRoot = KeepShellPublic.checkRoot()
            withContext(Dispatchers.Main) {
                startToFinish()
            }
        }
    }

    private fun startToFinish() {
        binding.startStateText.text = getString(R.string.pop_started)
        val config = KrScriptConfig().init(this)

        if (config.beforeStartSh.isNotEmpty()) {
            runBeforeStartSh(config, hasRoot)
        } else {
            gotoHome()
        }
    }

    private fun hasAgreed(): Boolean =
        getSharedPreferences("kr-script-config", MODE_PRIVATE).getBoolean("agreed_permissions", false)

    private fun saveAgreement() {
        getSharedPreferences("kr-script-config", MODE_PRIVATE)
            .edit()
            .putBoolean("agreed_permissions", true)
            .apply()
    }

    private fun gotoHome() {
        startActivity(
            if (intent?.getBooleanExtra("JumpActionPage", false) == true)
                Intent(this, ActionPage::class.java).apply { putExtras(intent!!) }
            else Intent(this, MainActivity::class.java)
        )
        finish()
    }

    private fun runBeforeStartSh(config: KrScriptConfig, hasRoot: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val process = if (hasRoot) ShellExecutor.getSuperUserRuntime() else ShellExecutor.getRuntime()
                process?.let {
                    DataOutputStream(it.outputStream).use { os ->
                        ScriptEnvironmen.executeShell(this@SplashActivity, os, config.beforeStartSh, config.variables, null, "pio-splash")
                    }
                    launch { readStreamAsync(it.inputStream.bufferedReader()) }
                    launch { readStreamAsync(it.errorStream.bufferedReader()) }
                    it.waitFor()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    gotoHome()
                }
            }
        }
    }

    private val rows = mutableListOf<String>()
    private var ignored = false
    private val maxLines = 5
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    // Cờ tạm cho biết dòng log hiện tại kết thúc bằng '\r' (cập nhật tại chỗ, kiểu
    // tiến trình tải của wget) hay '\n' (dòng log mới thật sự). Được set ngay trước
    // khi gọi processOutput() vì SilentShellOutputHandler.onReader() không cho
    // truyền kèm tham số riêng.
    private var pendingIsUpdate = false

    // Được tạo/truy cập LẦN ĐẦU tiên bên trong handler.post{} (tức luôn ở main thread) vì
    // SilentShellOutputHandler kế thừa android.os.Handler, cần Looper của thread hiện tại khi khởi tạo.
    private val splashLogHandler: SplashLogOutputHandler by lazy { SplashLogOutputHandler() }

    // Nhận diện am:[...] (mở activity, ẩn khỏi log) và tự resolve @string/ten_resource cho các
    // dòng log thông thường còn lại trước khi hiển thị lên màn hình splash.
    private inner class SplashLogOutputHandler : SilentShellOutputHandler(this@SplashActivity) {
        override fun onReader(msg: Any?) {
            val line = msg?.toString() ?: return
            onLogOutput(StringResRef.resolve(this@SplashActivity, line))
        }
    }

    // Đọc raw stream theo TỪNG KÝ TỰ (không dùng forEachLine/readLine) để tự phân biệt:
    // - '\n'  => dòng log mới thật sự
    // - '\r'  => cập nhật tại chỗ (ví dụ % tiến trình của wget), không phải dòng mới
    // BufferedReader.readLine() coi cả '\r' và '\n' là dấu kết thúc dòng, nên nếu dùng
    // forEachLine thì mỗi lần wget in '\r' để cập nhật % tải sẽ bị hiểu lầm là một dòng
    // log mới, khiến các dòng log xếp chồng lên nhau thay vì ghi đè.
    private fun readStreamAsync(reader: BufferedReader) {
        try {
            val buffer = StringBuilder()
            var code = reader.read()
            while (code != -1) {
                when (code.toChar()) {
                    '\n' -> {
                        handleRawLogLine(buffer.toString(), isUpdate = false)
                        buffer.clear()
                    }
                    '\r' -> {
                        handleRawLogLine(buffer.toString(), isUpdate = true)
                        buffer.clear()
                    }
                    else -> buffer.append(code.toChar())
                }
                code = reader.read()
            }
            if (buffer.isNotEmpty()) {
                handleRawLogLine(buffer.toString(), isUpdate = false)
            }
        } catch (e: Exception) {}
    }

    // Luôn xử lý trên main thread: vừa để an toàn cho SilentShellOutputHandler (cần Looper),
    // vừa vì onAm() bên trong có thể gọi startActivity().
    private fun handleRawLogLine(line: String, isUpdate: Boolean) {
        handler.post {
            pendingIsUpdate = isUpdate
            splashLogHandler.processOutput(line)
        }
    }

    private fun onLogOutput(log: String) {
        val isUpdate = pendingIsUpdate

        // Hỗ trợ "\n" dạng escape (2 ký tự '\' và 'n') xuất hiện bên trong nội dung log
        // (ví dụ từ string resource hoặc script), coi đó như xuống dòng thật và tách
        // thành nhiều dòng con trước khi hiển thị.
        val lines = log
            .replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .split("\n")

        synchronized(rows) {
            for ((index, l) in lines.withIndex()) {
                // Chỉ dòng con đầu tiên mới có thể là bản cập nhật tại chỗ (\r) của dòng
                // trước đó; các dòng con sau (nếu log có nhiều "\n" escape) luôn là dòng mới.
                val replaceLast = isUpdate && index == 0 && rows.isNotEmpty()
                if (replaceLast) {
                    rows[rows.size - 1] = l
                } else {
                    if (rows.size >= maxLines) {
                        rows.removeAt(0)
                        ignored = true
                    }
                    rows.add(l)
                }
            }
            binding.startStateText.text = rows.joinToString("\n", if (ignored) "……\n" else "")
        }
    }
}