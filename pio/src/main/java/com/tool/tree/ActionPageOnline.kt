package com.tool.tree

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.omarea.common.shared.FilePathResolver
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.common.ui.ThemeMode
import com.omarea.krscript.WebViewInjector
import com.omarea.krscript.downloader.Downloader
import com.omarea.krscript.ui.ParamsFileChooserRender
import com.tool.tree.databinding.ActivityActionPageOnlineBinding
import java.util.*
import androidx.core.net.toUri
import androidx.activity.OnBackPressedCallback

class ActionPageOnline : AppCompatActivity() {
    private val progressBarDialog = ProgressBarDialog(this)

    private lateinit var themeMode: ThemeMode
    private lateinit var binding : ActivityActionPageOnlineBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeMode = ThemeModeState.switchTheme(this)
        binding = ActivityActionPageOnlineBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        setTitle(R.string.app_name)

        // 显示返回按钮
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.krOnlineWebview.canGoBack()) {
                    binding.krOnlineWebview.goBack()
                } else {
                    finish()
                }
            }
        })

        loadIntentData()
    }

    private fun hideWindowTitle() {
        if (Build.VERSION.SDK_INT >= 21) {
            val decorView = window.decorView
            val option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            decorView.systemUiVisibility = option
            window.statusBarColor = Color.TRANSPARENT
        }
        val actionBar = supportActionBar
        actionBar!!.hide()
    }

    private fun setWindowTitleBar() {
        val window = window
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        var flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        if (themeMode.isDarkMode) {
        } else {
            window.statusBarColor = Color.WHITE
            window.navigationBarColor = Color.WHITE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
            }
        }
        getWindow().decorView.systemUiVisibility = flags
        binding.krOnlineRoot.fitsSystemWindows = true
    }

    private fun loadIntentData() {
        // 读取intent里的参数
        val intent = this.intent
        if (intent.extras != null) {
            val extras = intent.extras
            if (extras != null) {
                if (extras.containsKey("title")) {
                    title = extras.getString("title")!!
                }

                // config、url 都用于设定要打卡的网页
                setWindowTitleBar()
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
    }

    private fun initWebview(url: String?) {
        binding.krOnlineWebview.visibility = View.VISIBLE
        binding.krOnlineWebview.webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                DialogHelper.animDialog(
                        AlertDialog.Builder(this@ActionPageOnline)
                                .setMessage(message)
                                .setPositiveButton(com.omarea.krscript.R.string.btn_confirm) { _, _ -> }
                            .setOnDismissListener {
                                    result?.confirm()
                                }
                                .create()
                )?.setCancelable(false)
                return true
            }

            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                DialogHelper.animDialog(
                        AlertDialog.Builder(this@ActionPageOnline)
                                .setMessage(message)
                                .setPositiveButton(com.omarea.krscript.R.string.btn_confirm) { _, _ ->
                                    result?.confirm()
                                }
                                .setNeutralButton(com.omarea.krscript.R.string.btn_cancel) { _, _ ->
                                    result?.cancel()
                                }
                                .create()
                )?.setCancelable(false)
                return true
            }
        }

        binding.krOnlineWebview.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBarDialog.hideDialog()
                view?.run {
                    setTitle(this.title)
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBarDialog.showDialog(getString(R.string.please_wait))
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                try {
                    val requestUrl = request?.url
                    if (requestUrl != null && requestUrl.scheme?.startsWith("http") != true) {
                        val intent = Intent(Intent.ACTION_VIEW, requestUrl.toString().toUri())
                        startActivity(intent)
                        return true
                    } else {
                        return super.shouldOverrideUrlLoading(view, request)
                    }
                } catch (_: Exception) {
                    return super.shouldOverrideUrlLoading(view, request)
                }
            }
        }

        url?.let { binding.krOnlineWebview.loadUrl(it) }

        WebViewInjector(binding.krOnlineWebview,
                object : ParamsFileChooserRender.FileChooserInterface {
                    override fun openFileChooser(fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface): Boolean {
                        return chooseFilePath(fileSelectedInterface)
                    }
                }).inject(this, url?.startsWith("file:///android_asset") == true)
    }

    private var fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface? = null
    private val ACTION_FILE_PATH_CHOOSER = 65400
    private fun chooseFilePath(fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface): Boolean {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(intent, ACTION_FILE_PATH_CHOOSER)
            this.fileSelectedInterface = fileSelectedInterface
            return true
        } catch (ex: java.lang.Exception) {
            return false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ACTION_FILE_PATH_CHOOSER) {
            val result = if (data == null || resultCode != RESULT_OK) null else data.data
            if (fileSelectedInterface != null) {
                if (result != null) {
                    val absPath = getPath(result)
                    fileSelectedInterface?.onFileSelected(absPath)
                } else {
                    fileSelectedInterface?.onFileSelected(null)
                }
            }
            this.fileSelectedInterface = null
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getPath(uri: Uri): String? {
        return try {
            FilePathResolver().getPath(this, uri)
        } catch (_: java.lang.Exception) {
            null
        }
    }

    override fun onDestroy() {
        stopWatchDownloadProgress()
        super.onDestroy()
    }

    private fun stopWatchDownloadProgress() {
        if (progressPolling != null) {
            progressPolling?.cancel()
            progressPolling = null
        }
    }

    var progressPolling: Timer? = null
    /**
     * 监视下载进度
     */
    private fun watchDownloadProgress(downloadId: Long, autoClose: Boolean, taskAliasId: String?) {
        binding.krDownloadState.visibility = View.VISIBLE

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)

        binding.krDownloadNameCopy.setOnClickListener {
            val myClipboard: ClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val myClip = ClipData.newPlainText("text", binding.krDownloadName.text.toString())
            myClipboard.setPrimaryClip(myClip)
            Toast.makeText(this@ActionPageOnline, getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
        }
        binding.krDownloadUrlCopy.setOnClickListener {
            val myClipboard: ClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val myClip = ClipData.newPlainText("text", binding.krDownloadUrl.text.toString())
            myClipboard.setPrimaryClip(myClip)
            Toast.makeText(this@ActionPageOnline, getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
        }

        val handler = Handler()
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
                    val ratio = (downloadBytes * 100 / totalBytes).toInt()
                    if (fileName.isEmpty()) {
                        try {
                            val nameColumn = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)
                            fileName = cursor.getString(nameColumn)
                            absPath = FilePathResolver().getPath(this@ActionPageOnline,
                                fileName.toUri())
                            if (!absPath.isEmpty()) {
                                fileName = absPath
                            }
                        } catch (_: java.lang.Exception) {
                        }
                    }

                    handler.post {
                        binding.krDownloadName.text = fileName
                        binding.krDownloadProgress.progress = ratio
                        binding.krDownloadProgress.isIndeterminate = false
                        setTitle(com.omarea.krscript.R.string.kr_download_downloading)
                        downloader.saveTaskStatus(taskAliasId, ratio)
                    }

                    if (ratio >= 100) {
                        // 保存下载成功后的路径
                        downloader.saveTaskCompleted(downloadId, absPath)

                        handler.post {
                            setTitle(com.omarea.krscript.R.string.kr_download_completed)
                            binding.krDownloadProgress.visibility = View.GONE
                            stopWatchDownloadProgress()

                            val result = Intent()
                            result.putExtra("absPath", absPath)
                            setResult(0, result)

                            if (autoClose) {
                                finish()
                            }
                        }
                    }
                }
            }
        }, 200, 500)
    }
}
