package com.tool.tree

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.*
import java.io.File
import androidx.core.view.ViewCompat
import kotlin.io.path.writeText

class AnswerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        overridePendingTransition(R.anim.input_method_enter, R.anim.fade_out)

        // Tạo folder home/tmp trong cache
        val tmpDir = File(cacheDir, "home/tmp")
        if (!tmpDir.exists()) tmpDir.mkdirs()
        
        val answerFile = File(tmpDir, "answer.txt")
        if (answerFile.exists()) answerFile.delete()

        // Min luôn là 0, max lấy từ intent (có thể null)
        val min = 0
        val max = intent.getStringExtra("max")?.toIntOrNull()

        // EditText
        val etAnswer = EditText(this).apply {
            hint = "Nhập đáp án..."
            imeOptions = EditorInfo.IME_ACTION_SEND

            // Nếu có max → bàn phím số
            inputType = if (max != null) {
                android.text.InputType.TYPE_CLASS_NUMBER
            } else {
                android.text.InputType.TYPE_CLASS_TEXT // cho phép nhập chữ
            }
        }

        // Nút gửi
        val btnSend = Button(this).apply { text = "Xong" }

        // Layout ngang: EditText + Button
        val inputLayout = LinearLayout(this)
        inputLayout.orientation = LinearLayout.HORIZONTAL
        inputLayout.addView(etAnswer, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        inputLayout.addView(btnSend)

        // Root layout nửa màn hình dưới
        val rootLayout = LinearLayout(this)
        rootLayout.orientation = LinearLayout.VERTICAL
        rootLayout.setPadding(16, 16, 16, 16)
        rootLayout.setBackgroundColor(0xCCFFFFFF.toInt())
        ViewCompat.setFitsSystemWindows(rootLayout, true)
        rootLayout.addView(inputLayout)

        setContentView(rootLayout)

        // Window overlay kiểu chat head nửa dưới
        val params = window.attributes
        params.gravity = Gravity.BOTTOM
        params.height = WindowManager.LayoutParams.WRAP_CONTENT // chỉ chiếm đủ nội dung
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
                // Không có max → cho phép nhập chữ hoặc số ≥0
                .writeText(text)
            }

            finish()
            overridePendingTransition(0, R.anim.input_method_exit)
        }

        btnSend.setOnClickListener { sendAnswer() }
        etAnswer.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendAnswer()
                true
            } else false
        }

        etAnswer.requestFocus()
    }
}