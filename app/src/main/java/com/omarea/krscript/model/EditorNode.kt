package com.omarea.krscript.model

/**
 * Node cho thẻ <editor file="" title="" desc="" ... />
 * Khi người dùng bấm vào mục này, ứng dụng sẽ mở trang soạn thảo văn bản (TextEditorActivity)
 * để xem/sửa nội dung file được chỉ định bởi thuộc tính "file".
 * Nếu file chưa tồn tại thì trang soạn thảo sẽ tạo mới khi lưu.
 */
class EditorNode(currentConfigXml: String) : ClickableNode(currentConfigXml) {
    // Đường dẫn file cần mở để soạn thảo (bắt buộc)
    var file: String = ""

    // Trạng thái ngắt dòng mặc định khi mở trang soạn thảo (mặc định: bật ngắt dòng)
    var wrap: Boolean = true
}
