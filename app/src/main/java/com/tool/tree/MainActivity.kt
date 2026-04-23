package com.tool.tree

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ListPopupWindow
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val progressBarDialog by lazy { ProgressBarDialog(this) }
    private var krScriptConfig = KrScriptConfig()
    private val hasRoot by lazy { KeepShellPublic.checkRoot() }
    private var openedSubPage = false
    private var isFavoritesTab = false
    private var fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface? = null

    private val ACTION_FILE_PATH_CHOOSER = 65400
    private lateinit var adapter: MainPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeModeState.switchTheme(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        setTitle(R.string.app_name)

        if (ThemeConfig(this).getAllowNotificationUI()) {
            WakeLockService.startService(applicationContext)
        }

        initAdapter()
        loadTabs()

        onBackPressedDispatcher.addCallback(this) {
            startService(Intent(this@MainActivity, WakeLockService::class.java).apply {
                action = WakeLockService.ACTION_END_WAKELOCK
            })
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun initAdapter() {
        if (!::adapter.isInitialized) {
            adapter = MainPagerAdapter(this)
            binding.viewPager.adapter = adapter
            binding.viewPager.offscreenPageLimit = 4
        }
    }

    private fun loadTabs() {
        progressBarDialog.showDialog(getString(R.string.please_wait))
        
        lifecycleScope.launch(Dispatchers.IO) {
            val favorites = getItems(krScriptConfig.favoriteConfig)
            val pages = getItems(krScriptConfig.pageListConfig)
            val tab3Items = getItems(krScriptConfig.customTab3Config)
            val tab4Items = getItems(krScriptConfig.customTab4Config)

            if (!isActive) return@launch

            withContext(Dispatchers.Main) {
                progressBarDialog.hideDialog()
                val theme = ThemeModeState.getThemeMode()

                fun updateTab(pos: Int, items: ArrayList<NodeInfoBase>?, titleRes: Int, config: PageNode, isFav: Boolean) {
                    items?.takeIf { it.isNotEmpty() }?.let {
                        val fragment = ActionListFragment.create(it, getKrScriptActionHandler(config, isFav), null, theme)
                        if (adapter.getFragment(pos) == null) {
                            adapter.addFragment(fragment, getString(titleRes))
                        } else {
                            adapter.replaceFragment(pos, fragment)
                        }
                    }
                }

                updateTab(0, favorites, R.string.tab_favorites, krScriptConfig.favoriteConfig, true)
                updateTab(1, pages, R.string.tab_pages, krScriptConfig.pageListConfig, false)
                updateTab(2, tab3Items, R.string.tab_custom3, krScriptConfig.customTab3Config, false)
                updateTab(3, tab4Items, R.string.tab_custom4, krScriptConfig.customTab4Config, false)

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

            if (!isActive) return@launch

            withContext(Dispatchers.Main) {
                val theme = ThemeModeState.getThemeMode()
                
                favorites?.let { (adapter.getFragment(0) as? ActionListFragment)?.updateData(it, getKrScriptActionHandler(krScriptConfig.favoriteConfig, true), theme) }
                pages?.let { (adapter.getFragment(1) as? ActionListFragment)?.updateData(it, getKrScriptActionHandler(krScriptConfig.pageListConfig, false), theme) }
                tab3Items?.let { (adapter.getFragment(2) as? ActionListFragment)?.updateData(it, getKrScriptActionHandler(krScriptConfig.customTab3Config, false), theme) }
                tab4Items?.let { (adapter.getFragment(3) as? ActionListFragment)?.updateData(it, getKrScriptActionHandler(krScriptConfig.customTab4Config, false), theme) }
            }
        }
    }

    private fun setupTabs() {
        val tabHelper = TabIconHelper(this)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            val title = adapter.getTitle(position)
            val iconRes = when (position) {
                0 -> R.drawable.tab_favorites
                1 -> R.drawable.tab_pages
                2 -> R.drawable.tab_custom3
                3 -> R.drawable.tab_custom4
                else -> R.drawable.tab_home
            }
            tab.customView = tabHelper.createTabView(title, getDrawable(iconRes)!!, position == binding.viewPager.currentItem)
        }.attach()

        binding.tabLayout.clearOnTabSelectedListeners()
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                tabHelper.updateHighlight(binding.tabLayout, tab.position)
                isFavoritesTab = (tab.position == 0)
                invalidateOptionsMenu()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun getItems(pageNode: PageNode): ArrayList<NodeInfoBase>? {
        return try {
            if (pageNode.pageConfigSh.isNotEmpty()) {
                PageConfigSh(this, pageNode.pageConfigSh, null).execute()
            } else if (pageNode.pageConfigPath.isNotEmpty()) {
                PageConfigReader(applicationContext, pageNode.pageConfigPath, null).readConfigXml()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun restartApp() {
        val intent = Intent(this, SplashActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("force_reset", true)
        }
        startActivity(intent)
        finish()
    }

    private fun getKrScriptActionHandler(pageNode: PageNode, isFavorites: Boolean): KrScriptActionHandler {
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
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or Intent.FLAG_ACTIVITY_NO_HISTORY)
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
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTION_FILE_PATH_CHOOSER && resultCode == RESULT_OK) {
            val path = data?.data?.let { FilePathResolver().getPath(this, it) }
            fileSelectedInterface?.onFileSelected(path)
            fileSelectedInterface = null
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
        
        val themeSelector = layout.findViewById<TextView>(R.id.theme_selector)
        val themeNames = listOf(
            getString(R.string.theme_system_default), getString(R.string.theme_dark),
            getString(R.string.theme_light), getString(R.string.theme_wallpaper_system),
            getString(R.string.theme_wallpaper_dark), getString(R.string.theme_wallpaper_light)
        )
        
        themeSelector.text = themeNames[themeConfig.getThemeMode().coerceIn(0, themeNames.size - 1)]
        themeSelector.setOnClickListener {
            val popup = ListPopupWindow(this)
            popup.anchorView = themeSelector
            popup.setAdapter(ArrayAdapter(this, R.layout.kr_spinner_dropdown, themeNames))
            popup.setOnItemClickListener { _, _, position, _ ->
                themeConfig.setThemeMode(position)
                themeSelector.text = themeNames[position]
                popup.dismiss()
                ThemeModeState.switchTheme(this)
                recreate()
            }
            popup.width = 500
            popup.show()
        }

        layout.findViewById<CheckBox>(R.id.notification_ui).apply {
            isChecked = themeConfig.getAllowNotificationUI()
            setOnCheckedChangeListener { _, isChecked -> themeConfig.setAllowNotificationUI(isChecked) }
        }

        layout.findViewById<TextView>(R.id.appliction_authorText).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://zenlua.github.io/Tool-Tree/website/Information.html")))
        }
        layout.findViewById<TextView>(R.id.appliction_nameText).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Zenlua/Tool-Tree")))
        }

        DialogHelper.customDialog(this, layout)
    }
}
