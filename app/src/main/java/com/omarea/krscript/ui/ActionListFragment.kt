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
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.DialogItemChooser
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.common.ui.ThemeMode
import com.omarea.krscript.BgTaskThread
import com.omarea.krscript.HiddenTaskThread
import com.tool.tree.R
import com.omarea.krscript.TryOpenActivity
import com.omarea.krscript.config.IconPathAnalysis
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.*
import com.omarea.krscript.shortcut.ActionShortcutManager
import com.tool.tree.ThemeModeState
import kotlinx.coroutines.*

class ActionListFragment : androidx.fragment.app.Fragment(), PageLayoutRender.OnItemClickListener {
    companion object {
        fun create(
                actionInfos: ArrayList<NodeInfoBase>?,
                krScriptActionHandler: KrScriptActionHandler? = null,
                autoRunTask: AutoRunTask? = null,
                themeMode: ThemeMode? = null): ActionListFragment {
            val fragment = ActionListFragment()
            fragment.setListData(actionInfos, krScriptActionHandler, autoRunTask, themeMode)
            return fragment
        }
    }

    private var actionInfos: ArrayList<NodeInfoBase>? = null
    private lateinit var progressBarDialog: ProgressBarDialog
    private var krScriptActionHandler: KrScriptActionHandler? = null
    private var autoRunTask: AutoRunTask? = null
    private var themeMode: ThemeMode? = null
    private var pageLayoutRender: PageLayoutRender? = null
    private lateinit var rootGroup: ListItemGroup

    private fun setListData(
        actionInfos: ArrayList<NodeInfoBase>?,
        krScriptActionHandler: KrScriptActionHandler? = null,
        autoRunTask: AutoRunTask? = null,
        themeMode: ThemeMode? = null) {
        this.actionInfos = actionInfos
        this.krScriptActionHandler = krScriptActionHandler
        this.autoRunTask = autoRunTask
        this.themeMode = themeMode
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.kr_action_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.progressBarDialog = ProgressBarDialog(this.requireActivity())
        renderInterface()
    }

    private fun renderInterface() {
        val context = context ?: return
        val currentActionInfos = actionInfos ?: return
        rootGroup = ListItemGroup(context, true, GroupNode(""))
        pageLayoutRender = PageLayoutRender(context, currentActionInfos, this, rootGroup)
        val layout = rootGroup.getView()
        val rootView = (this.view?.findViewById<ScrollView?>(R.id.kr_content))
        rootView?.removeAllViews()
        rootView?.addView(layout)
        triggerAction(autoRunTask)
    }
    
    fun updateData(
        newItems: List<NodeInfoBase>,
        actionHandler: KrScriptActionHandler?,
        themeMode: ThemeMode?
    ) {
        this.actionInfos = ArrayList(newItems)
        this.krScriptActionHandler = actionHandler
        this.themeMode = themeMode
        if (isAdded && view != null) {
            renderInterface()
        }
    }

    private fun triggerAction(autoRunTask: AutoRunTask?) {
        autoRunTask?.run {
            if (!key.isNullOrEmpty()) {
                onCompleted(rootGroup.triggerActionByKey(key!!))
            }
        }
    }

    private fun nodeUnlocked(clickableNode: ClickableNode): Boolean {
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

        var message = ""
        val unlocked = (if (clickableNode.lockShell.isNotEmpty()) {
            message = ScriptEnvironmen.executeResultRoot(context, clickableNode.lockShell, clickableNode)
            message == "unlock" || message == "unlocked" || message == "false" || message == "0"
        } else {
            !clickableNode.locked
        })
        if (!unlocked) {
            Toast.makeText(context, if (message.isNotEmpty()) message else getString(R.string.kr_lock_message), Toast.LENGTH_LONG).show()
        }
        return unlocked
    }

