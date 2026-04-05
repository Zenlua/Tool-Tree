package com.tool.tree

import android.app.Activity
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import java.io.File

class AnswerActivity : Activity() {

    private val answerFile by lazy { File(cacheDir, "answer") }
    private lateinit var etAnswer: EditText
    private lateinit var rootLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindow()

        val max = intent.getStringExtra("max")?.toIntOrNull()
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val colorMain = if (isDark) Color.WHITE else Color.BLACK

        etAnswer = EditText(this).apply {
            hint = getString(R.string.hint_answer)
            setTextColor(colorMain)
            setHintTextColor(if (isDark) Color.LTGRAY else Color.GRAY)
            backgroundTintList = ColorStateList.valueOf(colorMain)
            imeOptions = EditorInfo.IME_ACTION_SEND
            // Chuyển sang TEXT để ổn định phím xóa trên mọi bàn phím
            inputType = if (max != null) android.text.InputType.TYPE_CLASS_NUMBER else android.text.InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            isFocusable = true
            isFocusableInTouchMode = true
            isCursorVisible = true
        }

        val btnSend = Button(this).apply {
            text = getString(R.string.btn_send)
            setOnClickListener { processSend(etAnswer.text.toString(), max) }
        }

        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(if (isDark) Color.parseColor("#2A2A2A") else Color.parseColor("#F5F5F5"))
            addView(etAnswer)
            addView(btnSend)
        }
        setContentView(rootLayout)

        // LẮNG NGHE SỰ KIỆN LAYOUT (Khi bàn phím đẩy ô nhập lên)
        rootLayout.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            private var lastHeight = 0
            override fun onGlobalLayout() {
                val r = Rect()
                rootLayout.getWindowVisibleDisplayFrame(r)
                val currentHeight = r.height()
                
                // Nếu chiều cao thay đổi (nghĩa là bàn phím vừa đẩy xong)
                if (currentHeight != lastHeight) {
                    lastHeight = currentHeight
                    reActivateCursor()
                }
            }
        })

        etAnswer.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEND) { processSend(etAnswer.text.toString(), max); true } else false
        }
    }

    private fun reActivateCursor() {
        etAnswer.post {
            if (!etAnswer.hasFocus()) etAnswer.requestFocus()
            
            // Ép hệ thống vẽ lại con trỏ
            etAnswer.isCursorVisible = false
            etAnswer.isCursorVisible = true
            
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etAnswer, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun setupWindow() {
        window.apply {
            // Xóa cờ gây lỗi phím xóa
            clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            
            // Ép chế độ đẩy giao diện
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or 
                             WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

            attributes = attributes.apply {
                gravity = Gravity.BOTTOM
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
            }
        }
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
                    answerFile.writeText(num.toString()); finish()
                } else Toast.makeText(this, getString(R.string.toast_out_of_range, 0, max), Toast.LENGTH_SHORT).show()
            } else {
                answerFile.writeText(text); finish()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
}
