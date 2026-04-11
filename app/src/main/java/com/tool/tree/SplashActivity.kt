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
    private val REQUEST_CODE_MANAGE_STORAGE = 1002

    private var hasRoot = false
    private var started = false
    private var starting = false
    
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

        if (ScriptEnvironmen.isInited() && isTaskRoot &&
            !intent.getBooleanExtra("force_reset", false)) {
            gotoHome()
            return
        }

        if (!hasAgreed()) {
            showAgreementDialog()
        }

        val rotateAnim = AnimationUtils.loadAnimation(this, R.anim.ic_settings_rotate)
        binding.startLogoXml.startAnimation(rotateAnim)

        applyTheme()
    }

    // =================== PERMISSIONS & STORAGE ===================
    
    private fun getRequiredPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
            }
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun hasPermissions(): Boolean {
        // 1. Kiểm tra các quyền Runtime cơ bản
        val basicPermissions = getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        // 2. Kiểm tra quyền MANAGE_EXTERNAL_STORAGE (Android 11+)
        val manageStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // Các bản cũ không có quyền này
        }

        return basicPermissions && manageStoragePermission
    }

    private fun requestAppPermissions() {
        saveAgreement()
        
        // Bước 1: Xin các quyền Runtime cơ bản trước
        val missingBasic = getRequiredPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingBasic.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingBasic.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        } 
        // Bước 2: Nếu quyền cơ bản có rồi nhưng thiếu MANAGE_EXTERNAL_STORAGE trên A11+
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            requestManageStoragePermission()
        } 
        // Bước 3: Đã đủ hết quyền
        else {
            startLogic()
        }
    }

    private fun requestManageStoragePermission() {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Sau khi có quyền cơ bản, kiểm tra tiếp MANAGE_STORAGE nếu cần
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                    requestManageStoragePermission()
                } else {
                    startLogic()
                }
            } else {
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_MANAGE_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    startLogic()
                } else {
                    finish() // Người dùng từ chối cấp quyền All Files Access
                }
            }
        }
    }

    private fun startLogic() {
        if (!started) {
            started = true
            checkRootAndStart()
        }
    }

    override fun onResume() {
        super.onResume()
        // Tự động kiểm tra nếu đã đồng nhất thỏa thuận nhưng chưa khởi chạy
        if (hasAgreed() && !started && hasPermissions()) {
            startLogic()
        }
    }

    // =================== SHELL & LOGS ===================

    private fun runBeforeStartSh(config: KrScriptConfig, hasRoot: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val process = if (hasRoot) ShellExecutor.getSuperUserRuntime() else ShellExecutor.getRuntime()
                process?.let { p ->
                    val shellJob = launch(Dispatchers.IO) {
                        DataOutputStream(p.outputStream).use { os ->
                            ScriptEnvironmen.executeShell(
                                this@SplashActivity, os, config.beforeStartSh, config.variables, null, "pio-splash"
                            )
                        }
                    }
                    
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

    // =================== HELPERS ===================

    private fun applyAppLanguage() {
        runCatching {
            val langFile = File(filesDir, "home/log/language")
            val lang = langFile.takeIf { it.exists() }?.readText()?.trim()?.takeIf { it.isNotEmpty() } ?: return
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
        controller.isAppearanceLightStatusBars = !isDark
        controller.isAppearanceLightNavigationBars = !isDark
    }

    private fun showAgreementDialog() {
        DialogHelper.warning(this, getString(R.string.permission_dialog_title), getString(R.string.permission_dialog_message), { requestAppPermissions() }, { finish() }).setCancelable(false)
    }

    private fun hasAgreed() = getSharedPreferences("kr-script-config", MODE_PRIVATE).getBoolean("agreed_permissions", false)

    private fun saveAgreement() = getSharedPreferences("kr-script-config", MODE_PRIVATE).edit().putBoolean("agreed_permissions", true).apply()

    @Synchronized
    private fun checkRootAndStart() {
        if (starting) return
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
