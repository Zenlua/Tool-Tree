package com.omarea.krscript.model

// Loại mục "download": bấm vào để tải 1 file qua HTTP/HTTPS, tiến trình (đã tải/tổng dung
// lượng) hiện NGAY trong item (progress bar + desc), không cần dialog riêng. Tải xong tự
// chạy script (dùng lại field "script"/"set"/"setstate" -> setState, kế thừa từ RunnableNode,
// y hệt [[group.action]]) với biến môi trường $state = đường dẫn tuyệt đối của file vừa tải.
//
// Kế thừa RunnableNode nên có sẵn miễn phí: confirm/warning (hỏi trước khi tải), reloadPage/
// updateBlocks (field "reload"/"reload-page" - làm mới trang sau khi script chạy xong nếu cần),
// autoFinish/autoKill/autoRestart, lockShell/locked (từ ClickableNode).
//
// title: điền thủ công như bình thường (NodeInfoBase.title/title-sh), không có gì đặc biệt.
// Chỉ hỗ trợ "url" tĩnh (không có url-sh) - xem PageConfigReader.downloadNodeToml().
class DownloadNode(currentConfigXml: String) : RunnableNode(currentConfigXml) {
    // Đường dẫn URL cần tải (bắt buộc)
    var url: String = ""
}
