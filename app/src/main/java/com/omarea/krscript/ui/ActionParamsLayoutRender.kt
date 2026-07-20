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
import androidx.core.graphics.toColorInt

class ActionParamsLayoutRender(private var linearLayout: LinearLayout, activity: FragmentActivity) {
    companion object {
        /**
         * 获取当前选中项索引（单选）
         * @param ActionParamInfo actionParamInfo 参数信息
         * @param ArrayList<HashMap<String, Any>> options 使用getParamOptions获得的数据（不为空时）
         */
        fun getParamOptionsCurrentIndex(actionParamInfo: ActionParamInfo, options: ArrayList<SelectItem>): Int {
            var selectedIndex = -1

            val valList = ArrayList<String>()
            if (actionParamInfo.valueFromShell != null)
                valList.add(actionParamInfo.valueFromShell!!)
            // TODO:这里可能有点争议
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
         * @param ActionParamInfo actionParamInfo 参数信息
         * @param ArrayList<HashMap<String, Any>> options 使用getParamOptions获得的数据（不为空时）
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
         * @param ActionParamInfo actionParamInfo 参数信息
         * @param ArrayList<HashMap<String, Any>> options 使用getParamOptions获得的数据（不为空时）
         */
        fun setParamOptionsSelectedStatus(actionParamInfo: ActionParamInfo, options: ArrayList<SelectItem>): ArrayList<SelectItem> {
            val values = getParamValues(actionParamInfo)

            for (index in 0 until options.size) {
                val option = options[index]
                options[index].selected = (values != null && values.contains(option.value))
            }
            return options
        }

        // 获取多选下拉的选中值列表
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

    fun renderList(actionParamInfos: ArrayList<ActionParamInfo>, fileChooser: ParamsFileChooserRender.FileChooserInterface?) {
        currentParamInfos = actionParamInfos
        rowViews.clear()
        valueReaders.clear()

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
                // Widget này lưu đường dẫn trong 1 EditText con có tag=name -> tìm sâu để đọc giá trị
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

        evaluateDependencies() // set trạng thái ẩn/hiện ngay khi vừa mở dialog
    }

    // Gắn value-reader + listener cho các widget mà input là 1 View cụ thể (Spinner/EditText/CheckBox/Switch/SeekBar)
    // hoặc 1 layout tổng hợp có chứa 1 trong các View đó bên trong (vd ParamsSeekBar/ParamsColorPicker bọc EditText/SeekBar).
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

    // Dò tìm 1 View con thuộc các kiểu input đã biết bên trong 1 ViewGroup (dùng cho widget tổng hợp như seekbar/color picker)
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

    // Trích các đoạn nằm trong dấu ngoặc đơn () của title, ví dụ "A (so)" -> "so".
    private val parenPattern = Regex("\\(([^()]*)\\)")

    // Xây tập "định danh" hiện tại của 1 giá trị đang được chọn ở param điều khiển (parent):
    // gồm chính giá trị (value), title tương ứng (nếu tìm thấy option khớp value), và phần
    // nội dung nằm trong dấu ngoặc () của title (khớp cả có ngoặc lẫn không ngoặc).
    // Ví dụ option-sh="echo -e 'a|A (so)'" (value=a, title="A (so)")
    //   -> identifiers = {"a", "A (so)", "so", "(so)"}
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

    // Kiểm tra lại toàn bộ param có "depend-on", ẩn/hiện dòng tương ứng.
    //
    // Hỗ trợ NHIỀU điều kiện phụ thuộc cùng lúc (nhiều param cha), nối bằng dấu "|" ở cả 3
    // thuộc tính depend-on / depend-value / depend-mode - các phần tử cùng vị trí (index) tương
    // ứng với nhau.
    //
    // Cách kết hợp các điều kiện được quyết định bởi depend-logic:
    // - "and" (mặc định, giữ tương thích hành vi cũ): TẤT CẢ điều kiện phải cùng thỏa (AND) thì
    //   param mới được hiện.
    //   Ví dụ: depend-on="mode|cam" depend-value="a|b" depend-mode="show|hide"
    //          -> hiện khi mode = a  VÀ  cam khác b
    // - "priority" / "or": hỗ trợ MỨC ƯU TIÊN, xét theo thứ tự khai báo trong depend-on, TỪ
    //   TRÁI SANG PHẢI. Điều kiện nào thỏa trước (đã tính cả depend-mode riêng của điều kiện
    //   đó) sẽ quyết định luôn là "show", không cần xét tiếp các điều kiện còn lại. Nếu không
    //   có điều kiện nào thỏa thì "hide".
    //   Ví dụ: depend-on="mode|level" depend-value="b|7" depend-mode="show|show"
    //          depend-logic="priority"
    //            -> Nếu "mode" khớp "b" thì show luôn.
    //            -> Nếu "mode" không khớp mà "level" khớp "7" thì vẫn show.
    //            -> Nếu cả hai đều không khớp thì hide.
    // - "priority-rtl" / "or-rtl": giống "priority" nhưng xét thứ tự ưu tiên TỪ PHẢI SANG TRÁI
    //   (điều kiện cuối cùng khai báo trong depend-on được xét trước tiên).
    //
    // Mỗi vị trí trong depend-value vẫn có thể chứa nhiều giá trị được chấp nhận, nối bằng dấu
    // ",", so khớp kiểu OR (giữ tương thích với cú pháp cũ), ví dụ "b,c" nghĩa là khớp khi giá
    // trị điều khiển là b HOẶC c.
    //
    // Việc so khớp không chỉ dựa vào value thực tế của param điều khiển, mà còn khớp cả theo
    // title (label hiển thị) của option đang chọn, và phần văn bản nằm trong dấu ngoặc () của
    // title (khớp cả có ngoặc lẫn không ngoặc). Ví dụ option-sh="echo -e 'a|A (so)\nb|B\nc|C'"
    // thì depend-value="a,A,(so)" khớp khi giá trị đang chọn là "a" (value), HOẶC "A" (title),
    // HOẶC "(so)"/"so" (phần trong ngoặc của title).
    private fun evaluateDependencies() {
        for (info in currentParamInfos) {
            val dependOnRaw = info.dependOn?.trim()
            val name = info.name
            if (dependOnRaw.isNullOrEmpty() || name.isNullOrEmpty()) continue

            val dependOnList = dependOnRaw.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            if (dependOnList.isEmpty()) continue

            val dependValueList = (info.dependValue ?: "").split("|")
            val dependModeList = info.dependMode.split("|")

            // Đánh giá điều kiện thứ i (theo đúng thứ tự khai báo trong dependOnList),
            // trả về null nếu không rõ param cha (bỏ qua điều kiện này), ngược lại trả về
            // true/false là kết quả "muốn show" của riêng điều kiện đó.
            fun evalCondition(i: Int): Boolean? {
                val parentName = dependOnList[i]
                val controllerInfo = currentParamInfos.find { it.name == parentName }
                val reader = valueReaders[parentName]
                if (controllerInfo == null || reader == null) return null // Không rõ param cha -> bỏ qua

                val currentValues = reader().split(controllerInfo.separator)
                    .map { it.trim() }.filter { it.isNotEmpty() }
                val parentOptions = controllerInfo.optionsFromShell ?: controllerInfo.options

                // Tập toàn bộ định danh (value + title + nội dung trong ngoặc) của các giá trị
                // đang được chọn ở param cha này.
                val currentIdentifiers = HashSet<String>()
                for (v in currentValues) {
                    currentIdentifiers.addAll(buildValueIdentifiers(v, parentOptions))
                }

                val wantedRaw = dependValueList.getOrNull(i) ?: dependValueList.lastOrNull() ?: ""
                val wanted = wantedRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val matched = wanted.isEmpty() || wanted.any { currentIdentifiers.contains(it) }

                val mode = (dependModeList.getOrNull(i) ?: dependModeList.lastOrNull() ?: "show").trim()
                return if (mode == "hide") !matched else matched
            }

            val logic = info.dependLogic.trim().lowercase()
            val shouldShow: Boolean = when (logic) {
                "priority", "or", "priority-ltr", "or-ltr" -> {
                    // Ưu tiên trái -> phải: điều kiện đầu tiên thỏa sẽ quyết định show luôn.
                    var result = false
                    for (i in dependOnList.indices) {
                        val conditionShow = evalCondition(i) ?: continue
                        if (conditionShow) {
                            result = true
                            break
                        }
                    }
                    result
                }
                "priority-rtl", "or-rtl" -> {
                    // Ưu tiên phải -> trái: xét từ điều kiện cuối cùng trở về đầu.
                    var result = false
                    for (i in dependOnList.indices.reversed()) {
                        val conditionShow = evalCondition(i) ?: continue
                        if (conditionShow) {
                            result = true
                            break
                        }
                    }
                    result
                }
                else -> {
                    // "and" (mặc định): tất cả điều kiện phải cùng thỏa.
                    var result = true
                    for (i in dependOnList.indices) {
                        val conditionShow = evalCondition(i) ?: continue
                        if (!conditionShow) {
                            result = false
                            break // AND: chỉ cần 1 điều kiện không thỏa là đủ để ẩn
                        }
                    }
                    result
                }
            }

            rowViews[name]?.visibility = if (shouldShow) View.VISIBLE else View.GONE
        }
    }

    // 隐藏label的参数类型
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
        // (layout.layoutParams as LinearLayout.LayoutParams).topMargin = dp2px(context, 1f)

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

    /**
     * 读取界面上填入的参数值
     */
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

            val isHiddenByDepend = rowViews[actionParamInfo.name]?.visibility == View.GONE
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

    /**
     * TODO:刷新界面上的参数输入框显示
     */
    fun updateParamsView(actionParamInfos: ArrayList<ActionParamInfo>) {
        for (actionParamInfo in actionParamInfos) {
            if (actionParamInfo.name == null) {
                continue
            }

            val view = linearLayout.findViewWithTag<View>(actionParamInfo.name)
            if (view != null) {
                // TODO:刷新界面显示
            }
        }
    }
}