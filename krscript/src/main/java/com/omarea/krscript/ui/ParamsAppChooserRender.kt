package com.omarea.krscript.ui

import android.content.pm.PackageManager
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.omarea.common.model.SelectItem
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

    fun render(): View {
        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_app, null)
        valueView = layout.findViewById(R.id.kr_param_app_package)
        nameView = layout.findViewById(R.id.kr_param_app_name)

        // giữ hành vi cũ
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
    // OPEN DIALOG + LOAD ASYNC
    // =======================
    private fun openAppChooser() {
        packages = ArrayList()

        val dialog = DialogAppChooser(
            darkMode,
            packages,
            actionParamInfo.multiple,
            this
        )

        dialog.show(context.supportFragmentManager, "app-chooser")
        loadPackagesAsync(dialog, actionParamInfo.type == "packages")
    }

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
                        packageName = pkg          // GIỮ NULL
                        appName = app.loadLabel(pm)?.toString() // GIỮ NULL
                    }
                    result[pkg] = info
                    batch.add(info)
                }

                if (batch.size == 10 || index == apps.lastIndex) {
                    val copy = ArrayList(batch)
                    batch.clear()

                    withContext(Dispatchers.Main) {
                        packages.addAll(copy)
                        setSelectStatus()
                        dialog.notifyDataChanged()
                    }
                }
            }

            // thêm app bị thiếu (giữ nguyên hành vi cũ)
            if (includeMissing && actionParamInfo.optionsFromShell != null) {
                val missing = ArrayList<AdapterAppChooser.AppInfo>()
                for (item in actionParamInfo.optionsFromShell!!) {
                    val pkg = item.value
                    if (pkg != null && !result.containsKey(pkg)) {
                        missing.add(
                            AdapterAppChooser.AppInfo().apply {
                                packageName = pkg
                                appName = item.title // có thể null
                            }
                        )
                    }
                }
                withContext(Dispatchers.Main) {
                    packages.addAll(missing)
                }
            }

            // sort cuối (giữ hành vi cũ)
            withContext(Dispatchers.Main) {
                val collator = Collator.getInstance(Locale.getDefault())
                packages.sortWith { a, b ->
                    collator.compare(a.appName ?: "", b.appName ?: "")
                }
                setSelectStatus()
                dialog.notifyDataChanged()
            }
        }
    }

    // =======================
    // SELECTION LOGIC (GIỮ NGUYÊN)
    // =======================
    private fun setSelectStatus() {
        packages.forEach { it.selected = false }

        val currentValue = valueView.text.toString()
        if (actionParamInfo.multiple) {
            currentValue.split(actionParamInfo.separator).forEach { value ->
                packages.find { it.packageName == value }?.selected = true
            }
        } else {
            packages.find { it.packageName == currentValue }?.selected = true
        }
    }

    // =======================
    // INIT UI VALUE (GIỮ NGUYÊN HÀNH VI)
    // =======================
    private fun setTextView() {
        packages = loadPackages(actionParamInfo.type == "packages")

        val packageMap = packages
            .filter { it.packageName != null }
            .associateBy { it.packageName!! }

        if (actionParamInfo.multiple) {
            ActionParamsLayoutRender.getParamValues(actionParamInfo)
                ?.forEach { value ->
                    packageMap[value]?.selected = true
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

    // =======================
    // GIỮ HÀM CŨ (FIX Unresolved reference)
    // =======================
    private fun loadPackages(includeMissing: Boolean): ArrayList<AdapterAppChooser.AppInfo> {
        val pm = context.packageManager
        val list = ArrayList<AdapterAppChooser.AppInfo>()

        val apps = pm.getInstalledApplications(PackageManager.MATCH_ALL)
        for (app in apps) {
            list.add(
                AdapterAppChooser.AppInfo().apply {
                    packageName = app.packageName
                    appName = app.loadLabel(pm)?.toString()
                }
            )
        }
        return list
    }

    // =======================
    // CALLBACK
    // =======================
    override fun onConfirm(apps: List<AdapterAppChooser.AppInfo>) {
        if (actionParamInfo.multiple) {
            valueView.text =
                apps.joinToString(actionParamInfo.separator) { it.packageName ?: "" }
            nameView.text =
                apps.joinToString("，") { it.appName ?: "" }
        } else {
            val item = apps.firstOrNull()
            valueView.text = item?.packageName ?: ""
            nameView.text = item?.appName ?: ""
        }
    }
}