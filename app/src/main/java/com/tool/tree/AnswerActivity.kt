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

class AnswerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Giữ nguyên logic kiểm tra cũ ---
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDarkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES

        val answerFile = File(cacheDir, "answer")
        if (answerFile.exists()) answerFile.delete()

        val min = 0
        val max = intent.getStringExtra("max")?.toIntOrNull()

        val backgroundColor = if (isDarkMode) Color.parseColor("#2A2A2A") else Color.parseColor("#F5F5F5")
        val textColor = if (isDarkMode) Color.parseColor("#FFFFFF") else Color.parseColor("#000000")
        val hintColor = if (isDarkMode) Color.parseColor("#AAAAAA") else Color.parseColor("#555555")

        // --- EditText: Giữ nguyên các String ID cũ ---
        val etAnswer = EditText(this).apply {
            hint = getString(R.string.hint_answer) // Giữ nguyên văn bản cũ
            imeOptions = EditorInfo.IME_ACTION_SEND
            inputType = if (max != null) android.text.InputType.TYPE_CLASS_NUMBER
                        else android.text.InputType.TYPE_CLASS_TEXT
            setTextColor(textColor)
            setHintTextColor(hintColor)
            background = null // Xóa gạch chân mặc định để dùng nền bo góc của root
        }

        // --- Nút gửi: Giữ nguyên văn bản cũ ---
        val btnSend = Button(this).apply { 
            text = getString(R.string.btn_send) // Giữ nguyên văn bản cũ
        }

        // --- Layout ngang ---
        val inputLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(etAnswer, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(btnSend)
        }

        // --- Root Layout: Xử lý bo góc 15px và cách đáy 45px ---
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 0) // Padding bên trong
            
            background = GradientDrawable().apply {
                setColor(backgroundColor)
                cornerRadius = 15f // Bo góc 15px như yêu cầu
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(20, 0, 20, 45) // Cách bàn phím (bottom) đúng 45px
            layoutParams = params

            addView(inputLayout)
        }

        // Dùng FrameLayout làm cha để căn chỉnh vị trí dễ hơn
        val container = FrameLayout(this).apply {
            addView(rootLayout, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            ))
        }

        setContentView(container)

        // --- Window Layout: Sửa lỗi không focus/không nhập được chữ ---
        window.apply {
            setGravity(Gravity.BOTTOM)
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            // Xóa bỏ các flag gây xung đột bàn phím của code cũ
            clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        // --- Hàm gửi đáp án: Giữ nguyên logic và Toast cũ ---
        fun sendAnswer() {
            val text = etAnswer.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, getString(R.string.do_not_empty), Toast.LENGTH_SHORT).show()
                return
            }

            if (max != null) {
                val num = text.toIntOrNull()
                if (num == null) {
                    Toast.makeText(this, getString(R.string.toast_invalid_number), Toast.LENGTH_SHORT).show()
                    return
                }
                if (num < min || num > max) {
                    Toast.makeText(this, getString(R.string.toast_out_of_range, min, max), Toast.LENGTH_SHORT).show()
                    return
                }
                answerFile.writeText(num.toString())
            } else {
                answerFile.outputStream().use { it.write(text.toByteArray()) }
            }
            finish()
        }

        // Sự kiện click & IME (Giữ nguyên)
        btnSend.setOnClickListener { sendAnswer() }
        etAnswer.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendAnswer()
                true
            } else false
        }

        // Focus tự động
        etAnswer.requestFocus()
    }
}
