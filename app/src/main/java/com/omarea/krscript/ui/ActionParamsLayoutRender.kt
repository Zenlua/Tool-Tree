package com.omarea.krscript.ui

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.FragmentActivity
import com.omarea.common.model.SelectItem
import com.tool.tree.R
import com.omarea.krscript.model.ActionParamInfo
import com.omarea.krscript.executor.ScriptEnvironmen
import androidx.core.graphics.toColorInt

class ActionParamsLayoutRender(private var linearLayout: LinearLayout, activity: FragmentActivity) {
    companion object {
        /**
         * 获取当前选中项索引（单选）
         */
        fun getParamOptionsCurrentIndex(actionParamInfo: ActionParamInfo, options: ArrayList<SelectItem>): Int {
            var selectedIndex = -1

            val valList = ArrayList<String>()
            if (actionParamInfo.valueFromShell != null)
                valList.add(actionParamInfo.valueFromShell!!)
            if (actionParamInfo.value != null) {
                valList.add(actionParamInfo.value!!)
            }
            if (valList.isNotEmpty()) {
                for (j in valList.indices) {
                    var index = 0
                    for (option in options) {
                        if (option.value == valList[j]) {
                            selectedIndex = index
                            break
                        }
                        index++
                    }
                    if (selectedIndex > -1)
                        break
                }
            }
            return selectedIndex
        }

        /**
         * 获取当前选中项索引（多选）
         */
        fun getParamOptionsSelectedStatus(actionParamInfo: ActionParamInfo, options: ArrayList<SelectItem>): BooleanArray {
            val status = BooleanArray(options.size)
            val values = getParamValues(actionParamInfo)

            for (index in 0 until options.size) {
                val option = options[index]
                status[index] = (values != null && values.contains(option.value))
            }
            return status
        }

        /**
         * 设置列表的选中状态
         */
        fun setParamOptionsSelectedStatus(actionParamInfo: ActionParamInfo, options: ArrayList<SelectItem>): ArrayList<SelectItem> {
            val values = getParamValues(actionParamInfo)

            for (index in 0 until options.size) {
                val option = options[index]
                options[index].selected = (values != null && values.contains(option.value))
            }
            return options
        }

        fun getParamValues (actionParamInfo: ActionParamInfo): List<String>? {
            val value = if (actionParamInfo.valueFromShell != null) actionParamInfo.valueFromShell else actionParamInfo.value
            val values = value?.split(actionParamInfo.separator)
            return values
        }
    }

    private var context: FragmentActivity = activity

    // key = actionParamInfo.name
    private val rowViews = HashMap<String, View>()
    private val valueReaders = HashMap<String, () -> String>()
    private var currentParamInfos: ArrayList<ActionParamInfo> = ArrayList()
    
    // ========== TÍNH NĂNG MỚI: LƯỚI TRẠNG THÁI HIỆN TẠI ==========
    // Lưu trạng thái ẩn/hiện hiện tại của từng param
    private val visibilityState = HashMap<String, Boolean>()

