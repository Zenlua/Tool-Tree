package com.omarea.krscript.ui

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.text.Editable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.omarea.common.ui.DialogHelper
import com.tool.tree.databinding.KrDialogLogBinding
import com.omarea.krscript.executor.ShellExecutor
import com.tool.tree.R
import com.omarea.krscript.model.RunnableNode
import com.omarea.krscript.model.ShellHandlerBase
import java.lang.ref.WeakReference
import com.tool.tree.AnsiColorParser

class DialogLogFragment : DialogFragment() {

    private var _binding: KrDialogLogBinding? = null
    private val binding get() = _binding!!
    private var running = false
    private var canceled = false
    private var uiVisible = true
    private var nodeInfo: RunnableNode? = null
    private lateinit var onExit: Runnable
    private lateinit var script: String
    private var params: HashMap<String, String>? = null
    private var themeResId: Int = 0
    private var onDismissRunnable: Runnable? = null
    private var currentHandler: MyShellHandler? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = KrDialogLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireActivity(), if (themeResId != 0) themeResId else R.style.kr_full_screen_dialog_light)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.let { window ->
            DialogHelper.setWindowBlurBg(window, requireActivity())
        }

        nodeInfo?.let { node ->
            if (node.reloadPage) {
                binding.btnHide.visibility = View.GONE
            }

            val shellHandler = openExecutor(node)

            ShellExecutor().execute(
                requireContext().applicationContext,
                node,
                script,
                onExit,
                params,
                shellHandler
            )
        } ?: dismissAllowingStateLoss()
    }

    private fun openExecutor(nodeInfo: RunnableNode): ShellHandlerBase {
        var forceStopRunnable: Runnable? = null
        canceled = false
        uiVisible = true

        binding.btnHide.setOnClickListener {
            uiVisible = false
            offScreen()
            closeView()
        }

        binding.btnCancel.setOnClickListener {
            if (running && !canceled) {
                canceled = true
                forceStopRunnable?.run()
                binding.btnExit.visibility = View.VISIBLE
                binding.btnCancel.visibility = View.GONE
            }
        }

        binding.btnExit.setOnClickListener {
            isCancelable = true
            closeView()
        }

        binding.btnCopy.setOnClickListener {
            try {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("text", binding.shellOutput.text.toString())
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), getString(R.string.copy_fail), Toast.LENGTH_SHORT).show()
            }
        }

        if (nodeInfo.interruptable) {
            binding.btnHide.visibility = View.VISIBLE
            binding.btnCancel.visibility = View.VISIBLE
        } else {
            binding.btnHide.visibility = View.GONE
            binding.btnCancel.visibility = View.GONE
        }

        binding.title.text = if (nodeInfo.title.isNotEmpty()) nodeInfo.title else { binding.title.visibility = View.GONE; "" }
        binding.desc.text = if (nodeInfo.desc.isNotEmpty()) nodeInfo.desc else { binding.desc.visibility = View.GONE; "" }
        binding.actionProgress.isIndeterminate = true

        // Sử dụng requireContext().applicationContext đảm bảo không bị lỗi Nullable Context
        val handler = MyShellHandler(requireContext().applicationContext, object : IActionEventHandler {
            override fun onStart(forceStop: Runnable?) {
                running = true
                canceled = false
                forceStopRunnable = forceStop
                dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                binding.btnExit.visibility = View.GONE
                binding.btnCancel.visibility = if (nodeInfo.interruptable && forceStop != null) View.VISIBLE else View.GONE
            }

            override fun onSuccess() {
                if (nodeInfo.autoOff) closeView()
            }

            override fun onCompleted() {
                running = false
                onExit.run()
                offScreen()
                _binding?.let { b ->
                    b.btnHide.visibility = View.GONE
                    b.btnCancel.visibility = View.GONE
                    b.btnExit.visibility = View.VISIBLE
                    b.actionProgress.visibility = View.GONE
                }
                isCancelable = true
            }
        }, binding.shellOutput, binding.actionProgress)

        this.currentHandler = handler
        return handler
    }

    @FunctionalInterface
    interface IActionEventHandler {
        fun onStart(forceStop: Runnable?)
        fun onSuccess()
        fun onCompleted()
    }

    // THAY ĐỔI: Thêm từ khóa "inner" để cho phép truy cập tài nguyên lớp ngoài một cách an toàn
    inner class MyShellHandler(
        context: Context,
        private var actionEventHandler: IActionEventHandler?,
        logView: TextView?,
        shellProgress: ProgressBar?
    ) : ShellHandlerBase(context) {

        private val logViewRef = WeakReference(logView)
        private val progressRef = WeakReference(shellProgress)

        private val errorColor = getColor(R.color.kr_shell_log_error)
        private val basicColor = getColor(R.color.kr_shell_log_basic)
        private val scriptColor = getColor(R.color.kr_shell_log_script)
        private val endColor = getColor(R.color.kr_shell_log_end)
        private var hasError = false
        private var lineCount = 0

        private val logBuffer = SpannableStringBuilder()
        private var overwriteStart = -1

        init {
            logView?.setText(logBuffer, TextView.BufferType.EDITABLE)
        }

        private fun getColor(resId: Int): Int {
            // Sử dụng requireContext() của Fragment bên ngoài để truy xuất tài nguyên một cách an toàn
            val safeContext = contextRef.get() ?: requireContext()
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                safeContext.getColor(resId)
            } else {
                @Suppress("DEPRECATION")
                safeContext.resources.getColor(resId)
            }
        }

        private fun dpToPx(dp: Float): Int {
            val safeContext = contextRef.get() ?: requireContext()
            return (dp * safeContext.resources.displayMetrics.density).toInt()
        }

        fun release() {
            logViewRef.clear()
            progressRef.clear()
            actionEventHandler = null
        }

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
        }

        override fun onReader(msg: Any) {
            updateLogWithColor(msg.toString(), basicColor)
        }

        override fun onWrite(msg: Any) {
            updateLogWithColor(msg.toString(), scriptColor)
        }

        override fun onError(msg: Any) {
            hasError = true
            updateLogWithColor(msg.toString(), errorColor)
        }

        override fun onStart(forceStop: Runnable?) {
            AnsiColorParser.reset()
            lineCount = 0
            overwriteStart = -1
            logBuffer.clear()
            logViewRef.get()?.text = logBuffer
            actionEventHandler?.onStart(forceStop)
        }

        override fun onProgress(current: Int, total: Int) {
            val shellProgress = progressRef.get() ?: return
            shellProgress.post {
                when {
                    current < 0 -> shellProgress.apply {
                        visibility = View.VISIBLE
                        isIndeterminate = true
                        (layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
                            params.height = dpToPx(7f)
                            params.topMargin = dpToPx(13.2f)
                            layoutParams = params
                        }
                    }
                    current >= total -> shellProgress.visibility = View.GONE
                    else -> shellProgress.apply {
                        visibility = View.VISIBLE
                        isIndeterminate = false
                        max = total
                        progress = current
                        (layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
                            params.height = dpToPx(1.6f)
                            params.topMargin = dpToPx(15.8f)
                            layoutParams = params
                        }
                    }
                }
            }
        }

        override fun onStart(msg: Any?) {
            AnsiColorParser.reset()
            lineCount = 0
            overwriteStart = -1
            logBuffer.clear()
            logViewRef.get()?.text = logBuffer
        }

        override fun onExit(msg: Any?) {
            val safeContext = contextRef.get() ?: requireContext()
            val code = (msg as? Int) ?: -1
            if (!hasError && code == 0) actionEventHandler?.onSuccess()
            updateLogWithColor(safeContext.getString(R.string.kr_shell_completed), endColor)
            actionEventHandler?.onCompleted()
        }

        override fun updateLog(msg: SpannableString?) {
            msg?.let {
                dispatchLogUpdate(it)
            }
        }

        private fun updateLogWithColor(text: String, forcedColor: Int?) {
            val cleanString = text
            var parsedLog: CharSequence = AnsiColorParser.parse(cleanString)
            
            if (forcedColor != null && !cleanString.contains("\u001B[")) {
                val spannable = SpannableString(parsedLog)
                spannable.setSpan(
                    ForegroundColorSpan(forcedColor),
                    0,
                    spannable.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                parsedLog = spannable
            }
            
            dispatchLogUpdate(parsedLog)
        }

        private fun dispatchLogUpdate(formattedText: CharSequence) {
            val logView = logViewRef.get() ?: return

            logView.post {
                val endsWithCR = formattedText.isNotEmpty() && formattedText.last() == '\r'
                val endsWithLF = formattedText.isNotEmpty() && formattedText.last() == '\n'

                val content: CharSequence = if (endsWithCR || endsWithLF) {
                    formattedText.subSequence(0, formattedText.length - 1)
                } else {
                    formattedText
                }

                if (overwriteStart in 0..logBuffer.length) {
                    logBuffer.delete(overwriteStart, logBuffer.length)
                }

                val insertStart = logBuffer.length
                logBuffer.append(content)

                when {
                    endsWithCR -> {
                        overwriteStart = insertStart
                    }
                    endsWithLF -> {
                        logBuffer.append('\n')
                        lineCount++
                        overwriteStart = -1
                    }
                    else -> {
                        overwriteStart = -1
                    }
                }

                if (lineCount > 5000) {
                    var deleteEndIndex = 0
                    var linesToTemplate = lineCount - 5000
                    val currentCharSequence = logBuffer.toString()
                    
                    for (i in currentCharSequence.indices) {
                        if (currentCharSequence[i] == '\n') {
                            linesToTemplate--
                            if (linesToTemplate <= 0) {
                                deleteEndIndex = i + 1
                                break
                            }
                        }
                    }
                    if (deleteEndIndex > 0) {
                        logBuffer.delete(0, deleteEndIndex)
                        lineCount = 5000
                        if (overwriteStart != -1) {
                            overwriteStart = (overwriteStart - deleteEndIndex).coerceAtLeast(0)
                        }
                    }
                }

                logView.text = logBuffer
                
                (logView.parent as? ScrollView)?.let { scrollView ->
                    scrollView.post {
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (!uiVisible || !running) return@setOnKeyListener false
            event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        }
    }

    private fun offScreen() = dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    private fun closeView() { try { dismissAllowingStateLoss() } catch (ex: Exception) {} }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissRunnable?.run()
    }

    override fun onDestroyView() {
        currentHandler?.release()
        currentHandler = null
        AnsiColorParser.reset()
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        offScreen()
        super.onDestroy()
    }

    companion object {
        fun create(nodeInfo: RunnableNode, onExit: Runnable, onDismiss: Runnable, script: String, params: HashMap<String, String>?, darkMode: Boolean = false): DialogLogFragment {
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