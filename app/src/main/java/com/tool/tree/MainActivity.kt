package com.tool.tree

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.tabs.TabLayoutMediator
import com.omarea.common.shared.FilePathResolver
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.krscript.config.PageConfigReader
import com.omarea.krscript.config.PageConfigSh
import com.omarea.krscript.model.*
import com.omarea.krscript.ui.ActionListFragment
import com.omarea.krscript.ui.ParamsFileChooserRender
import com.omarea.vtools.FloatMonitor
import com.tool.tree.databinding.ActivityMainBinding
import com.tool.tree.ui.MainPagerAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MainPagerAdapter

    private val handler = Handler(Looper.getMainLooper())
    private val progressBarDialog = ProgressBarDialog(this)

    private var krScriptConfig = KrScriptConfig()
    private val hasRoot by lazy { KeepShellPublic.checkRoot() }

    private var openedSubPage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeModeState.switchTheme(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        setTitle(R.string.app_name)

        progressBarDialog.showDialog(getString(R.string.please_wait))

        loadTabs()

        val themeConfig = ThemeConfig(applicationContext)
        if (themeConfig.getAllowNotificationUI()) {
            WakeLockService.startService(applicationContext)
        }

        onBackPressedDispatcher.addCallback(this) {
            startService(Intent(this@MainActivity, WakeLockService::class.java).apply {
                action = WakeLockService.ACTION_END_WAKELOCK
            })
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
        }
    }

    // ========================
    // LOAD TABS (THAY TABHOST)
    // ========================
    private fun loadTabs() {
        Thread {
            val page2Config = krScriptConfig.pageListConfig
            val favoritesConfig = krScriptConfig.favoriteConfig

            val pages = getItems(page2Config)
            val favorites = getItems(favoritesConfig)

            handler.post {
                progressBarDialog.hideDialog()

                adapter = MainPagerAdapter(this)

                // HOME
                if (hasRoot && krScriptConfig.allowHomePage) {
                    adapter.addFragment(FragmentHome(), getString(R.string.tab_home))
                }

                // FAVORITES
                if (!favorites.isNullOrEmpty()) {
                    val favoritesFragment = ActionListFragment.create(
                        favorites,
                        getKrScriptActionHandler(favoritesConfig, true),
                        null,
                        ThemeModeState.getThemeMode()
                    )
                    adapter.addFragment(favoritesFragment, getString(R.string.tab_favorites))
                }

                // PAGES
                if (!pages.isNullOrEmpty()) {
                    val pagesFragment = ActionListFragment.create(
                        pages,
                        getKrScriptActionHandler(page2Config, false),
                        null,
                        ThemeModeState.getThemeMode()
                    )
                    adapter.addFragment(pagesFragment, getString(R.string.tab_pages))
                }

                binding.viewPager.adapter = adapter

                // TAB + SWIPE
                TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                    tab.text = adapter.getTitle(position)

                    // icon giống code cũ
                    when (position) {
                        0 -> tab.setIcon(R.drawable.tab_home)
                        1 -> tab.setIcon(R.drawable.tab_favorites)
                        2 -> tab.setIcon(R.drawable.tab_pages)
                    }
                }.attach()
            }
        }.start()
    }

    // ========================
    // GIỮ NGUYÊN LOGIC CŨ
    // ========================
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

    private fun reloadTabs() {
        val position = if (isFavoritesTab) {
            adapter.getTitleIndex(getString(R.string.tab_favorites))
        } else {
            adapter.getTitleIndex(getString(R.string.tab_pages))
        }
        if (position == -1) return
        val pageNode = if (isFavoritesTab) {
            krScriptConfig.favoriteConfig
        } else {
            krScriptConfig.pageListConfig
        }
        Thread {
            val items = getItems(pageNode)
            items?.let {
                handler.post {
                    val fragment = adapter.getFragment(position) as? ActionListFragment
                    fragment?.updateData(it)
                }
            }
        }.start()
    }

    private fun restartApp() {
        val intent = Intent(this, SplashActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.putExtra("force_reset", true)
        startActivity(intent)
        finish()
    }

    private fun getKrScriptActionHandler(pageNode: PageNode, isFavoritesTab: Boolean): KrScriptActionHandler {
        return object : KrScriptActionHandler {
            override fun onActionCompleted(runnableNode: RunnableNode) {
                when {
                    runnableNode.autoFinish -> finishAndRemoveTask()
                    runnableNode.reloadPage -> reloadTabs()
                    runnableNode.autoRestart -> restartApp()
                    runnableNode.autoKill -> {
                        startService(Intent(this@MainActivity, WakeLockService::class.java).apply {
                            action = WakeLockService.ACTION_END_WAKELOCK
                        })
                        finishAffinity()
                    }
                }
            }

            override fun addToFavorites(clickableNode: ClickableNode, handler: KrScriptActionHandler.AddToFavoritesHandler) {
                val page = clickableNode as? PageNode ?: pageNode

                val intent = Intent().apply {
                    component = ComponentName(applicationContext, ActionPage::class.java)
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    putExtra("page", page)

                    if (clickableNode is RunnableNode) {
                        putExtra("autoRunItemId", clickableNode.key)
                    }
                }

                handler.onAddToFavorites(clickableNode, intent)
            }

            override fun onSubPageClick(pageNode: PageNode) {
                openedSubPage = true
                OpenPageHelper(this@MainActivity).openPage(pageNode)
            }

            override fun openFileChooser(fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface): Boolean {
                return chooseFilePath(fileSelectedInterface)
            }
        }
    }

    // ========================
    // FILE PICKER (GIỮ NGUYÊN)
    // ========================
    private var fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface? = null
    private val ACTION_FILE_PATH_CHOOSER = 65400
    private val ACTION_FILE_PATH_CHOOSER_INNER = 65300

    private fun chooseFilePath(fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = fileSelectedInterface.mimeType() ?: "*/*"
            }
            startActivityForResult(intent, ACTION_FILE_PATH_CHOOSER)
            this.fileSelectedInterface = fileSelectedInterface
            true
        } catch (e: Exception) {
            Toast.makeText(this, "File picker error: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ACTION_FILE_PATH_CHOOSER) {
            val uri = data?.data
            val path = uri?.let { FilePathResolver().getPath(this, it) }
            fileSelectedInterface?.onFileSelected(path)
            fileSelectedInterface = null
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    // ========================
    // MENU
    // ========================
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        menu.findItem(R.id.action_graph).isVisible = true
        return true
    }

    override fun onRestart() {
        super.onRestart()
        if (openedSubPage) {
            openedSubPage = false
            reloadTabs()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.option_menu_info -> {
                val layout = LayoutInflater.from(this).inflate(R.layout.dialog_about, null)
                val themeConfig = ThemeConfig(this)

                val transparentUi = layout.findViewById<CompoundButton>(R.id.transparent_ui)
                transparentUi.isChecked = themeConfig.getAllowTransparentUI()
                transparentUi.setOnCheckedChangeListener { _, isChecked ->
                    themeConfig.setAllowTransparentUI(isChecked)
                }

                val notificationUi = layout.findViewById<CompoundButton>(R.id.notification_ui)
                notificationUi.isChecked = themeConfig.getAllowNotificationUI()
                notificationUi.setOnCheckedChangeListener { _, isChecked ->
                    themeConfig.setAllowNotificationUI(isChecked)
                }

                DialogHelper.customDialog(this, layout)
            }

            R.id.option_menu_reboot -> {
                DialogPower(this).showPowerMenu()
            }

            R.id.action_graph -> {
                if (FloatMonitor.isShown == true) {
                    FloatMonitor(this).hidePopupWindow()
                } else if (Settings.canDrawOverlays(this)) {
                    FloatMonitor(this).showPopupWindow()
                } else {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    })
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }
}