package com.tool.tree

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebSettingsCompat.FORCE_DARK_OFF
import androidx.webkit.WebSettingsCompat.FORCE_DARK_ON
import androidx.webkit.WebViewFeature
import com.omarea.common.shared.FilePathResolver
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.common.ui.ThemeMode
import com.omarea.krscript.WebViewInjector
import com.omarea.krscript.downloader.Downloader
import com.omarea.krscript.ui.ParamsFileChooserRender
import com.tool.tree.databinding.ActivityActionPageOnlineBinding
import java.util.*

class ActionPageOnline : AppCompatActivity() {
    private val progressBarDialog = ProgressBarDialog(this)
    private lateinit var themeMode: ThemeMode
    private lateinit var binding: ActivityActionPageOnlineBinding
    private var progressPolling: Timer? = null
    private var fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface? = null
    private val ACTION_FILE_PATH_CHOOSER = 65400
    private val MENU_OPEN_BROWSER = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        themeMode = ThemeModeState.switchTheme(this)
        
        binding = ActivityActionPageOnlineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar: Toolbar = binding.webappbar.toolbar
        setSupportActionBar(toolbar)
        setTitle(R.string.app_name)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

        toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.krOnlineWebview.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        onBackPressedDispatcher.addCallback(this) {
            if (binding.krOnlineWebview.canGoBack()) {
                binding.krOnlineWebview.goBack()
            } else {
                finish()
            }
        }

