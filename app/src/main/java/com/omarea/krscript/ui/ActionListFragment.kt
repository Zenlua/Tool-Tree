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

// Import các tính năng đặc thù từ code cũ của bạn
import com.omarea.krscript.config.IconPathAnalysis
import com.omarea.krscript.shortcut.ActionShortcutManager
import com.omarea.krscript.TryOpenActivity

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
    private val progressBarDialog by lazy { ProgressBarDialog(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_action_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderList()
        
        // Tính năng: Tự động chạy tác vụ khi nạp Fragment (AutoRun)
        autoRunTask?.let { task ->
            actionInfos?.find { it.key == task.key }?.let { 
                onItemClick(it)
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
            PageLayoutRender(container, this, currentTheme).renderList(it)
        }
    }

    override fun onItemClick(item: NodeInfoBase) {
        when (item) {
            is ActionNode -> actionExecute(item) { renderList() }
            is PickerNode -> pickerExecute(item) { renderList() }
            is SwitchNode -> switchExecute(item) { renderList() }
            is PageNode -> krScriptActionHandler?.onSubPageClick(item)
        }
    }

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
                        val darkMode = ThemeModeState.isDarkMode()
                        DialogItemChooser(darkMode, options, item.multiple, object : DialogItemChooser.Callback {
                            override fun onConfirm(selected: List<SelectItem>, status: BooleanArray) {
                                val value = if (item.multiple) {
                                    selected.joinToString(item.separator ?: " ") { it.value.toString() }
                                } else {
                                    selected.firstOrNull()?.value?.toString() ?: ""
                                }
                                
                                if (value.isNotEmpty() || !item.multiple) {
                                    val script = item.setState?.replace("{value}", value) ?: ""
                                    actionExecute(item, script, onCompleted, null)
                                } else {
                                    Toast.makeText(context, getString(R.string.picker_select_none), Toast.LENGTH_SHORT).show()
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

        AlertDialog.Builder(ctx)
            .setTitle(action.title)
            .setView(view)
            .setPositiveButton(R.string.btn_confirm) { _, _ ->
                val values = render.readParamsValue(params)
                actionExecute(action, script, onExit, values)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun switchExecute(item: SwitchNode, onCompleted: Runnable) {
        progressBarDialog.showDialog(getString(R.string.please_wait))
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val state = executeScriptGetResult(item.getState, item).trim()
            val isChecked = state == "1" || state.equals("true", true)
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
                    name = parts[0].trim()
                    value = parts[1].trim() 
                })
            } else if (line.trim().isNotEmpty()) {
                items.add(SelectItem().apply { 
                    name = line.trim()
                    value = line.trim() 
                })
            }
        }
        return if (items.isEmpty()) param.options else items
    }

    private fun executeScriptGetResult(shellScript: String, nodeInfoBase: NodeInfoBase): String {
        return ScriptEnvironmen.executeResultRoot(requireContext(), shellScript, nodeInfoBase) ?: ""
    }

    private var hiddenTaskRunning = false
    private fun actionExecute(nodeInfo: RunnableNode, script: String, onExit: Runnable, params: HashMap<String, String>?) {
        val ctx = context ?: return

        val onDismiss = Runnable {
            krScriptActionHandler?.onActionCompleted(nodeInfo)
            onExit.run()
        }

        when (nodeInfo.shell) {
            RunnableNode.shellModeBgTask -> {
                BgTaskThread.startTask(ctx, script, params, nodeInfo, onExit, onDismiss)
            }
            RunnableNode.shellModeHidden -> {
                if (hiddenTaskRunning) {
                    Toast.makeText(ctx, getString(R.string.kr_hidden_task_running), Toast.LENGTH_SHORT).show()
                } else {
                    hiddenTaskRunning = true
                    val hiddenDismiss = Runnable {
                        hiddenTaskRunning = false
                        onDismiss.run()
                    }
                    HiddenTaskThread.startTask(ctx, script, params, nodeInfo, onExit, hiddenDismiss)
                }
            }
            else -> {
                val darkMode = ThemeModeState.isDarkMode()
                DialogLogFragment.create(nodeInfo, onExit, onDismiss, script, params, darkMode).apply {
                    isCancelable = false
                    show(childFragmentManager, "log")
                }
            }
        }
    }
}
