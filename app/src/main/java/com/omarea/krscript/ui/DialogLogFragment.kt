package com.omarea.krscript.ui

import android.app.Dialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.omarea.common.ui.DialogHelper
import com.omarea.krscript.config.IconPathAnalysis
import com.omarea.krscript.executor.ShellExecutor
import com.omarea.krscript.model.RunnableNode
import com.omarea.krscript.model.ShellHandlerBase
import com.tool.tree.AnsiColorParser
import com.tool.tree.R
import com.tool.tree.WakeLockService
import com.tool.tree.databinding.KrDialogLogBinding
import android.app.ActivityManager
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = KrDialogLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val style = if (themeResId != 0) themeResId else R.style.kr_full_screen_dialog_light
        return Dialog(requireActivity(), style)
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
        binding.shellOutput.movementMethod = LinkMovementMethod.getInstance()

        wrapEnabled = readWrapEnabled(requireContext().applicationContext)
        applyWrapState()

        setupClickListeners(
            onForceStop = { forceStopRunnable },
            nodeInfo = nodeInfo
        )

        val handler = MyShellHandler(
            context = requireContext().applicationContext,
            actionEventHandler = object : IActionEventHandler {
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
                        val transition = ChangeBounds().apply { duration = 200 }
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
            },
            logView = binding.shellOutput,
            shellProgress = binding.actionProgress,
            inputRow = binding.inputRow,
            shellInput = binding.shellInput,
            chooseRow = binding.chooseRow,
            chooseOptionsContainer = binding.chooseOptionsContainer
        )

        this.currentHandler = handler
        return handler
    }

    private fun setupClickListeners(onForceStop: () -> Runnable?, nodeInfo: RunnableNode) {
        val nodeInterruptable = nodeInfo.interruptable

        binding.btnWrap.setOnClickListener {
            wrapEnabled = !wrapEnabled
            applyWrapState()
            persistWrapEnabled(requireContext().applicationContext, wrapEnabled)
        }

        binding.btnHide.setOnClickListener {
            uiVisible = false
            // Đẩy toàn bộ log đã có + log tiếp theo (đến khi script kết thúc) lên thông báo
            // tiến trình hệ thống, giống cách BgTaskThread.ServiceShellHandler hiển thị, để
            // người dùng vẫn theo dõi được tiến trình dù đã đóng dialog.
            currentHandler?.enableNotificationMode(nodeInfo)
            offScreen()
            closeView()
        }

        binding.btnCancel.setOnClickListener {
            if (running && !canceled) {
                canceled = true
                onForceStop()?.run()
                binding.btnExit.visibility = View.VISIBLE
                binding.btnCancel.visibility = View.GONE
            }
        }

        binding.btnExit.setOnClickListener {
            isCancelable = true
            closeView()
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
            copyLogToClipboard()
        }

        if (nodeInterruptable) {
            binding.btnHide.visibility = View.VISIBLE
            binding.btnCancel.visibility = View.VISIBLE
        } else {
            binding.btnHide.visibility = View.GONE
            binding.btnCancel.visibility = View.GONE
        }

        binding.title.text = nodeInfo?.title?.takeIf { it.isNotEmpty() } ?: run {
            binding.title.visibility = View.GONE
            ""
        }
        binding.desc.text = nodeInfo?.desc?.takeIf { it.isNotEmpty() } ?: run {
            binding.desc.visibility = View.GONE
            ""
        }
        binding.actionProgress.isIndeterminate = true
    }

    private fun sendUserInput() {
        val text = binding.shellInput.text?.toString().orEmpty()
        if (text.isEmpty()) return

        if (currentHandler?.writeInput(text) == true) {
            binding.shellInput.setText("")
        } else {
            Toast.makeText(requireContext(), getString(R.string.input_send_fail), Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyLogToClipboard() {
        runCatching {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("text", binding.shellOutput.text.toString())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(requireContext(), getString(R.string.copy_fail), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Áp dụng trạng thái bật/tắt soft wrap cho log output:
     * - wrapEnabled = true: shellOutput nằm trực tiếp trong ScrollView (cuộn dọc), tự động xuống dòng.
     * - wrapEnabled = false: shellOutput được bọc trong HorizontalScrollView, cho phép cuộn ngang.
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

    private fun readWrapEnabled(context: Context): Boolean {
        return runCatching {
            val file = File(context.filesDir, WRAP_STATE_RELATIVE_PATH)
            if (file.exists()) file.readText().trim() != "1" else true
        }.getOrDefault(true)
    }

    private fun persistWrapEnabled(context: Context, wrapEnabled: Boolean) {
        IO_EXECUTOR.execute {
            runCatching {
                val file = File(context.filesDir, WRAP_STATE_RELATIVE_PATH)
                file.parentFile?.takeIf { !it.exists() }?.mkdirs()
                file.writeText(if (wrapEnabled) "0" else "1")
            }
        }
    }

    private fun hideKeyboard(view: View) {
        runCatching {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    @FunctionalInterface
    interface IActionEventHandler {
        fun onStart(forceStop: Runnable?)
        fun onSuccess()
        fun onCompleted()
    }

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

        private val choiceLinkColor = Color.parseColor("#4FC3F7")
        private val choiceAnsweredColor = Color.parseColor("#7CFC00")
        private val choiceDisabledColor = Color.parseColor("#808080")
        private var hasError = false
        private var lineCount = 0

        private val logBuffer = SpannableStringBuilder()
        private var lineStart = 0
        private var pendingOverwrite = false

        private var uiAppliedLength = 0
        private var uiInvalidFrom = Int.MAX_VALUE

        private val pendingUiUpdate = AtomicBoolean(false)

        // === Notification mode (kích hoạt khi ấn btnHide) ===
        // Khi bật, toàn bộ log (đã có sẵn trong logBuffer + log phát sinh về sau) sẽ được đẩy
        // vào 1 thông báo tiến trình hệ thống, dùng chung layout/kiểu hiển thị với
        // BgTaskThread.ServiceShellHandler, để người dùng vẫn theo dõi được dù đã đóng dialog.
        private var notificationMode = false
        private var notificationId = 0
        private var notificationManager: NotificationManager? = null
        private var notificationTitle = ""
        private var notificationInterruptable = false
        private val notificationRows = ArrayList<String>()
        private var notificationRowsTrimmed = false
        private var notificationShortMsg = ""
        private var notificationFinished = false
        private var notificationProgressCurrent = 0
        private var notificationProgressTotal = 0
        private var forceStopRunnable: Runnable? = null
        private var stopActionName: String? = null
        private var stopReceiver: BroadcastReceiver? = null
        private var stopPendingIntent: PendingIntent? = null

        // Icon riêng của tác vụ (nếu cấu hình có khai báo), dùng làm large icon của thông báo;
        // nếu không có thì rơi về icon app. Chỉ đọc từ đĩa 1 lần lúc bật notification mode.
        private var notificationLargeIcon: Bitmap? = null

        // Nút "Kết thúc": hiện khi shell đã chạy xong, bấm vào để đóng hẳn thông báo.
        private var dismissActionName: String? = null
        private var dismissReceiver: BroadcastReceiver? = null
        private var dismissPendingIntent: PendingIntent? = null

        init {
            logView?.setText("", TextView.BufferType.EDITABLE)
        }

        /**
         * Được gọi khi người dùng ấn btnHide: chuyển sang chế độ đẩy log lên thông báo tiến
         * trình hệ thống thay vì hiển thị trong dialog (dialog sắp bị đóng). Có thể gọi an
         * toàn nhiều lần, chỉ lần đầu có tác dụng.
         */
        fun enableNotificationMode(nodeInfo: RunnableNode) {
            if (notificationMode) return
            notificationMode = true

            notificationTitle = nodeInfo.title
            notificationInterruptable = nodeInfo.interruptable
            notificationShortMsg = context.getString(R.string.kr_script_task_running)
            notificationId = nextNotificationId()
            notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationLargeIcon = loadNotificationLargeIcon(nodeInfo)

            // Lấy toàn bộ log đã hiển thị trong dialog làm nội dung khởi đầu cho thông báo
            val existingLines = synchronized(logBuffer) {
                logBuffer.toString().split("\n").filter { it.isNotEmpty() }
            }
            synchronized(notificationRows) {
                existingLines.forEach { notificationRows.add("$it\n") }
                trimNotificationRows()
            }

            if (nodeInfo.interruptable) {
                val actionName = context.packageName + ".TaskStop.Hide." + notificationId
                stopActionName = actionName
                stopPendingIntent = PendingIntent.getBroadcast(
                    context, 0, Intent(actionName),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                stopReceiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context?, intent: Intent?) {
                        forceStopRunnable?.run()
                    }
                }
                // Android 13+ (API 33) bắt buộc phải khai báo rõ RECEIVER_EXPORTED/NOT_EXPORTED,
                // thiếu cờ này registerReceiver() sẽ ném SecurityException — trước đây bị
                // runCatching nuốt mất lỗi âm thầm nên receiver không hề được đăng ký.
                runCatching {
                    ContextCompat.registerReceiver(context, stopReceiver, IntentFilter(actionName), ContextCompat.RECEIVER_NOT_EXPORTED)
                }
            }

            // Nút "Kết thúc" (chỉ dùng được sau khi shell chạy xong) — đóng hẳn thông báo và
            // dọn dẹp tất cả receiver. Cũng được dùng làm deleteIntent khi người dùng vuốt bỏ
            // thông báo, để luôn dọn dẹp dù người dùng không bấm nút.
            val dismissAction = context.packageName + ".TaskDismiss.Hide." + notificationId
            dismissActionName = dismissAction
            dismissPendingIntent = PendingIntent.getBroadcast(
                context, 0, Intent(dismissAction),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            dismissReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    notificationManager?.cancel(notificationId)
                    cleanupNotificationReceivers()
                }
            }
            runCatching {
                ContextCompat.registerReceiver(context, dismissReceiver, IntentFilter(dismissAction), ContextCompat.RECEIVER_NOT_EXPORTED)
            }

            updateNotification()
        }

        /** Icon riêng của node nếu có khai báo trong cấu hình, ngược lại dùng icon của app. */
        private fun loadNotificationLargeIcon(nodeInfo: RunnableNode): Bitmap? {
            if (nodeInfo.iconPath.isNotEmpty()) {
                runCatching { IconPathAnalysis().loadIcon(context, nodeInfo) }.getOrNull()?.let {
                    return drawableToBitmap(it)
                }
            }
            return runCatching { context.packageManager.getApplicationIcon(context.packageName) }
                .getOrNull()?.let { drawableToBitmap(it) }
        }

        private fun drawableToBitmap(drawable: Drawable): Bitmap? {
            if (drawable is BitmapDrawable && drawable.bitmap != null) return drawable.bitmap
            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
            return runCatching {
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }.getOrNull()
        }

        private fun cleanupNotificationReceivers() {
            runCatching { stopReceiver?.let { context.unregisterReceiver(it) } }
            stopReceiver = null
            runCatching { dismissReceiver?.let { context.unregisterReceiver(it) } }
            dismissReceiver = null
        }

        private fun trimNotificationRows() {
            // Lưu nhiều dòng hơn để khi mở rộng có đủ log hiển thị, nhưng vẫn giới hạn để
            // tránh Notification quá nặng.
            if (notificationRows.size > 20) {
                notificationRows.removeAt(0)
                notificationRowsTrimmed = true
            }
        }

        private fun appendNotificationRow(text: String) {
            synchronized(notificationRows) {
                notificationRows.add(text)
                trimNotificationRows()
            }
            updateNotification()
        }

        private fun updateNotification() {
            val nm = notificationManager ?: return
            val id = notificationId

            val shortLog = notificationRows.lastOrNull()?.trim().orEmpty()

            // Log đầy đủ, MỚI NHẤT LÊN TRÊN CÙNG để nếu bị Android cắt bớt do quá dài (khi mở
            // rộng) thì phần bị cắt luôn là log CŨ, còn log mới nhất luôn được giữ lại.
            val fullLog = run {
                val rows = synchronized(notificationRows) { notificationRows.toList() }
                val sb = StringBuilder()
                for (i in rows.indices.reversed()) sb.append(rows[i])
                if (notificationRowsTrimmed) sb.append("……\n")
                sb.toString().trim()
            }

            val notificationBuilder = Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(notificationTitle)
                .setContentText("$notificationShortMsg >> $shortLog")
                .setSmallIcon(R.drawable.kr_run)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                // Dùng style mặc định của Android: hệ thống tự vẽ mũi tên mở rộng và cho phép
                // ấn giữ/vuốt để xem toàn bộ log, không cần tự vẽ layout hay nút toggle riêng.
                .setStyle(
                    Notification.BigTextStyle()
                        .bigText(fullLog)
                        .setSummaryText(notificationShortMsg)
                )
            notificationLargeIcon?.let { notificationBuilder.setLargeIcon(it) }
            if (notificationProgressTotal != notificationProgressCurrent) {
                notificationBuilder.setProgress(notificationProgressTotal, notificationProgressCurrent, notificationProgressTotal < 0)
            }

            // Chạm vào thông báo (ngoài các nút) sẽ mở lại app, giống WakeLockService.
            buildContentPendingIntent()?.let { notificationBuilder.setContentIntent(it) }

            // Nút hành động hiển thị ở HÀNG DƯỚI CÙNG do hệ thống tự vẽ (giống WakeLockService).
            if (notificationFinished) {
                dismissPendingIntent?.let {
                    notificationBuilder.addAction(R.drawable.kr_close, context.getString(R.string.kr_task_notify_dismiss), it)
                }
            } else {
                stopPendingIntent?.let {
                    notificationBuilder.addAction(R.drawable.kr_cancel, context.getString(R.string.kr_task_notify_cancel), it)
                }
            }

            // Dọn dẹp receiver mỗi khi thông báo bị người dùng vuốt bỏ, kể cả khi họ không
            // bấm nút "Kết thúc" trước đó.
            dismissPendingIntent?.let { notificationBuilder.setDeleteIntent(it) }

            if (!notificationChannelCreated) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.kr_script_task_notification),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                channel.enableLights(false)
                channel.enableVibration(false)
                channel.setSound(null, null)
                nm.createNotificationChannel(channel)
                notificationChannelCreated = true
            }
            notificationBuilder.setChannelId(NOTIFICATION_CHANNEL_ID)

            val notification = notificationBuilder.build()
            if (!notificationFinished) {
                notification.flags = Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
            }
            nm.notify(id, notification)
        }

        /** Intent mở lại app khi chạm vào thông báo (không tính vùng nút), giống WakeLockService. */
        private fun buildContentPendingIntent(): PendingIntent? {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            } ?: return null
            return PendingIntent.getActivity(
                context, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }


        private fun finishNotification(success: Boolean, code: Int) {
            notificationFinished = true
            notificationShortMsg = context.getString(R.string.kr_script_task_finished)
            val finishText = if (success) {
                context.getString(R.string.kr_shell_completed)
            } else {
                "${context.getString(R.string.kr_shell_finish_error)} $code"
            }
            // Không huỷ đăng ký dismissReceiver ở đây — nút "Kết thúc" vẫn phải dùng được sau
            // khi task đã chạy xong; chỉ dọn dẹp khi người dùng thật sự đóng thông báo (bấm
            // "Kết thúc" hoặc vuốt bỏ), xem cleanupNotificationReceivers().
            appendNotificationRow("\n$finishText")
        }

        companion object {
            // Dùng riêng kênh thông báo cho chế độ "Ẩn" (btnHide), tách biệt với kênh của
            // BgTaskThread.ServiceShellHandler dù cùng nội dung hiển thị, để tránh phụ thuộc
            // trạng thái channelCreated giữa 2 class không liên quan tới nhau.
            private const val NOTIFICATION_CHANNEL_ID = "kr_script_task_notification_hide"
            private var notificationChannelCreated = false

            // Base ID tách khỏi dải ID mà BgTaskThread.notificationCounter (bắt đầu từ 34050)
            // sử dụng, để tránh 2 chế độ (chạy nền từ đầu / ấn Hide giữa chừng) đè thông báo lẫn nhau.
            private var notificationIdCounter = 54050

            @Synchronized
            private fun nextNotificationId(): Int {
                notificationIdCounter += 1
                return notificationIdCounter
            }
        }

        private fun getColor(resId: Int): Int = ContextCompat.getColor(context, resId)

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

        // Được gọi bởi ShellHandlerBase.killApp() ngay trước khi tiến trình bị kill (do
        // script echo "exit:[kill]" / "exit:[restart]"). Nếu không dọn dẹp ở đây, WakeLockService
        // (foreground service, onStartCommand() trả về START_STICKY) đang chạy nền sẽ bị hệ thống
        // coi là "chết bất thường" và TỰ ĐỘNG KHỞI ĐỘNG LẠI service đó -> tiến trình app bị hồi
        // sinh, gây cảm giác app tự restart thay vì thoát hẳn. Xử lý y hệt logic dọn dẹp đã dùng
        // ở ActionPage.killApp() / MainActivity (nhánh autoKill): dừng WakeLockService một cách
        // tường minh (stopSelf từ trong service) rồi đóng hết task, để tránh kích hoạt START_STICKY.
        override fun onKillRequest() {
            try {
                context.startService(Intent(context, WakeLockService::class.java).apply {
                    action = WakeLockService.ACTION_END_WAKELOCK
                })
            } catch (ignored: Exception) {
                // Service có thể không chạy hoặc context không cho phép startService lúc này -> bỏ qua
            }

            try {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                activityManager?.appTasks?.forEach { task -> task.finishAndRemoveTask() }
            } catch (ignored: Exception) {
                // Không để lỗi dọn task cản trở việc kill process phía sau
            }
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

        override fun onChooseRequest(options: MutableList<ChoiceOption>) {
            val logView = logViewRef.get()
            val container = chooseOptionsContainerRef.get() ?: return
            val row = chooseRowRef.get() ?: return
            val measureView = logView ?: row

            val gapPx = dpToPx(8f)
            val minComfortableWidthPx = dpToPx(40f)
            val buttonHeightPx = dpToPx(40f)

            val answered = AtomicBoolean(false)
            val choiceSpans = mutableListOf<ChoiceSpan>()

            fun onAnswered(value: String) {
                if (!answered.compareAndSet(false, true)) return
                writeInput(value)
                row.visibility = View.GONE
                container.removeAllViews()

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
                } else 0

                val useFillMode = availableWidth > 0 && perButtonIfFilled >= minComfortableWidthPx

                container.layoutParams = FrameLayout.LayoutParams(
                    if (useFillMode) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                for ((index, option) in options.withIndex()) {
                    val button = Button(row.context, null, 0, R.style.dialogChoiceBtn).apply {
                        text = option.label
                        layoutParams = if (useFillMode) {
                            LinearLayout.LayoutParams(0, buttonHeightPx, 1f)
                        } else {
                            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, buttonHeightPx)
                        }.also {
                            if (index < count - 1) it.marginEnd = gapPx
                        }
                        setOnClickListener { onAnswered(option.value) }
                    }
                    container.addView(button)
                }
                row.visibility = View.VISIBLE
            }

            if (measureView.width > 0) {
                buildButtons(measureView.width)
            } else {
                measureView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (measureView.width > 0) {
                            measureView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            buildButtons(measureView.width)
                        }
                    }
                })
                measureView.requestLayout()
            }

            val builder = SpannableStringBuilder().apply { append('\n') }
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

            val builder = SpannableStringBuilder().apply { append('\n') }
            if (vertical) {
                options.forEach { option ->
                    appendChoice(builder, option.label, option.value)
                    builder.append("\n\n")
                }
            } else {
                options.forEachIndexed { index, option ->
                    appendChoice(builder, option.label, option.value)
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
            // Bug: thiếu dòng này khiến nút "Hủy bỏ" trong thông báo gọi forceStopRunnable?.run()
            // trên 1 field luôn null -> bấm không có tác dụng gì, shell vẫn chạy tiếp.
            forceStopRunnable = forceStop
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

            // Đồng bộ tiến độ vào thông báo hệ thống (nếu đang ở chế độ ẩn dialog)
            notificationProgressCurrent = current
            notificationProgressTotal = total
            if (notificationMode) updateNotification()
        }

        override fun onStart(msg: Any?) {
            resetLogState()
        }

        override fun onExit(msg: Any?) {
            val code = (msg as? Int) ?: -1
            val success = !hasError && code == 0
            updateLogWithColor(context.getString(R.string.kr_shell_completed), endColor, pushToNotification = false)

            if (notificationMode) {
                finishNotification(success, code)
            }

            if (success) {
                actionEventHandler?.onSuccess()
            }
            actionEventHandler?.onCompleted()
        }

        override fun updateLog(msg: SpannableString?) {
            msg?.let { dispatchLogUpdate(it) }
        }

        private fun updateLogWithColor(text: String, forcedColor: Int?, pushToNotification: Boolean = true) {
            var parsedLog: CharSequence = AnsiColorParser.parse(text)

            if (forcedColor != null && !text.contains("\u001B[")) {
                parsedLog = SpannableStringBuilder(parsedLog).apply {
                    setSpan(
                        ForegroundColorSpan(forcedColor),
                        0,
                        length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            dispatchLogUpdate(parsedLog)

            // Nếu đang ở chế độ thông báo (đã ấn btnHide), tiếp tục đẩy log phát sinh mới
            // vào thông báo hệ thống, không chỉ log tại thời điểm bấm nút. Dòng log kết thúc
            // (onExit) được finishNotification() tự thêm riêng nên bỏ qua ở đây để tránh lặp.
            if (notificationMode && pushToNotification) {
                pushNotificationLog(text)
            }
        }

        /**
         * Tách text log mới thành các dòng và thêm vào thông báo hệ thống. Dùng chung logic
         * trim (giữ tối đa 8 dòng gần nhất) với phần khởi tạo trong enableNotificationMode().
         */
        private fun pushNotificationLog(text: String) {
            val lines = text.replace("\r", "\n").split("\n").filter { it.isNotEmpty() }
            if (lines.isEmpty()) return
            synchronized(notificationRows) {
                lines.forEach { notificationRows.add("$it\n") }
                trimNotificationRows()
            }
            updateNotification()
        }

        private fun markInvalidFrom(from: Int) {
            if (from < uiInvalidFrom) uiInvalidFrom = from
        }

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
                        uiInvalidFrom = 0
                    }
                }
            }

            if (pendingUiUpdate.compareAndSet(false, true)) {
                logView.post {
                    pendingUiUpdate.set(false)
                    flushToUi(logView)
                }
            }
        }

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
                } else null

                uiAppliedLength = logBuffer.length
                uiInvalidFrom = Int.MAX_VALUE
            }

            if (tail == null) return

            var editable = logView.editableText
            if (editable == null) {
                logView.setText("", TextView.BufferType.EDITABLE)
                editable = logView.editableText ?: return
            }

            val safeOldEnd = oldUiLength.coerceIn(0, editable.length)
            val safeFrom = replaceFrom.coerceIn(0, safeOldEnd)
            editable.replace(safeFrom, safeOldEnd, tail)

            findScrollViewAncestor(logView)?.let { scrollView ->
                if (!scrollView.isAttachedToWindow) return@let
                scrollView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        scrollView.viewTreeObserver.removeOnPreDrawListener(this)
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN)

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

    private fun offScreen() {
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun closeView() {
        runCatching { dismissAllowingStateLoss() }
    }

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
        private const val WRAP_STATE_RELATIVE_PATH = "home/usr/log/scroll_ngang"
        private val IO_EXECUTOR = Executors.newSingleThreadExecutor()

        fun create(
            nodeInfo: RunnableNode,
            onExit: Runnable,
            onDismiss: Runnable,
            script: String,
            params: HashMap<String, String>?,
            darkMode: Boolean = false
        ): DialogLogFragment {
            return DialogLogFragment().apply {
                this.nodeInfo = nodeInfo
                this.onExit = onExit
                this.script = script
                this.params = params
                this.themeResId = if (darkMode) R.style.kr_full_screen_dialog_dark else R.style.kr_full_screen_dialog_light
                this.onDismissRunnable = onDismiss
            }
        }
    }
}