        loadIntentData()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, MENU_OPEN_BROWSER, 0, R.string.open_in_browser)?.apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_OPEN_BROWSER -> {
                openInDefaultBrowser()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openInDefaultBrowser() {
        val currentUrl = binding.krOnlineWebview.url
        if (!currentUrl.isNullOrEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No suitable browser found.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadIntentData() {
        val intent = this.intent
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey("title")) {
                title = extras.getString("title")
            }

            when {
                extras.containsKey("config") -> initWebview(extras.getString("config"))
                extras.containsKey("url") -> initWebview(extras.getString("url"))
            }

            if (extras.containsKey("downloadUrl")) {
                val downloader = Downloader(this)
                val url = extras.getString("downloadUrl")!!
                val taskAliasId = if (extras.containsKey("taskId")) extras.getString("taskId") else UUID.randomUUID().toString()

                val downloadId = downloader.downloadBySystem(url, null, null, taskAliasId)
                if (downloadId != null) {
                    binding.krDownloadUrl.text = url
                    val autoClose = extras.containsKey("autoClose") && extras.getBoolean("autoClose")
                    downloader.saveTaskStatus(taskAliasId, 0)
                    watchDownloadProgress(downloadId, autoClose, taskAliasId)
                } else {
                    downloader.saveTaskStatus(taskAliasId, -1)
                }
            }
        }
    }

    private fun initWebview(url: String?) {
        binding.krOnlineWebview.visibility = View.VISIBLE
        val settings = binding.krOnlineWebview.settings
        
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        
        settings.blockNetworkImage = false
        settings.loadsImagesAutomatically = true

        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            val isDark = ThemeModeState.isDarkMode()
            WebSettingsCompat.setForceDark(settings, if (isDark) FORCE_DARK_ON else FORCE_DARK_OFF)
        }

        binding.krOnlineWebview.webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                DialogHelper.animDialog(
                    AlertDialog.Builder(this@ActionPageOnline)
                        .setMessage(message)
                        .setPositiveButton(R.string.btn_confirm) { _, _ -> }
                        .setOnDismissListener { result?.confirm() }
                        .create()
                )?.setCancelable(false)
                return true
            }

            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                DialogHelper.animDialog(
                    AlertDialog.Builder(this@ActionPageOnline)
                        .setMessage(message)
                        .setPositiveButton(R.string.btn_confirm) { _, _ -> result?.confirm() }
                        .setNeutralButton(R.string.btn_cancel) { _, _ -> result?.cancel() }
                        .create()
                )?.setCancelable(false)
                return true
            }
        }

        binding.krOnlineWebview.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBarDialog.hideDialog()
                view?.title?.let { setTitle(it) }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                // CẬP NHẬT: Thêm nút hủy và xử lý stopLoading()
                progressBarDialog.showDialog(getString(R.string.please_wait), getString(R.string.btn_cancel)) {
                    binding.krOnlineWebview.stopLoading()
                    progressBarDialog.hideDialog()
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return try {
                    val requestUrl = request?.url
                    if (requestUrl != null && requestUrl.scheme?.startsWith("http") != true) {
                        val intent = Intent(Intent.ACTION_VIEW, requestUrl)
                        startActivity(intent)
                        true
                    } else {
                        super.shouldOverrideUrlLoading(view, request)
                    }
                } catch (_: Exception) {
                    super.shouldOverrideUrlLoading(view, request)
                }
            }
        }

        WebViewInjector(binding.krOnlineWebview,
            object : ParamsFileChooserRender.FileChooserInterface {
                override fun openFileChooser(fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface): Boolean {
                    return chooseFilePath(fileSelectedInterface)
                }
            }).inject(this, url?.startsWith("file:///android_asset") == true)

        url?.let { binding.krOnlineWebview.loadUrl(it) }
    }

    private fun chooseFilePath(fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, ACTION_FILE_PATH_CHOOSER)
            this.fileSelectedInterface = fileSelectedInterface
            true
        } catch (ex: Exception) {
            false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ACTION_FILE_PATH_CHOOSER) {
            val result = if (data == null || resultCode != RESULT_OK) null else data.data
            if (fileSelectedInterface != null) {
                val absPath = result?.let { getPath(it) }
                fileSelectedInterface?.onFileSelected(absPath)
            }
            this.fileSelectedInterface = null
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getPath(uri: Uri): String? {
        return try {
            FilePathResolver().getPath(this, uri)
        } catch (_: Exception) {
            null
        }
    }

    override fun onDestroy() {
        stopWatchDownloadProgress()
        binding.krOnlineWebview.apply {
            stopLoading()
            removeAllViews()
            destroy()
        }
        super.onDestroy()
    }

    private fun stopWatchDownloadProgress() {
        progressPolling?.cancel()
        progressPolling = null
    }

    private fun watchDownloadProgress(downloadId: Long, autoClose: Boolean, taskAliasId: String?) {
        binding.krDownloadState.visibility = View.VISIBLE
        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)

        binding.krDownloadNameCopy.setOnClickListener {
            val myClipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            myClipboard.setPrimaryClip(ClipData.newPlainText("text", binding.krDownloadName.text))
            Toast.makeText(this, getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
        }

        binding.krDownloadUrlCopy.setOnClickListener {
            val myClipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            myClipboard.setPrimaryClip(ClipData.newPlainText("text", binding.krDownloadUrl.text))
            Toast.makeText(this, getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
        }

        val handler = Handler(Looper.getMainLooper())
        val downloader = Downloader(this)
        progressPolling = Timer()
        progressPolling?.schedule(object : TimerTask() {
            override fun run() {
                val cursor = downloadManager.query(query)
                var fileName = ""
                var absPath = ""
                if (cursor.moveToFirst()) {
                    val downloadBytesIdx = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalBytesIdx = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val totalBytes = cursor.getLong(totalBytesIdx)
                    val downloadBytes = cursor.getLong(downloadBytesIdx)
                    val ratio = if (totalBytes > 0) (downloadBytes * 100 / totalBytes).toInt() else 0

                    try {
                        val nameColumn = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)
                        val uriStr = cursor.getString(nameColumn)
                        absPath = FilePathResolver().getPath(this@ActionPageOnline, uriStr.toUri()) ?: ""
                        fileName = absPath
                    } catch (_: Exception) {}

                    handler.post {
                        binding.krDownloadName.text = fileName
                        binding.krDownloadProgress.progress = ratio
                        binding.krDownloadProgress.isIndeterminate = false
                        setTitle(R.string.kr_download_downloading)
                        downloader.saveTaskStatus(taskAliasId, ratio)
                    }

                    if (ratio >= 100) {
                        downloader.saveTaskCompleted(downloadId, absPath)
                        handler.post {
                            setTitle(R.string.kr_download_completed)
                            binding.krDownloadProgress.visibility = View.GONE
                            stopWatchDownloadProgress()
                            setResult(0, Intent().apply { putExtra("absPath", absPath) })
                            if (autoClose) finish()
                        }
                    }
                }
                cursor.close()
            }
        }, 200, 500)
    }
}