    fun renderList(actionParamInfos: ArrayList<ActionParamInfo>, fileChooser: ParamsFileChooserRender.FileChooserInterface?) {
        currentParamInfos = actionParamInfos
        rowViews.clear()
        valueReaders.clear()
        visibilityState.clear()

        for (actionParamInfo in actionParamInfos) {
            val options = actionParamInfo.optionsFromShell
            // 下拉框渲染
            if (options != null && !(actionParamInfo.type == "app" || actionParamInfo.type == "packages")) {
                if (actionParamInfo.multiple) {
                    val widget = ParamsMultipleSelect(actionParamInfo, context) { evaluateDependencies() }
                    val view = widget.render()
                    addToLayout(view, actionParamInfo)
                    actionParamInfo.name?.let { valueReaders[it] = { widget.getValue() } }
                } else {
                    val widget = ParamsSingleSelect(actionParamInfo, context) { evaluateDependencies() }
                    val view = widget.render()
                    addToLayout(view, actionParamInfo)
                    actionParamInfo.name?.let { valueReaders[it] = { widget.getValue() } }
                }
            }
            // 选择框渲染
            else if (actionParamInfo.type == "bool" || actionParamInfo.type == "checkbox") {
                val view = ParamsCheckbox(actionParamInfo, context).render()
                addToLayout(view, actionParamInfo)
                attachDefaultListener(view, actionParamInfo)
            }
            // 开关渲染
            else if (actionParamInfo.type == "switch") {
                val view = ParamsSwitch(actionParamInfo, context).render()
                addToLayout(view, actionParamInfo)
                attachDefaultListener(view, actionParamInfo)
            }
            // 滑块
            else if (actionParamInfo.type == "seekbar") {
                val layout = ParamsSeekBar(actionParamInfo, context) { evaluateDependencies() }.render()

                addToLayout(layout, actionParamInfo)
                actionParamInfo.name?.let { name ->
                    valueReaders[name] = {
                        val seekBar = linearLayout.findViewWithTag<SeekBar?>(name)
                        if (seekBar != null) (seekBar.progress + actionParamInfo.min).toString() else ""
                    }
                }
            }
            // 文件选择
            else if (actionParamInfo.type == "file" || actionParamInfo.type == "folder") {
                val layout = ParamsFileChooserRender(actionParamInfo, context, fileChooser) { evaluateDependencies() }.render()

                addToLayout(layout, actionParamInfo)
                actionParamInfo.name?.let { name ->
                    valueReaders[name] = { linearLayout.findViewWithTag<TextView?>(name)?.text?.toString() ?: "" }
                }
            }
            // 应用选择
            else if (actionParamInfo.type == "app" || actionParamInfo.type == "packages") {
                val layout = ParamsAppChooserRender(actionParamInfo, context) { evaluateDependencies() }.render()

                addToLayout(layout, actionParamInfo)
                actionParamInfo.name?.let { name ->
                    valueReaders[name] = { linearLayout.findViewWithTag<TextView?>(name)?.text?.toString() ?: "" }
                }
            }
            // 颜色输入
            else if (actionParamInfo.type == "color") {
                val layout = ParamsColorPicker(actionParamInfo, context).render()

                addToLayout(layout, actionParamInfo)
                attachDefaultListener(layout, actionParamInfo)
            }
            // 文本框渲染
            else {
                val view = ParamsEditText(actionParamInfo, context).render()
                addToLayout(view, actionParamInfo)
                attachDefaultListener(view, actionParamInfo)
            }
        }

        // ========== TÍNH NĂNG MỚI: ĐẶT TRẠNG THÁI KHỞI ĐỘNG ==========
        initializeDependencyStates()
        evaluateDependencies()
    }

    // ========== TÍNH NĂNG MỚI: KHỞI TẠO TRẠNG THÁI PHỤ THUỘC ==========
    private fun initializeDependencyStates() {
        for (info in currentParamInfos) {
            val name = info.name ?: continue
            val initialState = info.dependInitialState.trim().lowercase()

            val initialVisibility = when (initialState) {
                "hide" -> false
                "show" -> true
                else -> {
                    // "auto": xác định tự động dựa trên dependDefault
                    info.dependDefault.trim().lowercase() != "hide"
                }
            }

            // CHỈ set View.visibility (hoặc mờ/khóa với depend-readonly) tức thời để tránh
            // nhấp nháy lúc mở dialog. KHÔNG ghi vào visibilityState ở đây: evaluateDependencies()
            // được gọi ngay sau initializeDependencyStates() sẽ tính trạng thái thật (có cascade)
            // và mới là nơi ghi visibilityState + quyết định gọi depend-onchange. Nếu ghi ở đây,
            // lần đánh giá đầu tiên sẽ hiểu nhầm đây là "thay đổi trạng thái" và gọi nhầm
            // callback ngay khi dialog vừa mở.
            val row = rowViews[name] ?: continue
            if (info.dependReadonly) {
                // Luôn hiện, chỉ mờ/khóa tương tác theo initialVisibility - không set GONE,
                // tránh hàng bị "biến mất" rồi mới hiện lại (nhấp nháy) khi dialog vừa mở.
                row.visibility = View.VISIBLE
                setRowInteractive(row, initialVisibility)
            } else {
                row.visibility = if (initialVisibility) View.VISIBLE else View.GONE
            }
        }
    }

    private fun attachDefaultListener(view: View, info: ActionParamInfo) {
        val name = info.name ?: return
        valueReaders[name] = { readValueDeep(view, info) }

        val target: View = when (view) {
            is Spinner, is CheckBox, is Switch, is EditText, is SeekBar -> view
            else -> (view as? ViewGroup)?.let { findTypedChild(it) } ?: view
        }

        when (target) {
            is Spinner -> target.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = evaluateDependencies()
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
            is CheckBox -> target.setOnCheckedChangeListener { _, _ -> evaluateDependencies() }
            is Switch -> target.setOnCheckedChangeListener { _, _ -> evaluateDependencies() }
            is EditText -> target.addTextChangedListener(object : android.text.TextWatcher {
                override fun afterTextChanged(s: android.text.Editable?) = evaluateDependencies()
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            })
            is SeekBar -> target.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, u: Boolean) = evaluateDependencies()
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
    }

