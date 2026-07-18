package com.tool.tree

import android.app.ActivityManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.omarea.common.shared.FilePathResolver
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.krscript.TryOpenActivity
import com.omarea.krscript.config.IconPathAnalysis
import com.omarea.krscript.config.PageConfigReader
import com.omarea.krscript.config.PageConfigSh
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.*
import com.omarea.krscript.shortcut.ActionShortcutManager
import com.omarea.krscript.ui.ActionListFragment
import com.omarea.krscript.ui.DialogLogFragment
import com.omarea.krscript.ui.ParamsFileChooserRender
import com.omarea.krscript.ui.PageMenuLoader
import com.tool.tree.databinding.ActivityActionPageBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActionPage : AppCompatActivity() {
    private val progressBarDialog by lazy { ProgressBarDialog(this) }
    private var actionsLoaded = false
    private val handler = Handler(Looper.getMainLooper())
    
    private var currentPageConfig: PageNode? = null
    private var autoRunItemId = ""
    private lateinit var binding: ActivityActionPageBinding
    private var openedSubPage = false

    // Sử dụng HashCode của String ID để khóa trạng thái tạm thời, không sợ lệch index
    private val justClickedItemIds = HashSet<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ScriptEnvironmen.isInited()) {
            val initIntent = Intent(this.applicationContext, SplashActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                putExtras(this@ActionPage.intent)
                putExtra("JumpActionPage", true)
            }
            startActivity(initIntent)
            finish()
            return
        }

        ThemeModeState.switchTheme(this)
        binding = ActivityActionPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        setTitle(R.string.app_name)

        supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
        toolbar.setNavigationOnClickListener { finish() }

        val extras = intent.extras
        if (extras != null) {
            currentPageConfig = if (extras.containsKey("page")) {
                extras.getSerializable("page") as? PageNode
            } else if (extras.containsKey("shortcutId")) {
                ActionShortcutManager(this).getShortcutTarget(extras.getString("shortcutId") ?: "")
            } else null
            
            autoRunItemId = extras.getString("autoRunItemId", "")
        }

        val config = currentPageConfig
        if (config == null) {
            Toast.makeText(this, "Invalid page information", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (config.activity.isNotEmpty()) {
            if (TryOpenActivity(this, config.activity).tryOpen()) {
                finish()
                return
            }
        }

        if (config.onlineHtmlPage.isNotEmpty()) {
            try {
                startActivity(Intent(this, ActionPageOnline::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("config", config.onlineHtmlPage)
                })
            } catch (_: Exception) {}
        }

        if (config.title.isNotEmpty()) {
            title = config.title
        }

        if (config.pageConfigPath.isEmpty() && config.pageConfigSh.isEmpty()) {
            setResult(2)
            finish()
        }
    }

    private val actionShortClickHandler = object : KrScriptActionHandler {
        override fun onActionCompleted(runnableNode: RunnableNode) {
            when {
                runnableNode.autoFinish -> finishAndRemoveTask()
                runnableNode.reloadPage -> loadPageConfig(true)
                runnableNode.autoKill -> killApp()
                runnableNode.autoRestart -> restartApp()
            }
        }

        override fun addToFavorites(clickableNode: ClickableNode, addToFavoritesHandler: KrScriptActionHandler.AddToFavoritesHandler) {
            val page = clickableNode as? PageNode ?: currentPageConfig ?: return
            val intent = Intent(applicationContext, ActionPage::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or Intent.FLAG_ACTIVITY_NO_HISTORY)
                putExtra("page", page)
                if (clickableNode is RunnableNode) putExtra("autoRunItemId", clickableNode.key)
            }
            addToFavoritesHandler.onAddToFavorites(clickableNode, intent)
        }

        override fun onSubPageClick(pageNode: PageNode) {
            _openPage(pageNode)
        }

        override fun openFileChooser(fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface): Boolean {
            return chooseFilePath(fileSelectedInterface)
        }
    }

    private var fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface? = null
    private val ACTION_FILE_PATH_CHOOSER = 65400
    private val ACTION_FILE_PATH_CHOOSER_INNER = 65300

    private var menuOptions: ArrayList<PageMenuOption>? = null
    private var menuCheckboxRefreshing = false

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val config = currentPageConfig ?: return false
        if (menuOptions == null) {
            menuOptions = PageMenuLoader(applicationContext, config).load()
        }

        menu?.clear()

        menuOptions?.forEach { option ->
            if (option.isFab) {
                addFab(option)
            } else {
                // ĐỔI: Sử dụng hashCode của chuỗi ID duy nhất (option.key hoặc option.id) làm itemId cho Menu
                val uniqueItemId = option.key.hashCode()
                val menuItem = menu?.add(Menu.NONE, uniqueItemId, Menu.NONE, option.title)
                if (option.type == "checkbox") {
                    menuItem?.isCheckable = true
                }
            }
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // Tìm và gán chính xác trạng thái checkbox theo uniqueId
        menuOptions?.forEach { option ->
            if (!option.isFab && option.type == "checkbox") {
                val uniqueItemId = option.key.hashCode()
                val menuItem = menu.findItem(uniqueItemId)
                menuItem?.isChecked = option.checked
            }
        }
        refreshCheckboxMenuStates()
        return super.onPrepareOptionsMenu(menu)
    }

    private fun refreshCheckboxMenuStates() {
        val config = currentPageConfig ?: return
        
        // Lọc danh sách checkbox dựa trên ID duy nhất
        val checkboxOptions = menuOptions?.filter { option -> 
            val uniqueItemId = option.key.hashCode()
            option.type == "checkbox" && option.checkedSh.isNotEmpty() && !justClickedItemIds.contains(uniqueItemId)
        }

        if (checkboxOptions.isNullOrEmpty() || menuCheckboxRefreshing) return
        menuCheckboxRefreshing = true

        lifecycleScope.launch(Dispatchers.IO) {
            val results = checkboxOptions.map { option ->
                val result = ScriptEnvironmen.executeResultRoot(this@ActionPage, option.checkedSh, config)?.trim()
                option to result
            }

            withContext(Dispatchers.Main) {
                menuCheckboxRefreshing = false
                var changed = false
                
                results.forEach { (option, result) ->
                    val uniqueItemId = option.key.hashCode()
                    // Khớp chính xác ID để tránh đè dữ liệu cũ trong khi lệnh shell click đang thực thi
                    if (!justClickedItemIds.contains(uniqueItemId)) {
                        val newChecked = result == "1" || result?.lowercase() == "true"
                        if (option.checked != newChecked) changed = true
                        option.checked = newChecked
                    }
                }
                
                if (changed && !isFinishing) {
                    invalidateOptionsMenu()
                }
            }
        }
    }

    private fun addFab(menuOption: PageMenuOption) {
        binding.actionPageFab.apply {
            visibility = View.VISIBLE
            setOnClickListener { onMenuItemClick(menuOption) }

            val iconRes = if (menuOption.type == "file" && menuOption.iconPath.isEmpty()) R.drawable.kr_folder else R.drawable.kr_fab
            val customIcon = if (menuOption.iconPath.isNotEmpty()) IconPathAnalysis().loadLogo(context, menuOption, false) else null
            
            setImageDrawable(customIcon ?: ContextCompat.getDrawable(context, iconRes))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val options = menuOptions ?: return false
        val targetItemId = item.itemId // Đây chính là hashCode của checkbox vừa click

        // ĐỔI: Tìm option khớp với hashCode của ID thay vì dùng vị trí mảng index
        val option = options.find { it.key.hashCode() == targetItemId }
        
        if (option != null) {
            if (option.type == "checkbox") {
                option.checked = !option.checked
                item.isChecked = option.checked

                // Khóa đồng bộ theo ID cụ thể
                justClickedItemIds.add(targetItemId)
                handler.postDelayed({
                    justClickedItemIds.remove(targetItemId)
                }, 1500)
            }

            onMenuItemClick(option)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onMenuItemClick(menuOption: PageMenuOption) {
        if (menuOption.link.isNotEmpty() || menuOption.activity.isNotEmpty() ||
            menuOption.onlineHtmlPage.isNotEmpty() || menuOption.pageConfigSh.isNotEmpty() ||
            menuOption.pageConfigPath.isNotEmpty()) {
            openMenuOptionAsPage(menuOption)
            return
        }

        when(menuOption.type) {
            "refresh", "reload" -> recreate()
            "restart" -> restartApp()
            "exit", "finish", "close" -> finish()
            "killapp" -> killApp()
            "file" -> menuItemChooseFile(menuOption)
            else -> {
                if (menuOption.silent) {
                    menuItemExecuteSilent(menuOption)
                } else {
                    menuItemExecute(menuOption, hashMapOf("state" to menuOption.key, "menu_id" to menuOption.key))
                }
            }
        }
    }

    private fun openMenuOptionAsPage(menuOption: PageMenuOption) {
        if (menuOption.link.isNotEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(menuOption.link))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(this, getString(R.string.kr_slice_activity_fail), Toast.LENGTH_SHORT).show()
            }
            return
        }

        if (menuOption.activity.isNotEmpty()) {
            TryOpenActivity(this, menuOption.activity).tryOpen()
            return
        }

        if (menuOption.onlineHtmlPage.isNotEmpty() || menuOption.pageConfigSh.isNotEmpty() || menuOption.pageConfigPath.isNotEmpty()) {
            val parentConfigPath = currentPageConfig?.currentPageConfigPath ?: ""
            val page = PageNode(parentConfigPath).apply {
                title = menuOption.title
                onlineHtmlPage = menuOption.onlineHtmlPage
                pageConfigSh = menuOption.pageConfigSh
                pageConfigPath = menuOption.pageConfigPath
            }
            OpenPageHelper(this).openPage(page)
        }
    }

    private fun menuItemExecuteSilent(menuOption: PageMenuOption) {
        val config = currentPageConfig ?: return
        val extraParams = hashMapOf("state" to menuOption.key, "menu_id" to menuOption.key)

        lifecycleScope.launch(Dispatchers.IO) {
            ScriptEnvironmen.executeResultRoot(this@ActionPage, config.pageHandlerSh, config, extraParams)

            if (!isActive) return@launch

            withContext(Dispatchers.Main) {
                if (isFinishing) return@withContext
                when {
                    menuOption.autoFinish -> finish()
                    menuOption.reloadPage -> recreate()
                    menuOption.autoKill -> killApp()
                    menuOption.autoRestart -> restartApp()
                }
                if (menuOption.type == "checkbox") {
                    refreshCheckboxMenuStates()
                }
            }
        }
    }

    private fun loadPageConfig(showLoading: Boolean = true) {
        val config = currentPageConfig ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            if (config.beforeRead.isNotEmpty()) {
                withContext(Dispatchers.Main) { progressBarDialog.showDialog(getString(R.string.kr_page_before_load)) }
                ScriptEnvironmen.executeResultRoot(this@ActionPage, config.beforeRead, config)
            }

            if (showLoading) {
                withContext(Dispatchers.Main) { progressBarDialog.showDialog(getString(R.string.kr_page_loading)) }
            }

            var items: ArrayList<NodeInfoBase>? = null
            if (config.pageConfigSh.isNotEmpty()) {
                items = PageConfigSh(this@ActionPage, config.pageConfigSh, config).execute()
            }
            if (items == null && config.pageConfigPath.isNotEmpty()) {
                items = PageConfigReader(applicationContext, config.pageConfigPath, config.pageConfigDir).readConfigXml()
            }

            if (config.afterRead.isNotEmpty()) {
                ScriptEnvironmen.executeResultRoot(this@ActionPage, config.afterRead, config)
            }

            withContext(Dispatchers.Main) {
                if (!isActive || isFinishing) return@withContext
                
                if (items != null && items.isNotEmpty()) {
                    if (config.loadSuccess.isNotEmpty()) {
                        ScriptEnvironmen.executeResultRoot(this@ActionPage, config.loadSuccess, config)
                    }
                    updateActionList(items, showLoading)
                } else {
                    handleLoadError(config)
                }
                progressBarDialog.hideDialog()
            }
        }
    }

    private fun updateActionList(items: ArrayList<NodeInfoBase>, showLoading: Boolean) {
        val autoRunTask = if (actionsLoaded) null else object : AutoRunTask {
            override val key = autoRunItemId
            override fun onCompleted(result: Boolean?) {
                if (result != true && autoRunItemId.isNotEmpty()) {
                    Toast.makeText(this@ActionPage, getString(R.string.kr_auto_run_item_losted), Toast.LENGTH_SHORT).show()
                }
            }
        }

        val existingFragment = supportFragmentManager.findFragmentById(R.id.main_list) as? ActionListFragment
        if (existingFragment != null && !showLoading) {
            existingFragment.updateData(items, actionShortClickHandler, ThemeModeState.getThemeMode())
        } else {
            val fragment = ActionListFragment.create(items, actionShortClickHandler, autoRunTask, ThemeModeState.getThemeMode())
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_list, fragment)
                .commitAllowingStateLoss()
        }
        actionsLoaded = true
    }

    private fun handleLoadError(config: PageNode) {
        if (config.loadFail.isNotEmpty()) {
            ScriptEnvironmen.executeResultRoot(this, config.loadFail, config)
        }
        Toast.makeText(this, getString(R.string.kr_page_load_fail), Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun restartApp() {
        val intent = Intent(this, SplashActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("force_reset", true)
        }
        startActivity(intent)
        finish()
    }

    private fun killApp() {
        startService(Intent(this, WakeLockService::class.java).apply { action = WakeLockService.ACTION_END_WAKELOCK })
        finishAffinity()
        System.exit(0)
    }

    private fun menuItemExecute(menuOption: PageMenuOption, params: HashMap<String, String>) {
        val onDismiss = Runnable {
            when {
                menuOption.autoFinish -> finish()
                menuOption.reloadPage -> recreate()
                menuOption.autoKill -> killApp()
                menuOption.autoRestart -> restartApp()
            }
        }
        val config = currentPageConfig ?: return
        val dialog = DialogLogFragment.create(menuOption, {}, onDismiss, config.pageHandlerSh, params, ThemeModeState.getThemeMode().isDarkMode)
        dialog.show(supportFragmentManager, "")
        dialog.isCancelable = false
    }

    private fun menuItemChooseFile(menuOption: PageMenuOption) {
        chooseFilePath(object : ParamsFileChooserRender.FileSelectedInterface {
            override fun onFileSelected(path: String?) {
                path?.let {
                    handler.post {
                        menuItemExecute(menuOption, hashMapOf("state" to menuOption.key, "menu_id" to menuOption.key, "file" to it, "folder" to it))
                    }
                }
            }
            override fun mimeType() = menuOption.mime.ifEmpty { null }
            override fun suffix() = menuOption.suffix.ifEmpty { null }
            override fun type() = if (menuOption.type == "folder") ParamsFileChooserRender.FileSelectedInterface.TYPE_FOLDER else ParamsFileChooserRender.FileSelectedInterface.TYPE_FILE
        })
    }

    private fun chooseFilePath(fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface): Boolean {
        return try {
            if (fileSelectedInterface.type() == ParamsFileChooserRender.FileSelectedInterface.TYPE_FOLDER) {
                startActivityForResult(Intent(this, ActivityFileSelector::class.java).apply { putExtra("mode", ActivityFileSelector.MODE_FOLDER) }, ACTION_FILE_PATH_CHOOSER_INNER)
            } else {
                val suffix = fileSelectedInterface.suffix()
                if (!suffix.isNullOrEmpty()) {
                    startActivityForResult(Intent(this, ActivityFileSelector::class.java).apply { 
                        putExtra("extension", suffix)
                        putExtra("mode", ActivityFileSelector.MODE_FILE) 
                    }, ACTION_FILE_PATH_CHOOSER_INNER)
                } else {
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = fileSelectedInterface.mimeType() ?: "*/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                    startActivityForResult(intent, ACTION_FILE_PATH_CHOOSER)
                }
            }
            this.fileSelectedInterface = fileSelectedInterface
            true
        } catch (_: Exception) { false }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && data != null) {
            val path = when (requestCode) {
                ACTION_FILE_PATH_CHOOSER -> data.data?.let { FilePathResolver().getPath(this, it) }
                ACTION_FILE_PATH_CHOOSER_INNER -> data.getStringExtra("file")
                else -> null
            }
            fileSelectedInterface?.onFileSelected(path)
        }
        fileSelectedInterface = null
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onResume() {
        super.onResume()
        if (!actionsLoaded) loadPageConfig(true)
    }

    override fun onRestart() {
        super.onRestart()
        if (openedSubPage && actionsLoaded) {
            openedSubPage = false
            loadPageConfig(false)
        }
    }

    fun _openPage(pageNode: PageNode) {
        if (openedSubPage) return
        openedSubPage = true
        OpenPageHelper(this).openPage(pageNode)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        setExcludeFromRecents()
        super.onDestroy()
    }

    private fun setExcludeFromRecents() {
        if (isTaskRoot) {
            try {
                val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                am.appTasks.find { it.taskInfo.id == taskId }?.setExcludeFromRecents(true)
            } catch (_: Exception) {}
        }
    }
}