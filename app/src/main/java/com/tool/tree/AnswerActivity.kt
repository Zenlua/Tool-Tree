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
    private lateinit var etAnswer: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Cấu hình Window (Xóa sạch các cờ cản trở)
        setupWindow()

        val max = intent.getStringExtra("max")?.toIntOrNull()
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val colorMain = if (isDark) Color.WHITE else Color.BLACK

        // 2. Khởi tạo EditText
        etAnswer = EditText(this).apply {
            hint = getString(R.string.hint_answer)
            setTextColor(colorMain)
            setHintTextColor(if (isDark) Color.LTGRAY else Color.GRAY)
            backgroundTintList = ColorStateList.valueOf(colorMain)
            imeOptions = EditorInfo.IME_ACTION_SEND
            inputType = if (max != null) android.text.InputType.TYPE_CLASS_NUMBER else android.text.InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            
            isCursorVisible = true
            isFocusable = true
            isFocusableInTouchMode = true
            
            // Tự động bôi đen toàn bộ khi nhận Focus (Giúp phím xóa hoạt động)
            setSelectAllOnFocus(true) 
        }

        val btnSend = Button(this).apply {
            text = getString(R.string.btn_send)
            setOnClickListener { processSend(etAnswer.text.toString(), max) }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(if (isDark) Color.parseColor("#2A2A2A") else Color.parseColor("#F5F5F5"))
            addView(etAnswer)
            addView(btnSend)
        }

        setContentView(root)

        etAnswer.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEND) { processSend(etAnswer.text.toString(), max); true } else false
        }
    }

    // KỸ THUẬT MỚI: Ép Focus và Bôi đen ngay khi cửa sổ sẵn sàng
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            etAnswer.postDelayed({
                etAnswer.requestFocus()
                // Lệnh bôi đen toàn bộ chữ hiện có
                etAnswer.selectAll() 
                
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                // Sử dụng SHOW_FORCED để ép kết nối bàn phím
                imm.showSoftInput(etAnswer, InputMethodManager.SHOW_FORCED)
            }, 100) // Delay cực ngắn để Window ổn định Focus
        }
    }

    private fun setupWindow() {
        window.apply {
            // Xóa FLAG_ALT_FOCUSABLE_IM là bắt buộc để dùng phím Xóa
            clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            
            addFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
            
            attributes = attributes.apply {
                gravity = Gravity.BOTTOM
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
            }
        }
    }

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
                if (num != null && num in 0..max) {
                    answerFile.writeText(num.toString())
                    finish()
                } else Toast.makeText(this, getString(R.string.toast_out_of_range, 0, max), Toast.LENGTH_SHORT).show()
            } else {
                answerFile.writeText(text); finish()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
}
