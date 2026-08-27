package com.omarea.krscript.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.omarea.common.model.SelectItem
import com.omarea.common.ui.DialogFullScreen
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.DialogItemChooser
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.common.ui.ThemeMode
import com.omarea.krscript.BgTaskThread
import com.omarea.krscript.HiddenTaskThread
import com.omarea.krscript.downloader.DownloadTaskHelper
import com.tool.tree.R
import com.omarea.krscript.TryOpenActivity
import com.omarea.krscript.config.IconPathAnalysis
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.*
import com.omarea.krscript.shortcut.ActionShortcutManager
import com.tool.tree.ThemeModeState
import kotlinx.coroutines.*
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.RelativeLayout

class ActionListFragment : androidx.fragment.app.Fragment(), PageLayoutRender.OnItemClickListener {
    companion object {
        fun create(
                actionInfos: ArrayList<NodeInfoBase>?,
                krScriptActionHandler: KrScriptActionHandler? = null,
                autoRunTask: AutoRunTask? = null,
                themeMode: ThemeMode? = null,
                // Gọi khi renderInterface() dựng xong TOÀN BỘ view (kể cả decode ảnh icon/logo
                // - việc này chạy đồng bộ trên main thread trong PageLayoutRender, có thể mất
                // khá lâu với trang nhiều ảnh). Bên gọi (ActionPage) dùng để biết lúc nào tắt
                // thanh tiến trình hiện tạm trong lúc dựng - xem ActionPage.updateActionList.
                onRendered: (() -> Unit)? = null): ActionListFragment {
            val fragment = ActionListFragment()
            fragment.setListData(actionInfos, krScriptActionHandler, autoRunTask, themeMode, onRendered)
            return fragment
        }

        // Dùng cho trang có process = true: tạo fragment với danh sách RỖNG ban đầu, các
        // mục sẽ được thêm dần từng cái một qua appendProgressiveItem() ngay khi ActionPage
        // build xong từng mục (xem ActionPage.loadPageConfig), thay vì đợi build xong toàn
        // bộ trang mới hiện danh sách như create() ở trên.
        fun createProgressive(
                krScriptActionHandler: KrScriptActionHandler? = null,
                autoRunTask: AutoRunTask? = null,
                themeMode: ThemeMode? = null): ActionListFragment {
            val fragment = ActionListFragment()
            fragment.progressiveMode = true
            fragment.setListData(ArrayList(), krScriptActionHandler, autoRunTask, themeMode)
            return fragment
        }
    }

    private var actionInfos: ArrayList<NodeInfoBase>? = null
    private lateinit var progressBarDialog: ProgressBarDialog
    private var activeLoadJob: Job? = null
    private var krScriptActionHandler: KrScriptActionHandler? = null
    private var autoRunTask: AutoRunTask? = null
    private var themeMode: ThemeMode? = null
    private var pageLayoutRender: PageLayoutRender? = null
    private lateinit var rootGroup: ListItemGroup
    // Xem create()/updateData() - báo cho bên gọi biết renderInterface() đã dựng xong.
    private var onRendered: (() -> Unit)? = null

    // process = true: xem createProgressive()/appendProgressiveItem()/finishProgressiveList()
    private var progressiveMode = false
    // Mục đến TRƯỚC khi onViewCreated() dựng xong rootGroup thì xếp hàng ở đây, tránh mất
    // mục do race giữa fragment transaction và các lệnh handler.post() thêm mục từ ActionPage.
    private val pendingProgressiveItems = ArrayList<NodeInfoBase>()

    // Biến lưu mốc thời gian của cú click cuối cùng (mili-giây)
    private var lastClickTime: Long = 0

