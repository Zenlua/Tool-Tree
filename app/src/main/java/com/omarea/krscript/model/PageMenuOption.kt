package com.omarea.krscript.model

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

    // Lệnh shell dùng để xác định trạng thái tích (checked) khi type = "checkbox".
    // Được chạy lại mỗi lần menu chuẩn bị hiển thị (không chỉ 1 lần lúc load trang).
    // Kết quả trả về "1" hoặc "true" => hiện dấu tích, ngược lại => bỏ tích.
    var checkedSh: String = ""

    // Trạng thái tích hiện tại - được cập nhật ở background thread (IO), chỉ đọc khi vẽ menu (Main thread).
    @Volatile
    var checked: Boolean = false
    // Nếu true: khi click, chạy script ẩn ở nền (không hiện dialog log/không cho người dùng thấy output)
    var silent: Boolean = false
}