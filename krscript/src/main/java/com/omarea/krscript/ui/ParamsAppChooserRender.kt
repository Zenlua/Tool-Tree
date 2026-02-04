package com.omarea.krscript.ui

import android.content.pm.PackageManager
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.omarea.common.model.SelectItem
import com.omarea.common.ui.AdapterAppChooser
import com.omarea.common.ui.DialogAppChooser
import com.omarea.krscript.R
import com.omarea.krscript.model.ActionParamInfo
import java.util.Locale
import java.text.Collator

class ParamsAppChooserRender(private var actionParamInfo: ActionParamInfo, private var context: FragmentActivity) : DialogAppChooser.Callback {
    // Sử dụng Configuration để xác định chế độ sáng/tối
    private val uiMode = context.resources.configuration.uiMode
    private var darkMode: Boolean = (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private lateinit var valueView: TextView
    private lateinit var nameView: TextView
    private lateinit var packages: ArrayList<AdapterAppChooser.AppInfo>

    fun render(): View {
        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_app, null)
        valueView = layout.findViewById(R.id.kr_param_app_package)
        nameView = layout.findViewById(R.id.kr_param_app_name)

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

    private fun openAppChooser() {
        setSelectStatus()

        // Gọi DialogAppChooser với chế độ tối/sáng
        DialogAppChooser(darkMode, packages, actionParamInfo.multiple, this).show(context.supportFragmentManager, "app-chooser")
    }

private fun loadPackages(includeMissing: Boolean = false): ArrayList<AdapterAppChooser.AppInfo> {
    val pm = context.packageManager

    val filterSet = actionParamInfo.optionsFromShell
        ?.map { it.value }
        ?.toHashSet()

    val result = HashMap<String, AdapterAppChooser.AppInfo>(128)

    pm.getInstalledApplications(PackageManager.MATCH_ALL).forEach { app ->
        val pkg = app.packageName
        if (filterSet == null || filterSet.contains(pkg)) {
            result[pkg] = AdapterAppChooser.AppInfo().apply {
                packageName = pkg
                appName = app.loadLabel(pm)?.toString() ?: pkg
            }
        }
    }

    // thêm app bị thiếu
    if (includeMissing && actionParamInfo.optionsFromShell != null) {
        for (item in actionParamInfo.optionsFromShell!!) {
            if (!result.containsKey(item.value)) {
                result[item.value] = AdapterAppChooser.AppInfo().apply {
                    packageName = item.value
                    appName = item.title ?: item.value
                }
            }
        }
    }

    return ArrayList(result.values)
}

    private fun setSelectStatus() {
        packages.forEach {
            it.selected = false
        }
        val currentValue = valueView.text
        if (actionParamInfo.multiple) {
            currentValue.split(actionParamInfo.separator).run {
                this.forEach {
                    val value = it
                    val app = packages.find { it.packageName == value }
                    if (app != null) {
                        app.selected = true
                    }
                }
            }
        } else {
            val current = packages.find { it.packageName == currentValue }
            val currentIndex = if (current != null) packages.indexOf(current) else -1
            if (currentIndex > -1) {
                packages[currentIndex].selected = true
            }
        }
    }

    // 设置界面显示和元素赋值
private fun setTextView() {
    packages = loadPackages(actionParamInfo.type == "packages")

    // sort theo locale
    val collator = Collator.getInstance(Locale.getDefault())
    packages.sortWith { a, b ->
        collator.compare(a.appName ?: "", b.appName ?: "")
    }

    // map nhanh theo packageName
    val packageMap = packages
        .filter { it.packageName != null }
        .associateBy { it.packageName!! }

    if (actionParamInfo.multiple) {
        ActionParamsLayoutRender.getParamValues(actionParamInfo)
            ?.forEach { value ->
                packageMap[value]?.selected = true
            }

        // giữ hành vi cũ: hiển thị ngay
        onConfirm(packages.filter { it.selected })

    } else {
        val validOptions = ArrayList<SelectItem>(packages.size)
        packages.forEach {
            validOptions.add(
                SelectItem().apply {
                    title = it.appName ?: ""
                    value = it.packageName ?: ""
                }
            )
        }

        val currentIndex =
            ActionParamsLayoutRender.getParamOptionsCurrentIndex(
                actionParamInfo,
                validOptions
            )

        if (currentIndex >= 0) {
            val item = packages[currentIndex]
            valueView.text = item.packageName.orEmpty()
            nameView.text = item.appName.orEmpty()
        } else {
            valueView.text = ""
            nameView.text = ""
        }
    }
}

    override fun onConfirm(apps: List<AdapterAppChooser.AppInfo>) {
        if (actionParamInfo.multiple) {
            val values = apps.joinToString(actionParamInfo.separator) { it.packageName }
            val labels = apps.joinToString("，") { it.appName }
            valueView.text = values
            nameView.text = labels
        } else {
            val item = apps.firstOrNull()
            if (item == null) {
                valueView.text = ""
                nameView.text = ""
            } else {
                valueView.text = item.packageName
                nameView.text = item.appName
            }
        }
    }
}