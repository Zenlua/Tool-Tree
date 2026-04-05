package com.tool.tree

import android.app.Activity
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import java.io.File

class AnswerActivity : Activity() {

    private val answerFile by lazy { File(cacheDir, "answer") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Cấu hình Window (Phải gọi trước setContentView)
        setupWindow()

        val max = intent.getStringExtra("max")?.toIntOrNull()
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        
        val colorMain = if (isDark) Color.WHITE else Color.BLACK
        val colorBg = if (isDark) Color.parseColor("#2A2A2A") else Color.parseColor("#F5F5F5")

        // 2. Tạo UI bằng Code
        val etAnswer = EditText(this).apply {
            hint = getString(R.string.hint_answer)
            setTextColor(colorMain)
            setHintTextColor(if (isDark) Color.LTGRAY else Color.GRAY)
            backgroundTintList = ColorStateList.valueOf(colorMain)
            imeOptions = EditorInfo.IME_ACTION_SEND
            inputType = if (max != null) android.text.InputType.TYPE_CLASS_NUMBER else android.text.InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            
            // Đảm bảo View sẵn sàng nhận tiêu điểm
            isFocusable = true
            isFocusableInTouchMode = true
        }

        val btnSend = Button(this).apply {
            text = getString(R.string.btn_send)
            setOnClickListener { processSend(etAnswer.text.toString(), max) }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(colorBg)
            addView(etAnswer)
            addView(btnSend)
        }

        setContentView(root)

        // 3. Kích hoạt bàn phím (SỬA LỖI FOCUS & BACKSPACE)
        etAnswer.postDelayed({
            etAnswer.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            // Sử dụng SHOW_FORCED để ép bàn phím liên kết chặt chẽ với EditText
            imm.showSoftInput(etAnswer, InputMethodManager.SHOW_FORCED)
        }, 300)

        etAnswer.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEND) {
                processSend(etAnswer.text.toString(), max)
                true
            } else false
        }
    }

    private fun setupWindow() {
        window.attributes = window.attributes.apply {
            gravity = Gravity.BOTTOM
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }
        
        // Loại bỏ FLAG_NOT_TOUCH_MODAL để tránh lỗi không xóa được chữ
        window.addFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }

    // Đóng activity khi chạm ra ngoài vùng nhập liệu
    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_OUTSIDE) {
            finish()
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun processSend(input: String, max: Int?) {
        val text = input.trim()
        if (text.isEmpty()) {
            Toast.makeText(this, getString(R.string.do_not_empty), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            if (max != null) {
                val num = text.toIntOrNull()
                if (num == null || num !in 0..max) {
                    Toast.makeText(this, getString(R.string.toast_invalid_number), Toast.LENGTH_SHORT).show()
                    return
                }
                answerFile.writeText(num.toString())
            } else {
                answerFile.writeText(text)
            }
            finish()
        } catch (e: Exception) { e.printStackTrace() }
    }
}
