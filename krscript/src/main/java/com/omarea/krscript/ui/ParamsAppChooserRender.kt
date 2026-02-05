package com.omarea.krscript.ui

import android.content.pm.PackageManager
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.omarea.common.ui.AdapterAppChooser
import com.omarea.common.ui.DialogAppChooser
import com.omarea.krscript.R
import com.omarea.krscript.model.ActionParamInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

class ParamsAppChooserRender(
    private var actionParamInfo: ActionParamInfo,
    private var context: FragmentActivity
) : DialogAppChooser.Callback {

    private val uiMode = context.resources.configuration.uiMode
    private val darkMode: Boolean =
        (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private lateinit var valueView: TextView
    private lateinit var nameView: TextView
    private lateinit var packages: ArrayList<AdapterAppChooser.AppInfo>

    // collator dùng chung cho toàn bộ quá trình sort
    private val collator: Collator = Collator.getInstance(Locale.getDefault())

    fun render(): View {
        val layout = LayoutInflater.from(context)
            .inflate(R.layout.kr_param_app, null)

        valueView = layout.findViewById(R.id.kr_param_app_package)
        nameView = layout.findViewById(R.id.kr_param_app_name)

        // giữ hành vi cũ: không load package ở đây
        setTextView()

        layout.findViewById<View>(R.id.kr_param_app_btn).setOnClickListener {
            openAppChooser()
        }
        nameView.setOnClickListener {
            openAppChooser()
        }

        valueView.tag = actionParamInfo.name
        return layout
    }

    // =======================
    // OPEN DIALOG
    // =======================
    private fun openAppChooser() {
        // 🔥 preload app đã chọn → có appName
        packages = preloadSelectedApps()

if (packages.isEmpty()) {
    // đảm bảo dialog không hiểu nhầm có selection
    packages.clear()
}
    
        val dialog = DialogAppChooser(
            darkMode,
            packages,
            actionParamInfo.multiple,
            this
        )
    
        dialog.show(context.supportFragmentManager, "app-chooser")
        dialog.showLoading(true)
    
        // load phần còn lại async
        loadPackagesAsync(dialog, actionParamInfo.type == "packages")
    }

    // =======================
    // SORTED INSERT
    // =======================
    private fun insertSorted(
        list: MutableList<AdapterAppChooser.AppInfo>,
        item: AdapterAppChooser.AppInfo
    ) {
        var low = 0
        var high = list.size
    
        val name = item.appName ?: ""
        val selected = item.selected
    
        while (low < high) {
            val mid = (low + high) ushr 1
            val m = list[mid]
    
            when {
                // 1️⃣ Ưu tiên app đã chọn
                m.selected != selected ->
                    if (selected) high = mid else low = mid + 1
    
                // 2️⃣ Cùng trạng thái → sort A–Z (đa ngôn ngữ)
                collator.compare(m.appName ?: "", name) < 0 ->
                    low = mid + 1
    
                else -> high = mid
            }
        }
    
        list.add(low, item)
    }

    // =======================
    // LOAD PACKAGE ASYNC + SORT TRONG LÚC LOAD
    // =======================
    private fun loadPackagesAsync(
        dialog: DialogAppChooser,
        includeMissing: Boolean
    ) {
        val pm = context.packageManager

        val filterSet = actionParamInfo.optionsFromShell
            ?.mapNotNull { it.value }
            ?.toHashSet()

        context.lifecycleScope.launch(Dispatchers.IO) {
            val apps = pm.getInstalledApplications(PackageManager.MATCH_ALL)

            val result = HashMap<String, AdapterAppChooser.AppInfo>(apps.size)
            val batch = ArrayList<AdapterAppChooser.AppInfo>(10)

            for ((index, app) in apps.withIndex()) {
                val pkg = app.packageName

                if (filterSet == null || filterSet.contains(pkg)) {
                    val info = AdapterAppChooser.AppInfo().apply {
                        packageName = pkg
                        appName = app.loadLabel(pm)?.toString()
                    }
                    result[pkg] = info
                    batch.add(info)
                }

                // đổ batch
                if (batch.size == 10 || index == apps.lastIndex) {
                    val copy = ArrayList(batch)
                    batch.clear()

                    withContext(Dispatchers.Main) {
                        for (info in copy) {
                            if (packages.any { it.packageName == info.packageName }) continue
                            insertSorted(packages, info)
                        }
                        // setSelectStatus()
                        dialog.notifyDataChanged()
                    }
                }
            }

            // thêm app thiếu (giữ hành vi cũ)
            if (includeMissing && actionParamInfo.optionsFromShell != null) {
                val missing = ArrayList<AdapterAppChooser.AppInfo>()
                for (item in actionParamInfo.optionsFromShell!!) {
                    val pkg = item.value
                    if (pkg != null && !result.containsKey(pkg)) {
                        missing.add(
                            AdapterAppChooser.AppInfo().apply {
                                packageName = pkg
                                appName = item.title
                            }
                        )
                    }
                }

                withContext(Dispatchers.Main) {
                    for (info in missing) {
                        insertSorted(packages, info)
                    }
                    dialog.notifyDataChanged()
                }
            }

            // kết thúc load
            withContext(Dispatchers.Main) {
                setSelectStatus()
                dialog.notifyDataChanged()
                dialog.showLoading(false)
            }
        }
    }

    // =======================
    // SELECTION LOGIC
    // =======================
    private fun setSelectStatus() {
        val currentValues = if (actionParamInfo.multiple) {
            valueView.text.toString()
                .split(actionParamInfo.separator)
                .filter { it.isNotEmpty() }
                .toSet()
        } else {
            setOf(valueView.text.toString())
        }
    
        packages.forEach {
            it.selected = it.packageName != null && currentValues.contains(it.packageName)
        }
    }

    // =======================
    // INIT UI VALUE (GIỮ NGUYÊN)
    // =======================
    private fun setTextView() {
        if (actionParamInfo.multiple) {
            val values = ActionParamsLayoutRender
                .getParamValues(actionParamInfo)
                ?.joinToString(actionParamInfo.separator)
                ?: ""

            valueView.text = values
            nameView.text = values
        } else {
            val value = ActionParamsLayoutRender
                .getParamValues(actionParamInfo)
                ?.firstOrNull()
                ?: ""

            valueView.text = value
            nameView.text = value
        }
    }

private fun resolveCurrentAppName() {
    val pm = context.packageManager

    if (actionParamInfo.multiple) {
        val pkgs = valueView.text.toString()
            .split(actionParamInfo.separator)
            .filter { it.isNotEmpty() }

        val names = ArrayList<String>(pkgs.size)
        for (pkg in pkgs) {
            try {
                val app = pm.getApplicationInfo(pkg, 0)
                names.add(app.loadLabel(pm)?.toString() ?: pkg)
            } catch (_: Exception) {
                names.add(pkg)
            }
        }
        nameView.text = names.joinToString("，")
    } else {
        val pkg = valueView.text.toString()
        if (pkg.isNotEmpty()) {
            try {
                val app = pm.getApplicationInfo(pkg, 0)
                nameView.text = app.loadLabel(pm)?.toString() ?: pkg
            } catch (_: Exception) {
                nameView.text = pkg
            }
        }
    }
}

private fun preloadSelectedApps(): ArrayList<AdapterAppChooser.AppInfo> {
    val pm = context.packageManager
    val result = ArrayList<AdapterAppChooser.AppInfo>()

    val values = if (actionParamInfo.multiple) {
        valueView.text.toString()
            .split(actionParamInfo.separator)
            .filter { it.isNotEmpty() }
    } else {
        listOf(valueView.text.toString()).filter { it.isNotEmpty() }
    }

    // ✅ nếu chưa chọn gì → trả list rỗng
    if (values.isEmpty()) return result

    for (pkg in values) {
        try {
            val app = pm.getApplicationInfo(pkg, 0)
            result.add(
                AdapterAppChooser.AppInfo().apply {
                    packageName = pkg
                    appName = app.loadLabel(pm)?.toString() ?: pkg
                    selected = true
                }
            )
        } catch (_: Exception) {
            result.add(
                AdapterAppChooser.AppInfo().apply {
                    packageName = pkg
                    appName = pkg
                    selected = true
                }
            )
        }
    }
    return result
}
    // =======================
    // CALLBACK
    // =======================
    override fun onConfirm(apps: List<AdapterAppChooser.AppInfo>) {
        if (actionParamInfo.multiple) {
            valueView.text =
                apps.joinToString(actionParamInfo.separator) { it.packageName ?: "" }
            nameView.text =
                apps.joinToString("，") { it.appName ?: it.packageName ?: "" }
        } else {
            val item = apps.firstOrNull()
            valueView.text = item?.packageName ?: ""
            nameView.text = item?.appName ?: item?.packageName ?: ""
        }
    }
}