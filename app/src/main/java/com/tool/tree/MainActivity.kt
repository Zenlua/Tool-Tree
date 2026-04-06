package com.tool.tree

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
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
import com.tool.tree.ui.TabIconHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())
    private val progressBarDialog by lazy { ProgressBarDialog(this) }
    private var krScriptConfig = KrScriptConfig()
    private val hasRoot by lazy { KeepShellPublic.checkRoot() }

    private var openedSubPage = false
    private var isFavoritesTab = false
    private var fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface? = null

    private val ACTION_FILE_PATH_CHOOSER = 65400
    private val ACTION_FILE_PATH_CHOOSER_INNER = 65300
    private val fragmentList = ArrayList<Fragment>()
    private val titleList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeModeState.switchTheme(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
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
    
                // Khởi tạo adapter mới nếu chưa có
                if (!::adapter.isInitialized) {
                    adapter = MainPagerAdapter(this@MainActivity)
                    binding.viewPager.adapter = adapter
                    binding.viewPager.offscreenPageLimit = 2
                } else {
                    // Xóa hết fragment cũ
                    for (i in adapter.itemCount - 1 downTo 0) {
                        adapter.replaceFragment(i, ActionListFragment.create(arrayListOf()))
                    }
                }
    
                // Tab Favorites
                if (!favorites.isNullOrEmpty()) {
                    val fragment = ActionListFragment.create(
                        favorites,
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
                if (!pages.isNullOrEmpty()) {
                    val fragment = ActionListFragment.create(
                        pages,
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
                if (!tab3Items.isNullOrEmpty()) {
                    val fragment = ActionListFragment.create(
                        tab3Items,
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
                if (!tab4Items.isNullOrEmpty()) {
                    val fragment = ActionListFragment.create(
                        tab4Items,
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
    
                // Thiết lập tab layout
                setupTabs()
            }
        }
    }

    // ========================
    // RELOAD TAB
    // ========================
    private fun reloadTabs() {
        lifecycleScope.launch(Dispatchers.IO) {
            val favorites = getItems(krScriptConfig.favoriteConfig)
            val pages = getItems(krScriptConfig.pageListConfig)
            val tab3Items = getItems(krScriptConfig.customTab3Config)
            val tab4Items = getItems(krScriptConfig.customTab4Config)

            withContext(Dispatchers.Main) {
                (fragmentList.getOrNull(0) as? ActionListFragment)?.updateData(
                    favorites ?: emptyList(),
                    getKrScriptActionHandler(krScriptConfig.favoriteConfig, true),
                    ThemeModeState.getThemeMode()
                )
                (fragmentList.getOrNull(1) as? ActionListFragment)?.updateData(
                    pages ?: emptyList(),
                    getKrScriptActionHandler(krScriptConfig.pageListConfig, false),
                    ThemeModeState.getThemeMode()
                )
                (fragmentList.getOrNull(2) as? ActionListFragment)?.updateData(
                    tab3Items ?: emptyList(),
                    getKrScriptActionHandler(krScriptConfig.customTab3Config, false),
                    ThemeModeState.getThemeMode()
                )
                (fragmentList.getOrNull(3) as? ActionListFragment)?.updateData(
                    tab4Items ?: emptyList(),
                    getKrScriptActionHandler(krScriptConfig.customTab4Config, false),
                    ThemeModeState.getThemeMode()
                )
            }
        }
    }

    // ========================
    // SETUP TAB LAYOUT
    // ========================
    private fun setupTabs() {
        val tabHelper = TabIconHelper(this)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            val title = titleList.getOrNull(position) ?: "Tab $position"
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
                    if (clickableNode is RunnableNode)
                        putExtra("autoRunItemId", clickableNode.key)
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
        if (fileSelectedInterface == null) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }
        val path = when (requestCode) {
            ACTION_FILE_PATH_CHOOSER -> if (resultCode == RESULT_OK) data?.data?.let { FilePathResolver().getPath(this, it) } else null
            ACTION_FILE_PATH_CHOOSER_INNER -> if (resultCode == RESULT_OK) data?.getStringExtra("file") else null
            else -> null
        }
        fileSelectedInterface?.onFileSelected(path)
        fileSelectedInterface = null
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getPath(uri: Uri): String? {
        return try {
            FilePathResolver().getPath(this, uri)
        } catch (ex: Exception) {
            null
        }
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