    private fun findTypedChild(group: ViewGroup): View? {
        for (i in 0 until group.childCount) {
            val child = group.getChildAt(i)
            if (child is Spinner || child is CheckBox || child is Switch || child is EditText || child is SeekBar) {
                return child
            }
            if (child is ViewGroup) {
                findTypedChild(child)?.let { return it }
            }
        }
        return null
    }

    private fun readValueDeep(view: View, info: ActionParamInfo): String {
        val target: View = when (view) {
            is Spinner, is CheckBox, is Switch, is EditText, is SeekBar -> view
            else -> (view as? ViewGroup)?.let { findTypedChild(it) } ?: view
        }
        return when (target) {
            is EditText -> target.text.toString()
            is CheckBox -> if (target.isChecked) "1" else "0"
            is Switch -> if (target.isChecked) "1" else "0"
            is SeekBar -> (target.progress + info.min).toString()
            is Spinner -> (target.selectedItem as? SelectItem)?.value ?: target.selectedItem?.toString().orEmpty()
            else -> ""
        }
    }

    private val parenPattern = Regex("\\(([^()]*)\\)")

    private fun buildValueIdentifiers(value: String, options: ArrayList<SelectItem>?): Set<String> {
        val identifiers = HashSet<String>()
        identifiers.add(value)

        val title = options?.find { it.value == value }?.title?.trim()
        if (!title.isNullOrEmpty()) {
            identifiers.add(title)
            parenPattern.findAll(title).forEach { m ->
                val inner = m.groupValues[1].trim()
                if (inner.isNotEmpty()) {
                    identifiers.add(inner)
                    identifiers.add("(" + inner + ")")
                }
            }
        }
        return identifiers
    }

    // ========== TÍNH NĂNG MỚI: ĐÁNH GIÁ PHỤ THUỘC NÂNG CẤP (có depend-cascade) ==========
    private fun evaluateDependencies() {
        // Trạng thái TRƯỚC lượt đánh giá này - dùng để phát hiện thay đổi thật sự và quyết
        // định có gọi depend-onchange hay không (so với trạng thái đã áp dụng lần trước, KHÔNG
        // so giữa các lượt lặp nội bộ bên dưới).
        val previousState = HashMap(visibilityState)

        // Bộ nhớ TẠM cho lượt đánh giá này. Lặp nhiều lượt để "cha ẩn thì con ẩn theo"
        // (depend-cascade) lan truyền đúng qua các chuỗi phụ thuộc nhiều cấp, bất kể param cha
        // được khai báo trước hay sau param con trong file XML. Dừng sớm khi không còn gì
        // thay đổi giữa 2 lượt liên tiếp (hầu hết trường hợp chỉ cần 1-2 lượt).
        val working = HashMap<String, Boolean>()
        val maxPasses = currentParamInfos.size.coerceAtLeast(1).coerceAtMost(20)

        for (pass in 0 until maxPasses) {
            var changedThisPass = false

            for (info in currentParamInfos) {
                val name = info.name ?: continue
                val shouldShow = computeShouldShow(info, working)

                if (working[name] != shouldShow) {
                    working[name] = shouldShow
                    changedThisPass = true
                }
            }

            if (!changedThisPass) break
        }

        // Áp dụng kết quả cuối cùng lên UI, ghi lại visibilityState chính thức, và gọi
        // depend-onchange đúng 1 lần cho mỗi param có trạng thái thực sự thay đổi so với
        // trước khi vào hàm evaluateDependencies() này.
        for (info in currentParamInfos) {
            val name = info.name ?: continue
            val shouldShow = working[name] ?: continue
            applyVisibility(name, shouldShow, previousState[name])
        }
    }