    override fun onSwitchClick(item: SwitchNode, onCompleted: Runnable) {
        if (nodeUnlocked(item)) {
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
        if (nodeUnlocked(item)) {
            if (context != null && item.link.isNotEmpty()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, item.link.toUri())
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context?.startActivity(intent)
                } catch (ex: Exception) {
                    Toast.makeText(context, context?.getString(R.string.kr_slice_activity_fail), Toast.LENGTH_SHORT).show()
                }
            } else if (context != null && item.activity.isNotEmpty()) {
                TryOpenActivity(requireContext(), item.activity).tryOpen()
            } else {
                krScriptActionHandler?.onSubPageClick(item)
            }
        }
    }

    override fun onItemLongClick(clickableNode: ClickableNode) {
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

    override fun onPickerClick(item: PickerNode, onCompleted: Runnable) {
        if (nodeUnlocked(item)) {
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

        progressBarDialog.showDialog(getString(R.string.kr_param_options_load))

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            if (item.getState != null) {
                paramInfo.valueFromShell = executeScriptGetResult(item.getState!!, item)
            }

            val options = getParamOptions(paramInfo, item)
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

    override fun onActionClick(item: ActionNode, onCompleted: Runnable) {
        if (nodeUnlocked(item)) {
            if (item.confirm) {
                DialogHelper.warning(requireActivity(), item.title, item.desc, { actionExecute(item, onCompleted) })
            } else if (item.warning.isNotEmpty() && (item.params == null || item.params?.isEmpty() == true)) {
                DialogHelper.warning(requireActivity(), item.title, item.warning, { actionExecute(item, onCompleted) })
            } else {
                actionExecute(item, onCompleted)
            }
        }
    }

    private fun actionExecute(action: ActionNode, onExit: Runnable) {
        val script = action.setState ?: return

        if (action.params != null && action.params!!.isNotEmpty()) {
            val actionParamInfos = action.params!!
            val layoutInflater = LayoutInflater.from(requireContext())
            val linearLayout = layoutInflater.inflate(R.layout.kr_params_list, null) as LinearLayout

            progressBarDialog.showDialog(getString(R.string.onloading))

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                for (param in actionParamInfos) {
                    withContext(Dispatchers.Main) {
                        progressBarDialog.showDialog(getString(R.string.kr_param_load) + (param.label ?: param.name))
                    }
                    if (param.valueShell != null) {
                        param.valueFromShell = executeScriptGetResult(param.valueShell!!, action)
                    }
                    withContext(Dispatchers.Main) {
                        progressBarDialog.showDialog(getString(R.string.kr_param_options_load) + (param.label ?: param.name))
                    }
                    param.optionsFromShell = getParamOptions(param, action)
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
                        center.removeAllViews()
                        center.addView(linearLayout)
                        
                        val darkMode = themeMode?.isDarkMode ?: false
                        val dialog = if (isLongList) {
                            AlertDialog.Builder(requireContext(), if (darkMode) R.style.kr_full_screen_dialog_dark else R.style.kr_full_screen_dialog_light)
                                .setView(dialogView).create().apply {
                                    show()
                                    window?.let { DialogHelper.setWindowBlurBg(it, requireActivity()) }
                                }
                        } else {
                            DialogHelper.customDialog(requireActivity(), dialogView).dialog
                        }

                        dialogView.findViewById<TextView>(R.id.title).text = action.title
                        dialogView.findViewById<TextView>(R.id.desc).apply { if (action.desc.isEmpty()) visibility = View.GONE else text = action.desc }
                        dialogView.findViewById<TextView>(R.id.warn).apply { if (action.warning.isEmpty()) visibility = View.GONE else text = action.warning }

                        dialogView.findViewById<View>(R.id.btn_cancel).setOnClickListener { dialog?.dismiss() }
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

    private fun getParamOptions(actionParamInfo: ActionParamInfo, nodeInfoBase: NodeInfoBase): ArrayList<SelectItem>? {
        val options = ArrayList<SelectItem>()
        var shellResult = ""
        if (actionParamInfo.optionsSh.isNotEmpty()) {
            shellResult = executeScriptGetResult(actionParamInfo.optionsSh, nodeInfoBase)
        }

        if (!(shellResult == "error" || shellResult == "null" || shellResult.isEmpty())) {
            for (item in shellResult.split("\n").filter { it.isNotEmpty() }) {
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

    private fun executeScriptGetResult(shellScript: String, nodeInfoBase: NodeInfoBase): String {
        return ScriptEnvironmen.executeResultRoot(this.requireContext(), shellScript, nodeInfoBase)
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
