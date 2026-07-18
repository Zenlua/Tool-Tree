package com.omarea.krscript.model

import android.content.Context
import android.text.SpannableString

/**
 * Lớp "câm" (không log, không progress bar, không hộp nhập liệu) chỉ để tái sử dụng đúng
 * cơ chế phân tích output đặc biệt (am:[...] / progress:[...] / input:[...]) đã có sẵn ở
 * ShellHandlerBase - dùng cho các nơi chạy shell KHÔNG có dialog/log UI riêng (vd: chạy ẩn ở
 * menu, log khởi động ở SplashActivity...) nhưng vẫn cần hỗ trợ directive "am:[...]" (mở activity).
 *
 * Mặc định các dòng log thông thường (không phải am:[...]/progress:[...]/input:[...]) sẽ bị bỏ
 * qua (không hiển thị gì). Muốn hiển thị lại, hãy tạo class con và override onReader(msg).
 */
open class SilentShellOutputHandler(context: Context) : ShellHandlerBase(context) {
    override fun onProgress(current: Int, total: Int) {
        // Không có UI để hiển thị tiến trình -> bỏ qua có chủ đích.
    }

    override fun onStart(msg: Any?) {}
    override fun onStart(forceStop: Runnable?) {}
    override fun onExit(msg: Any?) {}

    override fun updateLog(msg: SpannableString?) {
        // Không có log view -> bỏ qua có chủ đích.
    }

    /**
     * Đưa từng dòng output (đã tách theo \n) qua bộ phân tích của ShellHandlerBase.
     * Dòng nào khớp am:[...] sẽ tự động mở activity tương ứng; các directive khác
     * (progress:[...]/input:[...]) bị bỏ qua vì không có UI hiển thị.
     */
    fun processOutput(output: String) {
        output.lineSequence().forEach { line ->
            if (line.isNotBlank()) {
                onReaderMsg(line)
            }
        }
    }
}
