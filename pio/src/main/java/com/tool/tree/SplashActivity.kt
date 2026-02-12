package com.tool.tree

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.Settings
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.ShellExecutor
import com.omarea.common.ui.DialogHelper
import com.omarea.krscript.executor.ScriptEnvironmen
import com.tool.tree.databinding.ActivitySplashBinding
import com.tool.tree.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.util.Locale
import android.content.res.Configuration

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val REQUEST_CODE_PERMISSIONS = 1001

    private var hasRoot = false
    private var started = false
    private var starting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppLanguage()
        super.onCreate(savedInstanceState)
        ThemeModeState.switchTheme(this)

        if (ScriptEnvironmen.isInited() && isTaskRoot) {
            gotoHome()
            return
        }

        if (!hasAgreed()) showAgreementDialog()

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.startLogoXml.postDelayed({
            binding.startLogoXml.startAnimation(AnimationUtils.loadAnimation(this, R.anim.blink))
        }, 2000)

        applyTheme()
    }

    private fun applyAppLanguage() {
        runCatching {
            val langFile = File(filesDir, "home/log/language")
            val lang = langFile.takeIf { it.exists() }?.readText()?.trim()?.takeIf { it.isNotEmpty() } ?: return
            val locale = Locale.forLanguageTag(lang.replace("_", "-"))
            if (Locale.getDefault() != locale) {
                Locale.setDefault(locale)
                val config = Configuration(resources.configuration).apply { setLocale(locale) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    resources.updateConfiguration(config, resources.displayMetrics)
                } else {
                    @Suppress("DEPRECATION")
                    resources.updateConfiguration(config, resources.displayMetrics)
                }
            }
        }
    }

private fun applyTheme() {
    val bgColor = ContextCompat.getColor(this, R.color.splash_bg_color)

    window.statusBarColor = bgColor
    window.navigationBarColor = bgColor

    val controller = WindowCompat.getInsetsController(window, window.decorView)

    val isDark =
        (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

    controller?.isAppearanceLightStatusBars = !isDark
    controller?.isAppearanceLightNavigationBars = !isDark
}

    // =================== AGREEMENT ===================
    private fun showAgreementDialog() {
        DialogHelper.warning(
            this,
            getString(R.string.permission_dialog_title),
            getString(R.string.permission_dialog_message),
            Runnable { requestAppPermissions() },
            Runnable { finish() }
        ).setCancelable(false)
    }

    private fun hasAgreed(): Boolean =
        getSharedPreferences("kr-script-config", MODE_PRIVATE)
            .getBoolean("agreed_permissions", false)

    private fun saveAgreement() {
        getSharedPreferences("kr-script-config", MODE_PRIVATE)
            .edit()
            .putBoolean("agreed_permissions", true)
            .apply()
    }

    // =================== PERMISSION ===================
    private fun hasAllFilesPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            Environment.isExternalStorageManager()
        else ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED

    private fun requestAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            })
        } else {
            // Legacy permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun requestAppPermissions() {
        saveAgreement()
        if (!hasAllFilesPermission()) requestAllFilesPermission()
        else {
            started = true
            checkRootAndStart()
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasAgreed() && !started) {
            started = true
            checkRootAndStart()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                started = true
                checkRootAndStart()
            } else finish()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // =================== ROOT ===================
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

        if (config.beforeStartSh.isNotEmpty()) {
            runBeforeStartSh(config, hasRoot)
        } else gotoHome()
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
                val process = if (hasRoot)
                    ShellExecutor.getSuperUserRuntime()
                else
                    ShellExecutor.getRuntime()
                process?.let {
                    DataOutputStream(it.outputStream).use { os ->
                        ScriptEnvironmen.executeShell(
                            this@SplashActivity,
                            os,
                            config.beforeStartSh,
                            config.variables,
                            null,
                            "pio-splash"
                        )
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

    // Buffer lưu 4 dòng cuối
    private val rows = mutableListOf<String>()
    private var ignored = false
    private val maxLines = 5
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    private fun readStreamAsync(reader: BufferedReader) {
        Thread {
            reader.forEachLine { line ->
                onLogOutput(line)
            }
        }.start()
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