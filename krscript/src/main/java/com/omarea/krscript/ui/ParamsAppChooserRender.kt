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
    private var darkMode: Boolean =
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
        packages = ArrayList() // rỗng ban đầu

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
            ?.map { it.value }
            ?.toHashSet()

        context.lifecycleScope.launch(Dispatchers.IO) {
            val apps = pm.getInstalledApplications(PackageManager.MATCH_ALL)

            val batch = ArrayList<AdapterAppChooser.AppInfo>(10)
            val result = HashMap<String, AdapterAppChooser.AppInfo>(128)

            for ((index, app) in apps.withIndex()) {
                val pkg = app.packageName

                if (filterSet == null || filterSet.contains(pkg)) {
                    val info = AdapterAppChooser.AppInfo().apply {
                        packageName = pkg                     // GIỮ NULL
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
                        setSelectStatus()          // GIỮ LOGIC CŨ
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

            // SORT SAU KHI LOAD XONG (GIỮ HÀNH VI CŨ)
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

        val currentValue = valueView.text
        if (actionParamInfo.multiple) {
            currentValue.split(actionParamInfo.separator).forEach { value ->
                packages.find { it.packageName == value }?.selected = true
            }
        } else {
            val current = packages.find { it.packageName == currentValue }
            if (current != null) {
                current.selected = true
            }
        }
    }

    // =======================
    // INIT UI VALUE (GIỮ NGUYÊN 100%)
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
            valueView.text =
                apps.joinToString(actionParamInfo.separator) { it.packageName }
            nameView.text =
                apps.joinToString("，") { it.appName }
        } else {
            val item = apps.firstOrNull()
            valueView.text = item?.packageName ?: ""
            nameView.text = item?.appName ?: ""
        }
    }
}