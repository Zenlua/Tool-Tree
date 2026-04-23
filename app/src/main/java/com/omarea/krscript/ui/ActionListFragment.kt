package com.omarea.krscript.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.omarea.common.model.SelectItem
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.DialogItemChooser
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.common.ui.ThemeMode
import com.omarea.krscript.BgTaskThread
import com.omarea.krscript.HiddenTaskThread
import com.tool.tree.R
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.*
import com.tool.tree.ThemeModeState
import kotlinx.coroutines.*

// Khôi phục các tính năng đặc thù
import com.omarea.krscript.config.IconPathAnalysis
import com.omarea.krscript.shortcut.ActionShortcutManager

class ActionListFragment : androidx.fragment.app.Fragment(), PageLayoutRender.OnItemClickListener {
    companion object {
        fun create(
            actionInfos: ArrayList<NodeInfoBase>?,
            krScriptActionHandler: KrScriptActionHandler? = null,
            autoRunTask: AutoRunTask? = null,
            themeMode: ThemeMode? = null
        ): ActionListFragment {
            val fragment = ActionListFragment()
            fragment.actionInfos = actionInfos
            fragment.krScriptActionHandler = krScriptActionHandler
            fragment.autoRunTask = autoRunTask
            fragment.themeMode = themeMode
            return fragment
        }
    }

