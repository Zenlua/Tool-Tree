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
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
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
import java.util.concurrent.atomic.AtomicBoolean
import java.io.File
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager

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

    // Trạng thái bật/tắt soft wrap của log output (giống Text Editor Activity)
    private var wrapEnabled = true
    private var noWrapContainer: HorizontalScrollView? = null

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
            val shellHandler = openExecutor(node)

            // Đặt sau openExecutor() để không bị ghi đè bởi logic hiển thị btnHide
            // dựa theo nodeInfo.interruptable bên trong openExecutor().
            if (node.reloadPage) {
                binding.btnHide.visibility = View.GONE
            }

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

        wrapEnabled = readWrapEnabled(requireContext().applicationContext)
        applyWrapState()

        binding.btnWrap.setOnClickListener {
            wrapEnabled = !wrapEnabled
            applyWrapState()
            persistWrapEnabled(requireContext().applicationContext, wrapEnabled)
        }

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

        fun sendUserInput() {
            val text = binding.shellInput.text?.toString().orEmpty()
            if (text.isEmpty()) {
                return
            }
            if (currentHandler?.writeInput(text) == true) {
                binding.shellInput.setText("")
            } else {
                Toast.makeText(requireContext(), getString(R.string.input_send_fail), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSendInput.setOnClickListener { sendUserInput() }
        binding.shellInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                sendUserInput()
                true
            } else {
                false
            }
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
                binding.inputRow.visibility = View.GONE
            }

            override fun onSuccess() {
                if (nodeInfo.autoOff) closeView()
            }

            override fun onCompleted() {
                running = false
                onExit.run()
                offScreen()
                _binding?.let { b ->
                    val transition = ChangeBounds().apply {
                        duration = 200
                    }
                
                    TransitionManager.beginDelayedTransition(
                        b.root.findViewById(R.id.top_actions),
                        transition
                    )
                    b.btnHide.visibility = View.GONE
                    b.btnCancel.visibility = View.GONE
                    b.btnExit.visibility = View.VISIBLE
                    b.actionProgress.visibility = View.GONE
                    b.inputRow.visibility = View.GONE
                    hideKeyboard(b.shellInput)
                }
                isCancelable = true
            }
        }, binding.shellOutput, binding.actionProgress, binding.inputRow, binding.shellInput)

        this.currentHandler = handler
        return handler
    }

    /**
     * Áp dụng trạng thái bật/tắt soft wrap cho log output, tương tự cách làm ở TextEditorActivity:
     * - wrapEnabled = true: shellOutput nằm trực tiếp trong ScrollView (cuộn dọc), tự động xuống dòng.
     * - wrapEnabled = false: shellOutput được bọc trong một HorizontalScrollView (cho phép cuộn ngang),
     *   không tự động xuống dòng.
     */
    private fun applyWrapState() {
        val b = _binding ?: return
        val logView = b.shellOutput
        val scrollView = b.logScrollView

        (logView.parent as? ViewGroup)?.removeView(logView)
        noWrapContainer?.let { scrollView.removeView(it) }
        logView.setHorizontallyScrolling(!wrapEnabled)

        if (wrapEnabled) {
            logView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            scrollView.addView(logView)
        } else {
            val hsv = noWrapContainer ?: HorizontalScrollView(requireContext()).also {
                it.isFillViewport = false
                it.isHorizontalScrollBarEnabled = false
                noWrapContainer = it
            }
            logView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            hsv.removeAllViews()
            hsv.addView(logView)
            hsv.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            scrollView.addView(hsv)
        }

        b.btnWrap.alpha = if (wrapEnabled) 0.5f else 1f
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    /**
     * Đọc trạng thái soft wrap đã lưu từ file cấu hình trong thư mục riêng của app
     * (context.filesDir), thay vì dùng đường dẫn tuyệt đối cố định.
     * File chứa "1" => tắt soft wrap, "0" hoặc không tồn tại => bật soft wrap (mặc định, như cũ).
     */
    private fun readWrapEnabled(context: Context): Boolean {
        return try {
            val file = File(context.filesDir, WRAP_STATE_RELATIVE_PATH)
            if (file.exists()) {
                file.readText().trim() != "1"
            } else {
                true
            }
        } catch (ex: Exception) {
            true
        }
    }

    /**
     * Lưu trạng thái soft wrap xuống file trong context.filesDir (không dùng path tuyệt đối),
     * thực hiện ở luồng nền để tránh chặn UI thread.
     */
    private fun persistWrapEnabled(context: Context, wrapEnabled: Boolean) {
        Thread {
            try {
                val file = File(context.filesDir, WRAP_STATE_RELATIVE_PATH)
                file.parentFile?.let { parent -> if (!parent.exists()) parent.mkdirs() }
                file.writeText(if (wrapEnabled) "0" else "1")
            } catch (ex: Exception) {
            }
        }.start()
    }

    private fun hideKeyboard(view: View) {
        try {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        } catch (ex: Exception) {}
    }

    @FunctionalInterface
    interface IActionEventHandler {
        fun onStart(forceStop: Runnable?)
        fun onSuccess()
        fun onCompleted()
    }

    class MyShellHandler(
        context: Context,
        private var actionEventHandler: IActionEventHandler?,
        logView: TextView?,
        shellProgress: ProgressBar?,
        inputRow: View? = null,
        shellInput: EditText? = null
    ) : ShellHandlerBase(context) {

        private val logViewRef = WeakReference(logView)
        private val progressRef = WeakReference(shellProgress)
        private val inputRowRef = WeakReference(inputRow)
        private val shellInputRef = WeakReference(shellInput)

        private val errorColor = getColor(R.color.kr_shell_log_error)
        private val basicColor = getColor(R.color.kr_shell_log_basic)
        private val scriptColor = getColor(R.color.kr_shell_log_script)
        private val endColor = getColor(R.color.kr_shell_log_end)
        private var hasError = false
        private var lineCount = 0

        // logBuffer giờ chỉ chạy trên luồng gọi (background thread của Executor)
        private val logBuffer = SpannableStringBuilder()
        private var lineStart = 0
        private var pendingOverwrite = false

        // Độ dài logBuffer đã được đẩy lên UI (chỉ đọc/ghi trong synchronized(logBuffer)).
        // Bất biến quan trọng: SAU MỖI LẦN flush thành công, nội dung hiển thị trên UI luôn
        // trùng khớp CHÍNH XÁC với logBuffer[0, uiAppliedLength).
        private var uiAppliedLength = 0

        // Vị trí (tính theo tọa độ logBuffer/UI) SỚM NHẤT mà nội dung đã bị thay đổi so với
        // lần flush trước (do \r ghi đè dòng, hoặc do cắt tỉa log). Int.MAX_VALUE nghĩa là
        // "chưa có gì bị ghi đè, chỉ có nội dung mới được thêm vào cuối" (trường hợp append thường).
        // Nhờ theo dõi chính xác vị trí này (thay vì đoán qua so sánh từng ký tự), lần flush tiếp
        // theo chỉ cần thay thế ĐÚNG đoạn bị đổi bằng Editable.replace(), KHÔNG BAO GIỜ gán lại
        // toàn bộ `.text = ...`. Đây là điểm mấu chốt để tránh lỗi "nhảy lên đầu" khi có \r:
        // gán lại `.text` khiến TextView coi như nội dung hoàn toàn mới, tự reset con trỏ/selection
        // về vị trí 0, kéo theo ScrollView bị giật lên đầu; còn Editable.replace() chỉ sửa đúng
        // phần thay đổi tại chỗ, không làm mất "trạng thái" hiển thị hiện tại.
        private var uiInvalidFrom = Int.MAX_VALUE

        // Đảm bảo nhiều lần cập nhật log liên tiếp chỉ gộp lại thành 1 lần vẽ UI / 1 lần cuộn,
        // tránh spam Runnable + fullScroll() lên Main thread khi shell xả log dồn dập -> đây là
        // nguyên nhân chính gây giật/lag khi log in ra nhanh.
        private val pendingUiUpdate = AtomicBoolean(false)

        init {
            // Bắt buộc dùng BufferType.EDITABLE ngay từ đầu để có thể dùng editableText.replace()
            // ở flushToUi(), thay vì phải gán lại `.text` (nguyên nhân gây giật/nhảy scroll).
            logView?.setText("", TextView.BufferType.EDITABLE)
        }

        private fun getColor(resId: Int): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) context.getColor(resId) else context.resources.getColor(resId)
        }

        /**
         * Tìm ScrollView cha gần nhất trong cây view, vì khi tắt soft wrap logView có thể
         * nằm lồng trong 1 HorizontalScrollView trung gian thay vì là con trực tiếp của ScrollView.
         */
        private fun findScrollViewAncestor(view: View): ScrollView? {
            var parent = view.parent
            while (parent is View) {
                if (parent is ScrollView) return parent
                parent = parent.parent
            }
            return null
        }

        private fun dpToPx(dp: Float): Int {
            return (dp * context.resources.displayMetrics.density).toInt()
        }

        fun release() {
            logViewRef.clear()
            progressRef.clear()
            inputRowRef.clear()
            shellInputRef.clear()
            unbindStdin()
            actionEventHandler = null
        }

        override fun onInputRequest(prompt: String) {
            val logView = logViewRef.get()
            val row = inputRowRef.get() ?: return
            val input = shellInputRef.get()
            (logView ?: row).post {
                row.visibility = View.VISIBLE
                if (input != null) {
                    if (prompt.isNotEmpty()) {
                        input.hint = prompt
                    }
                    input.isFocusable = true
                    input.isFocusableInTouchMode = true
                    input.requestFocus()
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
                }
            }
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

        private fun resetLogState() {
            AnsiColorParser.reset()
            synchronized(logBuffer) {
                lineCount = 0
                lineStart = 0
                pendingOverwrite = false
                uiAppliedLength = 0
                uiInvalidFrom = Int.MAX_VALUE
                logBuffer.clear()
            }
            logViewRef.get()?.post {
                val tv = logViewRef.get() ?: return@post
                val editable = tv.editableText
                if (editable != null) {
                    editable.clear()
                } else {
                    tv.setText("", TextView.BufferType.EDITABLE)
                }
            }
        }

        override fun onStart(forceStop: Runnable?) {
            resetLogState()
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
            resetLogState()
        }

        override fun onExit(msg: Any?) {
            val code = (msg as? Int) ?: -1
            updateLogWithColor(context.getString(R.string.kr_shell_completed), endColor)
            if (!hasError && code == 0) {
                actionEventHandler?.onSuccess()
            }
            actionEventHandler?.onCompleted()
        }

        override fun updateLog(msg: SpannableString?) {
            msg?.let {
                dispatchLogUpdate(it)
            }
        }

        private fun updateLogWithColor(text: String, forcedColor: Int?) {
            var parsedLog: CharSequence = AnsiColorParser.parse(text)

            if (forcedColor != null && !text.contains("\u001B[")) {
                val spannable = SpannableStringBuilder(parsedLog)
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

        /**
         * Đánh dấu rằng nội dung từ vị trí [from] trở đi trong logBuffer đã bị thay đổi so với
         * lần flush trước, để flushToUi() biết chính xác cần Editable.replace() từ đâu.
         */
        private fun markInvalidFrom(from: Int) {
            if (from < uiInvalidFrom) uiInvalidFrom = from
        }

        /**
         * XỬ LÝ CHUỖI TẠI BACKGROUND THREAD ĐỂ GIẢM TẢI UI THREAD
         */
        private fun dispatchLogUpdate(formattedText: CharSequence) {
            val logView = logViewRef.get() ?: return

            synchronized(logBuffer) {
                var i = 0
                val len = formattedText.length

                while (i < len) {
                    var j = i
                    while (j < len && formattedText[j] != '\r' && formattedText[j] != '\n') j++

                    if (j > i) {
                        val segment = formattedText.subSequence(i, j)
                        if (pendingOverwrite) {
                            if (lineStart in 0..logBuffer.length) {
                                logBuffer.delete(lineStart, logBuffer.length)
                                markInvalidFrom(lineStart)
                            }
                            pendingOverwrite = false
                        }
                        logBuffer.append(segment)
                    }

                    if (j < len) {
                        when (formattedText[j]) {
                            '\r' -> {
                                val isCRLF = j + 1 < len && formattedText[j + 1] == '\n'
                                if (!isCRLF) {
                                    pendingOverwrite = true
                                }
                            }
                            '\n' -> {
                                if (pendingOverwrite && lineStart in 0..logBuffer.length) {
                                    logBuffer.delete(lineStart, logBuffer.length)
                                    markInvalidFrom(lineStart)
                                    pendingOverwrite = false
                                }
                                logBuffer.append('\n')
                                lineCount++
                                lineStart = logBuffer.length
                            }
                        }
                        j++
                    }
                    i = j
                }

                // Cắt tỉa log tối đa 5000 dòng ngay tại luồng nền
                if (lineCount > 5000) {
                    var deleteEndIndex = 0
                    var linesToTrim = lineCount - 5000
                    val currentCharSequence = logBuffer.toString()

                    for (k in currentCharSequence.indices) {
                        if (currentCharSequence[k] == '\n') {
                            linesToTrim--
                            if (linesToTrim <= 0) {
                                deleteEndIndex = k + 1
                                break
                            }
                        }
                    }
                    if (deleteEndIndex > 0) {
                        logBuffer.delete(0, deleteEndIndex)
                        lineCount = 5000
                        lineStart = (lineStart - deleteEndIndex).coerceAtLeast(0)
                        uiAppliedLength = (uiAppliedLength - deleteEndIndex).coerceAtLeast(0)
                        // Toàn bộ tọa độ đã dịch chuyển do cắt từ đầu -> cần đồng bộ lại từ đầu buffer
                        uiInvalidFrom = 0
                    }
                }
            }

            // GỘP CẬP NHẬT UI: nếu đã có 1 lần vẽ đang chờ chạy trên Main thread thì không post thêm.
            // Khi Main thread rảnh và chạy tới, flushToUi() sẽ tự đọc trạng thái MỚI NHẤT của logBuffer,
            // nên nhiều dòng log đến dồn dập chỉ gây ra 1 lần cập nhật + 1 lần cuộn, thay vì
            // một Runnable + một fullScroll() cho từng dòng -> đây là nguyên nhân chính gây giật.
            if (pendingUiUpdate.compareAndSet(false, true)) {
                logView.post {
                    pendingUiUpdate.set(false)
                    flushToUi(logView)
                }
            }
        }

        /**
         * Vẽ lên UI. CHỈ thay thế đúng đoạn nội dung đã thay đổi kể từ lần flush trước
         * (append thường: chỉ phần thêm mới ở cuối; có \r ghi đè: đoạn từ uiInvalidFrom trở đi),
         * bằng Editable.replace() tại chỗ. KHÔNG BAO GIỜ gán lại toàn bộ `.text = ...`, vì việc
         * gán lại text khiến TextView/ScrollView tự reset về vị trí 0 -> đây chính là nguyên nhân
         * gây hiện tượng "nhảy lên đầu" khi có \r.
         */
        private fun flushToUi(logView: TextView) {
            var replaceFrom: Int
            val oldUiLength: Int
            val tail: CharSequence?

            synchronized(logBuffer) {
                oldUiLength = uiAppliedLength
                replaceFrom = if (uiInvalidFrom == Int.MAX_VALUE) oldUiLength else uiInvalidFrom
                if (replaceFrom > oldUiLength) replaceFrom = oldUiLength
                if (replaceFrom < 0) replaceFrom = 0

                tail = if (replaceFrom < logBuffer.length || replaceFrom < oldUiLength) {
                    SpannableStringBuilder(logBuffer.subSequence(replaceFrom, logBuffer.length))
                } else {
                    null
                }

                uiAppliedLength = logBuffer.length
                uiInvalidFrom = Int.MAX_VALUE
            }

            if (tail == null) return

            var editable = logView.editableText
            if (editable == null) {
                // Phòng hờ: nếu vì lý do gì đó buffer type chưa phải EDITABLE (không nên xảy ra
                // do đã setText(..., BufferType.EDITABLE) từ init/resetLogState), thiết lập lại.
                logView.setText("", TextView.BufferType.EDITABLE)
                editable = logView.editableText ?: return
            }

            val safeOldEnd = oldUiLength.coerceIn(0, editable.length)
            val safeFrom = replaceFrom.coerceIn(0, safeOldEnd)
            editable.replace(safeFrom, safeOldEnd, tail)

            // Tối ưu cuộn ScrollView xuống cuối.
            // Dùng OnPreDrawListener: onPreDraw() được gọi ngay TRƯỚC khi frame kế tiếp được vẽ,
            // tức là chắc chắn measure()/layout() cho nội dung mới đã hoàn tất, nên fullScroll()
            // luôn tính đúng theo chiều cao mới nhất (không dùng số đo cũ/stale).
            findScrollViewAncestor(logView)?.let { scrollView ->
                if (!scrollView.isAttachedToWindow) return@let
                scrollView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        scrollView.viewTreeObserver.removeOnPreDrawListener(this)
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN)

                        // Giữ focus thông minh không cần lồng post{} nhiều tầng
                        val inputRow = inputRowRef.get()
                        val input = shellInputRef.get()
                        if (inputRow != null && inputRow.visibility == View.VISIBLE && input != null) {
                            if (!input.isFocused) input.requestFocus()
                        }
                        return true
                    }
                })
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
        // Đường dẫn tương đối bên trong context.filesDir, KHÔNG dùng path tuyệt đối
        // (vd: /data/user/0/com.tool.tree/files/home/usr/log/scroll_ngang)
        private const val WRAP_STATE_RELATIVE_PATH = "home/usr/log/scroll_ngang"

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