    // Tính shouldShow cho 1 param dựa trên trạng thái ẩn/hiện của các param cha TRONG LƯỢT
    // ĐÁNH GIÁ HIỆN TẠI (map "working"), không phải trạng thái đã chốt từ lần render trước.
    private fun computeShouldShow(info: ActionParamInfo, working: HashMap<String, Boolean>): Boolean {
        val dependOnRaw = info.dependOn?.trim()

        if (dependOnRaw.isNullOrEmpty()) {
            return info.dependDefault.trim().lowercase() != "hide"
        }

        val dependOnList = dependOnRaw.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        if (dependOnList.isEmpty()) {
            return info.dependDefault.trim().lowercase() != "hide"
        }

        // ========== TÍNH NĂNG MỚI: CHA ẨN THÌ CON ẨN THEO (depend-cascade) ==========
        // Nếu BẤT KỲ param cha nào trong depend-on đang bị ẩn (đã tính ở lượt trước/pass hiện
        // tại), param này ẩn theo luôn, không cần xét tiếp depend-value/depend-logic.
        if (info.dependCascade && dependOnList.any { working[it] == false }) {
            return false
        }

        val dependValueList = (info.dependValue ?: "").split("|")
        val dependModeList = info.dependMode.split("|")

        fun evalCondition(i: Int): Pair<Boolean, Boolean>? {
            val parentName = dependOnList[i]
            val controllerInfo = currentParamInfos.find { it.name == parentName }
            val reader = valueReaders[parentName]
            if (controllerInfo == null || reader == null) return null

            val currentValues = reader().split(controllerInfo.separator)
                .map { it.trim() }.filter { it.isNotEmpty() }
            val parentOptions = controllerInfo.optionsFromShell ?: controllerInfo.options

            val currentIdentifiers = HashSet<String>()
            for (v in currentValues) {
                currentIdentifiers.addAll(buildValueIdentifiers(v, parentOptions))
            }

            val wantedRaw = dependValueList.getOrNull(i) ?: dependValueList.lastOrNull() ?: ""
            val wanted = wantedRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val matched = wanted.isEmpty() || wanted.any { currentIdentifiers.contains(it) }

            val mode = (dependModeList.getOrNull(i) ?: dependModeList.lastOrNull() ?: "show").trim()
            val wantShow = if (mode == "hide") !matched else matched
            return Pair(matched, wantShow)
        }

        val logic = info.dependLogic.trim().lowercase()
        return when (logic) {
            "priority", "or", "priority-ltr", "or-ltr" -> {
                // Ưu tiên trái -> phải
                var result = info.dependDefault.trim().lowercase() != "hide"
                for (i in dependOnList.indices) {
                    val (matched, wantShow) = evalCondition(i) ?: continue
                    if (matched) {
                        result = wantShow
                        break
                    }
                }
                result
            }
            "priority-rtl", "or-rtl" -> {
                // Ưu tiên phải -> trái
                var result = info.dependDefault.trim().lowercase() != "hide"
                for (i in dependOnList.indices.reversed()) {
                    val (matched, wantShow) = evalCondition(i) ?: continue
                    if (matched) {
                        result = wantShow
                        break
                    }
                }
                result
            }
            "xor" -> {
                // Chỉ ĐÚNG MỘT điều kiện được thỏa
                var matchCount = 0
                for (i in dependOnList.indices) {
                    val (matched, _) = evalCondition(i) ?: continue
                    if (matched) matchCount++
                }
                (matchCount == 1) != info.dependNegate
            }
            "nand" -> {
                // Phủ định của AND (KHÔNG phải tất cả điều kiện đều thỏa)
                var result = true
                for (i in dependOnList.indices) {
                    val (_, wantShow) = evalCondition(i) ?: continue
                    if (!wantShow) {
                        result = false
                        break
                    }
                }
                !result != info.dependNegate
            }
            else -> {
                // "and" (mặc định)
                var satisfiedCount = 0
                var totalCount = 0

                for (i in dependOnList.indices) {
                    val (_, wantShow) = evalCondition(i) ?: continue
                    totalCount++
                    if (wantShow) satisfiedCount++
                }

                val threshold = if (info.dependThreshold < 0) {
                    // Mặc định: 100% (tất cả phải thỏa)
                    totalCount
                } else {
                    // Tính toán % ngưỡng
                    (totalCount * info.dependThreshold / 100).coerceAtLeast(1)
                }

                val result = satisfiedCount >= threshold
                result != info.dependNegate
            }
        }
    }

