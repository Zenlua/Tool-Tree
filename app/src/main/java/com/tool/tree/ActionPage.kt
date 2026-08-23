package com.tool.tree

import android.app.ActivityManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.tool.tree.ui.SwipeBackHelper
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActionPage : AppCompatActivity() {
    private val progressBarDialog by lazy { ProgressBarDialog(this) }
    // Thanh tiến trình mảnh dưới toolbar - chỉ dùng khi trang cấu hình process = true
    // (xem loadPageConfig/beginProgressiveList). Truy cập kiểu findViewById giống như
    // toolbar ở trên, vì app_bar_main.xml được <include> không có id riêng.
    private val loadProgressBar by lazy { findViewById<ProgressBar>(R.id.page_load_progress) }
    private var actionsLoaded = false
    private val handler = Handler(Looper.getMainLooper())

    private var currentPageConfig: PageNode? = null
    private var autoRunItemId = ""
    private lateinit var binding: ActivityActionPageBinding
    private var openedSubPage = false

    // Vuốt từ mép trái để trở lại (giống nút back trên toolbar), có hiệu ứng kéo theo tay
    private lateinit var swipeBackHelper: SwipeBackHelper

    // Khóa theo ID thật của item để không lệ thuộc vào vị trí trong mảng
    private val justClickedItemIds = HashSet<Int>()

    private var fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface? = null
    private val ACTION_FILE_PATH_CHOOSER = 65400
    private val ACTION_FILE_PATH_CHOOSER_INNER = 65300

    private var menuOptions: ArrayList<PageMenuOption>? = null
    private var menuCheckboxRefreshing = false
    private var checkboxRefreshJob: Job? = null
    private var loadPageJob: Job? = null

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
        swipeBackHelper = SwipeBackHelper(this, binding.root)

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

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // Có vài đường early-return trong onCreate() (chưa init xong ScriptEnvironmen,...)
        // thoát trước khi setContentView/khởi tạo swipeBackHelper -> phải kiểm tra tránh
        // UninitializedPropertyAccessException
        if (::swipeBackHelper.isInitialized && swipeBackHelper.dispatchTouchEvent(ev)) {
            return true
        }
        return super.dispatchTouchEvent(ev)
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
                val uniqueItemId = option.key.hashCode()
                val menuItem = menu?.add(Menu.NONE, uniqueItemId, Menu.NONE, option.title)
                if (option.type == "checkbox") {
                    menuItem?.isCheckable = true
                }
            }
        }
    
        // Chỉ refresh 1 lần sau khi menu được dựng xong
        handler.post {
            refreshCheckboxMenuStates()
        }
    
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // Chỉ cập nhật giao diện từ dữ liệu đã có sẵn, tuyệt đối không chạy shell ở đây
        menuOptions?.forEach { option ->
            if (!option.isFab && option.type == "checkbox") {
                val uniqueItemId = option.key.hashCode()
                menu.findItem(uniqueItemId)?.isChecked = option.checked
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun refreshCheckboxMenuStates() {
        val config = currentPageConfig ?: return
    
        val checkboxOptions = menuOptions?.filter { option ->
            option.type == "checkbox" &&
                    option.checkedSh.isNotEmpty() &&
                    !justClickedItemIds.contains(option.key.hashCode())
        }.orEmpty()
    
        if (checkboxOptions.isEmpty() || menuCheckboxRefreshing) return
    
        menuCheckboxRefreshing = true
        checkboxRefreshJob?.cancel()
    
        checkboxRefreshJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Root shell dùng chung 1 tiến trình duy nhất (có ReentrantLock bên trong),
                // nên dù gọi executeResultRoot() song song bằng N coroutine, các lệnh vẫn
                // phải xếp hàng chạy TUẦN TỰ, mỗi lệnh tốn round-trip riêng (ghi lệnh, chờ
                // đọc kết quả). Với 3 checkbox trở lên, tổng thời gian chờ tăng tuyến tính
                // dù mỗi lệnh chỉ là getprop đơn giản -> đó là lý do menu hiện dấu tích chậm.
                // Gộp toàn bộ checkedSh thành 1 lần gọi shell duy nhất để chỉ tốn 1 round-trip.
                val scripts = LinkedHashMap<String, String>()
                checkboxOptions.forEachIndexed { index, option ->
                    scripts[index.toString()] = option.checkedSh
                }
    
                val results = ScriptEnvironmen.executeMultipleResultRoot(this@ActionPage, scripts, config)
    
                withContext(Dispatchers.Main) {
                    if (isFinishing || isDestroyed) return@withContext
    
                    var changed = false
                    checkboxOptions.forEachIndexed { index, option ->
                        val uniqueItemId = option.key.hashCode()
                        if (!justClickedItemIds.contains(uniqueItemId)) {
                            val result = results[index.toString()]?.trim() ?: ""
                            val newChecked = result == "1" || result.equals("true", ignoreCase = true)
                            if (option.checked != newChecked) changed = true
                            option.checked = newChecked
                        }
                    }
    
                    if (changed) {
                        invalidateOptionsMenu()
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    menuCheckboxRefreshing = false
                }
            }
        }
    }

    private fun addFab(menuOption: PageMenuOption) {
        binding.actionPageFab.apply {
            visibility = View.VISIBLE
            setOnClickListener { onMenuItemClick(menuOption) }

            val iconRes = if ((menuOption.type == "file" || menuOption.type == "folder") && menuOption.iconPath.isEmpty()) {
                R.drawable.kr_folder
            } else {
                R.drawable.kr_fab
            }
            val customIcon = if (menuOption.iconPath.isNotEmpty()) {
                IconPathAnalysis().loadLogo(context, menuOption, false)
            } else null

            setImageDrawable(customIcon ?: ContextCompat.getDrawable(context, iconRes))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val options = menuOptions ?: return false
        val targetItemId = item.itemId
        val option = options.find { it.key.hashCode() == targetItemId }

        if (option != null) {
            if (option.type == "checkbox") {
                option.checked = !option.checked
                item.isChecked = option.checked

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
            menuOption.pageConfigPath.isNotEmpty()
        ) {
            openMenuOptionAsPage(menuOption)
            return
        }

        when (menuOption.type) {
            "refresh", "reload" -> recreate()
            "restart" -> restartApp()
            "exit", "finish", "close" -> finish()
            "killapp" -> killApp()
            "file", "folder" -> menuItemChooseFile(menuOption)
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
        // Nếu option có script riêng (script/set) thì chạy trực tiếp, không cần pageHandlerSh
        // + không cần dựa vào $menu_id/$state để tự phân biệt option nào được bấm nữa.
        val script = if (menuOption.script.isNotEmpty()) menuOption.script else config.pageHandlerSh

        lifecycleScope.launch(Dispatchers.IO) {
           val output = ScriptEnvironmen.executeResultRoot(this@ActionPage, script, config, extraParams)

            if (!isActive) return@launch

            withContext(Dispatchers.Main) {
                if (isFinishing || isDestroyed) return@withContext
                if (!output.isNullOrBlank()) {
                    SilentShellOutputHandler(this@ActionPage).processOutput(output)
                }
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

        loadPageJob?.cancel()
        progressBarDialog.setCancelCallback {
            loadPageJob?.cancel()
            finish()
        }

        // process = true + đang hiện loading (mở trang lần đầu, hoặc reload có showLoading):
        // hiện từng mục 1 ngay khi build xong thay vì đợi build xong hết trang mới hiện toàn
        // bộ - dành cho trang có rất nhiều mục/nhiều lệnh shell nên load rất lâu (xem
        // PageConfigReader.tomlChildren). Reload âm thầm (showLoading = false) vẫn dùng
        // luồng cũ.
        //
        // Thứ tự hiển thị: hộp thoại loading hiện trước như bình thường (chưa có mục nào thì
        // vẫn hiện loading); CHỈ khi mục ĐẦU TIÊN build xong mới ẩn hộp thoại và chuyển sang
        // thanh tiến trình dưới toolbar (chạy động, không hiện %). Trang KHÔNG bật
        // process = true dùng thanh này cho giai đoạn dựng UI/ảnh (xem nhánh else bên dưới),
        // và hộp thoại vẫn hiện suốt lúc build dữ liệu như cũ vì không có tín hiệu từng mục.
        val useProgressiveLoad = showLoading && config.process

        loadPageJob = lifecycleScope.launch(Dispatchers.IO) {
            if (showLoading) {
                withContext(Dispatchers.Main) {
                    // Reset lại từ lần load trước (nếu có) - chưa có mục nào nên chưa hiện.
                    hideLoadProgress()
                    val initialText = if (config.beforeRead.isNotEmpty())
                        getString(R.string.kr_page_before_load) else getString(R.string.kr_page_loading)
                    progressBarDialog.showDialog(initialText)
                }
            }

            if (config.beforeRead.isNotEmpty()) {
                ScriptEnvironmen.executeResultRoot(this@ActionPage, config.beforeRead, config)
                if (showLoading) {
                    withContext(Dispatchers.Main) {
                        progressBarDialog.showDialog(getString(R.string.kr_page_loading))
                    }
                }
            }

            var progressiveFragment: ActionListFragment? = null
            if (useProgressiveLoad) {
                withContext(Dispatchers.Main) {
                    // Fragment dựng rỗng ngay từ giờ để sẵn sàng nhận mục qua
                    // appendProgressiveItem() - hộp thoại loading vẫn đang che nên người dùng
                    // chưa thấy gì cho tới khi mục đầu tiên xong (xem onNodeReady bên dưới).
                    progressiveFragment = beginProgressiveList()
                }
            }

            // Mỗi mục build xong ở PageConfigReader/PageConfigSh (đang chạy tuần tự trên
            // luồng IO) được post lên main thread để thêm vào UI. QUAN TRỌNG: luồng IO phải
            // ĐỢI post() này thực thi xong (qua CountDownLatch) rồi mới được build/report mục
            // kế tiếp - nếu không, khi dữ liệu build nhanh hơn UI kịp vẽ (appendProgressiveItem
            // có thể decode ảnh trên main thread), IO thread sẽ dội hàng loạt post() liên tiếp
            // vào hàng đợi main thread. Main thread khi đó phải xử lý hết các Runnable đã xếp
            // hàng trước rồi mới tới lượt sự kiện chạm/vuốt -> không vuốt được dù thanh tiến
            // trình (animation riêng do Choreographer đảm nhiệm) trông như vẫn đang chạy bình
            // thường. Đồng bộ theo từng mục đảm bảo hàng đợi main thread không bao giờ có quá
            // 1 việc chờ xử lý cùng lúc, luôn còn khe hở cho vuốt/chạm giữa các mục.
            var barShown = false

            val onNodeReady: ((NodeInfoBase?, Int, Int) -> Unit)? = if (useProgressiveLoad) {
                { node, _, _ ->
                    // Giải mã trước (và tự nạp vào cache dùng chung của IconPathAnalysis)
                    // NGAY TRÊN LUỒNG IO này - không đụng gì tới UI nên an toàn. Nhờ vậy khi
                    // main thread thực sự dựng view cho mục này (bên dưới), lệnh decode ảnh
                    // bên trong chỉ còn là 1 lượt đọc cache (gần như tức thời) thay vì phải tự
                    // đọc file + giải mã bitmap ngay trên main thread - đây là phần tốn thời
                    // gian nhất mỗi lần thêm mục, nên gỡ nó ra khỏi main thread giúp cuộn mượt
                    // hơn hẳn dù vẫn phải đợi đồng bộ theo từng mục (xem latch bên dưới).
                    if (node != null) {
                        try {
                            prewarmNodeImages(node)
                        } catch (_: Exception) {
                        }
                    }

                    if (!isFinishing && !isDestroyed) {
                        val latch = java.util.concurrent.CountDownLatch(1)
                        handler.post {
                            try {
                                if (!isFinishing && !isDestroyed) {
                                    if (!barShown) {
                                        // Mục ĐẦU TIÊN đã sẵn sàng - ẩn hộp thoại, chuyển sang
                                        // thanh tiến trình chạy động (không hiện %).
                                        barShown = true
                                        progressBarDialog.hideDialog()
                                        loadProgressBar.apply {
                                            isIndeterminate = true
                                            visibility = View.VISIBLE
                                        }
                                    }
                                    if (node != null) progressiveFragment?.appendProgressiveItem(node)
                                }
                            } finally {
                                latch.countDown()
                            }
                        }
                        // Giới hạn thời gian chờ để luồng IO không treo vĩnh viễn trong
                        // trường hợp hiếm gặp Runnable trên không bao giờ chạy được (vd
                        // Activity vừa bị huỷ ngay lúc này).
                        latch.await(1000, java.util.concurrent.TimeUnit.MILLISECONDS)
                    }
                }
            } else null

            var items: ArrayList<NodeInfoBase>? = null
            if (config.pageConfigSh.isNotEmpty()) {
                items = PageConfigSh(this@ActionPage, config.pageConfigSh, config).execute(onNodeReady)
            }
            if (items == null && config.pageConfigPath.isNotEmpty()) {
                items = PageConfigReader(applicationContext, config.pageConfigPath, config.pageConfigDir).readConfigXml(onNodeReady)
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
                    progressBarDialog.hideDialog()
                    if (useProgressiveLoad) {
                        // resolvePendingStates() (chạy trong PageConfigReader) đã xong lúc
                        // này - làm mới hiển thị switch/picker về đúng trạng thái thật rồi
                        // mới chạy autoRunTask, xem ActionListFragment.finishProgressiveList.
                        progressiveFragment?.finishProgressiveList()
                        actionsLoaded = true
                        hideLoadProgress()
                    } else if (showLoading) {
                        // Trang KHÔNG bật process = true, đang ở lượt tải có hiện hộp thoại
                        // (mở trang lần đầu / reload có showLoading): ActionListFragment dựng
                        // TOÀN BỘ view (kể cả decode ảnh icon/logo) ĐỒNG BỘ trên main thread
                        // ngay khi fragment được add (xem PageLayoutRender) - với trang nhiều
                        // mục ảnh việc này có thể mất khá lâu, khiến app trông như bị đơ ngay
                        // sau khi hộp thoại loading vừa biến mất. Hiện thanh tiến trình chạy
                        // động (không có %, vì không biết trước mục ảnh nào chậm) trong lúc
                        // dựng, chỉ tắt khi ActionListFragment báo đã dựng xong qua onRendered.
                        loadProgressBar.apply {
                            isIndeterminate = true
                            visibility = View.VISIBLE
                        }
                        updateActionList(items, showLoading) { hideLoadProgress() }
                    } else {
                        // Reload âm thầm (showLoading = false, vd quay lại từ trang con) -
                        // giữ nguyên như cũ, không hiện thanh tiến trình.
                        updateActionList(items, showLoading)
                    }
                    refreshCheckboxMenuStates()
                } else {
                    handleLoadError(config)
                    hideLoadProgress()
                    progressBarDialog.hideDialog()
                }
            }
        }
    }

    // Giải mã trước toàn bộ ảnh (icon/logo/photo/bg) của 1 mục (và các mục con nếu là
    // GroupNode) NGAY TRÊN LUỒNG GỌI HÀM (không đụng UI, an toàn để gọi từ luồng IO) - kết
    // quả tự vào cache dùng chung của IconPathAnalysis (khoá theo pageConfigDir + đường dẫn),
    // nên lần load lại sau đó trên main thread (lúc dựng view) chỉ còn là đọc cache. Dùng cho
    // luồng tải từng mục 1 (process = true) - xem onNodeReady trong loadPageConfig().
    private fun prewarmNodeImages(node: NodeInfoBase) {
        val iconPathAnalysis = IconPathAnalysis()
        when (node) {
            is TextNode -> {
                node.rows.forEach { row ->
                    try {
                        iconPathAnalysis.loadtextPhoto(this, row, node.pageConfigDir)
                    } catch (_: Exception) {
                    }
                }
            }
            is GroupNode -> {
                node.children.forEach { child -> prewarmNodeImages(child) }
            }
            is ClickableNode -> {
                try { iconPathAnalysis.loadIcon(this, node) } catch (_: Exception) {}
                try { iconPathAnalysis.loadLogo(this, node, false) } catch (_: Exception) {}
                try { iconPathAnalysis.loadPhoto(this, node) } catch (_: Exception) {}
                try { iconPathAnalysis.loadBg(this, node) } catch (_: Exception) {}
            }
        }
    }

    private fun buildAutoRunTask(): AutoRunTask? {
        return if (actionsLoaded) null else object : AutoRunTask {
            override val key = autoRunItemId
            override fun onCompleted(result: Boolean?) {
                if (result != true && autoRunItemId.isNotEmpty()) {
                    Toast.makeText(this@ActionPage, getString(R.string.kr_auto_run_item_losted), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Dựng 1 ActionListFragment RỖNG - dùng khi trang cấu hình process = true. Thanh tiến
    // trình dưới toolbar đã được hiện từ đầu loadPageConfig() rồi (chung cho mọi trang có
    // showLoading), ở đây chỉ cần dựng danh sách rỗng để appendProgressiveItem() đổ mục vào.
    private fun beginProgressiveList(): ActionListFragment {
        val fragment = ActionListFragment.createProgressive(actionShortClickHandler, buildAutoRunTask(), ThemeModeState.getThemeMode())
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_list, fragment)
            .commitAllowingStateLoss()
        return fragment
    }

    private fun hideLoadProgress() {
        loadProgressBar.visibility = View.GONE
    }

    private fun updateActionList(items: ArrayList<NodeInfoBase>, showLoading: Boolean, onRendered: (() -> Unit)? = null) {
        val existingFragment = supportFragmentManager.findFragmentById(R.id.main_list) as? ActionListFragment
        if (existingFragment != null && !showLoading) {
            existingFragment.updateData(items, actionShortClickHandler, ThemeModeState.getThemeMode(), onRendered)
        } else {
            val fragment = ActionListFragment.create(items, actionShortClickHandler, buildAutoRunTask(), ThemeModeState.getThemeMode(), onRendered)
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
        startService(Intent(this, WakeLockService::class.java).apply {
            action = WakeLockService.ACTION_END_WAKELOCK
        })
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

            if (menuOption.type == "checkbox") {
                refreshCheckboxMenuStates()
            }
        }

        val config = currentPageConfig ?: return
        // Nếu option có script riêng (script/set) thì chạy trực tiếp, không cần pageHandlerSh
        val script = if (menuOption.script.isNotEmpty()) menuOption.script else config.pageHandlerSh
        val dialog = DialogLogFragment.create(
            menuOption,
            {},
            onDismiss,
            script,
            params,
            ThemeModeState.getThemeMode().isDarkMode
        )
        dialog.show(supportFragmentManager, "")
        dialog.isCancelable = false
    }

    private fun menuItemChooseFile(menuOption: PageMenuOption) {
        chooseFilePath(object : ParamsFileChooserRender.FileSelectedInterface {
            override fun onFileSelected(path: String?) {
                path?.let {
                    handler.post {
                        menuItemExecute(
                            menuOption,
                            hashMapOf(
                                "state" to menuOption.key,
                                "menu_id" to menuOption.key,
                                "file" to it,
                                "folder" to it
                            )
                        )
                    }
                }
            }

            override fun mimeType() = menuOption.mime.ifEmpty { null }
            override fun suffix() = menuOption.suffix.ifEmpty { null }
            override fun pathHome() = menuOption.pathHome.ifEmpty { null }
            override fun multiple() = menuOption.multiple
            override fun type() = if (menuOption.type == "folder") {
                ParamsFileChooserRender.FileSelectedInterface.TYPE_FOLDER
            } else {
                ParamsFileChooserRender.FileSelectedInterface.TYPE_FILE
            }
        })
    }

    private fun chooseFilePath(fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface): Boolean {
        return try {
            val multiple = fileSelectedInterface.multiple()
            val pathHome = fileSelectedInterface.pathHome()
            if (fileSelectedInterface.type() == ParamsFileChooserRender.FileSelectedInterface.TYPE_FOLDER) {
                startActivityForResult(
                    Intent(this, ActivityFileSelector::class.java).apply {
                        putExtra("mode", ActivityFileSelector.MODE_FOLDER)
                        putExtra("multiple", multiple)
                        if (!pathHome.isNullOrEmpty()) putExtra("path_home", pathHome)
                    },
                    ACTION_FILE_PATH_CHOOSER_INNER
                )
            } else {
                val suffix = fileSelectedInterface.suffix()
                if (!suffix.isNullOrEmpty() || !pathHome.isNullOrEmpty()) {
                    startActivityForResult(
                        Intent(this, ActivityFileSelector::class.java).apply {
                            if (!suffix.isNullOrEmpty()) putExtra("extension", suffix)
                            putExtra("mode", ActivityFileSelector.MODE_FILE)
                            putExtra("multiple", multiple)
                            if (!pathHome.isNullOrEmpty()) putExtra("path_home", pathHome)
                        },
                        ACTION_FILE_PATH_CHOOSER_INNER
                    )
                } else {
                    // Hỗ trợ nhiều mime type cùng lúc, ví dụ:
                    // mime="application/vnd.android.package-archive,application/java-archive,application/zip"
                    val mimeTypes = fileSelectedInterface.mimeType()
                        ?.split(",")
                        ?.map { it.trim() }
                        ?.filter { it.isNotEmpty() }
                        ?.toTypedArray()
                        ?: emptyArray()

                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        if (mimeTypes.size > 1) {
                            // Với nhiều mime type, "type" phải để "*/*" và liệt kê
                            // các mime cụ thể qua EXTRA_MIME_TYPES
                            type = "*/*"
                            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                        } else {
                            type = mimeTypes.firstOrNull() ?: "*/*"
                        }
                        addCategory(Intent.CATEGORY_OPENABLE)
                        if (multiple) {
                            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        }
                    }
                    startActivityForResult(intent, ACTION_FILE_PATH_CHOOSER)
                }
            }
            this.fileSelectedInterface = fileSelectedInterface
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && data != null) {
            val currentInterface = fileSelectedInterface
            val separator = currentInterface?.separator() ?: "\n"
            val path = when (requestCode) {
                ACTION_FILE_PATH_CHOOSER -> {
                    val clipData = data.clipData
                    if (currentInterface?.multiple() == true && clipData != null && clipData.itemCount > 0) {
                        (0 until clipData.itemCount)
                            .mapNotNull { FilePathResolver().getPath(this, clipData.getItemAt(it).uri) }
                            .joinToString(separator)
                    } else {
                        data.data?.let { FilePathResolver().getPath(this, it) }
                    }
                }
                ACTION_FILE_PATH_CHOOSER_INNER -> {
                    val files = data.getStringArrayListExtra("files")
                    if (currentInterface?.multiple() == true && files != null) {
                        files.joinToString(separator)
                    } else {
                        data.getStringExtra("file")
                    }
                }
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
        checkboxRefreshJob?.cancel()
        handler.removeCallbacksAndMessages(null)
        if (::swipeBackHelper.isInitialized) swipeBackHelper.release()
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