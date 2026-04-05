package com.tool.tree

import android.app.Activity
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
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import java.io.File

class AnswerActivity : Activity() {

    private val answerFile by lazy { File(cacheDir, "answer.log") }
    private lateinit var et: EditText
    private lateinit var root: LinearLayout

    private var isProcessed = false

    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable { sendNullAndFinish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (answerFile.exists()) answerFile.delete()
        setupWindow()

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

        // Thiết lập timeout
        timeoutHandler.postDelayed(timeoutRunnable, timeoutSeconds * 1000)

        // Xử lý vuốt trở lại (Gesture Back) và phím Back
        override fun onBackPressed() {
            sendNullAndFinish()
        }

        // Theo dõi layout để giữ bàn phím
        root.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            var lastHeight = 0
            override fun onGlobalLayout() {
                val r = Rect()
                root.getWindowVisibleDisplayFrame(r)
                val height = r.height()
                if (height != lastHeight) {
                    lastHeight = height
                    if (::et.isInitialized && !isProcessed) reActivateCursor()
                }
            }
        })
    }

    private fun setupWindow() {
        window.apply {
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            attributes = attributes.apply {
                gravity = Gravity.BOTTOM
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
            }
            // Cải thiện overlay và gesture
            // addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
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
                    layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
                        setMargins(5, 0, 5, 0)
                    }
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
            inputType = if (max != null) 
                android.text.InputType.TYPE_CLASS_NUMBER 
            else 
                android.text.InputType.TYPE_CLASS_TEXT
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
            if (id == EditorInfo.IME_ACTION_SEND) {
                send(et.text.toString(), max)
                true
            } else false
        }
    }

    private fun reActivateCursor() {
        if (isProcessed) return
        et.post {
            if (!et.hasFocus()) et.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun send(input: String, max: Int?) {
        if (isProcessed) return
        val text = input.trim()
        if (text.isEmpty()) {
            Toast.makeText(this, getString(R.string.do_not_empty), Toast.LENGTH_SHORT).show()
            return
        }

        isProcessed = true
        timeoutHandler.removeCallbacks(timeoutRunnable)

        try {
            val output = if (max != null) {
                val num = text.toIntOrNull()
                if (num != null && num in 0..max) num.toString() else text
            } else text

            answerFile.writeText(output)
            root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        finishWithCleanUp()
    }

    private fun sendNullAndFinish() {
        if (isProcessed) return
        isProcessed = true
        timeoutHandler.removeCallbacks(timeoutRunnable)

        try {
            answerFile.writeText("null")
            root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        finishWithCleanUp()
    }

    private fun finishWithCleanUp() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus ?: window.decorView
        imm.hideSoftInputFromWindow(view.windowToken, 0)

        finish()
        overridePendingTransition(0, 0)
    }

    // Xử lý thêm khi mất focus (rất quan trọng với Gesture Navigation)
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && !isProcessed && !isFinishing) {
            sendNullAndFinish()
        }
    }

    // override fun onPause() {
        // super.onPause()
        // if (!isFinishing && !isProcessed) {
            // sendNullAndFinish()
        // }
    // }

    override fun onDestroy() {
        timeoutHandler.removeCallbacks(timeoutRunnable)
        super.onDestroy()
    }
}