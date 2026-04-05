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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File

class AnswerActivity : Activity() {

    private val answerFile by lazy { File(cacheDir, "answer") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Khởi tạo dữ liệu & Xóa file cũ
        if (answerFile.exists()) answerFile.delete()
        val max = intent.getStringExtra("max")?.toIntOrNull()
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // 2. Định nghĩa màu sắc
        val colorMain = if (isDark) Color.WHITE else Color.BLACK
        val colorBg = if (isDark) Color.parseColor("#2A2A2A") else Color.parseColor("#F5F5F5")
        val colorHint = if (isDark) Color.LTGRAY else Color.GRAY

        // 3. Tạo UI
        val etAnswer = EditText(this).apply {
            hint = getString(R.string.hint_answer)
            setTextColor(colorMain)
            setHintTextColor(colorHint)
            backgroundTintList = ColorStateList.valueOf(colorMain)
            imeOptions = EditorInfo.IME_ACTION_SEND
            inputType = if (max != null) android.text.InputType.TYPE_CLASS_NUMBER else android.text.InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }

        val btnSend = Button(this).apply {
            text = getString(R.string.btn_send)
            setOnClickListener { processSend(etAnswer.text.toString(), max) }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(40, 40, 40, 40) // Tăng nhẹ padding cho cân đối
            setBackgroundColor(colorBg)
            addView(etAnswer)
            addView(btnSend)
        }

        setContentView(root)
        setupWindowAndKeyboard(root, etAnswer)

        // 4. Sự kiện phím Enter trên bàn phím
        etAnswer.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEND) { processSend(etAnswer.text.toString(), max); true } else false
        }
    }

    private fun setupWindowAndKeyboard(root: LinearLayout, et: EditText) {
        // Cấu hình vị trí Window
        window.attributes = window.attributes.apply {
            gravity = Gravity.BOTTOM
            width = -1
            height = -2
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)

        // Xử lý đẩy Layout khi hiện bàn phím
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, ime.bottom + 20)
            insets
        }

        // Tự động mở bàn phím
        et.post {
            et.requestFocus()
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(et, 0)
        }
    }

    private fun processSend(input: String, max: Int?) {
        val text = input.trim()
        if (text.isEmpty()) return showToast(getString(R.string.do_not_empty))

        if (max != null) {
            val num = text.toIntOrNull()
            if (num == null || num !in 0..max) {
                return showToast(if (num == null) getString(R.string.toast_invalid_number) 
                                 else getString(R.string.toast_out_of_range, 0, max))
            }
            answerFile.writeText(num.toString())
        } else {
            answerFile.writeText(text)
        }
        finish()
    }

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
