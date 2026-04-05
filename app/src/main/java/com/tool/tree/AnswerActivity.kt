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

        // 1. Cấu hình Window để nhận diện bàn phím và phím xóa (Backspace)
        setupWindow()

        val maxStr = intent.getStringExtra("max")
        val max = maxStr?.toIntOrNull()
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        
        val colorMain = if (isDark) Color.WHITE else Color.BLACK
        val colorBg = if (isDark) Color.parseColor("#2A2A2A") else Color.parseColor("#F5F5F5")
        val colorHint = if (isDark) Color.LTGRAY else Color.GRAY

        // 2. Khởi tạo EditText với đầy đủ thuộc tính Focus
        etAnswer = EditText(this).apply {
            hint = getString(R.string.hint_answer)
            setTextColor(colorMain)
            setHintTextColor(colorHint)
            backgroundTintList = ColorStateList.valueOf(colorMain)
            imeOptions = EditorInfo.IME_ACTION_SEND
            inputType = if (max != null) android.text.InputType.TYPE_CLASS_NUMBER 
                        else android.text.InputType.TYPE_CLASS_TEXT
            
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            
            // Ép buộc hiện con trỏ nhấp nháy ngay lập tức
            isCursorVisible = true
            isFocusable = true
            isFocusableInTouchMode = true
        }

        val btnSend = Button(this).apply {
            text = getString(R.string.btn_send)
            setOnClickListener { processSend(etAnswer.text.toString(), max) }
        }

        // 3. Layout Chính (Padding 20)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(colorBg)
            addView(etAnswer)
            addView(btnSend)
        }

        setContentView(root)

        // 4. Kích hoạt tiêu điểm và bàn phím (Sửa lỗi con trỏ & phím xóa)
        etAnswer.post {
            etAnswer.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            // SHOW_FORCED đảm bảo bàn phím gắn chặt với EditText để phím Backspace hoạt động
            imm.showSoftInput(etAnswer, InputMethodManager.SHOW_FORCED)
        }

        // Sự kiện phím Enter trên bàn phím
        etAnswer.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEND) {
                processSend(etAnswer.text.toString(), max)
                true
            } else false
        }
    }

    private fun setupWindow() {
        window.apply {
            // Xóa các cờ ngăn cản việc nhận phím hệ thống
            clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            
            // Cho phép đóng khi chạm ngoài nhưng vẫn giữ Focus để nhập liệu
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
        
        // --- GIỮ LẠI CÁC CHECK VĂN BẢN TRỐNG CŨ ---
        if (text.isEmpty()) {
            Toast.makeText(this, getString(R.string.do_not_empty), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            if (max != null) {
                val num = text.toIntOrNull()
                if (num == null) {
                    Toast.makeText(this, getString(R.string.toast_invalid_number), Toast.LENGTH_SHORT).show()
                    return
                }
                if (num < 0 || num > max) {
                    Toast.makeText(this, getString(R.string.toast_out_of_range, 0, max), Toast.LENGTH_SHORT).show()
                    return
                }
                answerFile.writeText(num.toString())
            } else {
                answerFile.writeText(text)
            }
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
