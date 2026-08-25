package com.tool.tree

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
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
        
        binding.fileDrawerContainer.engine.cornerRadius = 0f
        binding.fileDrawerContainer.isDrawStrokeEnabled = false

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        this.toolbar = toolbar
        setSupportActionBar(toolbar)

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
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

        // Cho phép tiêu đề toolbar xuống dòng (tối đa 2 dòng) thay vì bị cắt hiện dấu "..."
        // Toolbar tự tạo TextView tiêu đề khi layout, nên phải chờ tới lúc đó mới chỉnh được.
        toolbar.post {
            for (i in 0 until toolbar.childCount) {
                val child = toolbar.getChildAt(i)
                if (child is TextView && child.text?.toString() == toolbar.title?.toString()) {
                    child.isSingleLine = false
                    child.maxLines = 2
                    child.ellipsize = null
                    break
                }
            }
        }
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
            showSnackbar(R.string.msg_nothing_selected, Snackbar.LENGTH_SHORT)
            return
        }
        setResult(RESULT_OK, Intent().putStringArrayListExtra("files", ArrayList(selected)))
        finish()
    }

    override fun onResume() {
        super.onResume()
        loadData()
        if (mode == MODE_FOLDER && !multiple) {
            showSnackbar(R.string.msg_folder_mode, Snackbar.LENGTH_SHORT)
        } else if (multiple) {
            showSnackbar(R.string.msg_multiple_select_mode, Snackbar.LENGTH_LONG)
        }
    }

    private fun loadData() {
        val sdcard = Environment.getExternalStorageDirectory()
        val startDir = if (pathHome.isNotEmpty()) {
            val homeDir = File(pathHome)
            if (homeDir.exists() && homeDir.isDirectory && homeDir.canRead()) {
                homeDir
            } else {
                showSnackbar(getString(R.string.msg_path_home_not_found, pathHome), Snackbar.LENGTH_SHORT)
                sdcard
            }
        } else {
            sdcard
        }

        if (startDir.exists() && startDir.isDirectory) {
            val list = startDir.listFiles()
            if (list == null) {
                showSnackbar("Failed to retrieve file list!", Snackbar.LENGTH_LONG)
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

            // Hàng "Chọn tất cả" chỉ hiện khi đang ở chế độ chọn nhiều (multiple)
            if (multiple) {
                binding.selectAllBlock.visibility = View.VISIBLE

                fun syncSelectAllCheckbox() {
                    binding.selectAll.isChecked = adapterFileSelector?.isAllCurrentDirSelected() == true
                }
                syncSelectAllCheckbox()

                val toggleSelectAll = View.OnClickListener {
                    val nextState = adapterFileSelector?.isAllCurrentDirSelected() != true
                    adapterFileSelector?.setSelectAllState(nextState)
                    binding.selectAll.isChecked = nextState
                }
                binding.selectAllBlock.setOnClickListener(toggleSelectAll)
                binding.selectAll.setOnClickListener(toggleSelectAll)

                adapterFileSelector?.setSelectionChangedListener(object : AdapterFileSelector.SelectionChangedListener {
                    override fun onSelectionChanged(selectedCount: Int) {
                        syncSelectAllCheckbox()
                    }
                })
            } else {
                binding.selectAllBlock.visibility = View.GONE
            }

        } else {
            showSnackbar("External storage not available!", Snackbar.LENGTH_LONG)
        }
    }

    // Snackbar mặc định luôn dính ở đáy màn hình (nó tự leo lên tìm CoordinatorLayout, không có
    // thì rơi về FrameLayout gốc android.R.id.content - tức toàn bộ cửa sổ - nên vị trí không
    // liên quan gì tới Toolbar/list bên trong). Hàm này ép Snackbar hiện cố định ngay dưới
    // Toolbar (dùng ANIMATION_MODE_FADE thay vì trượt từ dưới lên, vì vị trí đã đổi lên trên).
    //
    // LƯU Ý: KHÔNG dùng toolbar.bottom để tính khoảng cách - app chạy edge-to-edge
    // (decorFitsSystemWindows = false) nên content root trải từ y=0 (dưới cả status bar).
    // Nếu gọi hàm này sớm (vd ngay trong onResume()) trước khi Toolbar kịp layout xong,
    // toolbar.bottom vẫn còn = 0, khiến Snackbar bị đẩy lên đè status bar. Thay vào đó tính
    // trực tiếp từ actionBarSize (chiều cao Toolbar) + chiều cao status bar hiện tại - luôn
    // đúng ngay cả khi View chưa layout lần nào.
    private fun statusBarHeightPx(): Int {
        val insets = ViewCompat.getRootWindowInsets(window.decorView)
        val fromInsets = insets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
        if (fromInsets > 0) return fromInsets
        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) resources.getDimensionPixelSize(resId) else 0
    }

    private fun actionBarHeightPx(): Int {
        val typedValue = TypedValue()
        return if (theme.resolveAttribute(androidx.appcompat.R.attr.actionBarSize, typedValue, true)) {
            TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
        } else {
            (56 * resources.displayMetrics.density).toInt()
        }
    }

    private fun showSnackbar(message: CharSequence, duration: Int) {
        val snackbar = Snackbar.make(binding.root, message, duration)
        snackbar.animationMode = BaseTransientBottomBar.ANIMATION_MODE_FADE
        val view = snackbar.view
        (view.layoutParams as? FrameLayout.LayoutParams)?.let { params ->
            params.gravity = Gravity.TOP
            params.topMargin = statusBarHeightPx() + actionBarHeightPx()
            view.layoutParams = params
        }
        snackbar.show()
    }

    private fun showSnackbar(messageRes: Int, duration: Int) {
        showSnackbar(getString(messageRes), duration)
    }
}