    private var actionInfos: ArrayList<NodeInfoBase>? = null
    private var krScriptActionHandler: KrScriptActionHandler? = null
    private var autoRunTask: AutoRunTask? = null
    private var themeMode: ThemeMode? = null
    private val progressBarDialog by lazy { ProgressBarDialog(requireActivity()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_action_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderList()
        
        autoRunTask?.let { task ->
            actionInfos?.find { it.key == task.key }?.let { 
                when (it) {
                    is ActionNode -> onActionClick(it, { renderList() })
                    is SwitchNode -> onSwitchClick(it, { renderList() })
                    is PickerNode -> onPickerClick(it, { renderList() })
                }
                autoRunTask = null 
            }
        }
    }

    fun updateData(actionInfos: ArrayList<NodeInfoBase>, krScriptActionHandler: KrScriptActionHandler?, themeMode: ThemeMode?) {
        this.actionInfos = actionInfos
        this.krScriptActionHandler = krScriptActionHandler
        this.themeMode = themeMode
        if (isAdded) renderList()
    }

    private fun renderList() {
        val view = view ?: return
        val container = view.findViewById<ViewGroup>(R.id.action_list_container) ?: return
        container.removeAllViews()

        actionInfos?.let {
            val currentTheme = themeMode ?: ThemeMode().apply { isDarkMode = ThemeModeState.isDarkMode() }
            // Khởi tạo PageLayoutRender với đúng tham số Context và Listener
            val render = PageLayoutRender(requireContext(), it, this, currentTheme)
            render.renderList(container)
        }
    }

    // --- Triển khai đầy đủ Interface OnItemClickListener (Sửa lỗi Build) ---

    override fun onActionClick(item: ActionNode, onCompleted: Runnable) {
        actionExecute(item, onCompleted)
    }

    override fun onSwitchClick(item: SwitchNode, onCompleted: Runnable) {
        switchExecute(item, onCompleted)
    }

    override fun onPickerClick(item: PickerNode, onCompleted: Runnable) {
        pickerExecute(item, onCompleted)
    }

    override fun onPageClick(item: PageNode, onCompleted: Runnable) {
        krScriptActionHandler?.onSubPageClick(item)
    }

    override fun onItemLongClick(clickableNode: ClickableNode) {
        // Tích hợp ActionShortcutManager khi nhấn giữ
        activity?.let {
            ActionShortcutManager(it).showShortcutDialog(clickableNode)
        }
    }

    // --- Logic xử lý chi tiết ---

    private fun pickerExecute(item: PickerNode, onCompleted: Runnable) {
        val paramInfo = ActionParamInfo().apply {
            options = item.options
            optionsSh = item.optionsSh
            separator = item.separator ?: " "
        }

        progressBarDialog.showDialog(getString(R.string.kr_param_options_load))

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (!item.getState.isNullOrEmpty()) {
                    paramInfo.valueFromShell = executeScriptGetResult(item.getState!!, item)
                }
                val options = getParamOptions(paramInfo, item)
                
                withContext(Dispatchers.Main) {
                    progressBarDialog.hideDialog()
                    if (options != null) {
                        ActionParamsLayoutRender.setParamOptionsSelectedStatus(paramInfo, options)
                        DialogItemChooser(ThemeModeState.isDarkMode(), options, item.multiple, object : DialogItemChooser.Callback {
                            override fun onConfirm(selected: List<SelectItem>, status: BooleanArray) {
                                val value = if (item.multiple) {
                                    selected.joinToString(item.separator ?: " ") { it.value.toString() }
                                } else {
                                    selected.firstOrNull()?.value?.toString() ?: ""
                                }
                                
                                if (value.isNotEmpty() || !item.multiple) {
                                    val script = item.setState?.replace("{value}", value) ?: ""
                                    actionExecute(item, script, onCompleted, null)
                                }
                            }
                        }).show(childFragmentManager, "picker")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBarDialog.hideDialog()
                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun actionExecute(action: ActionNode, onExit: Runnable) {
        val script = action.setState ?: return
        val params = action.params

        if (!params.isNullOrEmpty()) {
            progressBarDialog.showDialog(getString(R.string.onloading))
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                params.forEach { param ->
                    if (!param.valueShell.isNullOrEmpty()) {
                        param.valueFromShell = executeScriptGetResult(param.valueShell!!, action)
                    }
                    param.optionsFromShell = getParamOptions(param, action)
                }
                withContext(Dispatchers.Main) {
                    progressBarDialog.hideDialog()
                    showParamsDialog(action, params, script, onExit)
                }
            }
        } else {
            actionExecute(action, script, onExit, null)
        }
    }

    private fun showParamsDialog(action: ActionNode, params: ArrayList<ActionParamInfo>, script: String, onExit: Runnable) {
        val ctx = context ?: return
        val view = LayoutInflater.from(ctx).inflate(R.layout.kr_params_list, null) as LinearLayout
        val render = ActionParamsLayoutRender(view, requireActivity())
        
        render.renderList(params, object : ParamsFileChooserRender.FileChooserInterface {
            override fun openFileChooser(fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface): Boolean {
                return krScriptActionHandler?.openFileChooser(fileSelectedInterface) ?: false
            }
        })

        // Sử dụng DialogHelper để đồng bộ UI cũ
        DialogHelper.customDialog(requireActivity(), view, action.title, {
            val values = render.readParamsValue(params)
            actionExecute(action, script, onExit, values)
        }, {
            onExit.run()
        })
    }

    private fun switchExecute(item: SwitchNode, onCompleted: Runnable) {
        progressBarDialog.showDialog(getString(R.string.please_wait))
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val state = executeScriptGetResult(item.getState, item).trim()
            val isChecked = state == "1" || state.equals("true", true)
            // Sửa lỗi unresolved reference setOff/setOn tùy theo model của bạn
            val script = if (isChecked) item.setOff else item.setOn
            
            withContext(Dispatchers.Main) {
                progressBarDialog.hideDialog()
                actionExecute(item, script, onCompleted, null)
            }
        }
    }

    private fun getParamOptions(param: ActionParamInfo, node: NodeInfoBase): ArrayList<SelectItem>? {
        val shell = param.optionsSh ?: return param.options
        val result = executeScriptGetResult(shell, node)
        val items = ArrayList<SelectItem>()
        result.split("\n").forEach { line ->
            if (line.contains("|")) {
                val parts = line.split("|")
                items.add(SelectItem().apply { 
                    this.title = parts[0].trim() 
                    this.value = parts[1].trim() 
                })
            } else if (line.trim().isNotEmpty()) {
                items.add(SelectItem().apply { 
                    this.title = line.trim()
                    this.value = line.trim() 
                })
            }
        }
        return if (items.isEmpty()) param.options else items
    }

    private fun executeScriptGetResult(shellScript: String, nodeInfoBase: NodeInfoBase): String {
        // Tích hợp IconPathAnalysis nếu script cần phân tích icon
        val processedScript = IconPathAnalysis().analysis(shellScript)
        return ScriptEnvironmen.executeResultRoot(requireContext(), processedScript, nodeInfoBase) ?: ""
    }

    private var hiddenTaskRunning = false
    private fun actionExecute(nodeInfo: RunnableNode, script: String, onExit: Runnable, params: HashMap<String, String>?) {
        val ctx = context ?: return
        val onDismiss = Runnable {
            krScriptActionHandler?.onActionCompleted(nodeInfo)
            onExit.run()
        }

        when (nodeInfo.shell) {
            RunnableNode.shellModeBgTask -> BgTaskThread.startTask(ctx, script, params, nodeInfo, onExit, onDismiss)
            RunnableNode.shellModeHidden -> {
                if (hiddenTaskRunning) {
                    Toast.makeText(ctx, getString(R.string.kr_hidden_task_running), Toast.LENGTH_SHORT).show()
                } else {
                    hiddenTaskRunning = true
                    HiddenTaskThread.startTask(ctx, script, params, nodeInfo, onExit, Runnable {
                        hiddenTaskRunning = false
                        onDismiss.run()
                    })
                }
            }
            else -> {
                val dialog = DialogLogFragment.create(nodeInfo, onExit, onDismiss, script, params, ThemeModeState.isDarkMode())
                dialog.isCancelable = false
                dialog.show(childFragmentManager, "log")
            }
        }
    }
}
