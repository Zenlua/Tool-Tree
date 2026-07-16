package com.omarea.krscript.ui

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import android.widget.*
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.omarea.common.ui.DialogHelper
import com.tool.tree.databinding.KrDialogLogBinding
import com.omarea.krscript.executor.ShellExecutor
import com.omarea.krscript.model.RunnableNode
import com.omarea.krscript.model.ShellHandlerBase
import android.content.DialogInterface
import com.tool.tree.R
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

    interface IActionEventHandler {
        fun onStart(forceStop: Runnable?)
        fun onSuccess()
        fun onCompleted()
    }

    class MyShellHandler(
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

        private fun getColor(resId: Int): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) context.getColor(resId) else context.resources.getColor(resId)
        }

        private fun dpToPx(dp: Int): Int {
            return (dp * context.resources.displayMetrics.density).toInt()
        }

        fun release() {
            logViewRef.clear()
            progressRef.clear()
            actionEventHandler = null
        }

        override fun handleMessage(msg: Message) {
            // Rút gọn: Sử dụng trực tiếp logic phân phối của lớp cha ShellHandlerBase
            super.handleMessage(msg)
        }

        override fun onReader(msg: Any) {
            updateLog(msg.toString(), basicColor)
        }

        override fun onWrite(msg: Any) {
            updateLog(msg.toString(), scriptColor)
        }

        override fun onError(msg: Any) {
            hasError = true
            updateLog(msg.toString(), errorColor)
        }

        override fun onStart(forceStop: Runnable?) {
            AnsiColorParser.reset()
            lineCount = 0
            logViewRef.get()?.text = ""
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
                            params.height = dpToPx(4)
                            params.topMargin = dpToPx(22)
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
                            params.height = dpToPx(8)
                            params.topMargin = dpToPx(12.5)
                            layoutParams = params
                        }
                    }
                }
            }
        }

        override fun onStart(msg: Any?) {
            AnsiColorParser.reset()
            lineCount = 0
            logViewRef.get()?.text = ""
        }

        override fun onExit(msg: Any?) {
            val code = (msg as? Int) ?: -1
            if (!hasError && code == 0) actionEventHandler?.onSuccess()
            updateLog(context.getString(R.string.kr_shell_completed), endColor)
            actionEventHandler?.onCompleted()
        }

        // Override hàm này để giữ nguyên định dạng SpannableString từ lớp cha chuyển sang
        override fun updateLog(msg: SpannableString?) {
            msg?.let {
                dispatchLogUpdate(it)
            }
        }

        private fun updateLog(text: String, forcedColor: Int?) {
            val cleanString = text.replace("\r", "")
            
            // Bước A: Phân tích mã màu ANSI (Xử lý ngay trên luồng background hiện tại)
            var parsedLog: CharSequence = AnsiColorParser.parse(cleanString)
            
            // Bước B: Áp dụng màu mặc định nếu không có mã ANSI đặc trưng
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
            val newLines = formattedText.count { it == '\n' } + 1

            logView.post {
                val editable = logView.text as? Editable ?: SpannableStringBuilder(logView.text)
                editable.append(formattedText)
                lineCount += newLines

                // Cắt bớt log cũ nếu vượt quá 5000 dòng
                if (lineCount > 5000) {
                    var deleteEndIndex = 0
                    var linesToTemplate = lineCount - 5000
                    val currentCharSequence = editable.toString()
                    
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
                        editable.delete(0, deleteEndIndex)
                        lineCount = 5000
                    }
                }

                logView.text = editable
                
                // Cuộn xuống đáy tối ưu hơn
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