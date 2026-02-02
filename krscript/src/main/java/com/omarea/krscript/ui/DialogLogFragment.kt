package com.omarea.krscript.ui

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PowerManager
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.text.SpannableString
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.omarea.common.ui.DialogHelper
import com.omarea.krscript.R
import com.omarea.krscript.databinding.KrDialogLogBinding
import com.omarea.krscript.executor.ShellExecutor
import com.omarea.krscript.model.RunnableNode
import com.omarea.krscript.model.ShellHandlerBase
import android.view.WindowManager

class DialogLogFragment : androidx.fragment.app.DialogFragment() {

    private var binding: KrDialogLogBinding? = null
    private var running = false
    private var canceled = false
    private var uiVisible = true
    private var nodeInfo: RunnableNode? = null
    private lateinit var onExit: Runnable
    private lateinit var script: String
    private var params: HashMap<String, String>? = null
    private var themeResId: Int = 0
    private var onDismissRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = KrDialogLogBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireActivity(), if (themeResId != 0) themeResId else R.style.kr_full_screen_dialog_light)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let {
            dialog?.window?.let { window ->
                DialogHelper.setWindowBlurBg(window, it)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        nodeInfo?.let { node ->
            if (node.reloadPage) {
                binding?.btnHide?.visibility = View.GONE
            }
            openExecutor(node)?.let { shellHandler ->
                ShellExecutor().execute(activity, node, script, onExit, params, shellHandler)
            }
        } ?: dismissAllowingStateLoss()
    }

    private fun openExecutor(nodeInfo: RunnableNode): ShellHandlerBase {
        var forceStopRunnable: Runnable? = null
        canceled = false
        uiVisible = true

        binding?.btnHide?.setOnClickListener {
            uiVisible = false
            offScreen()
            closeView()
        }

        binding?.btnCancel?.setOnClickListener {
            if (running && !canceled) {
                canceled = true
                forceStopRunnable?.run()
                binding?.btnExit?.visibility = View.VISIBLE
                binding?.btnCancel?.visibility = View.GONE
            }
        }

        binding?.btnExit?.setOnClickListener {
            isCancelable = true
            closeView()
        }

        binding?.btnCopy?.setOnClickListener {
            try {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("text", binding?.shellOutput?.text.toString())
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, getString(R.string.copy_fail), Toast.LENGTH_SHORT).show()
            }
        }

        if (nodeInfo.interruptable) {
            binding?.btnHide?.visibility = View.VISIBLE
            binding?.btnCancel?.visibility = View.VISIBLE
        } else {
            binding?.btnHide?.visibility = View.GONE
            binding?.btnCancel?.visibility = View.GONE
        }

        if (nodeInfo.title.isNotEmpty()) {
            binding?.title?.text = nodeInfo.title
        } else {
            binding?.title?.visibility = View.GONE
        }

        if (nodeInfo.desc.isNotEmpty()) {
            binding?.desc?.text = nodeInfo.desc
        } else {
            binding?.desc?.visibility = View.GONE
        }

        binding?.actionProgress?.isIndeterminate = true

        return MyShellHandler(object : IActionEventHandler {
            override fun onCompleted() {
                running = false
                onExit.run()
                offScreen()
                binding?.btnHide?.visibility = View.GONE
                binding?.btnCancel?.visibility = View.GONE
                binding?.btnExit?.visibility = View.VISIBLE
                binding?.actionProgress?.visibility = View.GONE
                isCancelable = true
            }

            override fun onSuccess() {
                if (nodeInfo.autoOff) {
                    closeView()
                }
            }

            override fun onStart(forceStop: Runnable?) {
                running = true
                canceled = false
                forceStopRunnable = forceStop

                dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                if (nodeInfo.interruptable && forceStop != null) {
                    binding?.btnCancel?.visibility = View.VISIBLE
                    binding?.btnExit?.visibility = View.GONE
                } else {
                    binding?.btnCancel?.visibility = View.GONE
                }
            }

        }, binding?.shellOutput, binding?.actionProgress)
    }

    @FunctionalInterface
    interface IActionEventHandler {
        fun onStart(forceStop: Runnable?)
        fun onSuccess()
        fun onCompleted()
    }

    class MyShellHandler(
        private var actionEventHandler: IActionEventHandler,
        private var logView: TextView?,
        private var shellProgress: ProgressBar?
    ) : ShellHandlerBase() {

        private val context = logView?.context
        private val errorColor = getColor(R.color.kr_shell_log_error)
        private val basicColor = getColor(R.color.kr_shell_log_basic)
        private val scriptColor = getColor(R.color.kr_shell_log_script)
        private val endColor = getColor(R.color.kr_shell_log_end)
        private var hasError = false

        private fun getColor(resId: Int): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context!!.getColor(resId)
            } else {
                context!!.resources.getColor(resId)
            }
        }

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                EVENT_EXIT -> onExit(msg.obj)
                EVENT_START -> onStart(msg.obj)
                EVENT_REDE -> onReaderMsg(msg.obj)
                EVENT_READ_ERROR -> onError(msg.obj)
                EVENT_WRITE -> onWrite(msg.obj)
            }
        }

        override fun onReader(msg: Any) = updateLog(msg, basicColor)
        override fun onWrite(msg: Any) = updateLog(msg, scriptColor)
        override fun onError(msg: Any) {
            hasError = true
            updateLog(msg, errorColor)
        }

        override fun onStart(forceStop: Runnable?) = actionEventHandler.onStart(forceStop)

        override fun onProgress(current: Int, total: Int) {
            shellProgress?.post {
                when {
                    current < 0 -> {
                        shellProgress?.visibility = View.VISIBLE
                        shellProgress?.isIndeterminate = true
                    }
                    current >= total -> shellProgress?.visibility = View.GONE
                    else -> {
                        shellProgress?.visibility = View.VISIBLE
                        shellProgress?.isIndeterminate = false
                        shellProgress?.max = total
                        shellProgress?.progress = current
                    }
                }
            }
        }

        override fun onStart(msg: Any?) {
            logView?.text = ""
        }

        override fun onExit(msg: Any?) {
            if (!hasError) actionEventHandler.onSuccess()
            updateLog(context?.getString(R.string.kr_shell_completed), endColor)
            actionEventHandler.onCompleted()
        }

        override fun updateLog(msg: SpannableString?) {
            msg?.let {
                logView?.post {
                    logView?.append(it)
                    (logView?.parent as? ScrollView)?.fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (!uiVisible || !running) return@setOnKeyListener false
            event.action == KeyEvent.ACTION_DOWN &&
                    (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        }
    }

    private fun offScreen() {
        dialog?.window?.let { window ->
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun closeView() {
        try {
            dismissAllowingStateLoss()
        } catch (ex: Exception) {
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissRunnable?.run()
        onDismissRunnable = null
    }

    companion object {
        fun create(
            nodeInfo: RunnableNode,
            onExit: Runnable,
            onDismiss: Runnable,
            script: String,
            params: HashMap<String, String>?,
            darkMode: Boolean = false
        ): DialogLogFragment {
            val fragment = DialogLogFragment()
            fragment.nodeInfo = nodeInfo
            fragment.onExit = onExit
            fragment.script = script
            fragment.params = params
            fragment.themeResId = if (darkMode) R.style.kr_full_screen_dialog_dark else R.style.kr_full_screen_dialog_light
            fragment.onDismissRunnable = onDismiss
            return fragment
        }
    }
}
