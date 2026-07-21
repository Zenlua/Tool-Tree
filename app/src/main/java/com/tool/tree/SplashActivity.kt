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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_NOTIFICATIONS
            )
            return
        }
    
        // Nếu là Android 11+ và chưa có quyền All Files
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            !Environment.isExternalStorageManager()
        ) {
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

    // Được tạo/truy cập LẦN ĐẦU tiên bên trong handler.post{} (tức luôn ở main thread) vì
    // SilentShellOutputHandler kế thừa android.os.Handler, cần Looper của thread hiện tại khi khởi tạo.
    private val splashLogHandler: SplashLogOutputHandler by lazy { SplashLogOutputHandler() }

    // Nhận diện am:[...] (mở activity, ẩn khỏi log) và tự resolve @string/ten_resource cho các
    // dòng log thông thường còn lại trước khi hiển thị lên màn hình splash.
    // currentLineIsProgress được set ngay trước khi gọi processOutput() (luôn trên main thread,
    // đồng bộ) để onReader() biết dòng hiện tại có phải là cập nhật thanh tiến trình (\r) hay không.
    private var currentLineIsProgress = false

    private inner class SplashLogOutputHandler : SilentShellOutputHandler(this@SplashActivity) {
        override fun onReader(msg: Any?) {
            val line = msg?.toString() ?: return
            onLogOutput(StringResRef.resolve(this@SplashActivity, line), currentLineIsProgress)
        }
    }

    // Đọc stream theo từng ký tự thay vì dùng reader.forEachLine()/readLine(), vì readLine() coi
    // \r, \n và \r\n là tương đương nên không phân biệt được đâu là dòng log bình thường (kết
    // thúc bằng \n) và đâu là cập nhật thanh tiến trình tại chỗ (kết thúc bằng \r, không có \n,
    // ví dụ "Copying... 10%\rCopying... 20%\r..."). Ở đây ta giữ lại thông tin ký tự kết thúc để
    // xử lý đúng: \r -> ghi đè dòng cuối, \n -> thêm dòng mới. \r\n (CRLF) vẫn được gộp lại thành
    // một dòng duy nhất như bình thường.
    private fun readStreamAsync(reader: BufferedReader) {
        try {
            val buffer = StringBuilder()
            var lastWasCR = false
            var c: Int
            while (reader.read().also { c = it } != -1) {
                when (c) {
                    '\r'.code -> {
                        handleRawLogLine(buffer.toString(), isProgress = true)
                        buffer.setLength(0)
                        lastWasCR = true
                    }
                    '\n'.code -> {
                        if (lastWasCR) {
                            // Đây là \n của một cặp \r\n vừa xử lý ở nhánh \r phía trên, bỏ qua.
                            lastWasCR = false
                        } else {
                            handleRawLogLine(buffer.toString(), isProgress = false)
                            buffer.setLength(0)
                        }
                    }
                    else -> {
                        buffer.append(c.toChar())
                        lastWasCR = false
                    }
                }
            }
            if (buffer.isNotEmpty()) {
                handleRawLogLine(buffer.toString(), isProgress = false)
            }
        } catch (e: Exception) {}
    }

    // Luôn xử lý trên main thread: vừa để an toàn cho SilentShellOutputHandler (cần Looper),
    // vừa vì onAm() bên trong có thể gọi startActivity().
    private fun handleRawLogLine(line: String, isProgress: Boolean) {
        handler.post {
            currentLineIsProgress = isProgress
            splashLogHandler.processOutput(line)
        }
    }

    private fun onLogOutput(log: String, isProgress: Boolean) {
        synchronized(rows) {
            if (isProgress && rows.isNotEmpty()) {
                // Bỏ qua các cú \r rỗng liên tiếp (không có nội dung mới) để tránh xoá dòng cuối.
                if (log.isEmpty()) return
                // Cập nhật (ghi đè) dòng cuối cùng thay vì thêm dòng mới -> thanh tiến trình
                // đứng yên một dòng và chỉ thay đổi nội dung.
                rows[rows.size - 1] = log
            } else {
                if (rows.size >= maxLines) {
                    rows.removeAt(0)
                    ignored = true
                }
                rows.add(log)
            }
            binding.startStateText.text = rows.joinToString("\n", if (ignored) "…......…\n" else "")
        }
    }
}
