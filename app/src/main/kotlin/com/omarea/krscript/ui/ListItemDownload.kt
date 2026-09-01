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

    // desc tĩnh ban đầu (config.desc) - lưu lại lúc khởi tạo để khôi phục khi huỷ
    private val originalDesc = config.desc

    // true trong suốt lúc đang tải HOẶC đang chạy script hoặc đang tạm dừng
    var isBusy: Boolean = false
        private set

    // Mở khoá nhấn giữ khi đang bận tải (để hiện dialog xác nhận huỷ) kể cả khi item không có
    // key / không cho phép tạo shortcut - xem PageLayoutRender.onItemLongClickListener.
    override fun allowLongClick(): Boolean = isBusy || super.allowLongClick()

    // Hành động khi bấm lại: có thể là tạm dừng, tiếp tục, hoặc huỷ (tuỳ ngữ cảnh)
    private var cancelAction: (() -> Unit)? = null

    init {
        widgetView?.visibility = View.VISIBLE
        widgetView?.setImageDrawable(context.getDrawable(R.drawable.kr_download))
        RowsRenderHelper.bind(context, rowsView, rowsPhotoView, config.rows, config)
    }

    // Gọi ngay khi bắt đầu 1 phiên tải mới - khoá item lại, hiện vòng tròn tiến trình
    fun markBusy(onCancel: () -> Unit) {
        isBusy = true
        cancelAction = onCancel
        widgetView?.visibility = View.GONE
        ringView?.visibility = View.VISIBLE
        ringView?.setIndeterminate(true)
    }

    // Cập nhật tiến trình tải theo byte.
    fun updateDownloadProgress(downloaded: Long, total: Long) {
        desc = formatProgress(downloaded, total)
        if (total > 0) {
            ringView?.setIndeterminate(false)
            ringView?.setProgress(downloaded * 100f / total)
        } else {
            ringView?.setIndeterminate(true)
        }
    }

    // Gọi khi việc TẢI kết thúc (dù xong, lỗi, hay chuẩn bị chạy script)
    fun clearCancelAction() {
        cancelAction = null
    }

    // Người dùng bấm vào item trong lúc item đang bận.
    // Trả về true nếu đã gọi action (tạm dừng / tiếp tục / huỷ).
    fun cancelIfDownloading(): Boolean {
        val action = cancelAction ?: return false
        cancelAction = null
        action.invoke()
        return true
    }

    // Hiện 1 nhãn trạng thái thay cho desc dạng %.
    fun showStatusLabel(label: String) {
        desc = label
        ringView?.setIndeterminate(true)
    }

    // Khôi phục lại desc gốc (như lúc chưa bấm tải)
    fun restoreDesc() {
        desc = originalDesc
    }

    // Kết thúc phiên (thành công hoặc bị huỷ) - mở khoá lại, ẩn vòng tròn, hiện lại icon.
    // KHÔNG gọi hàm này khi đang ở trạng thái lỗi – cần giữ hiển thị lỗi.
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
