package com.omarea.krscript.model

import java.util.*

class ActionNode(currentConfigXml: String) : RunnableNode(currentConfigXml){
    var params: ArrayList<ActionParamInfo>? = null
    // Giống text.rows: cho phép action hiển thị thêm các dòng văn bản rich-text bên dưới (rows)
    // - CHỈ hiện ở item trong danh sách (list), không hiện trong dialog params.
    val rows = ArrayList<TextNode.TextRow>()
    // Rows riêng cho dialog params (mở khi action có "params") - tách khỏi "rows" ở trên để
    // không bị hiện trùng lặp (item trong list VÀ dialog cùng đọc chung 1 mảng). Khai báo bằng
    // "params-rows" trong TOML, không liên quan/không ảnh hưởng gì tới "rows".
    val paramsRows = ArrayList<TextNode.TextRow>()

    // menu = true: mục này KHÔNG xuất hiện trong danh sách nội dung của trang nữa, mà xuất
    // hiện như 1 icon riêng LUÔN hiện trên toolbar (cạnh nút "⋮"). Dialog params/confirm/warning
    // khi bấm vào icon đó y hệt bấm 1 group.action bình thường trong danh sách - dùng lại
    // NGUYÊN VẸN ActionListFragment.onActionClick/actionExecute, không có gì khác biệt ngoài vị
    // trí hiển thị - xem PageConfigReader (case "action") và ActionPage.onCreateOptionsMenu().
    var menu: Boolean = false

    // show = true (hoặc lệnh shell - dùng chung resolveBoolOrShell() như support/visible):
    // tự động mở dialog của mục này ngay khi vừa vào trang, KHÔNG cần bấm - chỉ 1 LẦN duy nhất
    // trong phiên mở trang đó, không lặp lại khi trang tự reload. Hoạt động ĐỘC LẬP với menu
    // (dùng được cho cả mục còn nằm trong danh sách lẫn mục đã chuyển ra icon toolbar) - xem
    // ActionPage.tryAutoShowActions().
    var show: Boolean = false
}
