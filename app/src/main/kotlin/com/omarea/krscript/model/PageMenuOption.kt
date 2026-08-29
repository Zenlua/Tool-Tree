package com.omarea.krscript.model

import com.omarea.common.model.SelectItem

class PageMenuOption(currentConfigXml: String) : RunnableNode(currentConfigXml) {
    // 类型为普通菜单项还是其它具有特定行为的菜单项
    // 例如，类型为finish 点击后会关闭当前页面，类型为refresh点击后会刷新当前页面，而类型为file点击后则需要先选择文件
    var type: String = ""
    // 是否显示为悬浮按钮
    var isFab = false

    // 文件mime类型（仅限type=file有效）
    var mime: String = ""
    // 文件后缀（仅限type=file有效）
    var suffix: String = ""
    // 打开文件/目录选择器时的初始目录（仅限type=file/folder有效）
    var pathHome: String = ""
    // Cho phép chọn nhiều tệp/thư mục cùng lúc (chỉ áp dụng khi type=file/folder)
    var multiple: Boolean = false

    // Lệnh shell dùng để xác định trạng thái tích (checked) khi type = "checkbox".
    // Được chạy lại mỗi lần menu chuẩn bị hiển thị (không chỉ 1 lần lúc load trang).
    // Kết quả trả về "1" hoặc "true" => hiện dấu tích, ngược lại => bỏ tích.
    var checkedSh: String = ""

    // Trạng thái tích hiện tại - được cập nhật ở background thread (IO), chỉ đọc khi vẽ menu (Main thread).
    @Volatile
    var checked: Boolean = false
    // Nếu true: khi click, chạy script ẩn ở nền (không hiện dialog log/không cho người dùng thấy output)
    var silent: Boolean = false
    // Cho phép menu item mở giống 1 "page" (như 1 dòng bình thường trong danh sách) thay vì chạy
    // script khi click. Thứ tự ưu tiên giống PageNode: link > activity > html/config-sh/config.
    var link: String = ""
    var activity: String = ""
    var onlineHtmlPage: String = ""
    var pageConfigPath: String = ""
    var pageConfigSh: String = ""
    // Script chạy khi bấm mục này. Nếu bản thân mục không tự khai báo "script" riêng, giá trị
    // này đã được PageConfigReader.menuGroupOptionsToml() gán sẵn = "handler" dùng chung của
    // nhóm [[menu]]/[[fab]] chứa nó NGAY LÚC PARSE - lúc click chỉ cần đọc thẳng script, không
    // còn fallback nào khác (page không còn handler riêng nữa).
    var script: String = ""

    // ========== TÍNH NĂNG MỚI: type = "spinner" ==========
    // Mục menu dạng dropdown chọn giá trị (giống Android Spinner/ParamsSingleSelect) thay vì
    // chạy thẳng script khi bấm. Bấm vào mục sẽ hiện popup dạng Spinner ngay tại toolbar, chọn
    // xong mới chạy "script" với tham số "state" = giá trị vừa chọn (xem
    // ActionPage.menuItemSpinner()/showSpinnerPopup()).

    // Danh sách lựa chọn khai báo tĩnh qua [[menu.items.options]] (title/value) - xem
    // PageConfigReader.pageMenuOptionToml()/selectItemToml().
    var options: ArrayList<SelectItem>? = null
    // Lệnh shell sinh danh sách lựa chọn động (mỗi dòng "value|title" hoặc chỉ "value"),
    // chạy lại mỗi lần mở dropdown - khai báo qua "options-sh"/"option-sh".
    var optionsSh: String = ""
    // Lệnh shell đọc giá trị đang được chọn hiện tại, dùng để tô sáng đúng mục tương ứng khi
    // mở dropdown - khai báo qua "get"/"getstate". Chạy lại mỗi lần mở dropdown, cùng lúc với
    // optionsSh (gộp 1 round-trip, xem ActionPage.menuItemSpinner()).
    var spinnerGetState: String = ""
}