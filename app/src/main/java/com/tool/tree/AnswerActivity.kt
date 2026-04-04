package com.tool.tree

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.*
import java.io.File
import androidx.core.view.ViewCompat
import android.content.res.ColorStateList

// ... các import giữ nguyên ...

class AnswerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Giữ nguyên logic dữ liệu và màu sắc cũ ---
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDarkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES
        val answerFile = File(cacheDir, "answer")
        val min = 0
        val max = intent.getStringExtra("max")?.toIntOrNull()

        val backgroundColor = if (isDarkMode) Color.parseColor("#2A2A2A") else Color.parseColor("#F5F5F5")
        val textColor = if (isDarkMode) Color.parseColor("#FFFFFF") else Color.parseColor("#000000")
        val hintColor = if (isDarkMode) Color.parseColor("#AAAAAA") else Color.parseColor("#555555")

        // 1. EditText (Giữ nguyên văn bản cũ)
        val etAnswer = EditText(this).apply {
            hint = getString(R.string.hint_answer)
            imeOptions = EditorInfo.IME_ACTION_SEND
            inputType = if (max != null) android.text.InputType.TYPE_CLASS_NUMBER
                        else android.text.InputType.TYPE_CLASS_TEXT
            setTextColor(textColor)
            setHintTextColor(hintColor)
            background = null // Để hiện nền của rootLayout
            setPadding(30, 30, 30, 30)
        }

        // 2. Nút gửi (Giữ nguyên văn bản cũ)
        val btnSend = Button(this).apply {
            text = getString(R.string.btn_send)
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(textColor)
        }

        // 3. Layout ngang chứa EditText + Button
        val inputLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(etAnswer, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(btnSend)
        }

        // 4. Root Layout: Xử lý bo góc 15px và Khoảng cách (Margins)
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(backgroundColor)
                cornerRadius = 15f // Bo góc 15px
            }
            
            // Thiết lập Margin: Trái 40, Trên 0, Phải 40, Dưới 45 (cách bàn phím)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(40, 0, 40, 45) 
            layoutParams = lp
            
            addView(inputLayout)
        }

        // 5. Container cha để đảm bảo Margin có tác dụng
        val container = FrameLayout(this).apply {
            // Quan trọng: Phải set padding hoặc dùng FrameLayout.LayoutParams để margin hoạt động
            addView(rootLayout)
            setBackgroundColor(Color.TRANSPARENT)
        }

        setContentView(container)

        // 6. Cấu hình Window (Sửa lỗi focus và tràn màn hình)
        window.apply {
            // Làm nền Activity trong suốt để thấy app phía sau
            setBackgroundDrawableResource(android.R.color.transparent)
            setGravity(Gravity.BOTTOM)
            
            // Cấu hình attributes để tránh lỗi Unresolved reference
            val lp = attributes
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or 
                               WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
            attributes = lp

            // Xóa các flag gây lỗi không nhập được văn bản
            clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        }

        // --- Giữ nguyên toàn bộ logic sendAnswer() và các Toast cũ của bạn ---
        fun sendAnswer() {
            val text = etAnswer.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, getString(R.string.do_not_empty), Toast.LENGTH_SHORT).show()
                return
            }
            // ... (Logic kiểm tra min/max giữ nguyên như code bạn đã viết) ...
            if (max != null) {
                val num = text.toIntOrNull()
                if (num == null || num < min || num > max) {
                    Toast.makeText(this, "Giá trị không hợp lệ", Toast.LENGTH_SHORT).show()
                    return
                }
                answerFile.writeText(num.toString())
            } else {
                answerFile.writeText(text)
            }
            finish()
        }

        btnSend.setOnClickListener { sendAnswer() }
        etAnswer.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEND) { sendAnswer(); true } else false
        }

        // Tự động focus
        etAnswer.requestFocus()
    }
}
