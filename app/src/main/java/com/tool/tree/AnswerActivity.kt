package com.tool.tree

import android.app.Activity
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import java.io.File

class AnswerActivity : Activity() {

    private val answerFile by lazy { File(cacheDir, "answer.log") }
    private lateinit var et: EditText
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindow()

        val answerData = intent.getStringExtra("answer")
        val max = intent.getStringExtra("max")?.toIntOrNull()
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

        // Kiểm tra nếu có dữ liệu câu trả lời (tính năng mới)
        if (answerData != null && answerData.contains("|")) {
            setupQuickButtons(answerData, max)
        } else {
            // Giữ lại tính năng cũ (ô nhập liệu + nút gửi)
            setupDefaultInput(textColor, isDark, max)
        }

        setContentView(root)

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

    private fun setupQuickButtons(data: String, max: Int?) {
        // Tách chuỗi theo dấu |
        val parts = data.split("|")
        parts.forEach { part ->
            // part có dạng "1:A Test" -> split lấy "1" làm kết quả và "A Test" làm nhãn
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
            else android.text.InputType.TYPE_CLASS_TEXT

            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            isFocusable = true
            isFocusableInTouchMode = true
            isCursorVisible = true
        }

        val btn = Button(this).apply {
            text = getString(R.string.btn_send)
            setOnClickListener { send(et.text.toString(), max) }
        }

        root.addView(et)
        root.addView(btn)

        et.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEND) {
                send(et.text.toString(), max); true
            } else false
        }
    }

    private fun reActivateCursor() {
        et.post {
            if (!et.hasFocus()) et.requestFocus()
            et.isCursorVisible = false
            et.isCursorVisible = true
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun setupWindow() {
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
            )
            attributes = attributes.apply {
                gravity = Gravity.BOTTOM
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
            }
        }
    }

    private fun send(input: String, max: Int?) {
        val text = input.trim()
        if (text.isEmpty()) {
            toast(getString(R.string.do_not_empty)); return
        }

        try {
            if (max != null) {
                val num = text.toIntOrNull()
                if (num != null && num in 0..max) {
                    answerFile.writeText(num.toString()); finish()
                } else {
                    // Nếu là kết quả từ nút bấm (A, B, C...) thì có thể không phải số, 
                    // nhưng logic cũ yêu cầu max thì kiểm tra range.
                    // Ở đây tôi cho phép gửi thẳng nếu đó là giá trị từ nút bấm.
                    answerFile.writeText(text); finish()
                }
            } else {
                answerFile.writeText(text); finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
