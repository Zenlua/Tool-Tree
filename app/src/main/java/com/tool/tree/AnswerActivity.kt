package com.tool.tree

import android.app.Activity
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.os.*
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
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
        val bgColor = if (isDark) 0xFF2A2A2A.toInt() else 0xFFF5F5F5.toInt()

        root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(bgColor)
            gravity = Gravity.CENTER_VERTICAL
            isFocusable = true
            isFocusableInTouchMode = true
        }

        if (answerData?.contains("|") == true) {
            setupQuickButtons(answerData, max)
        } else {
            setupDefaultInput(textColor, isDark, max)
        }

        setContentView(root)

        timeoutHandler.postDelayed(timeoutRunnable, timeoutSeconds * 1000)

        observeLayout()
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

    private fun setupQuickButtons(data: String, max: Int?) {
        data.split("|").forEach { part ->
            val pair = part.split(":", limit = 2)
            if (pair.size != 2) return@forEach

            val value = pair[0].trim()
            val label = pair[1].trim()

            val btn = Button(this).apply {
                text = label
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
                    setMargins(5, 0, 5, 0)
                }
                setOnClickListener { send(value, max) }
            }

            root.addView(btn)
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

    private fun observeLayout() {
        root.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {

            private var lastHeight = 0

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

    private fun reActivateCursor() {
        et.post {
            if (!et.hasFocus()) et.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun send(input: String, max: Int?) {
        if (isProcessed) return
        val trimmedInput = input.trim()
        val result = if (trimmedInput.isEmpty()) "null" else trimmedInput
        isProcessed = true
        timeoutHandler.removeCallbacks(timeoutRunnable)
        try {
            val output = if (max != null && result != "null") {
                val num = result.toIntOrNull()
                if (num != null && num in 0..max) num.toString() else result
            } else {
                result
            }
            answerFile.writeText(output)
            root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } catch (_: Exception) {
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
        } catch (_: Exception) {}

        finishWithCleanUp()
    }

    private fun finishWithCleanUp() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus ?: window.decorView
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        finish()
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        isProcessed = true
        timeoutHandler.removeCallbacks(timeoutRunnable)
        super.onDestroy()
    }

}