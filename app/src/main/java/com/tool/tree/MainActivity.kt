package com.tool.tree

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.tabs.TabLayout
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
import com.tool.tree.databinding.ActivityMainBinding
import com.tool.tree.ui.MainPagerAdapter
import com.tool.tree.ui.TabIconHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MainPagerAdapter

    private val handler = Handler(Looper.getMainLooper())
    private val progressBarDialog = ProgressBarDialog(this)

    private var krScriptConfig = KrScriptConfig()
    private val hasRoot by lazy { KeepShellPublic.checkRoot() }

    private var openedSubPage = false
    private var isFavoritesTab = false

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
    // LOAD TABS
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
    
                if (!favorites.isNullOrEmpty()) {
                    adapter.addFragment(
                        ActionListFragment.create(
                            favorites,
                            getKrScriptActionHandler(favoritesConfig, true),
                            null,
                            ThemeModeState.getThemeMode()
                        ),
                        getString(R.string.tab_favorites)
                    )
                }
    
                if (!pages.isNullOrEmpty()) {
                    adapter.addFragment(
                        ActionListFragment.create(
                            pages,
                            getKrScriptActionHandler(page2Config, false),
                            null,
                            ThemeModeState.getThemeMode()
                        ),
                        getString(R.string.tab_pages)
                    )
                }
    
                binding.viewPager.adapter = adapter
    
                // 🔥 TabIconHelper
                val tabHelper = TabIconHelper(this)
                TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                    val title = adapter.getTitle(position)
                    val iconRes = if (title == getString(R.string.tab_favorites))
                        R.drawable.tab_favorites
                    else
                        R.drawable.tab_pages
                    tab.customView = TabIconHelper(this).createTabView(title, getDrawable(iconRes)!!, position == 0)
                }.attach()
                isFavoritesTab = binding.tabLayout.getTabAt(binding.tabLayout.selectedTabPosition)?.text ==
                        getString(R.string.tab_favorites)
                binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab) {
                        isFavoritesTab = adapter.getTitle(tab.position) == getString(R.string.tab_favorites)
                        tabHelper.updateHighlight(binding.tabLayout, tab.position)
                        invalidateOptionsMenu()
                    }
                    override fun onTabUnselected(tab: TabLayout.Tab) {}
                    override fun onTabReselected(tab: TabLayout.Tab) {}
                })
            }
        }.start()
    }

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

    // ========================
    fun reloadTabs() {
        val title = if (isFavoritesTab) {
            getString(R.string.tab_favorites)
        } else {
            getString(R.string.tab_pages)
        }

        val position = adapter.getTitleIndex(title)
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
                    val fragment = adapter.getFragment(position)
                    if (fragment is ActionListFragment) {
                        fragment.updateData(it)
                    }
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
    private var fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface? = null
    private val ACTION_FILE_PATH_CHOOSER = 65400

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
        return when (item.itemId) {
            R.id.option_menu_info -> {
                showSettingsDialog()
                true
            }
            R.id.option_menu_reboot -> {
                DialogPower(this).showPowerMenu()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showSettingsDialog() {
        val layout = LayoutInflater.from(this).inflate(R.layout.dialog_about, null)
        val themeConfig = ThemeConfig(this)
    
        listOf(
            layout.findViewById<CompoundButton>(R.id.transparent_ui) to themeConfig.getAllowTransparentUI(),
            layout.findViewById<CompoundButton>(R.id.notification_ui) to themeConfig.getAllowNotificationUI()
        ).forEach { (button, checked) ->
            button.isChecked = checked
            button.setOnCheckedChangeListener { _, isChecked ->
                when (button.id) {
                    R.id.transparent_ui -> themeConfig.setAllowTransparentUI(isChecked)
                    R.id.notification_ui -> themeConfig.setAllowNotificationUI(isChecked)
                }
            }
        }
    
        DialogHelper.customDialog(this, layout)
    }
}