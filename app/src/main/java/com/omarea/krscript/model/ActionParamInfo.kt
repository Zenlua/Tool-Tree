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
    // 是否允许多选(options 多选下拉; type=file/folder 时允许多选多个文件/文件夹)
    var multiple: Boolean = false
    // 是否支持
    var supported: Boolean = true
    // 文本框的水印（提示占位符）
    var placeholder: String = ""
    // 文件mime类型（仅限type=file有效）
    var mime: String = ""
    // 文件后缀（仅限type=file有效），支持用逗号分隔多个后缀，例如 "zip,apk,7z"
    var suffix: String = ""
    // 打开文件/目录选择器时的初始目录（仅限type=file/folder有效），例如 "/sdcard/Android"
    // 用户仍然可以从这里返回到上一级目录
    var pathHome: String = ""
    // 是否允许用户手动输入路径
    var editable: Boolean = false
    // 多个值的分隔符（仅限多选下拉）
    var separator: String = "\n"

    // ========== 新增依赖管理功能 ==========

    // Tên (các) param điều khiển: param này sẽ ẩn/hiện dựa theo giá trị của (các) param có
    // "name" trùng dependOn. Có thể khai báo NHIỀU param cha cùng lúc, nối bằng dấu "|",
    // ví dụ: "mode|cam" -> phụ thuộc đồng thời vào cả param "mode" và "cam" (tất cả phải
    // cùng thỏa điều kiện tương ứng - AND).
    var dependOn: String? = null

    // Danh sách giá trị cần khớp cho từng param cha (theo đúng thứ tự khai báo ở dependOn),
    // các param cha cách nhau bởi dấu "|"; trong mỗi vị trí, các giá trị được chấp nhận (OR)
    // cách nhau bởi dấu phẩy, ví dụ: "a|b,c" -> cha 1 khớp khi = a, cha 2 khớp khi = b hoặc c.
    var dependValue: String? = null

    // "show": chỉ hiện khi khớp dependValue (mặc định) | "hide": ẩn khi khớp dependValue
    // Cũng có thể khai báo riêng cho từng param cha, nối bằng "|", theo đúng thứ tự dependOn,
    // ví dụ: "show|hide".
    var dependMode: String = "show"

    // Cách kết hợp nhiều điều kiện phụ thuộc (khi dependOn khai báo nhiều param cha):
    // - "and" (mặc định): TẤT CẢ điều kiện phải cùng thỏa (giữ tương thích hành vi cũ).
    // - "priority" (hoặc "or"): xét theo THỨ TỰ ưu tiên từ TRÁI SANG PHẢI theo đúng thứ tự
    //   khai báo trong dependOn. Điều kiện nào (đã tính cả dependMode của chính nó) thỏa
    //   trước sẽ quyết định luôn kết quả.
    //   Nếu không có điều kiện nào thỏa thì sử dụng dependDefault.
    // - "priority-rtl" (hoặc "or-rtl"): giống "priority" nhưng xét theo thứ tự ưu tiên từ
    //   PHẢI SANG TRÁI.
    // - "xor": chỉ ĐÚNG MỘT điều kiện phải thỏa.
    // - "nand": phủ định của "and" (không phải tất cả điều kiện đều thỏa).
    var dependLogic: String = "and"

    // ========== TÍNH NĂNG MỚI: MẶC ĐỊNH ẨN/HIỆN ==========
    // Giá trị mặc định khi KHÔNG có điều kiện phụ thuộc nào thỏa mãn:
    // - "show" (mặc định): hiển thị khi không có điều kiện nào khớp
    // - "hide": ẩn khi không có điều kiện nào khớp
    // Ví dụ: depend-on="mode" depend-value="advanced"
    //        depend-default="hide"
    //        -> Khi mode != advanced thì ẩn (không phải "show")
    var dependDefault: String = "show"

    // ========== TÍNH NĂNG MỚI: TRẠNG THÁI KHỞI ĐỘNG ==========
    // Trạng thái ẩn/hiện BAN ĐẦU khi chưa đánh giá bất kỳ điều kiện nào:
    // - "auto" (mặc định): tự động xác định dựa trên dependDefault
    // - "show": luôn hiển thị lúc đầu
    // - "hide": luôn ẩn lúc đầu
    // Hữu ích khi bạn không muốn param nhấp nháy lúc tải dialog.
    var dependInitialState: String = "auto"

    // ========== TÍNH NĂNG MỚI: ĐẢO NGƯỢC ĐIỀU KIỆN ==========
    // Nếu true, tất cả các điều kiện sẽ bị đảo ngược (NOT logic):
    // - "show" trở thành "hide"
    // - "hide" trở thành "show"
    // Ví dụ: depend-on="admin" depend-value="1" depend-negate="true"
    //        -> Hiện khi admin != 1 (ẩn khi admin = 1)
    var dependNegate: Boolean = false

    // ========== TÍNH NĂNG MỚI: NGƯỠNG ĐIỀU KIỆN (CHO "AND") ==========
    // Với logic "and", chỉ bao nhiêu % điều kiện cần thỏa mãn:
    // - -1 (mặc định): 100% (tất cả phải thỏa) - hành vi cũ
    // - 0-100: % số điều kiện cần thỏa, vd: 50 = ít nhất 50% điều kiện phải thỏa
    // Ví dụ: depend-on="a|b|c" 3 điều kiện
    //        depend-threshold="67" (tối thiểu 2/3 điều kiện)
    //        -> Chỉ cần tối thiểu 2 trong 3 điều kiện thỏa mãn
    var dependThreshold: Int = -1

    // ========== TÍNH NĂNG MỚI: HOẠT ĐỘNG KHÔNG ĐỒNG THỜI ==========
    // Nếu true, param ẩn sẽ vẫn được đưa vào kết quả readParamsValue() nếu nó có giá trị
    // (thay vì bỏ qua param ẩn). Hữu ích cho các param ẩn nhưng vẫn cần giá trị.
    var dependIncludeHidden: Boolean = false

    // ========== TÍNH NĂNG MỚI: CHA ẨN THÌ CON ẨN THEO ==========
    // Nếu true (mặc định), khi (bất kỳ) param cha trong depend-on đang bị ẨN (do chính
    // depend-on của nó, chuỗi phụ thuộc nhiều cấp...), param này CŨNG BỊ ẨN LUÔN, bất kể
    // giá trị hiện tại của cha có khớp depend-value hay không.
    // Đặt "false" nếu bạn muốn param con vẫn tự đánh giá theo giá trị của cha ngay cả khi
    // hàng chứa param cha đang ẩn khỏi màn hình (hiếm khi cần).
    // Ví dụ: depend-on="mode" depend-cascade="false"
    var dependCascade: Boolean = true

    // ========== TÍNH NĂNG MỚI: LÀMĐIỀU GỌILẠI KHI THAY ĐỔI DEPENDENCY ==========
    // Tên shell script/callback để gọi khi param này thay đổi trạng thái ẩn/hiện
    // (chỉ gọi khi trạng thái thực sự thay đổi, từ visible -> hidden hoặc ngược lại)
    var dependOnChangeCallback: String? = null
}
