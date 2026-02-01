package com.tool.tree

import android.app.Activity
import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.omarea.common.shared.FilePathResolver
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.krscript.config.PageConfigReader
import com.omarea.krscript.config.PageConfigSh
import com.omarea.krscript.model.*
import com.omarea.krscript.ui.ActionListFragment
import com.omarea.krscript.ui.ParamsFileChooserRender
import com.omarea.vtools.FloatMonitor
import com.tool.tree.databinding.ActivityMainBinding
import com.omarea.common.shell.KeepShellPublic
import com.tool.tree.ui.TabIconHelper
import androidx.core.view.isVisible
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

class MainActivity : AppCompatActivity() {
    private val progressBarDialog = ProgressBarDialog(this)
    private var handler = Handler()
    private var krScriptConfig = KrScriptConfig()
    private val hasRoot by lazy { KeepShellPublic.checkRoot() }
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeModeState.switchTheme(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyTheme()

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        setTitle(R.string.app_name)
        krScriptConfig = KrScriptConfig()
        binding.mainTabhost.setup()

        val tabIconHelper = TabIconHelper(binding.mainTabhost, this)
        if (tabIconHelper != null) {
            if (hasRoot && krScriptConfig.allowHomePage) {
                tabIconHelper.newTabSpec(getString(R.string.tab_home), getDrawable(R.drawable.tab_home)!!, R.id.main_tabhost_cpu)
            } else {
                binding.mainTabhostCpu.visibility = View.GONE
            }
        }
        binding.mainTabhost.setOnTabChangedListener {
            tabIconHelper.updateHighlight()
        }

        progressBarDialog.showDialog(getString(R.string.please_wait))
        Thread {
            val page2Config = krScriptConfig.pageListConfig
            val favoritesConfig = krScriptConfig.favoriteConfig

            val pages = getItems(page2Config)
            val favorites = getItems(favoritesConfig)
            handler.post {
                progressBarDialog.hideDialog()

                if (favorites != null && favorites.isNotEmpty()) {
                    updateFavoritesTab(favorites, favoritesConfig)
                    tabIconHelper.newTabSpec(
                        getString(R.string.tab_favorites),
                        ContextCompat.getDrawable(this, R.drawable.tab_favorites)!!,
                        R.id.main_tabhost_2
                    )
                } else {
                    binding.mainTabhost2.visibility = View.GONE
                }

                if (pages != null && pages.isNotEmpty()) {
                    updateMoreTab(pages, page2Config)
                    tabIconHelper.newTabSpec(
                        getString(R.string.tab_pages),
                        ContextCompat.getDrawable(this, R.drawable.tab_pages)!!,
                        R.id.main_tabhost_3
                    )
                } else {
                    binding.mainTabhost3.visibility = View.GONE
                }
            }
        }.start()

        if (hasRoot && krScriptConfig.allowHomePage) {
            val home = FragmentHome()
            val fragmentManager = supportFragmentManager
            val transaction = fragmentManager.beginTransaction()
            transaction.replace(R.id.main_tabhost_cpu, home)
            transaction.commitAllowingStateLoss()
        }

        val themeConfig = ThemeConfig(applicationContext)
        if (themeConfig.getAllowNotificationUI()) {
            WakeLockService.startService(applicationContext)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startService(Intent(this@MainActivity, WakeLockService::class.java).apply { action = WakeLockService.ACTION_END_WAKELOCK })
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    private fun applyTheme() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = ContextCompat.getColor(this, R.color.splash_bg_color)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        }
    }

    private fun getItems(pageNode: PageNode): ArrayList<NodeInfoBase>? {
        var items: ArrayList<NodeInfoBase>? = null

        if (pageNode.pageConfigSh.isNotEmpty()) {
            items = PageConfigSh(this, pageNode.pageConfigSh, null).execute()
        }
        if (items == null && pageNode.pageConfigPath.isNotEmpty()) {
            items = PageConfigReader(this.applicationContext, pageNode.pageConfigPath, null).readConfigXml()
        }

        return items
    }

    private fun updateFavoritesTab(items: ArrayList<NodeInfoBase>, pageNode: PageNode) {
        val favoritesFragment = ActionListFragment.create(items, getKrScriptActionHandler(pageNode, true), null, ThemeModeState.getThemeMode())
        supportFragmentManager.beginTransaction().replace(R.id.list_favorites, favoritesFragment).commitAllowingStateLoss()
    }

    private fun updateMoreTab(items: ArrayList<NodeInfoBase>, pageNode: PageNode) {
        val allItemFragment = ActionListFragment.create(items, getKrScriptActionHandler(pageNode, false), null, ThemeModeState.getThemeMode())
        supportFragmentManager.beginTransaction().replace(R.id.list_pages, allItemFragment).commitAllowingStateLoss()
    }

    private fun reloadFavoritesTab() {
        Thread {
            val favoritesConfig = krScriptConfig.favoriteConfig
            val favorites = getItems(favoritesConfig)
            favorites?.run {
                handler.post {
                    updateFavoritesTab(this, favoritesConfig)
                }
            }
        }.start()
    }

    private fun reloadMoreTab() {
        Thread {
            val page2Config = krScriptConfig.pageListConfig
            val pages = getItems(page2Config)

            pages?.run {
                handler.post {
                    updateMoreTab(this, page2Config)
                }
            }
        }.start()
    }