    /**
     * Hàm kiểm tra khoảng cách thời gian giữa 2 lần click liên tiếp.
     * @return true nếu khoảng cách lớn hơn 600ms (hợp lệ), false nếu quá nhanh (bị chặn).
     */
    private fun checkAndLockClick(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < 800) {
            return false
        }
        lastClickTime = currentTime
        return true
    }

    private fun setListData(
        actionInfos: ArrayList<NodeInfoBase>?,
        krScriptActionHandler: KrScriptActionHandler? = null,
        autoRunTask: AutoRunTask? = null,
        themeMode: ThemeMode? = null,
        onRendered: (() -> Unit)? = null) {
        this.actionInfos = actionInfos
        this.krScriptActionHandler = krScriptActionHandler
        this.autoRunTask = autoRunTask
        this.themeMode = themeMode
        this.onRendered = onRendered
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.kr_action_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.progressBarDialog = ProgressBarDialog(this.requireActivity())
        if (progressiveMode) {
            setupProgressiveRoot()
        } else {
            renderInterface()
        }
    }

    private fun renderInterface() {
        val context = context ?: run { onRendered?.invoke(); return }
        val currentActionInfos = actionInfos ?: run { onRendered?.invoke(); return }
        rootGroup = ListItemGroup(context, true, GroupNode(""))
        pageLayoutRender = PageLayoutRender(context, currentActionInfos, this, rootGroup)
        val layout = rootGroup.getView()
        val rootView = (this.view?.findViewById<ScrollView?>(R.id.kr_content))
        rootView?.removeAllViews()
        rootView?.addView(layout)
        triggerAction(autoRunTask)
        // Tới đây PageLayoutRender (kể cả decode ảnh icon/logo đồng bộ bên trong) đã
        // chạy xong ở trên rồi, nên gọi callback ngay là chính xác thời điểm dựng xong.
        onRendered?.invoke()
    }

    // Dựng rootGroup RỖNG (chưa có mục nào) cho chế độ process = true, rồi bơm ngay các
    // mục đã lỡ đến trước đó (pendingProgressiveItems) nếu có.
    private fun setupProgressiveRoot() {
        val context = context ?: return
        val currentActionInfos = actionInfos ?: ArrayList()
        rootGroup = ListItemGroup(context, true, GroupNode(""))
        pageLayoutRender = PageLayoutRender(context, currentActionInfos, this, rootGroup)
        val layout = rootGroup.getView()
        val rootView = (this.view?.findViewById<ScrollView?>(R.id.kr_content))
        rootView?.removeAllViews()
        rootView?.addView(layout)

        if (pendingProgressiveItems.isNotEmpty()) {
            val queued = ArrayList(pendingProgressiveItems)
            pendingProgressiveItems.clear()
            queued.forEach { pageLayoutRender?.appendNode(it) }
        }
    }

    // Thêm 1 mục mới vào danh sách ngay lập tức (không dựng lại các mục đã có). Gọi được từ
    // ActionPage bất kể rootGroup đã dựng xong hay chưa (nếu chưa, mục sẽ được xếp hàng).
    fun appendProgressiveItem(item: NodeInfoBase) {
        val render = pageLayoutRender
        if (render != null) {
            render.appendNode(item)
        } else {
            pendingProgressiveItems.add(item)
        }
    }

    // Gọi khi ActionPage đã build xong TOÀN BỘ trang (mọi mục đã appendProgressiveItem).
    // resolvePendingStates() ở PageConfigReader lúc này cũng đã chạy xong nên trạng thái
    // thật của switch/picker đã có sẵn trên model - làm mới hiển thị (không dựng lại view)
    // rồi mới chạy autoRunTask như luồng tải trang bình thường.
    fun finishProgressiveList() {
        if (::rootGroup.isInitialized) {
            rootGroup.triggerUpdate()
        }
        triggerAction(autoRunTask)
    }

    fun updateData(
        newItems: List<NodeInfoBase>,
        actionHandler: KrScriptActionHandler?,
        themeMode: ThemeMode?,
        onRendered: (() -> Unit)? = null
    ) {
        this.actionInfos = ArrayList(newItems)
        this.krScriptActionHandler = actionHandler
        this.themeMode = themeMode
        this.progressiveMode = false
        this.onRendered = onRendered
        if (isAdded && view != null) {
            renderInterface()
        } else {
            onRendered?.invoke()
        }
    }

    private fun triggerAction(autoRunTask: AutoRunTask?) {
        autoRunTask?.run {
            if (!key.isNullOrEmpty()) {
                onCompleted(rootGroup.triggerActionByKey(key!!))
            }
        }
    }

    // Kiểm tra tương thích SDK - đồng bộ, không cần chạy shell nên không cần đợi/hiện dialog
    // gì cả. T��ch riêng khỏi nodeUnlockedAsync() để onPageClick() có thể gọi thẳng cho trường
    // hợp mở trang con (không qua nodeUnlockedAsync nữa - xem onPageClick()).
    private fun checkSdkCompatibility(clickableNode: ClickableNode): Boolean {
        val currentSDK = Build.VERSION.SDK_INT
        if (clickableNode.targetSdkVersion > 0 && currentSDK != clickableNode.targetSdkVersion) {
            DialogHelper.helpInfo(requireContext(), getString(R.string.kr_sdk_discrepancy), getString(R.string.kr_sdk_discrepancy_message).format(clickableNode.targetSdkVersion))
            return false
        } else if (currentSDK > clickableNode.maxSdkVersion) {
            DialogHelper.helpInfo(requireContext(), getString(R.string.kr_sdk_overtop), getString(R.string.kr_sdk_message).format(clickableNode.minSdkVersion, clickableNode.maxSdkVersion))
            return false
        } else if (currentSDK < clickableNode.minSdkVersion) {
            DialogHelper.helpInfo(requireContext(), getString(R.string.kr_sdk_too_low), getString(R.string.kr_sdk_message).format(clickableNode.minSdkVersion, clickableNode.maxSdkVersion))
            return false
        }
        return true
    }

    // Kiểm tra khoá TRƯỚC khi cho thực hiện 1 mục (dùng cho switch/action/picker/editor, và
    // cho page loại link/activity - page loại mở ActionPage con thì KHÔNG còn qua đây nữa, xem
    // onPageClick()). Nếu có lockShell (lệnh shell kiểm tra khoá), chạy BẤT ĐỒNG BỘ trên luồng
    // IO, đồng thời hiện thanh tiến trình ngay trên trang (page_load_progress) trong lúc chờ
    // kết quả. Kiểm tra xong (dù khoá hay mở) mới ẩn thanh và gọi onUnlocked() nếu thật sự đã
    // mở khoá.
    private fun nodeUnlockedAsync(clickableNode: ClickableNode, onUnlocked: () -> Unit) {
        if (!checkSdkCompatibility(clickableNode)) return

        if (clickableNode.lockShell.isEmpty()) {
            // Không cần chạy shell - kiểm tra local tức thời, không có gì phải đợi/hiện dialog.
            if (clickableNode.locked) {
                Toast.makeText(context, getString(R.string.kr_lock_message), Toast.LENGTH_LONG).show()
            } else {
                onUnlocked()
            }
            return
        }

        val progressBar = activity?.findViewById<android.widget.ProgressBar>(R.id.page_load_progress)
        progressBar?.apply {
            isIndeterminate = true
            visibility = View.VISIBLE
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val message = ScriptEnvironmen.executeResultRoot(context, clickableNode.lockShell, clickableNode)
            withContext(Dispatchers.Main) {
                progressBar?.visibility = View.GONE
                if (!isAdded) return@withContext
                val unlocked = message == "unlock" || message == "unlocked" || message == "false" || message == "0"
                if (!unlocked) {
                    Toast.makeText(context, if (message.isNotEmpty()) message else getString(R.string.kr_lock_message), Toast.LENGTH_LONG).show()
                } else {
                    onUnlocked()
                }
            }
        }
    }

    override fun onSwitchClick(item: SwitchNode, onCompleted: Runnable) {
        if (!checkAndLockClick()) return
        nodeUnlockedAsync(item) {
            val toValue = !item.checked
            if (item.confirm) {
                DialogHelper.warning(requireActivity(), item.title, item.desc, { switchExecute(item, toValue, onCompleted) })
            } else if (item.warning.isNotEmpty()) {
                DialogHelper.warning(requireActivity(), item.title, item.warning, { switchExecute(item, toValue, onCompleted) })
            } else {
                switchExecute(item, toValue, onCompleted)
            }
        }
    }

    private fun switchExecute(switchNode: SwitchNode, toValue: Boolean, onExit: Runnable) {
        val script = switchNode.setState ?: return
        actionExecute(switchNode, script, onExit, object : java.util.HashMap<String, String>() {
            init { put("state", if (toValue) "1" else "0") }
        })
    }

    override fun onPageClick(item: PageNode, onCompleted: Runnable) {
        if (!checkAndLockClick()) return

        // link/activity: mở thẳng ra ngoài (trình duyệt/activity khác), không có "trang" riêng
        // nào để tự kiểm tra khoá SAU khi mở - vẫn phải kiểm tra khoá ở đây TRƯỚC khi mở như cũ.
        if (context != null && item.link.isNotEmpty()) {
            nodeUnlockedAsync(item) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, item.link.toUri())
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context?.startActivity(intent)
                } catch (ex: Exception) {
                    Toast.makeText(context, context?.getString(R.string.kr_slice_activity_fail), Toast.LENGTH_SHORT).show()
                }
            }
        } else if (context != null && item.activity.isNotEmpty()) {
            nodeUnlockedAsync(item) {
                TryOpenActivity(requireContext(), item.activity).tryOpen()
            }
        } else {
            // Trang con (ActionPage): vào trang NGAY, không đợi kiểm tra khoá ở đây nữa - nếu
            // trang có lockShell/locked, CHÍNH trang đó sẽ tự hiện dialog loading rồi kiểm tra
            // sau khi đã mở, và báo lỗi bằng dialog (thay vì toast) nếu khoá - xem
            // ActionPage.checkPageLockThenLoad(). Vẫn giữ kiểm tra SDK ở đây vì nó đồng bộ,
            // không cần đợi gì cả.
            if (!checkSdkCompatibility(item)) return
            krScriptActionHandler?.onSubPageClick(item)
        }
    }

    override fun onItemLongClick(clickableNode: ClickableNode) {
        if (!checkAndLockClick()) return
        if (clickableNode.key.isEmpty()) {
            DialogHelper.alert(this.requireActivity(), getString(R.string.kr_shortcut_create_fail), getString(R.string.kr_ushortcut_nsupported))
        } else {
            krScriptActionHandler?.addToFavorites(clickableNode, object : KrScriptActionHandler.AddToFavoritesHandler {
                override fun onAddToFavorites(clickableNode: ClickableNode, intent: Intent?) {
                    if (intent != null) {
                        DialogHelper.confirm(activity!!, getString(R.string.kr_shortcut_create), String.format(getString(R.string.kr_shortcut_create_desc), clickableNode.title), {
                            val result = ActionShortcutManager(context!!).addShortcut(intent, IconPathAnalysis().loadLogo(context!!, clickableNode), clickableNode)
                            if (!result) Toast.makeText(context, R.string.kr_shortcut_create_fail, Toast.LENGTH_SHORT).show()
                            else Toast.makeText(context, getString(R.string.kr_shortcut_create_success), Toast.LENGTH_SHORT).show()
                        })
                    }
                }
            })
        }
    }

    override fun onEditorClick(item: EditorNode, onCompleted: Runnable) {
        if (!checkAndLockClick()) return
        nodeUnlockedAsync(item) {
            val context = context ?: return@nodeUnlockedAsync
            if (item.file.isEmpty()) {
                Toast.makeText(context, getString(R.string.editor_file_missing), Toast.LENGTH_SHORT).show()
                return@nodeUnlockedAsync
            }
            com.tool.tree.TextEditorActivity.start(
                context, item.file, item.title, item.desc, item.wrap, item.pageConfigDir, item.placeholder,
                item.readonly, item.needInput, item.value, item.valueSh
            )
            onCompleted.run()
        }
    }

    override fun onPickerClick(item: PickerNode, onCompleted: Runnable) {
        if (!checkAndLockClick()) return
        nodeUnlockedAsync(item) {
            if (item.confirm) {
                DialogHelper.warning(requireActivity(), item.title, item.desc, { pickerExecute(item, onCompleted) })
            } else if (item.warning.isNotEmpty()) {
                DialogHelper.warning(requireActivity(), item.title, item.warning, { pickerExecute(item, onCompleted) })
            } else {
                pickerExecute(item, onCompleted)
            }
        }
    }

    private fun pickerExecute(item: PickerNode, onCompleted: Runnable) {
        val paramInfo = ActionParamInfo().apply {
            options = item.options
            optionsSh = item.optionsSh
            separator = item.separator
        }

        progressBarDialog.setCancelCallback {
            activeLoadJob?.cancel()
        }
        progressBarDialog.showDialog(getString(R.string.kr_param_options_load) + " ");

        activeLoadJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            // ========== TỐI ƯU: GỘP getState + optionsSh thành 1 lần gọi shell ==========
            // Trước đây 2 script này được gọi riêng (2 round-trip qua shell root dùng
            // chung, có khóa -> luôn chạy tuần tự). Gộp lại còn 1 round-trip duy nhất.
            val scripts = LinkedHashMap<String, String>()
            if (!item.getState.isNullOrEmpty()) {
                scripts["state"] = item.getState!!
            }
            if (paramInfo.optionsSh.isNotEmpty()) {
                scripts["options"] = paramInfo.optionsSh
            }

            val shellResults = if (scripts.isNotEmpty()) {
                ScriptEnvironmen.executeMultipleResultRoot(requireContext(), scripts, item)
            } else {
                LinkedHashMap()
            }

            shellResults["state"]?.let { paramInfo.valueFromShell = it }

            val options = parseOptionsResult(paramInfo, shellResults["options"])
            val optionsSorted = if (options != null) {
                ActionParamsLayoutRender.setParamOptionsSelectedStatus(paramInfo, options)
                options
            } else null

            withContext(Dispatchers.Main) {
                progressBarDialog.hideDialog()
                if (optionsSorted != null) {
                    val darkMode = ThemeModeState.isDarkMode()
                    DialogItemChooser(darkMode, optionsSorted, item.multiple, object : DialogItemChooser.Callback {
                        override fun onConfirm(selected: List<SelectItem>, status: BooleanArray) {
                            val value = if (item.multiple) {
                                selected.joinToString(item.separator ?: "") { "" + it.value }
                            } else {
                                if (selected.isNotEmpty()) "" + selected[0].value else ""
                            }
                            if (value.isNotEmpty() || !item.multiple) {
                                pickerExecute(item, value, onCompleted)
                            } else {
                                Toast.makeText(context, getString(R.string.picker_select_none), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }).show(requireActivity().supportFragmentManager, "picker-item-chooser")
                } else {
                    Toast.makeText(context, getString(R.string.picker_not_item), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun pickerExecute(pickerNode: PickerNode, toValue: String, onExit: Runnable) {
        val script = pickerNode.setState ?: return
        actionExecute(pickerNode, script, onExit, hashMapOf("state" to toValue))
    }

    // [[download]]: tải file (tiến trình hiện ngay trong item qua ListItemDownload), sau đó tự
    // chạy script (nếu có) với $state = đường dẫn file - xem DownloadTaskHelper. Bấm lại vào
    // item TRONG LÚC ĐANG TẢI sẽ huỷ tải (xem ListItemDownload.cancelIfDownloading()); bấm khi
    // đã chuyển sang giai đoạn chạy script thì bị bỏ qua (không huỷ được script đang chạy).
    override fun onDownloadClick(item: DownloadNode, listItemView: ListItemDownload, onCompleted: Runnable) {
        if (listItemView.isBusy) {
            listItemView.cancelIfDownloading()
            return
        }
        if (!checkAndLockClick()) return
        nodeUnlockedAsync(item) {
            if (item.confirm) {
                DialogHelper.warning(requireActivity(), item.title, item.desc, { downloadExecute(item, listItemView, onCompleted) })
            } else if (item.warning.isNotEmpty()) {
                DialogHelper.warning(requireActivity(), item.title, item.warning, { downloadExecute(item, listItemView, onCompleted) })
            } else {
                downloadExecute(item, listItemView, onCompleted)
            }
        }
    }

    private fun downloadExecute(item: DownloadNode, listItemView: ListItemDownload, onExit: Runnable) {
        DownloadTaskHelper.start(requireContext(), viewLifecycleOwner.lifecycleScope, item, listItemView) {
            krScriptActionHandler?.onActionCompleted(item)
            onExit.run()
        }
    }

    // isAutoShow = true: dialog được tự động mở khi vừa vào trang ([[group.action]] show=true,
    // ActionPage.tryAutoShowActions). Trong trường hợp này:
    //  - Không cho phép ấn ra ngoài dialog để đóng (cancelable = false).
    //  - Ấn "Hủy" sẽ thoát khỏi trang luôn thay vì chỉ đóng dialog.
    // Khi action được kích hoạt theo cách thông thường (bấm trong danh sách, hoặc bấm icon đã
    // chuyển ra toolbar/menu) thì isAutoShow = false và giữ nguyên hành vi mặc định.
    override fun onActionClick(item: ActionNode, onCompleted: Runnable, isAutoShow: Boolean) {
        if (!checkAndLockClick()) return
        nodeUnlockedAsync(item) {
            val onCancel = if (isAutoShow) Runnable { requireActivity().finish() } else null
            val cancelable = !isAutoShow
            if (item.confirm) {
                DialogHelper.warning(requireActivity(), item.title, item.desc, { actionExecute(item, onCompleted, isAutoShow) }, onCancel, cancelable)
            } else if (item.warning.isNotEmpty() && (item.params == null || item.params?.isEmpty() == true)) {
                DialogHelper.warning(requireActivity(), item.title, item.warning, { actionExecute(item, onCompleted, isAutoShow) }, onCancel, cancelable)
            } else {
                actionExecute(item, onCompleted, isAutoShow)
            }
        }
    }

    private fun actionExecute(action: ActionNode, onExit: Runnable, isAutoShow: Boolean = false) {
        val script = action.setState ?: return

        if (action.params != null && action.params!!.isNotEmpty()) {
            val actionParamInfos = action.params!!
            val layoutInflater = LayoutInflater.from(requireContext())
            val linearLayout = layoutInflater.inflate(R.layout.kr_params_list, null) as LinearLayout

            progressBarDialog.setCancelCallback {
                activeLoadJob?.cancel()
            }
            progressBarDialog.showDialog(getString(R.string.onloading))

            activeLoadJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                // ========== TỐI ƯU: GỘP TOÀN BỘ valueShell + optionsSh CỦA MỌI PARAM ==========
                // THÀNH 1 LẦN GỌI SHELL DUY NHẤT.
                // Trước đây: mỗi param tốn tới 2 round-trip riêng (valueShell rồi optionsSh),
                // chạy TUẦN TỰ cho từng param -> N param có valueShell+optionsSh sẽ tốn 2N
                // round-trip qua shell root (1 tiến trình dùng chung, có khóa, nên các lệnh
                // luôn phải xếp hàng dù có gọi song song bằng coroutine). Ngoài ra còn 2N lần
                // cập nhật progress dialog (chuyển ngữ cảnh Main<->IO liên tục).
                //
                // Giờ: gộp tất cả script thành 1 khối lệnh, gọi doCmdSync() ĐÚNG 1 LẦN, rồi
                // tách kết quả theo tag để gán lại cho từng param.
                withContext(Dispatchers.Main) {
                    progressBarDialog.showDialog(getString(R.string.kr_param_options_load) + " ");
                }

                val scripts = LinkedHashMap<String, String>()
                for (param in actionParamInfos) {
                    val name = param.name ?: continue
                    if (!param.valueShell.isNullOrEmpty()) {
                        scripts["value:$name"] = param.valueShell!!
                    }
                    if (param.optionsSh.isNotEmpty()) {
                        scripts["options:$name"] = param.optionsSh
                    }
                    if (!param.titleSh.isNullOrEmpty()) {
                        scripts["title:$name"] = param.titleSh!!
                    }
                    if (!param.labelSh.isNullOrEmpty()) {
                        scripts["label:$name"] = param.labelSh!!
                    }
                    if (!param.descSh.isNullOrEmpty()) {
                        scripts["desc:$name"] = param.descSh!!
                    }
                    if (!param.descOnSh.isNullOrEmpty()) {
                        scripts["desc-on:$name"] = param.descOnSh!!
                    }
                    if (!param.placeholderSh.isNullOrEmpty()) {
                        scripts["placeholder:$name"] = param.placeholderSh!!
                    }
                    if (!param.readonlySh.isNullOrEmpty()) {
                        scripts["readonly:$name"] = param.readonlySh!!
                    }
                }

                val shellResults = if (scripts.isNotEmpty()) {
                    ScriptEnvironmen.executeMultipleResultRoot(requireContext(), scripts, action)
                } else {
                    LinkedHashMap()
                }

                for (param in actionParamInfos) {
                    val name = param.name ?: continue
                    shellResults["value:$name"]?.let { param.valueFromShell = it }
                    param.optionsFromShell = parseOptionsResult(param, shellResults["options:$name"])
                    shellResults["title:$name"]?.let { param.title = it }
                    shellResults["label:$name"]?.let { param.label = it }
                    shellResults["desc:$name"]?.let { param.desc = it }
                    shellResults["desc-on:$name"]?.let { param.descOn = it }
                    shellResults["placeholder:$name"]?.let { param.placeholder = it }
                    shellResults["readonly:$name"]?.let { param.readonly = it.trim() == "1" }
                }

                withContext(Dispatchers.Main) {
                    progressBarDialog.showDialog(getString(R.string.kr_params_render))
                    val render = ActionParamsLayoutRender(linearLayout, requireActivity())
                    render.renderList(actionParamInfos, object : ParamsFileChooserRender.FileChooserInterface {
                        override fun openFileChooser(callback: ParamsFileChooserRender.FileSelectedInterface): Boolean {
                            return krScriptActionHandler?.openFileChooser(callback) ?: false
                        }
                    })
                    progressBarDialog.hideDialog()

                    val customRunner = krScriptActionHandler?.openParamsPage(action, linearLayout) {
                        try {
                            actionExecute(action, script, onExit, render.readParamsValue(actionParamInfos))
                        } catch (ex: Exception) {
                            Toast.makeText(requireContext(), "" + ex.message, Toast.LENGTH_LONG).show()
                        }
                    }

                    if (customRunner != true) {
                        val isLongList = actionParamInfos.size > 4
                        val dialogView = LayoutInflater.from(context).inflate(if (isLongList) R.layout.kr_dialog_params else R.layout.kr_dialog_params_small, null)
                        val center = dialogView.findViewById<ViewGroup>(R.id.kr_params_center)

                        // ensure linearLayout not attached elsewhere and add with suitable LayoutParams
                        (linearLayout.parent as? ViewGroup)?.removeView(linearLayout)
                        center.removeAllViews()

                        val lp = when (center) {
                            is FrameLayout -> FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                Gravity.CENTER
                            )
                            is LinearLayout -> LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply { gravity = Gravity.CENTER }
                            is RelativeLayout -> RelativeLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply { addRule(RelativeLayout.CENTER_IN_PARENT) }
                            else -> ViewGroup.MarginLayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                        }

                        center.addView(linearLayout, lp)

                        // isAutoShow = true (dialog tự mở khi vào trang): không cho ấn ra ngoài
                        // để đóng, và ấn "Hủy" phải thoát khỏi trang thay vì chỉ đóng dialog.
                        val cancelable = !isAutoShow

                        val darkMode = themeMode?.isDarkMode ?: false
                        val dialog = if (isLongList) {
                            AlertDialog.Builder(requireContext(), if (darkMode) R.style.kr_full_screen_dialog_dark else R.style.kr_full_screen_dialog_light)
                                .setView(dialogView).setCancelable(cancelable).create().apply {
                                    setCanceledOnTouchOutside(cancelable)
                                    show()
                                    window?.let { DialogHelper.applyEdgeToEdge(it, darkMode, dialogView) }
                                }
                        } else {
                            // Nhánh <=4 mục dùng chung DialogHelper.customDialog() - nền blur
                            // (+ vuốt lùi khi cancelable) đã được xử lý sẵn bên trong đó (xem
                            // DialogHelper.customDialog()), nhưng vẫn apply edge-to-edge so dialog
                            // window status/navigation drawing matches full-screen branch.
                            val dlgWrap = DialogHelper.customDialog(requireActivity(), dialogView, cancelable)
                            val dlg = dlgWrap.dialog
                            dlg.window?.let { DialogHelper.applyEdgeToEdge(it, darkMode, dialogView) }
                            dlg
                        }
                        if (isLongList) {
                            if (cancelable) {
                                // Vuốt lùi để đóng - dùng chung 1 hàm với DialogFullScreen (xem
                                // DialogFullScreen.bindSwipeToDismiss()) thay vì lặp lại logic
                                // bọc blur + bind DialogSwipeBackHelper + predictive-back ở đây.
                                val binding = DialogFullScreen.bindSwipeToDismiss(requireActivity(), dialog, dialogView) { dialog.dismiss() }
                                dialog.setOnDismissListener { binding?.release(dialog) }
                            } else {
                                // Không cancelable -> không vuốt lùi, giữ nguyên nền blur tĩnh cố định.
                                dialog.window?.let { DialogHelper.setWindowBlurBg(it, requireActivity()) }
                            }
                        }


                        dialogView.findViewById<TextView>(R.id.title).text = action.title
                        dialogView.findViewById<TextView>(R.id.desc).apply { if (action.desc.isEmpty()) visibility = View.GONE else text = action.desc }
                        dialogView.findViewById<TextView>(R.id.warn).apply { if (action.warning.isEmpty()) visibility = View.GONE else text = action.warning }

                        // Cho phép action.rows (text/photo/icon/toggle, giống rows ở item trong list)
                        // hiện thêm ngay trên form nhập tham số của dialog params.
                        RowsRenderHelper.bind(
                            requireContext(),
                            dialogView.findViewById<TextView>(R.id.kr_rows),
                            dialogView.findViewById<android.widget.ImageView>(R.id.kr_rows_photo),
                            action.paramsRows,
                            action
                        )

                        dialogView.findViewById<View>(R.id.btn_cancel).setOnClickListener {
                            dialog?.dismiss()
                            if (isAutoShow) {
                                requireActivity().finish()
                            }
                        }
                        dialogView.findViewById<View>(R.id.btn_confirm).setOnClickListener {
                            try {
                                actionExecute(action, script, onExit, render.readParamsValue(actionParamInfos))
                                dialog?.dismiss()
                            } catch (ex: Exception) {
                                Toast.makeText(requireContext(), "" + ex.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
            return
        }
        actionExecute(action, script, onExit, null)
    }

    // ========== TỐI ƯU: TÁCH RIÊNG PHẦN PARSE, KHÔNG TỰ GỌI SHELL NỮA ==========
    // Trước đây hàm này (getParamOptions) tự gọi executeScriptGetResult() bên trong, nghĩa
    // là mỗi param một round-trip shell riêng. Giờ shellResult đã được lấy từ TRƯỚC (gộp
    // chung 1 lần gọi cho mọi param qua ScriptEnvironmen.executeMultipleResultRoot), hàm
    // này chỉ còn nhiệm vụ parse chuỗi kết quả thành danh sách SelectItem như cũ.
    private fun parseOptionsResult(actionParamInfo: ActionParamInfo, shellResult: String?): ArrayList<SelectItem>? {
        val options = ArrayList<SelectItem>()
        val result = shellResult ?: ""

        if (!(result == "error" || result == "null" || result.isEmpty())) {
            for (item in result.split("\n").filter { it.isNotEmpty() }) {
                if (item.contains("|")) {
                    val itemSplit = item.split("|")
                    options.add(SelectItem().apply {
                        value = itemSplit[0]
                        title = if (itemSplit.size > 1) itemSplit[1] else itemSplit[0]
                    })
                } else {
                    options.add(SelectItem().apply { title = item; value = item })
                }
            }
        } else if (actionParamInfo.options != null) {
            options.addAll(actionParamInfo.options!!)
        } else return null

        return options
    }

    var hiddenTaskRunning = false
    private fun actionExecute(nodeInfo: RunnableNode, script: String, onExit: Runnable, params: HashMap<String, String>?) {
        val context = requireContext()
        val onDismiss = Runnable { krScriptActionHandler?.onActionCompleted(nodeInfo) }

        when (nodeInfo.shell) {
            RunnableNode.shellModeBgTask -> {
                BgTaskThread.startTask(context, script, params, nodeInfo, onExit, onDismiss)
            }
            RunnableNode.shellModeHidden -> {
                if (hiddenTaskRunning) {
                    Toast.makeText(context, getString(R.string.kr_hidden_task_running), Toast.LENGTH_SHORT).show()
                } else {
                    hiddenTaskRunning = true
                    val hiddenDismiss = Runnable {
                        hiddenTaskRunning = false
                        onDismiss.run()
                    }
                    HiddenTaskThread.startTask(context, script, params, nodeInfo, onExit, hiddenDismiss)
                }
            }
            else -> {
                val darkMode = themeMode?.isDarkMode ?: false
                val dialog = DialogLogFragment.create(nodeInfo, onExit, onDismiss, script, params, darkMode)
                dialog.isCancelable = false
                dialog.show(parentFragmentManager, "")
            }
        }
    }
}