    // ========== TÍNH NĂNG MỚI: ÁP DỤNG TRẠNG THÁI LÊN UI + GỌI CALLBACK ==========
    private fun applyVisibility(name: String, shouldShow: Boolean, oldState: Boolean?) {
        visibilityState[name] = shouldShow

        val view = rowViews[name]
        val info = currentParamInfos.find { it.name == name }

        if (info?.dependReadonly == true) {
            // ========== TÍNH NĂNG MỚI: depend-readonly ==========
            // Không ẩn (GONE) mà giữ VISIBLE, chỉ làm mờ + khóa tương tác (isEnabled = false)
            // khi điều kiện phụ thuộc không thỏa (shouldShow = false).
            view?.visibility = View.VISIBLE
            view?.let { setRowInteractive(it, shouldShow) }
        } else {
            view?.visibility = if (shouldShow) View.VISIBLE else View.GONE
        }

        // Chỉ gọi callback khi ĐÃ TỪNG có trạng thái trước đó (oldState != null) VÀ trạng thái
        // thực sự đổi - tránh gọi callback ngay khi dialog vừa mở (lần đầu tiên oldState luôn
        // null vì initializeDependencyStates() không ghi vào visibilityState).
        if (oldState != null && oldState != shouldShow) {
            val script = info?.dependOnChangeCallback
            if (!script.isNullOrEmpty()) {
                executeDependOnChangeCallback(name, shouldShow, script)
            }
        }
    }

    // ========== TÍNH NĂNG MỚI: BẬT/TẮT TƯƠNG TÁC + LÀM MỜ CHO depend-readonly ==========
    // enabled = true  -> hiện bình thường, hết mờ, có thể bấm/nhập/chọn
    // enabled = false -> làm mờ cả hàng (alpha) và vô hiệu hóa toàn bộ control con
    //                     (EditText/CheckBox/Switch/SeekBar/Spinner/nút bấm...) để
    //                     người dùng không thể chỉnh sửa giá trị, nhưng vẫn nhìn thấy nó.
    private fun setRowInteractive(row: View, enabled: Boolean) {
        row.alpha = if (enabled) 1f else 0.4f
        setEnabledRecursively(row, enabled)
    }

