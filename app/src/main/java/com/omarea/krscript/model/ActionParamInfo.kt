package com.omarea.krscript.model

import com.omarea.common.model.SelectItem

class ActionParamInfo {
    // 参数名：必需保持唯一
    var name: String? = null

    var title: String? = null

    var label: String? = null

    // 描述
    var desc: String? = null

    // 值
    var value: String? = null
    var valueShell: String? = null
    var valueFromShell: String? = null
    var maxLength = -1 // input only
    var type: String? = null
    var max: Int = Int.MAX_VALUE // seekbar only
    var min: Int = Int.MIN_VALUE // seekbar only
    var required: Boolean = false // 是否是必需的
    var readonly: Boolean = false
    var options: ArrayList<SelectItem>? = null
    var optionsFromShell: ArrayList<SelectItem>? = null
    var optionsSh = ""
    // 是否允许多选(options only)
    var multiple: Boolean = false
    // 是否支持
    var supported: Boolean = true
    // 文本框的水印（提示占位符）
    var placeholder: String = ""
    // 文件mime类型（仅限type=file有效）
    var mime: String = ""
    // 文件后缀（仅限type=file有效）
    var suffix: String = ""
    // 是否允许用户手动输入路径
    var editable: Boolean = false
    // 多个值的分隔符（仅限多选下拉）
    var separator: String = "\n"

    // Tên (các) param điều khiển: param này sẽ ẩn/hiện dựa theo giá trị của (các) param có
    // "name" trùng dependOn. Có thể khai báo NHIỀU param cha cùng lúc, nối bằng dấu "|",
    // ví dụ: "mode|cam" -> phụ thuộc đồng thời vào cả param "mode" và "cam" (tất cả phải
    // cùng thỏa điều kiện tương ứng - AND).
    var dependOn: String? = null
    // Danh sách giá trị cần khớp cho từng param cha (theo đúng thứ tự khai báo ở dependOn),
    // các param cha cách nhau bởi dấu "|"; trong mỗi vị trí, các giá trị được chấp nhận (OR)
    // cách nhau bởi dấu phẩy, ví dụ: "a|b,c" -> cha 1 khớp khi = a, cha 2 khớp khi = b hoặc c.
    // Giá trị so khớp không chỉ là value thực tế của option, mà còn có thể là title (label)
    // hiển thị của option, hoặc phần văn bản nằm trong dấu ngoặc () của title (khớp cả có
    // ngoặc lẫn không ngoặc). Ví dụ option-sh="echo -e 'a|A (so)'" (value=a, title="A (so)")
    // thì depend-value="a,A,(so)" đều khớp được (qua value "a", qua title "A", qua "(so)"/"so").
    var dependValue: String? = null
    // "show": chỉ hiện khi khớp dependValue (mặc định) | "hide": ẩn khi khớp dependValue
    // Cũng có thể khai báo riêng cho từng param cha, nối bằng "|", theo đúng thứ tự dependOn,
    // ví dụ: "show|hide".
    var dependMode: String = "show"
}
