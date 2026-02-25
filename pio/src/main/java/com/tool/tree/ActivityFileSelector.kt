package com.tool.tree

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.omarea.common.ui.ProgressBarDialog
import com.tool.tree.databinding.ActivityFileSelectorBinding
import com.tool.tree.ui.AdapterFileSelector
import java.io.File
import androidx.activity.OnBackPressedCallback

class ActivityFileSelector : AppCompatActivity() {
    companion object {
        const val MODE_FILE = 0
        const val MODE_FOLDER = 1
    }

    private var adapterFileSelector: AdapterFileSelector? = null
    var extension = ""
    var mode = MODE_FILE
    private lateinit var binding : ActivityFileSelectorBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        // TODO:ThemeSwitch.switchTheme(this)
        super.onCreate(savedInstanceState)
        ThemeModeState.switchTheme(this)
        binding = ActivityFileSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        // setTitle(R.string.app_name)

        // 显示返回按钮
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { _ ->
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (adapterFileSelector != null && adapterFileSelector!!.goParent()) {
                    return
                }
                setResult(RESULT_CANCELED, Intent())
                finish()
            }
        })

        intent.extras?.run {
            if (containsKey("extension")) {
                extension = "" + intent.extras?.getString("extension")
                if (!extension.startsWith(".")) {
                    extension = ".$extension"
                }
                if (extension.isNotEmpty()) {
                    title = "$title($extension)"
                }
            }
            if (containsKey("mode")) {
                mode = getInt("mode")
                if (mode == MODE_FOLDER) {
                    title = getString(R.string.title_activity_folder_selector)
                    Toast.makeText(this@ActivityFileSelector, R.string.msg_folder_mode, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        val sdcard = File(Environment.getExternalStorageDirectory().absolutePath)
        if (sdcard.exists() && sdcard.isDirectory) {
            val list = sdcard.listFiles()
            if (list == null) {
                Toast.makeText(applicationContext, "Failed to retrieve file list!", Toast.LENGTH_LONG).show()
                return
            }
            val onSelected =  Runnable {
                val file: File? = adapterFileSelector!!.selectedFile
                if (file != null) {
                    this.setResult(RESULT_OK, Intent().putExtra("file", file.absolutePath))
                    this.finish()
                }
            }
            adapterFileSelector = if (mode == MODE_FOLDER) {
                AdapterFileSelector.FolderChooser(sdcard, onSelected, ProgressBarDialog(this))
            } else {
                AdapterFileSelector.FileChooser(sdcard, onSelected, ProgressBarDialog(this), extension)
            }

            binding.fileSelectorList.adapter = adapterFileSelector
        } else {
            Toast.makeText(applicationContext, "External storage not available!", Toast.LENGTH_LONG).show()
        }
    }
}