    private fun getKrScriptActionHandler(pageNode: PageNode, isFavoritesTab: Boolean): KrScriptActionHandler {
        return object : KrScriptActionHandler {
            override fun onActionCompleted(runnableNode: RunnableNode) {
                if (runnableNode.autoFinish) {
                    finishAndRemoveTask()
                } else if (runnableNode.reloadPage) {
                    if (isFavoritesTab) {
                        reloadFavoritesTab()
                    } else {
                        reloadMoreTab()
                    }
                } else if (runnableNode.autoKill) {
                    startService(Intent(this@MainActivity, WakeLockService::class.java).apply { action = WakeLockService.ACTION_END_WAKELOCK })
                    finishAffinity()
                    System.exit(0)
                }
            }

            override fun addToFavorites(clickableNode: ClickableNode, addToFavoritesHandler: KrScriptActionHandler.AddToFavoritesHandler) {
                val page = clickableNode as? PageNode
                    ?: if (clickableNode is RunnableNode) {
                        pageNode
                    } else {
                        return
                    }

                val intent = Intent()

                intent.component = ComponentName(this@MainActivity.applicationContext, ActionPage::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)

                if (clickableNode is RunnableNode) {
                    intent.putExtra("autoRunItemId", clickableNode.key)
                }
                intent.putExtra("page", page)

                addToFavoritesHandler.onAddToFavorites(clickableNode, intent)
            }

            override fun onSubPageClick(pageNode: PageNode) {
                _openPage(pageNode)
            }

            override fun openFileChooser(fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface): Boolean {
                return chooseFilePath(fileSelectedInterface)
            }
        }
    }

    private var fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface? = null
    private val ACTION_FILE_PATH_CHOOSER = 65400
    private val ACTION_FILE_PATH_CHOOSER_INNER = 65300

    private fun chooseFilePath(extension: String) {
        try {
            val intent = Intent(this, ActivityFileSelector::class.java)
            intent.putExtra("extension", extension)
            startActivityForResult(intent, ACTION_FILE_PATH_CHOOSER_INNER)
        } catch (ex: java.lang.Exception) {
            Toast.makeText(this, "Failed to launch the built-in file selector!", Toast.LENGTH_SHORT).show()
        }
    }

private fun chooseFilePath(fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface): Boolean {
    return try {
        val suffix = fileSelectedInterface.suffix()
        if (suffix != null && suffix.isNotEmpty()) {
            chooseFilePath(suffix)
        } else {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                val mimeType = fileSelectedInterface.mimeType() ?: "*/*"
                type = mimeType
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(mimeType))
                }
            }
            startActivityForResult(intent, ACTION_FILE_PATH_CHOOSER)
        }
        this.fileSelectedInterface = fileSelectedInterface
        true
    } catch (e: Exception) {
        Toast.makeText(this, "Unable to open picker file: ${e.message}", Toast.LENGTH_SHORT).show()
        false
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
        } else if (requestCode == ACTION_FILE_PATH_CHOOSER_INNER) {
            val absPath = if (data == null || resultCode != RESULT_OK) null else data.getStringExtra("file")
            fileSelectedInterface?.onFileSelected(absPath)
            this.fileSelectedInterface = null
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getPath(uri: Uri): String? {
        return try {
            FilePathResolver().getPath(this, uri)
        } catch (ex: Exception) {
            null
        }
    }

    fun _openPage(pageNode: PageNode) {
        OpenPageHelper(this).openPage(pageNode)
    }

    private fun getDensity(): Int {
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)
        return dm.densityDpi
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        menu.findItem(R.id.action_graph).isVisible = (binding.mainTabhostCpu.isVisible)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.option_menu_info -> {
                val layoutInflater = LayoutInflater.from(this)
                val layout = layoutInflater.inflate(R.layout.dialog_about, null)
                val themeConfig = ThemeConfig(this)

                val transparentUi = layout.findViewById<CompoundButton>(R.id.transparent_ui)
                transparentUi.setOnClickListener {
                    val isChecked = (it as CompoundButton).isChecked
                    themeConfig.setAllowTransparentUI(isChecked)
                }
                transparentUi.isChecked = themeConfig.getAllowTransparentUI()

                val notificationUi = layout.findViewById<CompoundButton>(R.id.notification_ui)
                notificationUi.setOnClickListener {
                    val isChecked = (it as CompoundButton).isChecked
                    themeConfig.setAllowNotificationUI(isChecked)
                }
                notificationUi.isChecked = themeConfig.getAllowNotificationUI()

                DialogHelper.customDialog(this, layout)
            }
            R.id.option_menu_reboot -> {
                DialogPower(this).showPowerMenu()
            }
            R.id.action_graph -> {
                if (FloatMonitor.isShown == true) {
                    FloatMonitor(this).hidePopupWindow()
                    return false
                }
                if (Settings.canDrawOverlays(this)) {
                    FloatMonitor(this).showPopupWindow()
                    Toast.makeText(this, getString(R.string.float_monitor_tips), Toast.LENGTH_LONG).show()
                } else {
                    val intent = Intent()
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                    intent.data = Uri.fromParts("package", this.packageName, null)

                    Toast.makeText(applicationContext, getString(R.string.permission_float), Toast.LENGTH_LONG).show()

                    try {
                        startActivity(intent)
                    } catch (_: Exception) {
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }
}