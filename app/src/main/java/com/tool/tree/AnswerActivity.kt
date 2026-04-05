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

        // 1. Cấu hình Window trước khi nạp giao diện
        setupWindowLayout()

        // 2. Chuẩn bị dữ liệu & màu sắc
        if (answerFile.exists()) answerFile.delete()
        val max = intent.getStringExtra("max")?.toIntOrNull()
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        
        val colorMain = if (isDark) Color.WHITE else Color.BLACK
        val colorBg = if (isDark) Color.parseColor("#2A2A2A") else Color.parseColor("#F5F5F5")
        val colorHint = if (isDark) Color.LTGRAY else Color.GRAY

        // 3. Khởi tạo EditText
        val etAnswer = EditText(this).apply {
            hint = getString(R.string.hint_answer)
            setTextColor(colorMain)
            setHintTextColor(colorHint)
            backgroundTintList = ColorStateList.valueOf(colorMain)
            imeOptions = EditorInfo.IME_ACTION_SEND
            inputType = if (max != null) android.text.InputType.TYPE_CLASS_NUMBER else android.text.InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            isFocusableInTouchMode = true
        }

        // 4. Khởi tạo Button
        val btnSend = Button(this).apply {
            text = getString(R.string.btn_send)
            setOnClickListener { processSend(etAnswer.text.toString(), max) }
        }

        // 5. Root Layout (Dùng ngang để gọn code)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(40, 40, 40, 40)
            setBackgroundColor(colorBg)
            addView(etAnswer)
            addView(btnSend)
        }

        setContentView(root)

        // 6. Sửa lỗi Focus & Bàn phím: Delay nhẹ để Window ổn định vị trí Gravity.BOTTOM
        etAnswer.postDelayed({
            etAnswer.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etAnswer, InputMethodManager.SHOW_IMPLICIT)
        }, 200)

        // Sự kiện phím Enter trên bàn phím ảo
        etAnswer.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEND) {
                processSend(etAnswer.text.toString(), max)
                true
            } else false
        }
    }

    private fun setupWindowLayout() {
        window.attributes = window.attributes.apply {
            gravity = Gravity.BOTTOM
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }
        // Cho phép tương tác bên ngoài để đóng hoặc giữ focus
        window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or 
                         WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
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
                    val errorMsg = if (num == null) getString(R.string.toast_invalid_number) 
                                   else getString(R.string.toast_out_of_range, 0, max)
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
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
