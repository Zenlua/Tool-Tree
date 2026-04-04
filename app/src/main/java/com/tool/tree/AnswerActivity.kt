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

        // Kiểm tra chế độ tối/sáng
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDarkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES

        // File lưu đáp án
        val answerFile = File(cacheDir, "answer")
        if (answerFile.exists()) answerFile.delete()

        val min = 0
        val max = intent.getStringExtra("max")?.toIntOrNull()

        // Màu theo chế độ
        val backgroundColor = if (isDarkMode) Color.parseColor("#2A2A2A") else Color.parseColor("#F5F5F5")
        val textColor = if (isDarkMode) Color.parseColor("#FFFFFF") else Color.parseColor("#000000")
        val hintColor = if (isDarkMode) Color.parseColor("#AAAAAA") else Color.parseColor("#555555")

        // EditText với background bo góc, gạch ngang màu chữ, cao hơn, rộng hơn
        val etAnswer = EditText(this).apply {
            hint = getString(R.string.hint_answer)
            imeOptions = EditorInfo.IME_ACTION_SEND
            inputType = if (max != null) android.text.InputType.TYPE_CLASS_NUMBER
                        else android.text.InputType.TYPE_CLASS_TEXT
            setTextColor(textColor)       // chữ nhập theo sáng/tối
            setHintTextColor(hintColor)   // hint theo sáng/tối
            backgroundTintList = ColorStateList.valueOf(textColor) // background để mặc định → gạch dưới vẫn như cũ
        }

        // Nút gửi
        val btnSend = Button(this).apply { text = getString(R.string.btn_send) }

        // Layout ngang: EditText + Button
        val inputLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(etAnswer, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(btnSend)
        }

        // Root layout nửa dưới
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(backgroundColor)
            ViewCompat.setFitsSystemWindows(this, true)
            addView(inputLayout)
        }

        setContentView(rootLayout)

        window.attributes.apply {
            gravity = Gravity.BOTTOM
            height = WindowManager.LayoutParams.WRAP_CONTENT
            width = WindowManager.LayoutParams.MATCH_PARENT
        }

        window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
        window.addFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        // Hàm gửi đáp án
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

        // --- MỤC ĐÃ SỬA: Focus và gọi bàn phím ---
        etAnswer.post {
            etAnswer.isFocusableInTouchMode = true
            etAnswer.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(etAnswer, 0)
        }

        // Sự kiện click & IME
        btnSend.setOnClickListener { sendAnswer() }
        etAnswer.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendAnswer()
                true
            } else false
        }
    }
}