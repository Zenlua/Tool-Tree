package com.omarea.common.ui

import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AbsListView
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Filterable
import com.omarea.common.R

class DialogAppChooser(
    private val darkMode: Boolean,
    private var packages: ArrayList<AdapterAppChooser.AppInfo>,
    private val multiple: Boolean = false,
    private var callback: Callback? = null
) : DialogFullScreen(R.layout.dialog_app_chooser, darkMode) {

    private var allowAllSelect = true
    private var excludeApps: Array<String> = arrayOf()
    private lateinit var adapter: AdapterAppChooser
    private var loadingView: View? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingView = view.findViewById(R.id.loading)
        showLoading(true)
        
        val absListView = view.findViewById<AbsListView>(R.id.app_list)
        setup(absListView)

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dismiss()
        }
        view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            onConfirm(absListView)
        }

        // 全选
        val selectAll = view.findViewById<CompoundButton?>(R.id.select_all)
        if (selectAll != null) {
            if (multiple) {
                val adapter = absListView.adapter as? AdapterAppChooser
                selectAll.visibility = View.VISIBLE
                selectAll.isChecked = packages.count { it.selected } == packages.size
            
                selectAll.setOnClickListener {
                    adapter?.setSelectAllState((it as CompoundButton).isChecked)
                }
            
                adapter?.setSelectStateListener(object : AdapterAppChooser.SelectStateListener {
                    override fun onSelectChange(selected: List<AdapterAppChooser.AppInfo>) {
                        selectAll.isChecked = selected.size == packages.size
                    }
                })
            
                if (!allowAllSelect) {
                    selectAll.visibility = View.GONE
                }
            } else {
                selectAll.visibility = View.GONE
            }
        }

        val clearBtn = view.findViewById<View>(R.id.search_box_clear)
        val searchBox = view.findViewById<EditText>(R.id.search_box).apply {
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun afterTextChanged(s: Editable?) {
                    clearBtn.visibility =
                        if (!s.isNullOrEmpty()) View.VISIBLE else View.GONE
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    (absListView.adapter as? Filterable)
                        ?.filter
                        ?.filter(s?.toString() ?: "")
                }
            })
        }

        clearBtn.visibility =
            if (searchBox.text.isNullOrEmpty()) View.GONE else View.VISIBLE

        clearBtn.setOnClickListener {
            searchBox.text = null
        }
    }

    private fun setup(gridView: AbsListView) {
        // ⚠️ excludeApps chỉ áp dụng tại thời điểm init
        // Load dần -> nên xử lý exclude trong Adapter
        val filtered =
            if (excludeApps.isEmpty()) packages
            else ArrayList(packages.filter { !excludeApps.contains(it.packageName) })

        adapter = AdapterAppChooser(gridView.context, filtered, multiple)
        gridView.adapter = adapter
    }

    fun showLoading(show: Boolean) {
        loadingView?.visibility = if (show) View.VISIBLE else View.GONE
        view?.findViewById<CompoundButton?>(R.id.select_all)?.isEnabled = !show
    }

    fun notifyDataChanged() {
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
    }

    interface Callback {
        fun onConfirm(apps: List<AdapterAppChooser.AppInfo>)
    }

    fun setExcludeApps(apps: Array<String>): DialogAppChooser {
        this.excludeApps = apps
        if (this.view != null) {
            Log.e(
                "@DialogAppChooser",
                "Exclusion list was set after view created, it may not take effect"
            )
        }
        return this
    }

    fun setAllowAllSelect(allow: Boolean): DialogAppChooser {
        this.allowAllSelect = allow
        view?.findViewById<CompoundButton?>(R.id.select_all)?.visibility =
            if (allow) View.VISIBLE else View.GONE
        return this
    }

    private fun onConfirm(gridView: AbsListView) {
        val apps =
            (gridView.adapter as? AdapterAppChooser)?.getSelectedItems() ?: emptyList()

        callback?.onConfirm(apps)
        dismiss()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
    }
}