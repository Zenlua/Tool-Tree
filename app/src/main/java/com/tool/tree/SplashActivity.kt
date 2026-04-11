package com.tool.tree

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import android.content.res.Configuration
import java.util.Locale

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val REQUEST_CODE_PERMISSIONS = 1001
    private val REQUEST_CODE_MANAGE_STORAGE = 1002

    private var hasRoot = false
    private var startingJob: Job? = null // Dùng Job để quản lý luồng khởi động
    
    private val rows = mutableListOf<String>()
    private var ignored = false
    private val maxLines = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppLanguage()
        super.onCreate(savedInstanceState)
        
        ThemeModeState.switchTheme(this)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Kiểm tra khởi tạo nhanh
        if (ScriptEnvironmen.isInited() && isTaskRoot &&
            !intent.getBooleanExtra("force_reset", false)) {
            gotoHome()
            return
        }

        if (!hasAgreed()) {
            showAgreementDialog()
        }

        binding.startLogoXml.startAnimation(AnimationUtils.loadAnimation(this, R.anim.ic_settings_rotate))
        applyTheme()
    }

    // Luồng khởi động duy nhất
    private fun startLogic() {
        if (startingJob?.isActive == true) return // Nếu đang chạy thì không chạy thêm luồng mới

        startingJob = lifecycleScope.launch(Dispatchers.Main) {
            saveAgreement()
            
            // Bước 1: Kiểm tra Root với Timeout tránh treo cứng
            hasRoot = withContext(Dispatchers.IO) {
                try {
                    withTimeoutOrNull(5000) { KeepShellPublic.checkRoot() } ?: false
                } catch (e: Exception) { false }
            }

            // Bước 2: Chuẩn bị config và chạy script
            binding.startStateText.text = getString(R.string.pop_started)
            val config = KrScriptConfig().init(this@SplashActivity)
            
            if (config.beforeStartSh.isNotEmpty()) {
                executeShellLogic(config, hasRoot)
            } else {
                gotoHome()
            }
        }
    }

    private suspend fun executeShellLogic(config: KrScriptConfig, hasRoot: Boolean) {
        withContext(Dispatchers.IO) {
            var process: Process? = null
            try {
                process = if (hasRoot) ShellExecutor.getSuperUserRuntime() else ShellExecutor.getRuntime()
                process?.let { p ->
                    // Đọc log song song
                    val logJob = launch {
                        p.inputStream.bufferedReader().useLines { lines ->
                            lines.forEach { line ->
                                withContext(Dispatchers.Main) { onLogOutput(line) }
                            }
                        }
                    }

                    // Thực thi shell
                    DataOutputStream(p.outputStream).use { os ->
                        ScriptEnvironmen.executeShell(
                            this@SplashActivity, os, config.beforeStartSh, config.variables, null, "pio-splash"
                        )
                    }
                    
                    // Đợi tối đa 15s cho script
                    withTimeoutOrNull(15000) { p.waitFor() }
                    logJob.cancel()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                process?.destroy() // Giải phóng tiến trình
                withContext(Dispatchers.Main) { gotoHome() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Chỉ chạy nếu đã đồng ý và các quyền đã sẵn sàng
        if (hasAgreed() && startingJob == null && hasPermissions()) {
            startLogic()
        }
    }

    // --- Giữ nguyên các hàm bổ trợ của bạn ---

    private fun hasPermissions(): Boolean {
        val basic = getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        val manage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager() else true
        return basic && manage
    }

    private fun getRequiredPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun requestAppPermissions() {
        val missing = getRequiredPermissions().filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            requestManageStoragePermission()
        } else {
            startLogic()
        }
    }

    private fun requestManageStoragePermission() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply { data = Uri.parse("package:$packageName") }
        try { startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE) }
        catch (e: Exception) { startActivityForResult(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION), REQUEST_CODE_MANAGE_STORAGE) }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) requestAppPermissions() else finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_MANAGE_STORAGE) {
            if (hasPermissions()) startLogic() else finish()
        }
    }

    private fun onLogOutput(log: String) {
        synchronized(rows) {
            if (rows.size >= maxLines) { rows.removeAt(0); ignored = true }
            rows.add(log)
            binding.startStateText.text = rows.joinToString("\n", if (ignored) "……\n" else "")
        }
    }

    private fun applyAppLanguage() {
        runCatching {
            val langFile = File(filesDir, "home/log/language")
            val lang = langFile.takeIf { it.exists() }?.readText()?.trim() ?: return
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
        val bgColor = if (typedValue.resourceId != 0) ContextCompat.getColor(this, typedValue.resourceId) else typedValue.data
        window.statusBarColor = bgColor
        window.navigationBarColor = bgColor
        WindowCompat.getInsetsController(window, window.decorView).apply {
            val isDark = ThemeModeState.isDarkMode()
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }

    private fun showAgreementDialog() {
        DialogHelper.warning(this, getString(R.string.permission_dialog_title), getString(R.string.permission_dialog_message), { requestAppPermissions() }, { finish() }).setCancelable(false)
    }

    private fun hasAgreed() = getSharedPreferences("kr-script-config", MODE_PRIVATE).getBoolean("agreed_permissions", false)
    private fun saveAgreement() = getSharedPreferences("kr-script-config", MODE_PRIVATE).edit().putBoolean("agreed_permissions", true).apply()

    private fun gotoHome() {
        val nextIntent = if (intent?.getBooleanExtra("JumpActionPage", false) == true) {
            Intent(this, ActionPage::class.java).apply { intent?.extras?.let { putExtras(it) } }
        } else Intent(this, MainActivity::class.java)
        startActivity(nextIntent)
        finish()
    }
}
