package com.tool.tree

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.*
import java.io.File
import androidx.core.view.ViewCompat

class AnswerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kiểm tra chế độ tối/sáng
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDarkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES

        // Tạo folder home/tmp trong cache
        val answerFile = File(cacheDir, "answer")
        if (answerFile.exists()) answerFile.delete()

        // Min luôn là 0, max lấy từ intent (có thể null)
        val min = 0
        val max = intent.getStringExtra("max")?.toIntOrNull()

        // Chọn màu theo chế độ
        val backgroundColor = if (isDarkMode) Color.parseColor("#2A2A2A") else Color.parseColor("#F5F5F5")
        val textColor = if (isDarkMode) Color.parseColor("#FFFFFF") else Color.parseColor("#000000")
        val hintColor = if (isDarkMode) Color.parseColor("#AAAAAA") else Color.parseColor("#555555")

        // EditText
        val etAnswer = EditText(this).apply {
            hint = "Nhập đáp án..."
            imeOptions = EditorInfo.IME_ACTION_SEND
            setTextColor(textColor)
            setHintTextColor(hintColor)
            inputType = if (max != null) {
                android.text.InputType.TYPE_CLASS_NUMBER
            } else {
                android.text.InputType.TYPE_CLASS_TEXT
            }
        }

        // Nút gửi
        val btnSend = Button(this).apply {
            text = "Xong"
            setTextColor(textColor)
        }

        // Layout ngang: EditText + Button
        val inputLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(etAnswer, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(btnSend)
        }

        // Root layout nửa màn hình dưới
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 18, 16, 18)
            setBackgroundColor(backgroundColor)
            ViewCompat.setFitsSystemWindows(this, true)
            addView(inputLayout)
        }

        setContentView(rootLayout)

        // Window overlay kiểu chat head nửa dưới
        val params = window.attributes
        params.gravity = Gravity.BOTTOM
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        window.attributes = params

        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        )
        window.setFlags(
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        )

        // Hàm gửi đáp án
        fun sendAnswer() {
            val text = etAnswer.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "Không được để trống", Toast.LENGTH_SHORT).show()
                return
            }

            if (max != null) {
                val num = text.toIntOrNull()
                if (num == null) {
                    Toast.makeText(this, "Vui lòng nhập số", Toast.LENGTH_SHORT).show()
                    return
                }
                if (num < min || num > max) {
                    Toast.makeText(this, "Số phải trong khoảng $min đến $max", Toast.LENGTH_SHORT).show()
                    return
                }
                answerFile.writeText(num.toString())
            } else {
                answerFile.outputStream().use { it.write(text.toByteArray()) }
            }

            finish()
        }

        // Sự kiện click & gửi khi nhấn IME
        btnSend.setOnClickListener { sendAnswer() }
        etAnswer.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendAnswer()
                true
            } else false
        }

        etAnswer.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
imm.showSoftInput(etAnswer, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }
}