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
// Hỗ trợ "url" tĩnh và/hoặc "url-sh" (chạy shell lấy url) - xem PageConfigReader.downloadNodeToml()
// và ActionListFragment.resolveDownloadUrlThenClick().
class DownloadNode(currentConfigXml: String) : RunnableNode(currentConfigXml) {
    // Đường dẫn URL cần tải (tĩnh, khai báo trực tiếp trong config). Bắt buộc nếu không có url-sh.
    var url: String = ""

    // Đường dẫn URL cần tải, lấy từ kết quả 1 lệnh shell. Chỉ chạy 1 LẦN DUY NHẤT khi bấm tải
    // lần đầu (xem urlResolved), kết quả cache thẳng vào "url" rồi dùng lại cho các lần bấm sau
    // (tạm dừng/tiếp tục/tải lại) - KHÔNG chạy lại shell mỗi lần bấm, vì session tải đang tra theo
    // "url" (DownloadTaskHelper), nếu shell trả về giá trị khác nhau mỗi lần gọi (vd URL có chữ ký
    // tạm thời) thì sẽ làm lệch/mất session đang chạy dở.
    var urlSh: String = ""

    // Đánh dấu "url-sh" (nếu có) đã chạy xong 1 lần cho instance node này chưa.
    var urlResolved: Boolean = false

    // Giống group.action.rows: cho phép hiển thị thêm các dòng rich-text (text/icon/toggle/photo...)
    // ngay bên dưới item, y hệt cơ chế của ActionNode.rows - dùng chung TextNode.TextRow/RowsRenderHelper.
    val rows = ArrayList<TextNode.TextRow>()
}
