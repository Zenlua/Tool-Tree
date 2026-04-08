package com.tool.tree

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.TextView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.ListPopupWindow
import android.widget.CheckBox
import android.net.Uri

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val progressBarDialog by lazy { ProgressBarDialog(this) }
    private var krScriptConfig = KrScriptConfig()
    private val hasRoot by lazy { KeepShellPublic.checkRoot() }
    private var openedSubPage = false
    private var isFavoritesTab = false
    private var fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface? = null

    private val ACTION_FILE_PATH_CHOOSER = 65400
    private val ACTION_FILE_PATH_CHOOSER_INNER = 65300
    private lateinit var adapter: MainPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeModeState.switchTheme(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        setTitle(R.string.app_name)

        progressBarDialog.showDialog(getString(R.string.please_wait))
        loadTabs() // Load tab ngay khi vào

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
    // LOAD TAB
    // ========================
    private fun loadTabs() {
        lifecycleScope.launch(Dispatchers.IO) {
            val favorites = getItems(krScriptConfig.favoriteConfig)
            val pages = getItems(krScriptConfig.pageListConfig)
            val tab3Items = getItems(krScriptConfig.customTab3Config)
            val tab4Items = getItems(krScriptConfig.customTab4Config)

            withContext(Dispatchers.Main) {
                progressBarDialog.hideDialog()

                if (!::adapter.isInitialized) {
                    adapter = MainPagerAdapter(this@MainActivity)
                    binding.viewPager.adapter = adapter
                    binding.viewPager.offscreenPageLimit = 2
                }

                // Tab Favorites
                favorites?.takeIf { it.isNotEmpty() }?.let {
                    val fragment = ActionListFragment.create(
                        it,
                        getKrScriptActionHandler(krScriptConfig.favoriteConfig, true),
                        null,
                        ThemeModeState.getThemeMode()
                    )
                    if (adapter.getFragment(0) == null) {
                        adapter.addFragment(fragment, getString(R.string.tab_favorites))
                    } else {
                        adapter.replaceFragment(0, fragment)
                    }
                }

                // Tab Pages
                pages?.takeIf { it.isNotEmpty() }?.let {
                    val fragment = ActionListFragment.create(
                        it,
                        getKrScriptActionHandler(krScriptConfig.pageListConfig, false),
                        null,
                        ThemeModeState.getThemeMode()
                    )
                    if (adapter.getFragment(1) == null) {
                        adapter.addFragment(fragment, getString(R.string.tab_pages))
                    } else {
                        adapter.replaceFragment(1, fragment)
                    }
                }

                // Tab 3
                tab3Items?.takeIf { it.isNotEmpty() }?.let {
                    val fragment = ActionListFragment.create(
                        it,
                        getKrScriptActionHandler(krScriptConfig.customTab3Config, false),
                        null,
                        ThemeModeState.getThemeMode()
                    )
                    if (adapter.getFragment(2) == null) {
                        adapter.addFragment(fragment, getString(R.string.tab_custom3))
                    } else {
                        adapter.replaceFragment(2, fragment)
                    }
                }

                // Tab 4
                tab4Items?.takeIf { it.isNotEmpty() }?.let {
                    val fragment = ActionListFragment.create(
                        it,
                        getKrScriptActionHandler(krScriptConfig.customTab4Config, false),
                        null,
                        ThemeModeState.getThemeMode()
                    )
                    if (adapter.getFragment(3) == null) {
                        adapter.addFragment(fragment, getString(R.string.tab_custom4))
                    } else {
                        adapter.replaceFragment(3, fragment)
                    }
                }

                setupTabs()
            }
        }
    }

    private fun reloadTabs() {
        lifecycleScope.launch(Dispatchers.IO) {
            val favorites = getItems(krScriptConfig.favoriteConfig)
            val pages = getItems(krScriptConfig.pageListConfig)
            val tab3Items = getItems(krScriptConfig.customTab3Config)
            val tab4Items = getItems(krScriptConfig.customTab4Config)

            withContext(Dispatchers.Main) {
                // Reload Favorites
                if (!favorites.isNullOrEmpty()) {
                    (adapter.getFragment(0) as? ActionListFragment)?.updateData(
                        favorites,
                        getKrScriptActionHandler(krScriptConfig.favoriteConfig, true),
                        ThemeModeState.getThemeMode()
                    )
                }

                // Reload Pages
                if (!pages.isNullOrEmpty()) {
                    (adapter.getFragment(1) as? ActionListFragment)?.updateData(
                        pages,
                        getKrScriptActionHandler(krScriptConfig.pageListConfig, false),
                        ThemeModeState.getThemeMode()
                    )
                }

                // Reload Tab 3
                if (!tab3Items.isNullOrEmpty()) {
                    (adapter.getFragment(2) as? ActionListFragment)?.updateData(
                        tab3Items,
                        getKrScriptActionHandler(krScriptConfig.customTab3Config, false),
                        ThemeModeState.getThemeMode()
                    )
                }

                // Reload Tab 4
                if (!tab4Items.isNullOrEmpty()) {
                    (adapter.getFragment(3) as? ActionListFragment)?.updateData(
                        tab4Items,
                        getKrScriptActionHandler(krScriptConfig.customTab4Config, false),
                        ThemeModeState.getThemeMode()
                    )
                }
            }
        }
    }

    // ========================
    // SETUP TAB LAYOUT
    // ========================
    private fun setupTabs() {
        val tabHelper = TabIconHelper(this)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            val title = adapter.getTitle(position)
            val iconRes = when (title) {
                getString(R.string.tab_favorites) -> R.drawable.tab_favorites
                getString(R.string.tab_pages) -> R.drawable.tab_pages
                getString(R.string.tab_custom3) -> R.drawable.tab_custom3
                getString(R.string.tab_custom4) -> R.drawable.tab_custom4
                else -> R.drawable.tab_home
            }
            tab.customView = tabHelper.createTabView(title, getDrawable(iconRes)!!, position == binding.viewPager.currentItem)
        }.attach()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                tabHelper.updateHighlight(binding.tabLayout, tab.position)
                isFavoritesTab = tab.position == 0
                invalidateOptionsMenu()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun getItems(pageNode: PageNode): ArrayList<NodeInfoBase>? {
        var items: ArrayList<NodeInfoBase>? = null
        if (pageNode.pageConfigSh.isNotEmpty())
            items = PageConfigSh(this, pageNode.pageConfigSh, null).execute()
        if (items == null && pageNode.pageConfigPath.isNotEmpty())
            items = PageConfigReader(this.applicationContext, pageNode.pageConfigPath, null).readConfigXml()
        return items
    }

    private fun restartApp() {
        startActivity(Intent(this, SplashActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("force_reset", true)
        })
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
                    if (clickableNode is RunnableNode) putExtra("autoRunItemId", clickableNode.key)
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

    private fun chooseFilePath(fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface): Boolean {
        return try {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = fileSelectedInterface.mimeType() ?: "*/*"
            }, ACTION_FILE_PATH_CHOOSER)
            this.fileSelectedInterface = fileSelectedInterface
            true
        } catch (e: Exception) {
            Toast.makeText(this, "File picker error: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val path = when (requestCode) {
            ACTION_FILE_PATH_CHOOSER -> if (resultCode == RESULT_OK) data?.data?.let { FilePathResolver().getPath(this, it) } else null
            ACTION_FILE_PATH_CHOOSER_INNER -> if (resultCode == RESULT_OK) data?.getStringExtra("file") else null
            else -> null
        }
        fileSelectedInterface?.onFileSelected(path)
        fileSelectedInterface = null
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.option_menu_reboot)?.isVisible = hasRoot
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.option_menu_info -> { showSettingsDialog(); true }
            R.id.option_menu_reboot -> { DialogPower(this).showPowerMenu(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRestart() {
        super.onRestart()
        if (openedSubPage) {
            openedSubPage = false
            reloadTabs()
        }
    }

    private fun showSettingsDialog() {
        val layout = LayoutInflater.from(this).inflate(R.layout.dialog_about, null)
        val themeConfig = ThemeConfig(this)
        val themeSelector = layout.findViewById<TextView>(R.id.theme_selector)
        val themeNames = listOf(
            getString(R.string.theme_system_default),
            getString(R.string.theme_light),
            getString(R.string.theme_dark),
            getString(R.string.theme_wallpaper_system),
            getString(R.string.theme_wallpaper_dark),
            getString(R.string.theme_wallpaper_light)
        )
        themeSelector.text = themeNames[themeConfig.getThemeMode().coerceAtMost(themeNames.size - 1)]
        themeSelector.setOnClickListener {
            val popup = ListPopupWindow(this)
            popup.anchorView = themeSelector
            popup.setAdapter(ArrayAdapter(this, R.layout.kr_spinner_dropdown, themeNames))
            popup.setOnItemClickListener { _, _, position, _ ->
                themeConfig.setThemeMode(position)
                ThemeModeState.switchTheme(this, position)
                themeSelector.text = themeNames[position]
                popup.dismiss()
            }
            popup.width = 490
            popup.show()
        }

        val checkNotification = layout.findViewById<CheckBox>(R.id.notification_ui)
        checkNotification.isChecked = themeConfig.getAllowNotificationUI()
        checkNotification.setOnCheckedChangeListener { _, isChecked ->
            themeConfig.setAllowNotificationUI(isChecked)
        }

        val appliction_nameText = layout.findViewById<TextView>(R.id.appliction_nameText)
        val appliction_authorText = layout.findViewById<TextView>(R.id.appliction_authorText)
        val authorUrl = "https://zenlua.github.io/Tool-Tree/website/Information.html"
        val engineUrl = "https://github.com/Zenlua/Tool-Tree"
        appliction_authorText.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authorUrl))
            startActivity(intent)
        }
        appliction_nameText.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(engineUrl))
            startActivity(intent)
        }
        DialogHelper.customDialog(this, layout)
    }
}