    private fun setEnabledRecursively(view: View, enabled: Boolean) {
        view.isEnabled = enabled
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setEnabledRecursively(view.getChildAt(i), enabled)
            }
        }
    }

    // ========== TÍNH NĂNG MỚI: THỰC THI depend-onchange ==========
    // Chạy script/callback trên luồng nền (KHÔNG được chạy trực tiếp trên UI thread vì lệnh
    // shell root có thể mất nhiều thời gian và sẽ làm treo giao diện). Truyền tên param vừa
    // đổi trạng thái + trạng thái mới (1 = đang hiện, 0 = đang ẩn) làm biến môi trường để
    // script có thể tự xử lý theo ngữ cảnh.
    private fun executeDependOnChangeCallback(paramName: String, visible: Boolean, script: String) {
        Thread {
            try {
                val extraParams = HashMap<String, String>()
                extraParams["PARAM_NAME"] = paramName
                extraParams["PARAM_VISIBLE"] = if (visible) "1" else "0"
                ScriptEnvironmen.executeResultRoot(context, script, null, extraParams)
            } catch (ex: Exception) {
                // Lỗi khi chạy callback không được làm crash hay ảnh hưởng tới UI chính
            }
        }.start()
    }

    private val hideLabelTypes = arrayOf("bool", "checkbox", "switch")
    private fun addToLayout(inputView: View, actionParamInfo: ActionParamInfo) {
        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_row, null)
        if (!actionParamInfo.title.isNullOrEmpty()) {
            layout.findViewById<TextView>(R.id.kr_param_title).text = actionParamInfo.title
        } else {
            layout.findViewById<TextView>(R.id.kr_param_title).visibility = View.GONE
        }

        if ((!actionParamInfo.label.isNullOrEmpty()) && !hideLabelTypes.contains(actionParamInfo.type)) {
            layout.findViewById<TextView>(R.id.kr_param_label).run {
                text = actionParamInfo.label
            }
        } else {
            layout.findViewById<TextView>(R.id.kr_param_label).visibility = View.GONE
        }

        if (!actionParamInfo.desc.isNullOrEmpty()) {
            layout.findViewById<TextView>(R.id.kr_param_desc).text = actionParamInfo.desc
        } else {
            layout.findViewById<TextView>(R.id.kr_param_desc).visibility = View.GONE
        }

        layout.findViewById<FrameLayout>(R.id.kr_param_input).addView(inputView)
        linearLayout.addView(layout)

        (inputView.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.CENTER_VERTICAL

        actionParamInfo.name?.let { rowViews[it] = layout }
    }

    private fun getFieldTips(actionParamInfo: ActionParamInfo): String {
        val tips = StringBuilder()
        if (!actionParamInfo.title.isNullOrEmpty()) {
            tips.append(actionParamInfo.title)
            tips.append(" ")
        }
        if (!actionParamInfo.label.isNullOrEmpty()) {
            tips.append(actionParamInfo.label)
            tips.append(" ")
        }
        tips.append("(")
        tips.append(actionParamInfo.name)
        tips.append(") ")
        return tips.toString()
    }

    fun readParamsValue(actionParamInfos: ArrayList<ActionParamInfo>): HashMap<String, String> {
        val params = HashMap<String, String>()
        for (actionParamInfo in actionParamInfos) {
            if (actionParamInfo.name == null) {
                continue
            }

            when (val view = linearLayout.findViewWithTag<View>(actionParamInfo.name)) {
                is EditText -> {
                    val text = view.text.toString()
                    if (text.isNotEmpty()) {
                        if ((actionParamInfo.type == "int" || actionParamInfo.type == "number")) {
                            try {
                                val value = text.toInt()
                                if (value < actionParamInfo.min) {
                                    throw Exception("${getFieldTips(actionParamInfo)} $value < ${actionParamInfo.min} !!!")
                                } else if (value > actionParamInfo.max) {
                                    throw Exception("${getFieldTips(actionParamInfo)} $value > ${actionParamInfo.max} !!!")
                                }
                            } catch (ex: java.lang.NumberFormatException) {
                            }
                        } else if (actionParamInfo.type == "color") {
                            try {
                                text.toColorInt()
                            } catch (ex: java.lang.Exception) {
                                throw Exception(
                                    "" + getFieldTips(actionParamInfo) + "  \n" + context.getString(
                                        R.string.kr_invalid_color
                                    )
                                )
                            }
                        }
                    }
                    actionParamInfo.value = text
                }

                is CheckBox -> {
                    actionParamInfo.value = if (view.isChecked) "1" else "0"
                }

                is Switch -> {
                    actionParamInfo.value = if (view.isChecked) "1" else "0"
                }

                is SeekBar -> {
                    val text = (view.progress + actionParamInfo.min).toString()
                    actionParamInfo.value = text
                }

                is TextView -> {
                    actionParamInfo.value = view.text.toString()
                }

                is Spinner -> {
                    val item = view.selectedItem
                    when {
                        item is SelectItem -> {
                            actionParamInfo.value = item.value
                        }

                        item != null -> actionParamInfo.value = item.toString()
                        else -> actionParamInfo.value = ""
                    }
                }
            }

            // Dùng visibilityState (kết quả logic của evaluateDependencies) thay vì đọc trực
            // tiếp View.visibility, vì với depend-readonly=true, view vẫn ở trạng thái
            // VISIBLE (chỉ bị mờ + khóa tương tác) dù về mặt logic vẫn được xem là "không
            // thỏa điều kiện" (shouldShow = false).
            val isHiddenByDepend = visibilityState[actionParamInfo.name] == false
            
            // ========== TÍNH NĂNG MỚI: BỎ QUA PARAM ẨN NẾU KHÔNG CÓ dependIncludeHidden ==========
            if (isHiddenByDepend && !actionParamInfo.dependIncludeHidden) {
                // Bỏ qua param ẩn - không kiểm tra required
                if (!actionParamInfo.value.isNullOrEmpty()) {
                    params[actionParamInfo.name!!] = actionParamInfo.value!!
                }
                continue
            }
            
            if (actionParamInfo.value.isNullOrEmpty()) {
                if (actionParamInfo.required && !isHiddenByDepend) {
                    throw Exception(getFieldTips(actionParamInfo) + context.getString(R.string.do_not_empty))
                } else {
                    params[actionParamInfo.name!!] = ""
                }
            } else {
                params[actionParamInfo.name!!] = actionParamInfo.value!!
            }
        }
        return params
    }

    fun updateParamsView(actionParamInfos: ArrayList<ActionParamInfo>) {
        for (actionParamInfo in actionParamInfos) {
            if (actionParamInfo.name == null) {
                continue
            }

            val view = linearLayout.findViewWithTag<View>(actionParamInfo.name)
            if (view != null) {
                // TODO: Refresh interface display
            }
        }
    }
}
