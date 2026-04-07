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
    private lateinit var root: LinearLayout
    private lateinit var et: EditText
    private var isProcessed = false
    private var hasInput = false
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable { finishWithResult("null") }
    private var layoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (answerFile.exists()) answerFile.delete()
        setupWindow()

        val answerData = intent.getStringExtra("answer")
        val max = intent.getStringExtra("max")?.toIntOrNull()
        val timeoutSeconds = intent.getStringExtra("time")?.toLongOrNull() ?: 20L
        val isDark = ThemeModeState.isDarkMode()
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
            val index = part.indexOf(":")
            if (index == -1) return@forEach
            val value = part.substring(0, index).trim()
            val label = part.substring(index + 1).trim()
            val btn = Button(this).apply {
                text = label
                layoutParams = LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f).apply {
                    setMargins(5, 0, 5, 0)
                }
                setOnClickListener { send(value, max) }
            }
            root.addView(btn)
        }
    }

    private fun setupDefaultInput(textColor: Int, isDark: Boolean, max: Int?) {
        hasInput = true
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
            layoutParams = LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f)
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
        layoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
            private var lastHeight = 0
            override fun onGlobalLayout() {
                val r = Rect()
                root.getWindowVisibleDisplayFrame(r)
                val height = r.height()
                if (height != lastHeight) {
                    lastHeight = height
                    if (hasInput && !isProcessed) reActivateCursor()
                }
            }
        }
        root.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
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
        val text = input.trim()
        if (text.isEmpty()) {
            finishWithResult("null")
            return
        }
        if (max != null) {
            val num = text.toIntOrNull()
            if (num == null || num !in 0..max) {
                Toast.makeText(this,getString(R.string.toast_out_of_range, 0, max),Toast.LENGTH_SHORT).show()
                return
            }
        }
        finishWithResult(text)
    }

    private fun finishWithResult(content: String) {
        if (isProcessed) return
        isProcessed = true
        timeoutHandler.removeCallbacks(timeoutRunnable)
        try {
            answerFile.writeText(content)
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
