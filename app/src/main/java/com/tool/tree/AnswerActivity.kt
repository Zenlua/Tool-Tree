package com.tool.tree

import androidx.activity.ComponentActivity
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import java.io.File
import androidx.activity.addCallback

class AnswerActivity : ComponentActivity() {

    private val answerFile by lazy { File(cacheDir, "answer.log") }
    private lateinit var et: EditText
    private lateinit var root: LinearLayout
    
    // Biến quan trọng để kiểm soát việc ghi file
    private var isProcessed = false
    
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable { sendNullAndFinish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Luôn làm sạch file cũ khi khởi tạo
        if (answerFile.exists()) { answerFile.delete() }
        setupWindow()

        // Lớp bảo vệ 1: Xử lý phím Back/Vuốt Back truyền thống
        onBackPressedDispatcher.addCallback(this) {
            sendNullAndFinish()
        }

        val answerData = intent.getStringExtra("answer")
        val max = intent.getStringExtra("max")?.toIntOrNull()
        val timeoutSeconds = intent.getStringExtra("time")?.toLongOrNull() ?: 20L

        val isDark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES

        val textColor = if (isDark) Color.WHITE else Color.BLACK
        val bgColor = if (isDark) Color.parseColor("#2A2A2A") else Color.parseColor("#F5F5F5")

        root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(bgColor)
            gravity = Gravity.CENTER_VERTICAL
        }

        if (answerData != null && answerData.contains("|")) {
            setupQuickButtons(answerData, max)
        } else {
            setupDefaultInput(textColor, isDark, max)
        }

        setContentView(root)

        // Kích hoạt đếm ngược tự động tắt
        timeoutHandler.postDelayed(timeoutRunnable, timeoutSeconds * 1000)

        root.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            var last = 0
            override fun onGlobalLayout() {
                val r = Rect()
                root.getWindowVisibleDisplayFrame(r)
                val h = r.height()
                if (h != last) {
                    last = h
                    if (::et.isInitialized) reActivateCursor()
                }
            }
        })
    }

    // Lớp bảo vệ 2: Chốt chặn cuối cùng khi Activity bị đóng/ẩn (Vuốt Back cử chỉ)
    override fun onPause() {
        super.onPause()
        if (!isProcessed) {
            // Nếu người dùng vuốt thoát mà chưa nhấn gửi, ghi null luôn
            sendNullAndFinish()
        }
    }

    private fun setupQuickButtons(data: String, max: Int?) {
        val parts = data.split("|")
        parts.forEach { part ->
            val pair = part.split(":", limit = 2)
            if (pair.size == 2) {
                val resultValue = pair[0].trim()
                val label = pair[1].trim()
                val btn = Button(this).apply {
                    text = label
                    layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(5, 0, 5, 0) }
                    setOnClickListener { send(resultValue, max) }
                }
                root.addView(btn)
            }
        }
    }

    private fun setupDefaultInput(textColor: Int, isDark: Boolean, max: Int?) {
        et = EditText(this).apply {
            hint = getString(R.string.hint_answer)
            setTextColor(textColor)
            setHintTextColor(if (isDark) Color.LTGRAY else Color.GRAY)
            backgroundTintList = ColorStateList.valueOf(textColor)
            imeOptions = EditorInfo.IME_ACTION_SEND
            inputType = if (max != null) android.text.InputType.TYPE_CLASS_NUMBER else android.text.InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            isFocusable = true
            isFocusableInTouchMode = true
        }

        val btn = Button(this).apply {
            text = getString(R.string.btn_send)
            setOnClickListener { send(et.text.toString(), max) }
        }
        root.addView(et)
        root.addView(btn)

        et.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEND) { send(et.text.toString(), max); true } else false
        }
    }

    private fun reActivateCursor() {
        et.post {
            if (!et.hasFocus()) et.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun setupWindow() {
        window.apply {
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            attributes = attributes.apply {
                gravity = Gravity.BOTTOM
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
            }
        }
    }

    private fun send(input: String, max: Int?) {
        if (isProcessed) return
        val text = input.trim()
        if (text.isEmpty()) {
            toast(getString(R.string.do_not_empty))
            return
        }

        isProcessed = true // Đánh dấu đã xử lý thành công
        timeoutHandler.removeCallbacks(timeoutRunnable)
        
        try {
            val finalVal = if (max != null) {
                val num = text.toIntOrNull()
                if (num != null && num in 0..max) num.toString() else text
            } else text
            answerFile.writeText(finalVal)
        } catch (e: Exception) { e.printStackTrace() }
        
        finish()
    }

    private fun sendNullAndFinish() {
        if (isProcessed) return
        isProcessed = true
        
        timeoutHandler.removeCallbacks(timeoutRunnable)
        
        // Ẩn phím ngay lập tức
        if (::et.isInitialized) et.clearFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)

        try {
            answerFile.writeText("null")
        } catch (e: Exception) { e.printStackTrace() }
        
        finish()
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        timeoutHandler.removeCallbacks(timeoutRunnable)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
