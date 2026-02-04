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
import java.text.Collator
import java.util.Locale
import java.util.HashMap
import java.util.HashSet
import java.util.ArrayList

class ParamsAppChooserRender(
    private var actionParamInfo: ActionParamInfo,
    private var context: FragmentActivity
) : DialogAppChooser.Callback {

    companion object {
        // Cache danh sách app để tránh load lại nhiều lần
        private var cachedApps: List<AdapterAppChooser.AppInfo>? = null
        private var cachedAppsWithMissing: List<AdapterAppChooser.AppInfo>? = null
    }

    private val uiMode = context.resources.configuration.uiMode
    private val darkMode =
        (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private lateinit var valueView: TextView
    private lateinit var nameView: TextView
    private lateinit var packages: ArrayList<AdapterAppChooser.AppInfo>

    fun render(): View {
        val layout = LayoutInflater.from(context)
            .inflate(R.layout.kr_param_app, null)

        valueView = layout.findViewById(R.id.kr_param_app_package)
        nameView = layout.findViewById(R.id.kr_param_app_name)

        setTextView()

        layout.findViewById<View>(R.id.kr_param_app_btn)
            .setOnClickListener { openAppChooser() }

        nameView.setOnClickListener { openAppChooser() }

        valueView.tag = actionParamInfo.name
        return layout
    }

    private fun openAppChooser() {
        setSelectStatus()
        DialogAppChooser(
            darkMode,
            packages,
            actionParamInfo.multiple,
            this
        ).show(context.supportFragmentManager, "app-chooser")
    }

    /**
     * Load & cache danh sách app (tối ưu PM + tránh O(n²))
     */
    private fun loadPackages(includeMissing: Boolean): List<AdapterAppChooser.AppInfo> {
        if (!includeMissing && cachedApps != null) return cachedApps!!
        if (includeMissing && cachedAppsWithMissing != null) return cachedAppsWithMissing!!

        val pm = context.packageManager
        val filterSet = actionParamInfo.optionsFromShell
            ?.mapTo(HashSet()) { it.value }

        val appMap = HashMap<String, AdapterAppChooser.AppInfo>(128)

        pm.getInstalledApplications(PackageManager.MATCH_ALL).forEach { app ->
            val pkg = app.packageName
            if (filterSet == null || filterSet.contains(pkg)) {
                appMap[pkg] = AdapterAppChooser.AppInfo().apply {
                    packageName = pkg
                    appName = app.loadLabel(pm).toString()
                }
            }
        }

        // include missing packages
        if (includeMissing && actionParamInfo.optionsFromShell != null) {
            for (item in actionParamInfo.optionsFromShell!!) {
                val pkg = item.value ?: continue
                if (!appMap.containsKey(pkg)) {
                    appMap[pkg] = AdapterAppChooser.AppInfo().apply {
                        packageName = pkg
                        appName = item.title ?: ""
                    }
                }
            }
        }

        val collator = Collator.getInstance(Locale.getDefault())
        val result = appMap.values.sortedWith { a, b ->
            collator.compare(a.appName ?: "", b.appName ?: "")
        }

        if (includeMissing) {
            cachedAppsWithMissing = result
        } else {
            cachedApps = result
        }
        return result
    }

    /**
     * Đặt trạng thái selected (O(n))
     */
    private fun setSelectStatus() {
        val map = packages
            .filter { it.packageName != null }
            .associateBy { it.packageName!! }
        packages.forEach { it.selected = false }

        if (actionParamInfo.multiple) {
            valueView.text
                .split(actionParamInfo.separator)
                .forEach { map[it]?.selected = true }
        } else {
            map[valueView.text]?.selected = true
        }
    }

    /**
     * Gán dữ liệu hiển thị ban đầu
     */
    private fun setTextView() {
        packages = ArrayList(loadPackages(actionParamInfo.type == "packages"))

        if (actionParamInfo.multiple) {
            ActionParamsLayoutRender.getParamValues(actionParamInfo)
                ?.forEach { value ->
                    packages.firstOrNull { it.packageName == value }?.selected = true
                }
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
                valueView.text = item.packageName ?: ""
                nameView.text = item.appName ?: ""
            } else {
                valueView.text = ""
                nameView.text = ""
            }
        }
    }

    /**
     * Callback từ DialogAppChooser
     */
    override fun onConfirm(apps: List<AdapterAppChooser.AppInfo>) {
        if (actionParamInfo.multiple) {
            valueView.text =
                apps.joinToString(actionParamInfo.separator) { it.packageName ?: "" }
            nameView.text =
                apps.joinToString("，") { it.appName ?: "" }
        } else {
            val item = apps.firstOrNull()
            valueView.text = item?.packageName.orEmpty()
            nameView.text = item?.appName.orEmpty()
        }
    }
}