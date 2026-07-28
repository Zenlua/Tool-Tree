package com.omarea.krscript.ui

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.text.Editable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
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

        // Cho phép ấn vào các URLSpan (hyperlink OSC 8 hoặc URL trần) trong log để mở trình duyệt.
        // Đặt sau khi view đã inflate với textIsSelectable="true" để không bị ghi đè
        // bởi movement method mặc định của chế độ chọn văn bản.
        binding.shellOutput.movementMethod = LinkMovementMethod.getInstance()

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
                binding.chooseRow.visibility = View.GONE
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
                    b.chooseRow.visibility = View.GONE
                    hideKeyboard(b.shellInput)
                }
                isCancelable = true
            }
        }, binding.shellOutput, binding.actionProgress, binding.inputRow, binding.shellInput, binding.chooseRow, binding.chooseOptionsContainer)

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
                it.isFillViewport = true
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

    /**
     * Span dùng để hiển thị 1 đáp án của "choose:[...]" dưới dạng link có thể ấn NGAY TRONG
     * log (tương tự URLSpan cho hyperlink), bên cạnh cách chọn bằng nút bấm đã có sẵn.
     * Không giữ tham chiếu tới View/Editable - chỉ gọi callback [onTap] với giá trị tương ứng,
     * để nơi gọi (onChooseRequest) tự quyết định phải làm gì tiếp theo (ghi stdin, cập nhật
     * giao diện...). Nhờ vậy có thể tái sử dụng đúng 1 luồng xử lý dùng chung cho cả nút bấm
     * lẫn link trong log.
     */
    private class ChoiceSpan(val value: String, private val onTap: (String) -> Unit) : ClickableSpan() {
        override fun onClick(widget: View) {
            onTap(value)
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = true
        }
    }

    class MyShellHandler(
        context: Context,
        private var actionEventHandler: IActionEventHandler?,
        logView: TextView?,
        shellProgress: ProgressBar?,
        inputRow: View? = null,
        shellInput: EditText? = null,
        chooseRow: View? = null,
        chooseOptionsContainer: LinearLayout? = null
    ) : ShellHandlerBase(context) {

        private val logViewRef = WeakReference(logView)
        private val progressRef = WeakReference(shellProgress)
        private val inputRowRef = WeakReference(inputRow)
        private val shellInputRef = WeakReference(shellInput)
        private val chooseRowRef = WeakReference(chooseRow)
        private val chooseOptionsContainerRef = WeakReference(chooseOptionsContainer)

        private val errorColor = getColor(R.color.kr_shell_log_error)
        private val basicColor = getColor(R.color.kr_shell_log_basic)
        private val scriptColor = getColor(R.color.kr_shell_log_script)
        private val endColor = getColor(R.color.kr_shell_log_end)

        // Màu cho các đáp án "choose:[...]" hiển thị dạng link ngay trong log (giống URL):
        // - choiceLinkColor: màu đáp án còn có thể ấn (chưa trả lời)
        // - choiceAnsweredColor: màu đáp án ĐÃ chọn, dùng để đánh dấu kết quả
        // - choiceDisabledColor: màu các đáp án còn lại (không được chọn) sau khi đã trả lời
        private val choiceLinkColor = Color.parseColor("#4FC3F7")
        private val choiceAnsweredColor = Color.parseColor("#7CFC00")
        private val choiceDisabledColor = Color.parseColor("#808080")
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
            chooseRowRef.clear()
            chooseOptionsContainerRef.clear()
            unbindStdin()
            actionEventHandler = null
        }

        override fun onInputRequest(prompt: String) {
            val logView = logViewRef.get()
            val row = inputRowRef.get() ?: return
            val input = shellInputRef.get()
            (logView ?: row).post {
                chooseRowRef.get()?.visibility = View.GONE
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

        /**
         * Hiện các nút bấm tương ứng với từng phương án script yêu cầu chọn (cú pháp
         * "choose:[giá_trị|nhãn,...]"). Ấn vào 1 nút sẽ ghi thẳng [ChoiceOption.value] vào
         * stdin (giống như gõ tay rồi nhấn Enter), để lệnh `read` trong script nhận được kết quả.
         *
         * Cách bố trí: TÍNH TRƯỚC xem nếu chia đều chiều rộng khả dụng cho tất cả phương án
         * (kiểu weight=1, giống hệt 2 nút "Sao chép"/"Hủy bỏ" bên dưới) thì mỗi nút có đủ rộng
         * tối thiểu để đọc được không (>= MIN_COMFORTABLE_WIDTH_DP):
         *   - Đủ rộng (ít đáp án) -> dùng chế độ "lấp đầy": mỗi nút weight=1, tự co giãn theo
         *     tổng số đáp án, luôn lấp đầy trọn chiều ngang, không cuộn.
         *   - Không đủ rộng (quá nhiều đáp án) -> chuyển sang chế độ "chip": mỗi nút chỉ rộng
         *     vừa đủ nội dung (wrap_content), xếp cạnh nhau và cho phép cuộn ngang.
         */
        override fun onChooseRequest(options: MutableList<ChoiceOption>) {
            val logView = logViewRef.get()
            val container = chooseOptionsContainerRef.get() ?: return
            val row = chooseRowRef.get() ?: return
            val measureView = logView ?: row

            val gapPx = dpToPx(8f)
            // Ngưỡng chiều rộng tối thiểu (khi chia đều) để còn dùng chế độ "lấp đầy" thay vì
            // co về chế độ "chip". Đặt bằng đúng android:minWidth (40dp) của dialogChoiceBtn -
            // đây là giới hạn thấp nhất để nút còn đọc được nội dung, cho phép nhiều đáp án hơn
            // (khoảng 7 thay vì 5 trước đây) vẫn được chia đều lấp đầy thay vì co lại thành chip.
            val minComfortableWidthPx = dpToPx(40f)
            val buttonHeightPx = dpToPx(40f)

            // Người dùng có 2 cách trả lời: ấn nút bên dưới, HOẶC ấn thẳng vào đáp án hiển thị
            // dạng link ngay trong log. Cờ này đảm bảo dù chọn cách nào trước, đáp án cũng chỉ
            // được gửi (writeInput) đúng 1 lần, và cách còn lại sẽ được vô hiệu hoá/cập nhật
            // giao diện ngay sau đó.
            val answered = AtomicBoolean(false)
            val choiceSpans = mutableListOf<ChoiceSpan>()

            fun onAnswered(value: String) {
                if (!answered.compareAndSet(false, true)) return
                writeInput(value)
                row.visibility = View.GONE
                container.removeAllViews()

                // Vô hiệu hoá + tô lại màu các link trong log: đáp án vừa chọn được tô sáng,
                // các đáp án còn lại chuyển màu xám (không còn ấn được vì đã gỡ ClickableSpan).
                val editable = logViewRef.get()?.editableText ?: return
                for (span in choiceSpans) {
                    val start = editable.getSpanStart(span)
                    val end = editable.getSpanEnd(span)
                    if (start < 0 || end < 0) continue
                    editable.removeSpan(span)
                    editable.setSpan(
                        ForegroundColorSpan(if (span.value == value) choiceAnsweredColor else choiceDisabledColor),
                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            fun buildButtons(availableWidth: Int) {
                inputRowRef.get()?.visibility = View.GONE
                container.removeAllViews()

                val count = options.size
                if (count == 0) return

                val perButtonIfFilled = if (availableWidth > 0) {
                    (availableWidth - gapPx * (count - 1)) / count
                } else {
                    0
                }
                val useFillMode = availableWidth > 0 && perButtonIfFilled >= minComfortableWidthPx

                // LƯU Ý: container nằm trong HorizontalScrollView (choose_row). Bình thường một
                // HorizontalScrollView LUÔN đo con của nó theo UNSPECIFIED (bỏ qua layout_width
                // của con dù là match_parent hay 1 số px cụ thể) -> con tự co về kích thước nội
                // dung -> các nút weight=1 bên trong không có gì để chia, co về minWidth (tròn
                // nhỏ, dồn 1 góc). Phải bật android:fillViewport="true" trên choose_row (đã sửa
                // trong kr_dialog_log.xml) để ScrollView tự đo lại con bằng EXACTLY(viewport)
                // khi con nhỏ hơn viewport -> lúc đó match_parent ở đây mới thực sự có tác dụng.
                container.layoutParams = FrameLayout.LayoutParams(
                    if (useFillMode) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                for ((index, option) in options.withIndex()) {
                    val button = Button(row.context, null, 0, R.style.dialogChoiceBtn).apply {
                        text = option.label
                        layoutParams = if (useFillMode) {
                            // Chia đều trọn chiều ngang, tự co giãn theo số lượng đáp án
                            LinearLayout.LayoutParams(0, buttonHeightPx, 1f)
                        } else {
                            // Quá nhiều đáp án: mỗi nút chỉ rộng vừa đủ nội dung, cho cuộn ngang
                            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, buttonHeightPx)
                        }.also {
                            if (index < count - 1) it.marginEnd = gapPx
                        }
                        setOnClickListener {
                            onAnswered(option.value)
                        }
                    }
                    container.addView(button)
                }
                row.visibility = View.VISIBLE
            }

            // Yêu cầu "choose" có thể tới ngay khi script vừa khởi động, tức là TRƯỚC KHI
            // Dialog hoàn tất lượt layout đầu tiên. Lúc đó measureView.width vẫn là 0, và một
            // post{} DUY NHẤT là KHÔNG ĐỦ TIN CẬY: nếu view chưa attach vào window, runnable
            // post() sẽ được chạy ngay tại thời điểm attach - tức là VẪN TRƯỚC lượt đo/layout
            // đầu tiên -> width vẫn đọc ra 0, khiến hệ thống luôn rơi vào "chế độ chip" (nút
            // tròn nhỏ dồn 1 góc) dù có đủ chỗ trống để lấp đầy. Do đó cần chờ bằng
            // OnGlobalLayoutListener, chỉ build nút SAU KHI đã đo được width thật (> 0), thay vì
            // build ngay dựa trên width = 0.
            if (measureView.width > 0) {
                buildButtons(measureView.width)
            } else {
                val vto = measureView.viewTreeObserver
                vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (measureView.width > 0) {
                            if (vto.isAlive) {
                                vto.removeOnGlobalLayoutListener(this)
                            }
                            buildButtons(measureView.width)
                        }
                        // Nếu width vẫn = 0, giữ listener lại chờ lượt layout kế tiếp.
                    }
                })
                // Chủ động yêu cầu 1 lượt layout, phòng trường hợp view đã attach nhưng chưa
                // có lượt layout nào được lên lịch (ví dụ view vừa addView() xong).
                measureView.requestLayout()
            }

            // Đồng thời chèn các đáp án dạng link ngay trong log (giống hyperlink URL), đi qua
            // ĐÚNG cơ chế dispatchLogUpdate() như mọi dòng log khác, để logBuffer/uiAppliedLength
            // luôn nhất quán - không thao tác trực tiếp lên editableText ở đây.
            val builder = SpannableStringBuilder()
            builder.append('\n')
            for (option in options) {
                val start = builder.length
                builder.append(option.label)
                val end = builder.length
                val span = ChoiceSpan(option.value) { value -> onAnswered(value) }
                choiceSpans.add(span)
                builder.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder.setSpan(ForegroundColorSpan(choiceLinkColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder.append('\n')
            }
            dispatchLogUpdate(builder)
        }

        /**
         * Hiện các đáp án dưới dạng LINK ngay trong log (không có nút bấm riêng), cho cú pháp
         * "pick:[...]" / "pickv:[...]" (dọc) / "pickh:[...]" (ngang). Tách biệt hoàn toàn với
         * onChooseRequest() ở trên - không tạo nút, không dùng chooseRow/chooseOptionsContainer.
         */
        override fun onPickRequest(options: MutableList<ChoiceOption>, vertical: Boolean) {
            val answered = AtomicBoolean(false)
            val choiceSpans = mutableListOf<ChoiceSpan>()

            fun onAnswered(value: String) {
                if (!answered.compareAndSet(false, true)) return
                writeInput(value)

                val editable = logViewRef.get()?.editableText ?: return
                for (span in choiceSpans) {
                    val start = editable.getSpanStart(span)
                    val end = editable.getSpanEnd(span)
                    if (start < 0 || end < 0) continue
                    editable.removeSpan(span)
                    editable.setSpan(
                        ForegroundColorSpan(if (span.value == value) choiceAnsweredColor else choiceDisabledColor),
                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            fun appendChoice(builder: SpannableStringBuilder, text: String, value: String) {
                val start = builder.length
                builder.append(text)
                val end = builder.length
                val span = ChoiceSpan(value) { v -> onAnswered(v) }
                choiceSpans.add(span)
                builder.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder.setSpan(ForegroundColorSpan(choiceLinkColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            val builder = SpannableStringBuilder()
            builder.append('\n')
            if (vertical) {
                // Xếp DỌC: mỗi đáp án 1 dòng, dạng "1. Nhãn", cách nhau 1 dòng trống để tạo
                // vùng chạm đủ lớn trên di động. Toàn bộ "1. Nhãn" (kể cả số thứ tự) đều bấm được.
                options.forEachIndexed { index, option ->
                    appendChoice(builder, "${index + 1}. ${option.label}", option.value)
                    builder.append("\n\n")
                }
            } else {
                // Xếp NGANG: dạng "[ Nhãn ]" nối cạnh nhau trên cùng 1 dòng.
                options.forEachIndexed { index, option ->
                    appendChoice(builder, "[ ${option.label} ]", option.value)
                    if (index != options.lastIndex) builder.append("  ")
                }
                builder.append('\n')
            }
            dispatchLogUpdate(builder)
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

            // Lệnh `clear` (hoặc chương trình vẽ lại 1 phần màn hình như progress bar) sẽ phát ra
            // mã CSI "Erase in Display" (ESC[nJ), được AnsiColorParser nhận diện ở trên. Vì log
            // buffer ở đây chỉ append tuần tự (không theo dõi toạ độ cursor thật như terminal),
            // ta xấp xỉ "vị trí cursor" như sau:
            // - Nếu vừa gặp '\r' (pendingOverwrite = true, đang chờ ghi đè dòng hiện tại) -> cursor
            //   coi như đang ở ĐẦU dòng hiện tại (lineStart).
            // - Ngược lại (append bình thường, chưa ghi đè gì) -> cursor coi như đang ở CUỐI buffer.
            // Xấp xỉ này khớp chính xác với các chương trình dùng '\r' để vẽ lại dòng (progress
            // bar, spinner...), nhưng sẽ không chính xác nếu chương trình dùng CUP (ESC[r;cH) để
            // di chuyển cursor tự do - các mã di chuyển cursor này hiện chưa được theo dõi.
            when (AnsiColorParser.consumePendingErase()) {
                2, 3 -> {
                    // Xoá toàn bộ màn hình (lệnh `clear`)
                    synchronized(logBuffer) {
                        logBuffer.clear()
                        lineCount = 0
                        lineStart = 0
                        pendingOverwrite = false
                        markInvalidFrom(0)
                    }
                }
                0 -> {
                    // Xoá từ cursor (xấp xỉ) tới hết buffer
                    synchronized(logBuffer) {
                        val cursorPos = (if (pendingOverwrite) lineStart else logBuffer.length)
                            .coerceIn(0, logBuffer.length)
                        if (cursorPos < logBuffer.length) {
                            logBuffer.delete(cursorPos, logBuffer.length)
                            markInvalidFrom(cursorPos)
                        }
                        pendingOverwrite = false
                    }
                }
                1 -> {
                    // Xoá từ đầu buffer tới cursor (xấp xỉ). Vì phần đầu bị xoá, mọi toạ độ phía
                    // sau đều dịch chuyển - xử lý tương tự logic "cắt tỉa 5000 dòng" bên dưới.
                    synchronized(logBuffer) {
                        val cursorPos = (if (pendingOverwrite) lineStart else logBuffer.length)
                            .coerceIn(0, logBuffer.length)
                        if (cursorPos > 0) {
                            var removedLines = 0
                            for (k in 0 until cursorPos) {
                                if (logBuffer[k] == '\n') removedLines++
                            }
                            logBuffer.delete(0, cursorPos)
                            lineCount = (lineCount - removedLines).coerceAtLeast(0)
                            lineStart = (lineStart - cursorPos).coerceAtLeast(0)
                            uiAppliedLength = (uiAppliedLength - cursorPos).coerceAtLeast(0)
                            uiInvalidFrom = 0
                        }
                        pendingOverwrite = false
                    }
                }
                else -> {}
            }

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