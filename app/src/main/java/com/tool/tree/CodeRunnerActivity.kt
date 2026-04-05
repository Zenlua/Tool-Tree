package com.tool.tree

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.omarea.common.ui.OverScrollView
import com.omarea.krscript.ui.DialogLogFragment
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
        super.onCreate(savedInstanceState)

        // ===== ROOT =====
        val root = FrameLayout(this)
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        // ===== TOOLBAR =====
        val toolbar = Toolbar(this)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Code Runner"
        toolbar.setNavigationOnClickListener { finish() }

        // ===== RIGHT ACTIONS (← → ⋮) =====
        val rightLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

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
                val popup = android.widget.PopupMenu(this@CodeRunnerActivity, this)
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

        val params = Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT,
            Toolbar.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.END }
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

        editor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                try { cacheFile.writeText(s.toString()) } catch (_: Exception) {}
            }
        })

        btnRun.setOnClickListener { runCode(editor.text.toString()) }
        setFont()
    }

    private fun setFont() {
        editor.typeface =
            if (useMono) android.graphics.Typeface.MONOSPACE
            else android.graphics.Typeface.DEFAULT
    }

    private fun loadCache() {
        try { if (cacheFile.exists()) editor.setText(cacheFile.readText()) } catch (_: Exception) {}
    }

    // ===== HISTORY =====
    private fun historyBack() {
        if (historyIndex > 0) historyIndex--; setEditor(historyList[historyIndex])
    }
    private fun historyForward() {
        if (historyIndex < historyList.size - 1) historyIndex++; setEditor(historyList[historyIndex])
    }
    private fun setEditor(text: String) {
        editor.setText(text)
        editor.setSelection(text.length)
    }

    // ===== RUN CODE =====
    private fun runCode(code: String) {
        try { cacheFile.writeText(code) } catch (_: Exception) {}

        if (code.isNotBlank() && (historyList.isEmpty() || historyList.last() != code)) {
            historyList.add(code)
            historyIndex = historyList.size - 1
        }

        val node = RunnableNode("")

        node.shell = code

        val dialog = DialogLogFragment.create(
            node,
            {}, // onStart
            Runnable {}, // onDismiss
            "", // pageHandlerSh
            HashMap(),
            false
        )
        dialog.show(supportFragmentManager, "")
        dialog.isCancelable = false
    }
}