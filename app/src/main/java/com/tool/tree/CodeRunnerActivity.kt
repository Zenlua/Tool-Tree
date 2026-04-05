package com.tool.tree

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.OverScrollView
import com.omarea.common.ui.ThemeModeState
import com.omarea.krscript.model.RunnableNode
import java.io.File

class CodeRunnerActivity : AppCompatActivity() {

    private lateinit var editor: EditText
    private lateinit var scroll: OverScrollView
    private lateinit var cacheFile: File

    private val historyList = mutableListOf<String>()
    private var historyIndex = -1

    private var useMono = true

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeModeState.switchTheme(this)

        super.onCreate(savedInstanceState)

        // ===== ROOT =====
        val root = FrameLayout(this)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // ===== TOOLBAR =====
        val toolbar = Toolbar(this)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Code Runner"

        toolbar.setNavigationOnClickListener {
            finish()
        }

        // ===== RIGHT ACTIONS (← → ⋮) =====
        val rightLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val btnBackHistory = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_media_previous)
            setPadding(20, 20, 20, 20)
            setBackgroundResource(android.R.attr.selectableItemBackgroundBorderless)
            setOnClickListener { historyBack() }
        }

        val btnForwardHistory = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_media_next)
            setPadding(20, 20, 20, 20)
            setBackgroundResource(android.R.attr.selectableItemBackgroundBorderless)
            setOnClickListener { historyForward() }
        }

        val btnMenu = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_more)
            setPadding(20, 20, 20, 20)
            setBackgroundResource(android.R.attr.selectableItemBackgroundBorderless)

            setOnClickListener {
                val popup = PopupMenu(this@CodeRunnerActivity, this)

                popup.menu.add("Font code")

                popup.setOnMenuItemClickListener {
                    useMono = !useMono
                    setFont()
                    true
                }

                popup.show()
            }
        }

        rightLayout.addView(btnBackHistory)
        rightLayout.addView(btnForwardHistory)
        rightLayout.addView(btnMenu)

        val params = Toolbar.LayoutParams(
            Toolbar.LayoutParams.WRAP_CONTENT,
            Toolbar.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.END
        }

        toolbar.addView(rightLayout, params)

        container.addView(toolbar)

        // ===== EDITOR =====
        scroll = OverScrollView(this)

        editor = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            textSize = 14f
            gravity = Gravity.TOP or Gravity.START
        }

        scroll.addView(editor)

        container.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        // ===== FAB =====
        val btnRun = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_media_play)
            setPadding(30, 30, 30, 30)
        }

        root.addView(container)

        root.addView(btnRun, FrameLayout.LayoutParams(150, 150).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            setMargins(0, 0, 40, 40)
        })

        setContentView(root)

        // ===== CACHE =====
        cacheFile = File(cacheDir, "code_cache.txt")
        loadCache()

        // auto save khi gõ
        editor.addTextChangedListener {
            try {
                cacheFile.writeText(it.toString())
            } catch (_: Exception) {}
        }

        // run
        btnRun.setOnClickListener {
            val code = editor.text.toString()
            runCode(code)
        }

        setFont()
    }

    // ===== HISTORY =====
    private fun historyBack() {
        if (historyIndex > 0) {
            historyIndex--
            setEditor(historyList[historyIndex])
        }
    }

    private fun historyForward() {
        if (historyIndex < historyList.size - 1) {
            historyIndex++
            setEditor(historyList[historyIndex])
        }
    }

    private fun setEditor(text: String) {
        editor.setText(text)
        editor.setSelection(text.length)
    }

    // ===== CACHE =====
    private fun loadCache() {
        try {
            if (cacheFile.exists()) {
                val text = cacheFile.readText()
                setEditor(text)
            }
        } catch (_: Exception) {}
    }

    // ===== FONT =====
    private fun setFont() {
        editor.typeface =
            if (useMono) android.graphics.Typeface.MONOSPACE
            else android.graphics.Typeface.DEFAULT
    }

    // ===== RUN =====
    private fun runCode(code: String) {
        try {
            cacheFile.writeText(code)
        } catch (_: Exception) {}

        if (code.isNotBlank() && (historyList.isEmpty() || historyList.last() != code)) {
            historyList.add(code)
            historyIndex = historyList.size - 1
        }

        val node = RunnableNode().apply {
            name = "Code Runner"
            shell = code
            isRoot = true
        }

        DialogHelper.execute(this, node)
    }
}