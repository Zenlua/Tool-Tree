package com.tool.tree

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
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

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val REQUEST_CODE_PERMISSIONS = 1001

    private var hasRoot = false
    private var started = false
    private var starting = false
    
    private val rows = mutableListOf<String>()
    private var ignored = false
    private val maxLines = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Áp dụng ngôn ngữ sớm nhất có thể
        applyAppLanguage()
        super.onCreate(savedInstanceState)
        
        ThemeModeState.switchTheme(this)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 2. Hỗ trợ hiển thị tràn viền (Edge-to-Edge) cho các máy đời mới
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (ScriptEnvironmen.isInited() && isTaskRoot &&
            !intent.getBooleanExtra("force_reset", false)) {
            gotoHome()
            return
        }

        if (!hasAgreed()) {
            showAgreementDialog()
        }

        val imageView = findViewById<ImageView>(R.id.start_logo_xml)
        val rotateAnim = AnimationUtils.loadAnimation(this, R.anim.ic_settings_rotate)
        imageView.startAnimation(rotateAnim)

        applyTheme()
    }

    private fun applyAppLanguage() {
        runCatching {
            val langFile = File(filesDir, "home/log/language")
            val lang = langFile.takeIf { it.exists() }?.readText()?.trim()?.takeIf { it.isNotEmpty() } ?: return
            
            // Cách chuẩn hỗ trợ từ SDK thấp đến cao
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(lang.replace("_", "-"))
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    private fun applyTheme() {
        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.windowBackground, typedValue, true)
        val bgColor = if (typedValue.resourceId != 0) ContextCompat.getColor(this, typedValue.resourceId) else typedValue.data
        
        window.statusBarColor = bgColor
        window.navigationBarColor = bgColor
    
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val isDark = ThemeModeState.isDarkMode()

        // WindowInsetsControllerCompat tự động xử lý các bản Android cũ hơn (dưới SDK 30)
        controller.isAppearanceLightStatusBars = !isDark
        controller.isAppearanceLightNavigationBars = !isDark
    }

    // =================== PERMISSIONS (Hỗ trợ SDK 23+) ===================
    private fun getRequiredPermissions(): Array<String> {
        return when {
            // Android 13 (API 33) trở lên
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            }
            // Android 6.0 (API 23) đến Android 12
            else -> {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun hasPermissions(): Boolean {
        return getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestAppPermissions() {
        saveAgreement()
        if (hasPermissions()) {
            started = true
            checkRootAndStart()
        } else {
            ActivityCompat.requestPermissions(this, getRequiredPermissions(), REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // Kiểm tra xem tất cả quyền được cấp chưa
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                started = true
                checkRootAndStart()
            } else {
                finish() // Hoặc thông báo yêu cầu quyền
            }
        }
    }

    // =================== SHELL & LOGS (Tối ưu Coroutines) ===================
    private fun runBeforeStartSh(config: KrScriptConfig, hasRoot: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val process = if (hasRoot) ShellExecutor.getSuperUserRuntime() else ShellExecutor.getRuntime()
                process?.let { p ->
                    // Thực thi lệnh trong một job riêng
                    val shellJob = launch(Dispatchers.IO) {
                        DataOutputStream(p.outputStream).use { os ->
                            ScriptEnvironmen.executeShell(
                                this@SplashActivity, os, config.beforeStartSh, config.variables, null, "pio-splash"
                            )
                        }
                    }
                    
                    // Đọc log đồng thời (hỗ trợ SDK 23+)
                    val outJob = launch { readStreamAsync(p.inputStream.bufferedReader()) }
                    val errJob = launch { readStreamAsync(p.errorStream.bufferedReader()) }

                    p.waitFor()
                    shellJob.join()
                    outJob.join()
                    errJob.join()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) {
                    gotoHome()
                }
            }
        }
    }

    private suspend fun readStreamAsync(reader: BufferedReader) {
        withContext(Dispatchers.IO) {
            reader.useLines { lines ->
                lines.forEach { line ->
                    withContext(Dispatchers.Main) {
                        onLogOutput(line)
                    }
                }
            }
        }
    }

    private fun onLogOutput(log: String) {
        synchronized(rows) {
            if (rows.size >= maxLines) {
                rows.removeAt(0)
                ignored = true
            }
            rows.add(log)
            binding.startStateText.text = rows.joinToString("\n", if (ignored) "……\n" else "")
        }
    }

    // Các hàm phụ trợ khác giữ nguyên logic của bạn...
    private fun showAgreementDialog() {
        DialogHelper.warning(this, getString(R.string.permission_dialog_title), getString(R.string.permission_dialog_message), { requestAppPermissions() }, { finish() }).setCancelable(false)
    }

    private fun hasAgreed() = getSharedPreferences("kr-script-config", MODE_PRIVATE).getBoolean("agreed_permissions", false)

    private fun saveAgreement() = getSharedPreferences("kr-script-config", MODE_PRIVATE).edit().putBoolean("agreed_permissions", true).apply()

    @Synchronized
    private fun checkRootAndStart() {
        if (!started || starting) return
        starting = true
        lifecycleScope.launch(Dispatchers.IO) {
            hasRoot = KeepShellPublic.checkRoot()
            withContext(Dispatchers.Main) {
                starting = false
                startToFinish()
            }
        }
    }

    private fun startToFinish() {
        binding.startStateText.text = getString(R.string.pop_started)
        val config = KrScriptConfig().init(this)
        if (config.beforeStartSh.isNotEmpty()) runBeforeStartSh(config, hasRoot) else gotoHome()
    }

    private fun gotoHome() {
        val nextIntent = if (intent?.getBooleanExtra("JumpActionPage", false) == true) {
            Intent(this, ActionPage::class.java).apply { intent?.extras?.let { putExtras(it) } }
        } else Intent(this, MainActivity::class.java)
        startActivity(nextIntent)
        finish()
    }
}
