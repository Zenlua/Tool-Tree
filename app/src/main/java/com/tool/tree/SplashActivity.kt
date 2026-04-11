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
import com.omarea.krscript.executor.ScriptEnvironmen
import com.tool.tree.databinding.ActivitySplashBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.util.Locale

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val REQUEST_CODE_PERMISSIONS = 1001
    private val REQUEST_CODE_MANAGE_ALL_FILES = 1002

    private var hasRoot = false
    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppLanguage()
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

        binding.startLogoXml.startAnimation(AnimationUtils.loadAnimation(this, R.anim.ic_settings_rotate))
        // applyTheme()
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
            Runnable { finishAffinity() }
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                checkPermissionsNextStep()
            } else {
                finishAffinity()
            }
        }
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

    // =================== CÁC HÀM TIỆN ÍCH KHÁC ===================

    private fun applyAppLanguage() {
        runCatching {
            val langFile = File(filesDir, "home/log/language")
            val lang = langFile.takeIf { it.exists() }?.readText()?.trim()?.takeIf { it.isNotEmpty() } ?: return
            val locale = Locale.forLanguageTag(lang.replace("_", "-"))
            if (Locale.getDefault() != locale) {
                Locale.setDefault(locale)
                val config = Configuration(resources.configuration).apply { setLocale(locale) }
                resources.updateConfiguration(config, resources.displayMetrics)
            }
        }
    }

    private fun applyTheme() {
        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.windowBackground, typedValue, true)
        val bgColor = if (typedValue.resourceId != 0) getColor(typedValue.resourceId) else typedValue.data
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = bgColor
        window.navigationBarColor = bgColor
    
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val isDark = ThemeModeState.isDarkMode()
        controller.isAppearanceLightStatusBars = !isDark
        controller.isAppearanceLightNavigationBars = !isDark
    }

    private fun gotoHome() {
        val intentToStart = if (intent?.getBooleanExtra("JumpActionPage", false) == true) {
            Intent(this, ActionPage::class.java).apply { putExtras(intent!!) }
        } else {
            Intent(this, MainActivity::class.java)
        }
        startActivity(intentToStart)
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

    private fun readStreamAsync(reader: BufferedReader) {
        try {
            reader.forEachLine { line -> onLogOutput(line) }
        } catch (e: Exception) {}
    }

    private fun onLogOutput(log: String) {
        handler.post {
            synchronized(rows) {
                if (rows.size >= maxLines) {
                    rows.removeAt(0)
                    ignored = true
                }
                rows.add(log)
                binding.startStateText.text = rows.joinToString("\n", if (ignored) "……\n" else "")
            }
        }
    }
}
