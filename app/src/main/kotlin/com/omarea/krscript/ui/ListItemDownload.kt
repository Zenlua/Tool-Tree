package com.omarea.krscript.ui

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.tool.tree.R
import com.omarea.krscript.model.DownloadNode

class ListItemDownload(context: Context, config: DownloadNode) :
    ListItemClickable(context, R.layout.kr_download_list_item, config) {

    private val widgetView = layout.findViewById<ImageView?>(R.id.kr_widget)
    private val ringView = layout.findViewById<DownloadProgressRing?>(R.id.kr_download_ring)
    private val rowsView = layout.findViewById<TextView?>(R.id.kr_rows)
    private val rowsPhotoView = layout.findViewById<ImageView?>(R.id.kr_rows_photo)

    // desc tĩnh ban đầu (config.desc) - lưu lại lúc khởi tạo (trước khi bất kỳ lần tải nào bắt
    // đầu ghi đè desc bằng "% tiến trình") để có thể khôi phục lại đúng nội dung này khi người
    // dùng HUỶ tải giữa chừng (xem restoreDesc() / DownloadTaskHelper nhánh cancelled).
    private val originalDesc = config.desc

    // true trong suốt lúc đang tải HOẶC đang chạy script sau khi tải xong - PageLayoutRender
    // vẫn gọi onClick bình thường (không disable layout), nên bên xử lý click (ActionListFragment)
    // tự kiểm tra cờ này: nếu đang trong giai đoạn TẢI (xem cancelAction) thì bấm = huỷ tải; nếu
    // đang chạy script thì bỏ qua tap (không huỷ được nữa).
    var isBusy: Boolean = false
        private set

    // Hành động huỷ phiên tải HIỆN TẠI - chỉ khác null trong lúc đang thật sự tải byte (từ
    // markBusy() tới ngay trước khi chuyển sang chạy script/kết thúc, xem
    // DownloadTaskHelper.clearCancelAction()). Dùng để item biết được có nên coi 1 lần bấm tiếp
    // theo là "huỷ" hay không.
    private var cancelAction: (() -> Unit)? = null

    init {
        widgetView?.visibility = View.VISIBLE
        widgetView?.setImageDrawable(context.getDrawable(R.drawable.kr_download))

        // Giống action.rows: hiển thị thêm các dòng rich-text (nếu có khai báo download.rows).
        // desc tĩnh (config.desc, nếu có) vẫn hiện bình thường ở đây (đã set qua init của
        // ListItemView/ListItemClickable) - chỉ bị đè bởi updateDownloadProgress() SAU khi
        // người dùng bấm tải, không đụng gì lúc khởi tạo.
        RowsRenderHelper.bind(context, rowsView, rowsPhotoView, config.rows, config)
    }

    // Gọi ngay khi bắt đầu 1 phiên tải mới - khoá item lại, ẩn icon tải, thế bằng vòng tròn
    // tiến trình (dạng xoay/indeterminate cho tới khi biết được Content-Length thật từ server).
    // [onCancel] được gọi nếu người dùng bấm lại vào item TRONG LÚC đang tải (xem
    // cancelIfDownloading()) - DownloadTaskHelper truyền vào 1 lambda huỷ HTTP request dở dang.
    fun markBusy(onCancel: () -> Unit) {
        isBusy = true
        cancelAction = onCancel
        widgetView?.visibility = View.GONE
        ringView?.visibility = View.VISIBLE
        ringView?.setIndeterminate(true)
    }

    // Cập nhật tiến trình tải theo byte. total <= 0 nghĩa là server không trả Content-Length
    // (không rõ tổng dung lượng) - giữ nguyên vòng tròn dạng xoay/indeterminate.
    fun updateDownloadProgress(downloaded: Long, total: Long) {
        desc = formatProgress(downloaded, total)
        if (total > 0) {
            ringView?.setIndeterminate(false)
            ringView?.setProgress(downloaded * 100f / total)
        } else {
            ringView?.setIndeterminate(true)
        }
    }

    // Gọi ngay khi việc TẢI kết thúc (dù xong, lỗi, hay chuẩn bị chạy script) - từ giờ không
    // còn gì để huỷ nữa, 1 lần bấm tiếp theo (nếu còn isBusy vì đang chạy script) sẽ bị bỏ qua
    // thay vì coi là huỷ.
    fun clearCancelAction() {
        cancelAction = null
    }

    // Người dùng bấm vào item trong lúc item đang bận. Trả về true nếu đây thực sự là giai đoạn
    // có thể huỷ (đang tải) và đã gọi onCancel - bên gọi (ActionListFragment) không cần làm gì
    // thêm. Trả về false nếu không có gì để huỷ (đang chạy script, hoặc đã lỡ kết thúc) - bên
    // gọi tự bỏ qua tap này.
    fun cancelIfDownloading(): Boolean {
        val action = cancelAction ?: return false
        cancelAction = null
        action.invoke()
        return true
    }

    // Hiện 1 nhãn trạng thái (đang chạy script / thành công / lỗi...) thay cho desc dạng %.
    // Vòng tròn chuyển về dạng xoay (không rõ script chạy trong bao lâu).
    fun showStatusLabel(label: String) {
        desc = label
        ringView?.setIndeterminate(true)
    }

    // Khôi phục lại desc gốc (như lúc chưa bấm tải) - CHỈ gọi khi người dùng chủ động huỷ tải
    // giữa chừng (xem DownloadTaskHelper, nhánh cancelled), để không để sót nhãn "đã tải x/y"
    // trên item sau khi huỷ. Không gọi trong trường hợp tải xong/lỗi/chạy script xong - các
    // trường hợp đó đã tự có nhãn trạng thái riêng qua showStatusLabel().
    fun restoreDesc() {
        desc = originalDesc
    }

    // Kết thúc phiên (dù thành công, lỗi, hay bị huỷ) - mở khoá lại, ẩn vòng tròn, hiện lại icon.
    fun finishBusy() {
        isBusy = false
        cancelAction = null
        ringView?.visibility = View.GONE
        widgetView?.visibility = View.VISIBLE
    }

    companion object {
        fun formatProgress(downloaded: Long, total: Long): String {
            val d = formatBytes(downloaded)
            return if (total > 0) "$d / ${formatBytes(total)}" else d
        }

        private fun formatBytes(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            val kb = bytes / 1024.0
            if (kb < 1024) return String.format("%.1f KB", kb)
            val mb = kb / 1024.0
            if (mb < 1024) return String.format("%.1f MB", mb)
            return String.format("%.2f GB", mb / 1024.0)
        }
    }
}
