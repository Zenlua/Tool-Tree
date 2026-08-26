package com.omarea.krscript.ui

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.tool.tree.R
import com.omarea.krscript.model.DownloadNode

class ListItemDownload(context: Context, config: DownloadNode) :
    ListItemClickable(context, R.layout.kr_download_list_item, config) {

    private val widgetView = layout.findViewById<ImageView?>(R.id.kr_widget)
    private val progressView = layout.findViewById<ProgressBar?>(R.id.kr_download_progress)
    private val rowsView = layout.findViewById<TextView?>(R.id.kr_rows)
    private val rowsPhotoView = layout.findViewById<ImageView?>(R.id.kr_rows_photo)

    // true trong suốt lúc đang tải HOẶC đang chạy script sau khi tải xong - PageLayoutRender
    // vẫn gọi onClick bình thường (không disable layout), nên bên xử lý click (ActionListFragment)
    // tự kiểm tra cờ này để bỏ qua các lần bấm thêm trong lúc đang bận.
    var isBusy: Boolean = false
        private set

    init {
        widgetView?.visibility = View.VISIBLE
        widgetView?.setImageDrawable(context.getDrawable(R.drawable.kr_download))

        // Giống action.rows: hiển thị thêm các dòng rich-text (nếu có khai báo download.rows).
        // desc tĩnh (config.desc, nếu có) vẫn hiện bình thường ở đây (đã set qua init của
        // ListItemView/ListItemClickable) - chỉ bị đè bởi updateDownloadProgress() SAU khi
        // người dùng bấm tải, không đụng gì lúc khởi tạo.
        RowsRenderHelper.bind(context, rowsView, rowsPhotoView, config.rows, config)
    }

    // Gọi ngay khi bắt đầu 1 phiên tải mới - khoá item lại, hiện progress bar dạng không xác
    // định (indeterminate) cho tới khi biết được Content-Length thật từ server.
    fun markBusy() {
        isBusy = true
        progressView?.visibility = View.VISIBLE
        progressView?.isIndeterminate = true
    }

    // Cập nhật tiến trình tải theo byte. total <= 0 nghĩa là server không trả Content-Length
    // (không rõ tổng dung lượng) - giữ nguyên progress bar dạng indeterminate.
    fun updateDownloadProgress(downloaded: Long, total: Long) {
        desc = formatProgress(downloaded, total)
        progressView?.apply {
            if (total > 0) {
                isIndeterminate = false
                max = 1000
                progress = ((downloaded * 1000L) / total).toInt().coerceIn(0, 1000)
            } else {
                isIndeterminate = true
            }
        }
    }

    // Hiện 1 nhãn trạng thái (đang chạy script / thành công / lỗi...) thay cho desc dạng %.
    fun showStatusLabel(label: String) {
        desc = label
        progressView?.isIndeterminate = true
    }

    // Kết thúc phiên (dù thành công hay lỗi) - mở khoá lại, ẩn progress bar.
    fun finishBusy() {
        isBusy = false
        progressView?.visibility = View.GONE
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
