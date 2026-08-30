package com.tool.tree

import android.app.ActivityManager
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListPopupWindow
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.tool.tree.ui.PopupMenuListAdapter
import com.tool.tree.ui.PopupMenuRow
import com.tool.tree.ui.PopupRowRightIcon
import com.tool.tree.ui.SwipeBackHelper
import com.tool.tree.ui.SwipeBackPreviewCache
import com.omarea.common.model.SelectItem
import com.omarea.common.shared.FilePathResolver
import com.omarea.common.ui.DialogHelper
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
import com.tool.tree.databinding.ActivityActionPageBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActionPage : AppCompatActivity() {
    companion object {
        // Icon fab lúc bấm "refresh"/"reload" (xem spinFabThenRecreate()) - lưu ở static (KHÔNG
        // phải field instance) vì recreate() huỷ hẳn activity cũ rồi dựng instance HOÀN TOÀN
        // MỚI, field instance thường sẽ mất; static field vẫn sống vì class chưa bị unload.
        // != null nghĩa là "đang có 1 lượt xoay dở dang, đợi trang mới tải xong" - xem
        // onCreateOptionsMenu()/stopFabSpinIfPending(). Luôn được set về null ngay khi tiêu thụ
        // xong (tải xong HOẶC tải lỗi HOẶC activity đóng hẳn giữa chừng - xem handleLoadError()/
        // onDestroy()) để không lỡ tay làm fab của 1 trang khác không liên quan tự xoay.
        private var pendingSpinIcon: android.graphics.drawable.Drawable? = null
    }

    private val progressBarDialog by lazy { ProgressBarDialog(this) }
    private val loadProgressBar by lazy { findViewById<ProgressBar>(R.id.page_load_progress) }
    private var actionsLoaded = false
    private val handler = Handler(Looper.getMainLooper())

    private var currentPageConfig: PageNode? = null
    private var autoRunItemId = ""
    private lateinit var binding: ActivityActionPageBinding
    private var openedSubPage = false

    private lateinit var swipeBackHelper: SwipeBackHelper

    // Ảnh preview (sharp/blurred) đang hiển thị phía sau lúc vuốt lùi - lưu lại instance field
    // (thay vì chỉ val cục bộ trong onCreate) để onRetainCustomNonConfigurationInstance() có
    // thể lấy ra và giữ nó sống sót qua các lần recreate() CỦA CHÍNH trang này (xoay màn hình,
    // hoặc gọi recreate() thủ công như spinFabThenRecreate()/reloadPage/restartApp...) - xem
    // giải thích chi tiết ở onDestroy().
    private var swipePreview: SwipeBackPreviewCache.Preview? = null

    private val justClickedItemIds = HashSet<Int>()

    private var fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface? = null
    private val ACTION_FILE_PATH_CHOOSER = 65400
    private val ACTION_FILE_PATH_CHOOSER_INNER = 65300

    private var menuOptions: ArrayList<PageMenuOption>? = null
    // [[group.action]] menu = true: icon riêng luôn hiện trên toolbar - xem onCreateOptionsMenu()
    private var headerActions: ArrayList<ActionNode>? = null
    // true sau khi đã tự mở dialog show=true 1 LẦN trong phiên mở trang hiện tại - tránh lặp
    // lại mỗi khi trang reload (vd action khác gọi reload-page). Reset về false khi mở trang
    // mới thật sự (activity mới), KHÔNG reset khi chỉ reload nội dung trang hiện tại.
    private var autoShowTriggered = false
    private var menuCheckboxRefreshing = false
    private var checkboxRefreshJob: Job? = null
    private var loadPageJob: Job? = null
    private var spinnerLoadJob: Job? = null
    private var lockCheckJob: Job? = null
    // true ngay khi checkPageLockThenLoad() đã BẮT ĐẦU chạy 1 lần cho phiên mở trang hiện tại -
    // tránh chạy lại (và hiện chồng thêm dialog loading/lock) nếu onResume() gọi lại trong lúc
    // vẫn đang đợi kết quả lockShell hoặc đang hiện dialog báo khoá (ví dụ activity resume lại
    // do người dùng vừa quay lại từ 1 activity hệ thống nào đó trong lúc dialog còn hiện) - áp
    // dụng cho cả nhánh gọi từ onEnterAnimationComplete() lẫn nhánh dự phòng trong onResume().
    private var lockCheckStarted = false

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

        // Ưu tiên lấy lại đúng cặp bitmap đã retain từ lần sống trước của CHÍNH activity này
        // (xem onRetainCustomNonConfigurationInstance() ở dưới) - trường hợp onCreate() này
        // chạy do recreate()/xoay màn hình chứ không phải do mở trang mới. Chỉ khi không có gì
        // được retain (tức đây thực sự là lần mở trang đầu tiên) mới tiêu thụ
        // SwipeBackPreviewCache - cache dùng 1 lần, chụp bởi OpenPageHelper NGAY TRƯỚC khi mở
        // trang này từ trang cha.
        @Suppress("DEPRECATION")
        val retainedPreview = lastCustomNonConfigurationInstance as? SwipeBackPreviewCache.Preview
        swipePreview = retainedPreview ?: SwipeBackPreviewCache.consume()
        swipePreview?.let {
            binding.swipeBackPreviewSharp.setImageBitmap(it.sharp)
            if (it.blurred != null) {
                binding.swipeBackPreviewBlur.setImageBitmap(it.blurred)
            } else {
                binding.swipeBackPreviewBlur.setImageBitmap(it.sharp)
            }
        }

        swipeBackHelper = SwipeBackHelper(
            activity = this,
            contentView = binding.swipeForeground,
            onDragStateChanged = { dragging ->
                if (swipePreview != null) {
                    val visibility = if (dragging) View.VISIBLE else View.GONE
                    binding.swipeBackPreviewBlur.visibility = visibility
                    binding.swipeBackPreviewSharp.visibility = visibility
        
                    if (!dragging) {
                        binding.swipeBackPreviewSharp.alpha = 0f
                    }
                }
            },
            onDragProgress = { progress ->
                binding.swipeBackPreviewSharp.alpha = progress * progress
            }
        )

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        setTitle(R.string.app_name)

        supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            // ActionBar dựng từ Toolbar (setSupportActionBar) KHÔNG đọc thuộc tính style
            // homeAsUpIndicator/android:homeAsUpIndicator (thuộc tính đó chỉ áp dụng cho
            // WindowActionBar gốc, mà app đã tắt windowActionBar=false) - phải set icon
            // trực tiếp bằng code như dưới đây thì mới đổi được icon back.
            setHomeAsUpIndicator(R.drawable.ic_arrow_back)
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
        } else {
            // Thêm dòng này để gọi ngay lập tức khi Activity vừa được khởi tạo
            checkPageLockThenLoad()
        }
        
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
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
            // Đã bỏ hẳn option-sh (script sinh menu động ở trang cha) - menu/fab giờ luôn đến
            // thẳng từ config.pageMenuOptions, được PageConfigReader/PageConfigSh gán sẵn ngay
            // khi đọc xong toml của CHÍNH trang này (xem loadPageConfig()).
            menuOptions = config.pageMenuOptions
        }
        if (headerActions == null) {
            headerActions = config.headerActions
        }
    
        menu?.clear()

        // [[group.action]] menu = true: khác các mục ở trên (luôn ẩn trong popup "⋮"), mục này
        // LUÔN hiện như 1 icon riêng ngay trên toolbar (SHOW_AS_ACTION_ALWAYS) - cạnh nút "⋮".
        headerActions?.forEach { action ->
            val uniqueItemId = ("header:" + action.key).hashCode()
            val menuItem = menu?.add(Menu.NONE, uniqueItemId, Menu.NONE, action.title)
            menuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menuItem?.icon = ContextCompat.getDrawable(this, R.drawable.ic_menu)
        }

        // menu/fab giờ được đọc lại mỗi lần trang load/reload (cùng lúc với items, xem
        // loadPageConfig()) thay vì cố định 1 lần từ trang cha như trước - nên phải ẩn fab
        // trước, tránh còn sót fab của lần build cũ khi lần build mới không có mục fab nào.
        binding.actionPageFab.visibility = View.GONE

        val fabOptions = ArrayList<PageMenuOption>()
        val overflowOptions = ArrayList<PageMenuOption>()
        menuOptions?.forEach { option ->
            // type = "spinner" LUÔN hiện như 1 mục bình thường trong popup "⋮" (icon mũi tên
            // dropdown bên phải, giống cách checkbox hiện dấu tích bên phải - xem
            // PopupMenuListAdapter/buildPopupRow()) - kể cả khi mục này được khai báo trong
            // [[fab]] (isFab = true). Trước đây spinner+isFab bị gộp chung vào fabOptions rồi
            // resolveFabIcon() lại gắn icon dropdown lên thẳng nút FAB - sai vì FAB không có
            // chỗ hiện tiêu đề đi kèm icon, khiến người dùng không biết đây là 1 dropdown chọn
            // giá trị. Chỉ những type KHÁC spinner mới thực sự được phép làm fab.
            if (option.isFab) {
                fabOptions.add(option)
            } else {
                overflowOptions.add(option)
            }
        }
        setupFab(fabOptions)
        // Popup List Item mới cho menu "⋮" (thay hẳn cách cũ để mỗi option tự thêm 1 MenuItem
        // native - chỉ còn 1 MenuItem duy nhất đóng vai trò nút mở popup, xem
        // setupOverflowMenuButton()).
        setupOverflowMenuButton(menu, overflowOptions)

        // Trang vẫn đang tải dở (đợi xong sau khi bấm "refresh"/"reload") - đè lên trạng thái
        // fab vừa build ở trên (setupFab() dựa vào config, lúc này có thể chưa đọc xong toml).
        if (pendingSpinIcon != null) {
            startFabSpin()
        }

        handler.post {
            refreshCheckboxMenuStates()
        }
    
        return true
    }

    // Không còn onPrepareOptionsMenu() đồng bộ trạng thái tích - các mục checkbox giờ không
    // còn là native MenuItem nào cả (xem setupOverflowMenuButton()/showOverflowMenuPopup()),
    // nên chỉ cần đọc đúng option.checked hiện tại MỖI LẦN popup được mở (buildPopupRow()) là
    // đủ, không cần cơ chế "chuẩn bị hiển thị" riêng như của native Menu nữa.
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

    private fun setupFab(fabOptions: List<PageMenuOption>) {
        when (fabOptions.size) {
            0 -> return // đã ẩn sẵn ở onCreateOptionsMenu
            1 -> addFab(fabOptions[0])
            else -> {
                // Nhiều [[fab.items]] cùng lúc: 1 nút fab duy nhất, bấm vào mở popup danh sách
                // để chọn item thực sự muốn chạy - xem showFabChooser().
                binding.actionPageFab.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { showFabChooser(fabOptions) }
                    setImageDrawable(resolveFabIcon(fabOptions))
                }
            }
        }
    }

    private fun addFab(menuOption: PageMenuOption) {
        binding.actionPageFab.apply {
            visibility = View.VISIBLE
            setOnClickListener { onMenuItemClick(menuOption, this) }
            setImageDrawable(resolveFabIcon(listOf(menuOption)))
        }
    }

    // Icon của nút fab: nếu chỉ có 1 item thì dùng icon của chính nó (như cũ). Nếu nhiều item
    // cùng chung 1 icon-path thì vẫn tôn trọng icon đó; khác nhau thì dùng icon mặc định (dấu +)
    // vì không có icon nào đại diện được cho tất cả các lựa chọn bên trong.
    // Lưu ý: fabOptions không bao giờ chứa type = "spinner" nữa (xem onCreateOptionsMenu()) nên
    // ở đây không còn cần xử lý riêng icon mũi tên dropdown cho fab.
    private fun resolveFabIcon(fabOptions: List<PageMenuOption>): android.graphics.drawable.Drawable? {
        val distinctIconPaths = fabOptions.map { it.iconPath }.distinct()
        val representative = if (distinctIconPaths.size == 1) fabOptions[0] else null

        val iconRes = if (representative != null &&
            (representative.type == "file" || representative.type == "folder") &&
            representative.iconPath.isEmpty()
        ) {
            R.drawable.kr_folder
        } else {
            R.drawable.kr_fab
        }
        val customIcon = if (representative != null && representative.iconPath.isNotEmpty()) {
            IconPathAnalysis().loadLogo(this, representative, false)
        } else null

        return customIcon ?: ContextCompat.getDrawable(this, iconRes)
    }

    // Nút "⋮" trên toolbar - thay vì để native Menu tự vẽ submenu CHỈ CÓ CHỮ như trước (mỗi
    // option = 1 MenuItem riêng), giờ chỉ thêm ĐÚNG 1 MenuItem duy nhất đóng vai trò nút mở
    // popup List Item mới (showOverflowMenuPopup()). Dùng actionView (thay vì để hệ thống tự
    // vẽ icon "⋮" mặc định) để có 1 View THẬT làm điểm neo cho ListPopupWindow - nếu không có
    // actionView, MenuItem không có View nào để anchor popup vào đúng vị trí nút "⋮".
    // Ẩn hẳn nút này nếu trang không có mục [[menu]] nào (overflowOptions rỗng), giống cách
    // fab tự ẩn khi không có mục [[fab]] nào (xem setupFab()).
    private fun setupOverflowMenuButton(menu: Menu?, overflowOptions: List<PageMenuOption>) {
        if (overflowOptions.isEmpty()) return

        val menuItem = menu?.add(Menu.NONE, Menu.NONE, Menu.NONE, getString(R.string.kr_more_options))
        menuItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        val button = layoutInflater.inflate(R.layout.toolbar_more_action, null)
        button.setOnClickListener { showOverflowMenuPopup(button, overflowOptions) }
        menuItem?.actionView = button
    }

    // Popup List Item mới cho menu "⋮" - đọc lại danh sách row MỖI LẦN mở popup (buildPopupRow())
    // nên luôn phản ánh đúng trạng thái checkbox mới nhất, không cần cơ chế đồng bộ riêng như
    // onPrepareOptionsMenu() của native Menu trước đây.
    private fun showOverflowMenuPopup(anchor: View, overflowOptions: List<PageMenuOption>) {
        showListPopup(anchor, overflowOptions.map { buildPopupRow(it, anchor) })
    }

    // Dựng 1 dòng cho popup List Item mới từ 1 PageMenuOption - dùng chung cho CẢ popup menu
    // "⋮" (showOverflowMenuPopup()) LẪN popup chọn khi FAB có nhiều item (showFabChooser()).
    // Icon PHẢI mặc định theo type (xem PopupRowRightIcon): checkbox -> dấu tích, spinner ->
    // mũi tên dropdown, mục "mở trang" (link/activity/onlineHtmlPage/pageConfigSh/
    // pageConfigPath) -> mũi tên ">"; các type còn lại (run/action...) -> icon "script"
    // (ic_editor_run), báo hiệu bấm vào sẽ chạy 1 script/hành động.
    // Icon TRÁI luôn lấy theo icon-path riêng của từng mục (nếu có khai báo) - KHÔNG suy ra
    // icon mặc định theo type (icon phải đã đủ báo hiệu loại mục rồi).
    private fun buildPopupRow(option: PageMenuOption, anchor: View?): PopupMenuRow {
        val opensPage = option.link.isNotEmpty() || option.activity.isNotEmpty() ||
            option.onlineHtmlPage.isNotEmpty() || option.pageConfigSh.isNotEmpty() ||
            option.pageConfigPath.isNotEmpty()

        val rightIcon = when {
            option.type == "checkbox" -> PopupRowRightIcon.CHECKBOX
            option.type == "spinner" -> PopupRowRightIcon.DROPDOWN
            opensPage -> PopupRowRightIcon.OPEN_PAGE
            else -> PopupRowRightIcon.SCRIPT
        }

        val leftIcon = try {
            IconPathAnalysis().loadIcon(this, option)
        } catch (_: Exception) {
            null
        }

        return PopupMenuRow(
            title = option.title,
            leftIcon = leftIcon,
            rightIcon = rightIcon,
            checked = option.checked
        ) {
            if (option.type == "checkbox") {
                option.checked = !option.checked

                val uniqueItemId = option.key.hashCode()
                justClickedItemIds.add(uniqueItemId)
                handler.postDelayed({
                    justClickedItemIds.remove(uniqueItemId)
                }, 1500)
            }
            onMenuItemClick(option, anchor)
        }
    }

    // Mở dialog của 1 action menu=true - dùng lại NGUYÊN VẸN logic dialog params/confirm/
    // warning của group.action (ActionListFragment.onActionClick).
    private fun openHeaderActionDialog(action: ActionNode) {
        val fragment = supportFragmentManager.findFragmentById(R.id.main_list) as? ActionListFragment ?: return
        fragment.onActionClick(action, Runnable {})
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Các mục [[menu]] (không phải header action) không còn là native MenuItem nào nữa -
        // giờ luôn nằm trong popup List Item mới (xem setupOverflowMenuButton()/
        // showOverflowMenuPopup()/buildPopupRow()), tự xử lý click ngay trong
        // PopupMenuRow.onClick chứ không đi qua onOptionsItemSelected().
        val targetItemId = item.itemId

        val headerAction = headerActions?.find { ("header:" + it.key).hashCode() == targetItemId }
        if (headerAction != null) {
            openHeaderActionDialog(headerAction)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // anchor: view dùng làm điểm neo cho popup dạng "spinner" (xem menuItemSpinner()) - mặc
    // định null nghĩa là dùng toolbar (trường hợp bấm từ menu 3 chấm). Khi gọi từ FAB
    // (addFab()/showFabChooser()) truyền thẳng binding.actionPageFab vào để popup "spinner"
    // đè lên đúng vị trí fab vừa bấm thay vì luôn hiện ở toolbar bất kể nguồn gọi.
    private fun onMenuItemClick(menuOption: PageMenuOption, anchor: View? = null) {
        if (menuOption.link.isNotEmpty() || menuOption.activity.isNotEmpty() ||
            menuOption.onlineHtmlPage.isNotEmpty() || menuOption.pageConfigSh.isNotEmpty() ||
            menuOption.pageConfigPath.isNotEmpty()
        ) {
            openMenuOptionAsPage(menuOption)
            return
        }

        when (menuOption.type) {
            "refresh", "reload" -> spinFabThenRecreate()
            "restart" -> restartApp()
            "exit", "finish", "close" -> finish()
            "killapp" -> killApp()
            "file", "folder" -> menuItemChooseFile(menuOption)
            "spinner" -> menuItemSpinner(menuOption, anchor)
            else -> {
                if (menuOption.silent) {
                    menuItemExecuteSilent(menuOption)
                } else {
                    menuItemExecute(menuOption, hashMapOf("state" to menuOption.key, "menu_id" to menuOption.key))
                }
            }
        }
    }

    // Gọi recreate() NGAY LẬP TỨC khi bấm mục type = "refresh"/"reload" ("Làm mới") - không đợi
    // animation nào cả. Icon fab hiện tại được lưu lại (static, xem pendingSpinIcon ở companion
    // object) để instance MỚI dựng lên biết cần tiếp tục xoay đúng icon đó - xem
    // onCreateOptionsMenu()/startFabSpin(). Vòng xoay sẽ chạy vô hạn XUYÊN SUỐT qua activity mới
    // cho tới khi trang thật sự tải xong hoàn toàn (hoặc lỗi) - xem stopFabSpinIfPending(),
    // được gọi từ tryAutoShowActions()/handleLoadError().
    private fun spinFabThenRecreate() {
        pendingSpinIcon = binding.actionPageFab.drawable
        recreate()
    }

    // Ép fab hiện + xoay bằng icon đã lưu (pendingSpinIcon) - gọi lại mỗi lần
    // onCreateOptionsMenu() build lại menu/fab trong lúc vẫn còn đang đợi trang tải xong, để đè
    // lên trạng thái GONE/icon thật mà setupFab() vừa set theo config (config lúc này CÓ THỂ vẫn
    // chưa đọc xong - xem loadPageConfig()).
    private fun startFabSpin() {
        val fab = binding.actionPageFab
        pendingSpinIcon?.let { fab.setImageDrawable(it) }
        fab.visibility = View.VISIBLE
        fab.isEnabled = false
        fab.clearAnimation()
        val rotate = android.view.animation.RotateAnimation(
            0f, 360f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 600
            repeatCount = android.view.animation.Animation.INFINITE
            interpolator = android.view.animation.LinearInterpolator()
        }
        fab.startAnimation(rotate)
    }

    // Gọi ĐÚNG lúc nội dung trang đã render xong thật sự (tryAutoShowActions()) hoặc tải lỗi
    // (handleLoadError()) - dừng xoay, cho phép bấm lại fab, rồi invalidateOptionsMenu() để
    // onCreateOptionsMenu() build lại đúng trạng thái fab thật sự theo config vừa tải (có thể
    // ẩn hẳn nếu trang không có mục fab nào).
    private fun stopFabSpinIfPending() {
        if (pendingSpinIcon == null) return
        pendingSpinIcon = null
        if (::binding.isInitialized) {
            binding.actionPageFab.apply {
                clearAnimation()
                isEnabled = true
            }
        }
        invalidateOptionsMenu()
    }

    private fun openMenuOptionAsPage(menuOption: PageMenuOption) {
        if (menuOption.link.isNotEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(menuOption.link))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (_: Exception) {
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
        // menuOption.script đã được gán sẵn handler dùng chung của nhóm [[menu]]/[[fab]] (nếu
        // bản thân mục không tự khai báo script riêng) ngay lúc parse - page không còn handler
        // riêng nữa (xem PageConfigReader.menuGroupOptionsToml()).
        val script = menuOption.script

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

    // Kiểm tra khoá của CHÍNH trang này (config.lockShell/config.locked) - gọi TỪ onResume()
    // thay cho loadPageConfig(true) trực tiếp như trước. Khác với cách cũ (kiểm tra khoá ở
    // trang CHA, TRƯỚC khi mở trang - xem ActionListFragment.onPageClick()/nodeUnlockedAsync()):
    // giờ luôn VÀO TRANG NGAY (activity đã mở, toolbar/title đã hiện), rồi mới hiện dialog
    // loading trong lúc chờ lockShell chạy xong. Nếu khoá, hiện dialog báo lỗi (thay vì toast)
    // với nút OK - bấm OK mới thật sự thoát khỏi trang (finish()), thay vì tự thoát ngay lập
    // tức không cho người dùng kịp đọc thông báo.
    private fun checkPageLockThenLoad() {
        if (lockCheckStarted) return
        lockCheckStarted = true

        val config = currentPageConfig ?: return

        if (config.lockShell.isNotEmpty()) {
            lockCheckJob?.cancel()
            lockCheckJob = lifecycleScope.launch(Dispatchers.IO) {
                val message = ScriptEnvironmen.executeResultRoot(this@ActionPage, config.lockShell, config)
                withContext(Dispatchers.Main) {
                    if (!isActive || isFinishing || isDestroyed) return@withContext
                    val unlocked = message == "unlock" || message == "unlocked" || message == "false" || message == "0"
                    if (unlocked) {
                        loadPageConfig(true)
                    } else {
                        val msg = if (message.isNotEmpty()) message else getString(R.string.kr_lock_message)
                        showPageLockedDialog(msg)
                    }
                }
            }
        } else if (config.locked) {
            // lock = "1|message" → khoá, hiện thông báo tuỳ chỉnh
            val msg = config.lockMessage.ifEmpty { getString(R.string.kr_lock_message) }
            showPageLockedDialog(msg)
        } else {
            loadPageConfig(true)
        }
    }

    private fun showPageLockedDialog(message: String) {
        DialogHelper.helpInfo(this, getString(R.string.kr_lock_title), message) {
            progressBarDialog.hideDialog()
            finish()
        }
    }

    private fun loadPageConfig(showLoading: Boolean = true) {
        val config = currentPageConfig ?: return

        loadPageJob?.cancel()
        progressBarDialog.setCancelCallback {
            loadPageJob?.cancel()
            finish()
        }

        val useProgressiveLoad = showLoading && config.process

        loadPageJob = lifecycleScope.launch(Dispatchers.IO) {
            // Chế độ load tiến trình (progressive - hiện từng item dần) đã có sẵn thanh
            // loadProgressBar ngay trong danh sách để báo đang tải, không cần thêm dialog che
            // kín màn hình nữa -> chỉ hiện dialog cho chế độ load thường (mặc định, không bật
            // process trong config trang)
            if (showLoading && !useProgressiveLoad) {
                withContext(Dispatchers.Main) {
                    hideLoadProgress()
                    val initialText = if (config.beforeRead.isNotEmpty())
                        getString(R.string.kr_page_before_load) else getString(R.string.kr_page_loading)
                    progressBarDialog.showDialog(initialText)
                }
            }

            if (config.beforeRead.isNotEmpty()) {
                ScriptEnvironmen.executeResultRoot(this@ActionPage, config.beforeRead, config)
                if (showLoading && !useProgressiveLoad) {
                    withContext(Dispatchers.Main) {
                        progressBarDialog.showDialog(getString(R.string.kr_page_loading))
                    }
                }
            }

            var progressiveFragment: ActionListFragment? = null
            if (useProgressiveLoad) {
                withContext(Dispatchers.Main) {
                    progressiveFragment = beginProgressiveList()
                    // Không có dialog ở chế độ này -> hiện luôn thanh progress inline ngay từ
                    // đầu, thay vì đợi tới khi có item đầu tiên mới hiện như trước
                    loadProgressBar.apply {
                        isIndeterminate = true
                        visibility = View.VISIBLE
                    }
                }
            }

            val onNodeReady: ((NodeInfoBase?, Int, Int) -> Unit)? = if (useProgressiveLoad) {
                { node, _, _ ->
                    if (node != null) {
                        try {
                            prewarmNodeImages(node)
                        } catch (_: Exception) {
                        }
                    }

                    // ========== FIX: bỏ CountDownLatch.await(1000ms) blocking ==========
                    // Trước đây dùng latch.await(1000ms) để đồng bộ IO thread -> Main thread
                    // cho TỪNG item. Điều này gây block IO thread tối đa 1s/item, làm chậm toàn
                    // bộ quá trình parse - VI PHẠM mục đích của progressive mode (hiện item
                    // nhanh). Giờ chỉ post lên Main thread không chờ (fire-and-forget).
                    if (!isFinishing && !isDestroyed) {
                        handler.post {
                            if (!isFinishing && !isDestroyed) {
                                if (node != null) progressiveFragment?.appendProgressiveItem(node)
                            }
                        }
                    }
                }
            } else null

            var items: ArrayList<NodeInfoBase>? = null
            // [[menu]]/[[fab]] gom được TRONG LÚC parse toml của CHÍNH trang này (thay cho
            // [[page.options]] cũ khai báo ở trang cha) - đọc cùng lúc, cùng 1 lượt với các
            // mục nội dung (items) bên dưới, KHÔNG sớm hơn.
            var loadedMenuOptions: ArrayList<PageMenuOption>? = null
            var loadedHeaderActions: ArrayList<ActionNode>? = null
            var loadedAutoShowActions: ArrayList<ActionNode>? = null
            if (config.pageConfigSh.isNotEmpty()) {
                val shReader = PageConfigSh(this@ActionPage, config.pageConfigSh, config)
                items = shReader.execute(onNodeReady)
                loadedMenuOptions = shReader.pageMenuOptions
                loadedHeaderActions = shReader.headerActions
                loadedAutoShowActions = shReader.autoShowActions
            }
            if (items == null && config.pageConfigPath.isNotEmpty()) {
                val reader = PageConfigReader(applicationContext, config.pageConfigPath, config.pageConfigDir)
                items = reader.readConfigXml(onNodeReady)
                loadedMenuOptions = reader.pageMenuOptions
                loadedHeaderActions = reader.headerActions
                loadedAutoShowActions = reader.autoShowActions
            }
            config.pageMenuOptions = loadedMenuOptions
            config.headerActions = loadedHeaderActions
            config.autoShowActions = loadedAutoShowActions

            if (config.afterRead.isNotEmpty()) {
                ScriptEnvironmen.executeResultRoot(this@ActionPage, config.afterRead, config)
            }

            withContext(Dispatchers.Main) {
                if (!isActive || isFinishing) return@withContext

                // Trang có thể KHÔNG có mục nội dung nào (items rỗng) nhưng vẫn hợp lệ nếu có
                // [[menu]]/[[fab]] - ví dụ trang chỉ dùng để hiện 1 fab hành động, không cần
                // danh sách. Trước đây "items rỗng" luôn bị coi là lỗi tải trang (đóng luôn
                // activity) vì menu/fab từng nằm ở trang CHA nên luôn tách biệt với items; giờ
                // cả hai cùng đọc từ 1 file toml nên phải tính thêm điều kiện này.
                val hasMenuOrFab = loadedMenuOptions?.isNotEmpty() == true || loadedHeaderActions?.isNotEmpty() == true
                if (items != null && (items.isNotEmpty() || hasMenuOrFab)) {
                    if (config.loadSuccess.isNotEmpty()) {
                        ScriptEnvironmen.executeResultRoot(this@ActionPage, config.loadSuccess, config)
                    }
                    progressBarDialog.hideDialog()
                    if (useProgressiveLoad) {
                        progressiveFragment?.finishProgressiveList()
                        actionsLoaded = true
                        hideLoadProgress()
                        tryAutoShowActions()
                    } else if (showLoading) {
                        loadProgressBar.apply {
                            isIndeterminate = true
                            visibility = View.VISIBLE
                        }
                        updateActionList(items, showLoading) { hideLoadProgress(); tryAutoShowActions() }
                    } else {
                        updateActionList(items, showLoading) { tryAutoShowActions() }
                    }
                    // Menu 3 chấm + fab của trang chỉ thật sự sẵn sàng tới đây (vừa đọc xong
                    // cùng lượt với items ở trên) - bỏ cache cũ (nếu có, vd sau reload) rồi yêu
                    // cầu vẽ lại toolbar để onCreateOptionsMenu() build lại menu/fab theo dữ liệu
                    // mới nhất của config.pageMenuOptions.
                    menuOptions = null
                    headerActions = null
                    invalidateOptionsMenu()
                    refreshCheckboxMenuStates()
                } else {
                    handleLoadError(config)
                    hideLoadProgress()
                    progressBarDialog.hideDialog()
                }
            }
        }
    }

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

    // [[group.action]] show=true: tự mở dialog ngay khi vào trang - CHỈ 1 LẦN trong phiên mở
    // trang hiện tại (autoShowTriggered), không lặp lại khi trang tự reload (vd reload-page
    // của action khác) - gọi ở đúng thời điểm nội dung trang đã render xong (onRendered/
    // finishProgressiveList) để fragment.onActionClick() có UI sẵn sàng hiện dialog lên trên.
    // Hoạt động với CẢ action còn nằm trong danh sách lẫn action đã chuyển ra icon toolbar
    // (menu=true) - onActionClick() không quan tâm action có đang gắn vào view nào hay không.
    private fun tryAutoShowActions() {
        stopFabSpinIfPending()
        if (autoShowTriggered) return
        val toShow = currentPageConfig?.autoShowActions?.filter { it.show }.orEmpty()
        if (toShow.isEmpty()) return
        autoShowTriggered = true
        val fragment = supportFragmentManager.findFragmentById(R.id.main_list) as? ActionListFragment ?: return
        toShow.forEach { fragment.onActionClick(it, Runnable {}, true) }
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
        // Trang sắp bị finish() ngay dưới - không cần invalidateOptionsMenu() nữa, chỉ cần dọn
        // biến static để không ảnh hưởng tới các trang khác mở sau này.
        pendingSpinIcon = null
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

        // menuOption.script đã được gán sẵn handler dùng chung của nhóm [[menu]]/[[fab]] (nếu
        // bản thân mục không tự khai báo script riêng) ngay lúc parse - page không còn handler
        // riêng nữa (xem PageConfigReader.menuGroupOptionsToml()).
        val script = menuOption.script
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

    // type = "spinner": tải danh sách lựa chọn (tĩnh option.options + động option.optionsSh)
    // và giá trị đang chọn hiện tại (option.spinnerGetState) - gộp 1 round-trip shell giống
    // ActionListFragment.pickerExecute() - rồi mở dropdown Spinner ngay tại vị trí vừa bấm
    // (toolbar nếu chọn từ menu 3 chấm, hoặc FAB nếu chọn từ fab - xem tham số anchor).
    private fun menuItemSpinner(menuOption: PageMenuOption, anchor: View? = null) {
        val config = currentPageConfig ?: return
        val resolvedAnchor = anchor ?: findViewById<View>(R.id.toolbar) ?: binding.root

        progressBarDialog.setCancelCallback { spinnerLoadJob?.cancel() }
        progressBarDialog.showDialog(getString(R.string.kr_param_options_load) + " ")

        spinnerLoadJob = lifecycleScope.launch(Dispatchers.IO) {
            val scripts = LinkedHashMap<String, String>()
            if (menuOption.spinnerGetState.isNotEmpty()) {
                scripts["state"] = menuOption.spinnerGetState
            }
            if (menuOption.optionsSh.isNotEmpty()) {
                scripts["options"] = menuOption.optionsSh
            }

            val shellResults = if (scripts.isNotEmpty()) {
                ScriptEnvironmen.executeMultipleResultRoot(this@ActionPage, scripts, config)
            } else {
                LinkedHashMap()
            }

            if (!isActive) return@launch

            val options = parseSpinnerOptions(menuOption, shellResults["options"])
            val currentValue = shellResults["state"]?.trim()

            withContext(Dispatchers.Main) {
                progressBarDialog.hideDialog()
                if (isFinishing || isDestroyed) return@withContext
                if (options.isNullOrEmpty()) {
                    Toast.makeText(this@ActionPage, getString(R.string.picker_not_item), Toast.LENGTH_SHORT).show()
                } else {
                    showSpinnerPopup(resolvedAnchor, menuOption, options, currentValue)
                }
            }
        }
    }

    // Giống ActionListFragment.parseOptionsResult(): mỗi dòng kết quả shell dạng "value|title"
    // hoặc chỉ "value"; nếu options-sh rỗng/lỗi thì dùng lại danh sách tĩnh option.options.
    private fun parseSpinnerOptions(menuOption: PageMenuOption, shellResult: String?): ArrayList<SelectItem>? {
        val result = shellResult ?: ""
        if (result == "error" || result == "null" || result.isEmpty()) {
            return menuOption.options
        }
        val options = ArrayList<SelectItem>()
        for (line in result.split("\n").filter { it.isNotEmpty() }) {
            if (line.contains("|")) {
                val split = line.split("|")
                options.add(SelectItem().apply {
                    value = split[0]
                    title = if (split.size > 1) split[1] else split[0]
                })
            } else {
                options.add(SelectItem().apply { title = line; value = line })
            }
        }
        return options
    }

    // Hiện dropdown chọn giá trị kiểu Android Spinner (dùng ListPopupWindow, style y hệt
    // ParamsSingleSelect.openSingleSelectPopup()) neo về góc phải toolbar - đè lên đúng vị trí
    // menu vừa bấm thay vì tràn full chiều rộng màn hình (xem applyPopupWidthAndPosition).
    // Chọn xong chạy script của menu item với tham số "state" = giá trị vừa chọn - giống hệt
    // các loại menu item khác.
    private fun showSpinnerPopup(
        anchor: View,
        menuOption: PageMenuOption,
        options: ArrayList<SelectItem>,
        currentValue: String?
    ) {
        val selectedIndex = options.indexOfFirst { it.value == currentValue }.let { if (it < 0) 0 else it }

        val adapter = ArrayAdapter(this, R.layout.kr_spinner_dropdown, R.id.text, options)
        val background = ContextCompat.getDrawable(this, R.drawable.kr_spinner_popup_bg)

        val popup = ListPopupWindow(this)
        popup.anchorView = anchor
        popup.setAdapter(adapter)
        popup.setBackgroundDrawable(background)
        popup.isModal = true
        popup.setOnItemClickListener { _, _, position, _ ->
            popup.dismiss()
            val selected = options.getOrNull(position) ?: return@setOnItemClickListener
            val value = selected.value ?: selected.title ?: ""
            menuItemExecute(
                menuOption,
                hashMapOf("state" to value, "menu_id" to menuOption.key)
            )
        }

        // Mục "spinner" trong menu 3 chấm/toolbar trước đây neo cả popup theo CHIỀU RỘNG
        // TOÀN BỘ toolbar (anchor.width.coerceAtLeast(400)) - vì toolbar gần như chiếm hết
        // chiều ngang màn hình nên trông giống 1 danh sách full màn hình thay vì 1 popup nhỏ
        // "đè lên" đúng chỗ menu vừa bấm. Giờ đo width theo NỘI DUNG thực tế (giống
        // ParamsSingleSelect.applyPopupWidthAndPosition) rồi neo popup về SÁT GÓC PHẢI của
        // anchor (nơi icon menu 3 chấm/fab thường nằm) bằng horizontalOffset âm, để popup hiện
        // ra gọn, đúng cảm giác "đè lên vị trí menu cũ" thay vì tràn ngang cả toolbar.
        // anchor == fab (menuItemSpinner được gọi từ addFab() với anchor = actionPageFab, xem
        // onMenuItemClick()) nghĩa là popup cũng sẽ tự lật lên TRÊN fab giống showFabChooser() -
        // cần chừa khoảng hở (fabPopupGap()) như nhau để không dính sát cạnh trên fab.
        val extraTopGapPx = if (anchor === binding.actionPageFab) fabPopupGap() else 0
        val itemViews = options.map { option ->
            layoutInflater.inflate(R.layout.kr_spinner_dropdown, anchor.parent as? android.view.ViewGroup, false).apply {
                findViewById<android.widget.TextView>(R.id.text).text = option.toString()
            }
        }
        applyPopupWidthAndPosition(popup, anchor, measureMaxItemWidth(itemViews), background, extraTopGapPx)

        popup.show()
        if (selectedIndex in options.indices) {
            popup.listView?.setSelection(selectedIndex)
        }
    }

    // Đo bề rộng lớn nhất trong 1 danh sách item đã inflate (nhưng CHƯA đo) - dùng chung cho cả
    // showSpinnerPopup() (item dạng text - kr_spinner_dropdown) LẪN showListPopup() (item dạng
    // List Item có icon - popup_menu_list_item, xem PopupMenuListAdapter) để tính bề rộng popup
    // theo đúng NỘI DUNG thực tế, xem applyPopupWidthAndPosition().
    private fun measureMaxItemWidth(itemViews: List<View>): Int {
        val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        var maxItemWidth = 0
        for (itemView in itemViews) {
            itemView.measure(unspecified, unspecified)
            if (itemView.measuredWidth > maxItemWidth) {
                maxItemWidth = itemView.measuredWidth
            }
        }
        return maxItemWidth
    }

    // Dùng chung cho cả showSpinnerPopup() (neo ở toolbar/menu 3 chấm) LẪN showListPopup()
    // (menu "⋮" kiểu List Item mới + popup chọn khi FAB nhiều item) - đặt bề rộng popup theo
    // NỘI DUNG thực tế (thay vì tràn hết chiều ngang màn hình) rồi neo sát mép phải anchor.
    // extraTopGapPx > 0 dùng riêng cho FAB (luôn nằm ở đáy màn hình - xem showFabChooser()):
    // đẩy thêm popup lên CAO HƠN vị trí ListPopupWindow tự tính (nó đã tự lật lên trên anchor
    // do không đủ chỗ hiện xuống dưới, nhưng mặc định lật sát liền cạnh trên của FAB, trông
    // dính chùm) để chừa 1 khoảng hở giữa popup và FAB.
    private fun applyPopupWidthAndPosition(
        popup: ListPopupWindow,
        anchor: View,
        maxItemWidth: Int,
        background: android.graphics.drawable.Drawable?,
        extraTopGapPx: Int = 0
    ) {
        val bgPadding = Rect()
        background?.getPadding(bgPadding)
        val screenWidth = resources.displayMetrics.widthPixels
        val contentWidth = maxItemWidth + bgPadding.left + bgPadding.right
        val minWidth = resources.displayMetrics.density * 200 // tối thiểu ~200dp cho dễ bấm
        val desiredWidth = contentWidth.coerceAtLeast(minWidth.toInt()).coerceAtMost(screenWidth)
        popup.width = desiredWidth

        // Neo sát góc phải anchor (toolbar/FAB) - đúng vị trí icon menu 3 chấm/FAB thường nằm -
        // thay vì để mặc định ListPopupWindow căn trái (tràn từ mép trái anchor).
        val anchorLocation = IntArray(2)
        anchor.getLocationOnScreen(anchorLocation)
        val rightAligned = anchor.width - desiredWidth
        val overflowLeft = anchorLocation[0] + rightAligned
        popup.horizontalOffset = if (overflowLeft < 0) -anchorLocation[0] else rightAligned

        if (extraTopGapPx > 0) {
            popup.verticalOffset = -extraTopGapPx
        }
    }

    // Popup kiểu List Item mới (RecyclerView/ListView có icon trái/phải) - dùng chung cho CẢ
    // popup menu "⋮" (showOverflowMenuPopup()) LẪN popup chọn khi FAB có nhiều item
    // (showFabChooser()) để đồng bộ giao diện, thay cho ArrayAdapter chỉ có chữ trước đây. Vẫn
    // giữ NGUYÊN VẸN cơ chế PopupWindow neo góc/tự lật lên trên khi không đủ chỗ như cũ
    // (ListPopupWindow + applyPopupWidthAndPosition) - chỉ đổi adapter/nội dung bên trong từng
    // dòng sang PopupMenuListAdapter (xem PopupMenuListAdapter.kt).
    private fun showListPopup(anchor: View, rows: List<PopupMenuRow>, extraTopGapPx: Int = 0) {
        if (rows.isEmpty()) return

        val adapter = PopupMenuListAdapter(this, rows)
        val background = ContextCompat.getDrawable(this, R.drawable.kr_spinner_popup_bg)

        val popup = ListPopupWindow(this)
        popup.anchorView = anchor
        popup.setAdapter(adapter)
        popup.setBackgroundDrawable(background)
        popup.isModal = true
        popup.setOnItemClickListener { _, _, position, _ ->
            popup.dismiss()
            rows.getOrNull(position)?.onClick?.invoke()
        }

        val parent = anchor.parent as? android.view.ViewGroup
        val itemViews = rows.indices.map { adapter.getView(it, null, parent) }
        applyPopupWidthAndPosition(popup, anchor, measureMaxItemWidth(itemViews), background, extraTopGapPx)

        popup.show()
    }

    // Danh sách chọn khi fab có nhiều item - popup nhỏ neo ngay tại nút fab, chọn xong chạy y
    // hệt như bấm thẳng 1 fab đơn (onMenuItemClick). Cùng kiểu List Item + cùng nền
    // kr_spinner_popup_bg với popup menu "⋮" (showOverflowMenuPopup()) để đồng bộ giao diện -
    // xem showListPopup(). FAB luôn nằm sát đáy màn hình nên popup luôn tự lật lên TRÊN fab;
    // nếu không có gì tách biệt, cạnh dưới popup dính liền cạnh trên fab trông rất chật, nên
    // đẩy thêm 1 khoảng hở nhỏ (fabPopupGap) giữa 2 bên.
    private fun showFabChooser(fabOptions: List<PageMenuOption>) {
        val anchor = binding.actionPageFab
        val rows = fabOptions.map { buildPopupRow(it, anchor) }
        showListPopup(anchor, rows, fabPopupGap())
    }

    // Khoảng hở (~8dp) giữa popup và FAB - dùng chung cho showFabChooser() (chọn giữa nhiều
    // fab item) LẪN showSpinnerPopup() khi anchor là fab (mục type="spinner" được gán làm fab) -
    // xem applyPopupWidthAndPosition().
    private fun fabPopupGap(): Int = (8 * resources.displayMetrics.density).toInt()

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
                    val mimeTypes = fileSelectedInterface.mimeType()
                        ?.split(",")
                        ?.map { it.trim() }
                        ?.filter { it.isNotEmpty() }
                        ?.toTypedArray()
                        ?: emptyArray()

                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        if (mimeTypes.size > 1) {
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
        OpenPageHelper(this).openPage(pageNode) {
            openedSubPage = false
        }
    }


    // Trả về cặp bitmap preview đang giữ để nó SỐNG SÓT qua đúng lần huỷ activity này (nếu
    // activity sắp được dựng lại ngay - do xoay màn hình HOẶC do gọi recreate() thủ công, ví
    // dụ spinFabThenRecreate()/menuOption.reloadPage/restartApp...). Android tự động trả lại
    // đúng giá trị này qua lastCustomNonConfigurationInstance ở onCreate() kế tiếp CỦA CÙNG
    // activity - xem đọc lại ở trên. Đây là cách duy nhất đáng tin cậy để không phải phụ
    // thuộc lại vào SwipeBackPreviewCache (cache dùng 1 lần, đã bị tiêu thụ ngay từ lần mở
    // trang đầu tiên nên KHÔNG còn gì để lấy lại ở các lần recreate() sau).
    @Suppress("DEPRECATION")
    override fun onRetainCustomNonConfigurationInstance(): Any? = swipePreview

    override fun onDestroy() {
        // isFinishing = true nghĩa là trang đóng HẲN (vd người dùng bấm back giữa lúc đang tải
        // dở sau khi bấm refresh) - KHÔNG phải bị huỷ để dựng lại bởi chính recreate() của
        // spinFabThenRecreate(). Phải dọn biến static ở đây, nếu không lần mở 1 trang KHÁC
        // (không liên quan) sau này sẽ vô tình thấy pendingSpinIcon còn sót và tự xoay fab.
        if (isFinishing) {
            pendingSpinIcon = null
        }
        checkboxRefreshJob?.cancel()
        lockCheckJob?.cancel()
        handler.removeCallbacksAndMessages(null)
        if (::swipeBackHelper.isInitialized) swipeBackHelper.release()
        // CHỈ recycle bitmap khi trang thực sự đóng hẳn (isFinishing() true - người dùng bấm
        // back/thoát trang, không còn activity nào dùng lại ảnh này nữa). Khi isFinishing() =
        // false, activity đang bị huỷ để DỰNG LẠI ngay (recreate()/xoay màn hình) -
        // onRetainCustomNonConfigurationInstance() ở trên đã giữ lại đúng object Preview
        // (bitmap CHƯA recycle) để lần onCreate() kế tiếp dùng lại - nếu vẫn recycle() ở đây
        // như trước, bitmap bị huỷ ngay trước khi instance mới kịp dùng lại, gây ra đúng lỗi
        // "vuốt lùi bị mất ảnh phía sau" sau mỗi lần trang tự làm mới.
        if (isFinishing && ::binding.isInitialized) {
            recycleImageViewBitmap(binding.swipeBackPreviewBlur)
            recycleImageViewBitmap(binding.swipeBackPreviewSharp)
        }
        setExcludeFromRecents()
        super.onDestroy()
    }

    private fun recycleImageViewBitmap(imageView: android.widget.ImageView) {
        val drawable = imageView.drawable
        if (drawable is android.graphics.drawable.BitmapDrawable) {
            imageView.setImageDrawable(null)
            drawable.bitmap?.takeIf { !it.isRecycled }?.recycle()
        }
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