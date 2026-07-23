package com.tool.tree

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.snackbar.Snackbar
import com.omarea.common.ui.BlurEngine
import com.omarea.common.ui.ProgressBarDialog
import com.tool.tree.databinding.ActivityFileSelectorBinding
import com.tool.tree.ui.AdapterFileSelector
import java.io.File

class ActivityFileSelector : AppCompatActivity() {
    companion object {
        const val MODE_FILE = 0
        const val MODE_FOLDER = 1
    }

    private var adapterFileSelector: AdapterFileSelector? = null
    var extension = ""
    var mode = MODE_FILE
    var multiple = false
    var pathHome = ""
    private lateinit var binding: ActivityFileSelectorBinding
    private var toolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeModeState.switchTheme(this)
        binding = ActivityFileSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.fileDrawerContainer.isDrawStrokeEnabled = false

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        this.toolbar = toolbar
        setSupportActionBar(toolbar)

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        onBackPressedDispatcher.addCallback(this) {
            if (adapterFileSelector?.goParent() == true) return@addCallback
            setResult(RESULT_CANCELED, Intent())
            finish()
        }

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
                }
            }
            if (containsKey("multiple")) {
                multiple = getBoolean("multiple")
            }
            if (containsKey("path_home")) {
                pathHome = "" + intent.extras?.getString("path_home")
            }
        }

        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (multiple) {
            menuInflater.inflate(R.menu.menu_file_selector, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_confirm_selection) {
            finishWithSelection()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun finishWithSelection() {
        val selected = adapterFileSelector?.selectedFiles?.map { it.absolutePath }
        if (selected.isNullOrEmpty()) {
            Snackbar.make(binding.root, R.string.msg_nothing_selected, Snackbar.LENGTH_SHORT).show()
            return
        }
        setResult(RESULT_OK, Intent().putStringArrayListExtra("files", ArrayList(selected)))
        finish()
    }

    override fun onResume() {
        super.onResume()
        loadData()
        if (mode == MODE_FOLDER && !multiple) {
            Snackbar.make(binding.root, R.string.msg_folder_mode, Snackbar.LENGTH_SHORT).show()
        } else if (multiple) {
            Snackbar.make(binding.root, R.string.msg_multiple_select_mode, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun loadData() {
        val sdcard = Environment.getExternalStorageDirectory()
        val startDir = if (pathHome.isNotEmpty()) {
            val homeDir = File(pathHome)
            if (homeDir.exists() && homeDir.isDirectory && homeDir.canRead()) {
                homeDir
            } else {
                Snackbar.make(binding.root, getString(R.string.msg_path_home_not_found, pathHome), Snackbar.LENGTH_SHORT).show()
                sdcard
            }
        } else {
            sdcard
        }

        if (startDir.exists() && startDir.isDirectory) {
            val list = startDir.listFiles()
            if (list == null) {
                Snackbar.make(binding.root, "Failed to retrieve file list!", Snackbar.LENGTH_LONG).show()
                return
            }
            val onSelected = Runnable {
                val file = adapterFileSelector?.selectedFile
                if (file != null) {
                    this.setResult(RESULT_OK, Intent().putExtra("file", file.absolutePath))
                    this.finish()
                }
            }
            adapterFileSelector = if (mode == MODE_FOLDER) {
                AdapterFileSelector.FolderChooser(startDir, onSelected, ProgressBarDialog(this), multiple)
            } else {
                AdapterFileSelector.FileChooser(startDir, onSelected, ProgressBarDialog(this), extension, multiple)
            }

            binding.fileSelectorList.adapter = adapterFileSelector

            // ✅ Chụp ảnh mờ sau khi gán Adapter và Render dữ liệu xong
            binding.root.post {
                BlurEngine.controller.captureAndBlur(this)
            }
        } else {
            Snackbar.make(binding.root, "External storage not available!", Snackbar.LENGTH_LONG).show()
        }
    }
}
