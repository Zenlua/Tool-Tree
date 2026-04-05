package com.tool.tree

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import java.io.File
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.res.ColorStateList

class AnswerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val answerFile = File(cacheDir, "answer").apply { delete() }

        val max = intent.getStringExtra("max")?.toIntOrNull()
        val isNumberMode = max != null

        // Màu sắc
        val backgroundColor = if (isDarkMode) Color.parseColor("#2A2A2A") else Color.parseColor("#F5F5F5")
        val textColor = if (isDarkMode) Color.parseColor("#FFFFFF") else Color.parseColor("#000000")
        val hintColor = if (isDarkMode) Color.parseColor("#AAAAAA") else Color.parseColor("#666666")

        // EditText
        val etAnswer = EditText(this).apply {
            hint = getString(R.string.hint_answer)
            imeOptions = EditorInfo.IME_ACTION_SEND
            inputType = if (isNumberMode) 
                android.text.InputType.TYPE_CLASS_NUMBER 
            else android.text.InputType.TYPE_CLASS_TEXT

            setTextColor(textColor)
            setHintTextColor(hintColor)
            setBackground(createEditTextBackground())
            backgroundTintList = ColorStateList.valueOf(textColor)
            setPadding(20, 16, 20, 16)
        }

        val btnSend = Button(this).apply {
            text = getString(R.string.btn_send)
            minWidth = 140
        }

        val inputLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 8)
        
            addView(etAnswer, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(btnSend, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 40)
            setBackgroundColor(backgroundColor)
            addView(inputLayout)
        }

        setContentView(rootLayout)

        // ==================== XỬ LÝ ĐẨY LAYOUT KHI MỞ BÀN PHÍM ====================
ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, insets ->
        val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
        if (imeInsets.bottom > 0) {
            view.translationY = -imeInsets.bottom.toFloat() + 40f
        } else {
            view.translationY = 0f
        }
        insets
    }
        // Cấu hình Window
window.apply {
        setGravity(Gravity.BOTTOM)
        setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        
        clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
        addFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
    }

        // Tự động hiện bàn phím
etAnswer.postDelayed({
        etAnswer.isFocusableInTouchMode = true
        etAnswer.requestFocus()

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        
        // Cách mạnh nhất hiện nay
        imm.showSoftInput(etAnswer, InputMethodManager.SHOW_IMPLICIT)
        
        // Nếu vẫn không hiện, thử cách này (thêm 1 dòng nữa)
        etAnswer.postDelayed({
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }, 300)

    }, 250) // delay 250ms là ổn nhất

        // Hàm gửi đáp án
        fun sendAnswer() {
            val text = etAnswer.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, getString(R.string.do_not_empty), Toast.LENGTH_SHORT).show()
                return
            }

            if (isNumberMode) {
                val num = text.toIntOrNull()
                if (num == null || num < 0 || num > max!!) {
                    Toast.makeText(this, getString(R.string.toast_out_of_range, 0, max), Toast.LENGTH_SHORT).show()
                    return
                }
                answerFile.writeText(num.toString())
            } else {
                answerFile.writeText(text)
            }
            finish()
        }

        btnSend.setOnClickListener { sendAnswer() }
        etAnswer.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendAnswer()
                true
            } else false
        }
    }

    private fun createEditTextBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setStroke(3, Color.parseColor("#888888"))
            cornerRadius = 16f
            setColor(Color.TRANSPARENT)
        }
    }
}