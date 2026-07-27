package com.omarea.common.ui

import android.app.Dialog
import java.lang.ref.WeakReference

/**
 * Theo dõi các Dialog đang được hiển thị trong app (đăng ký từ [DialogHelper.customDialog]).
 * Dùng để BannerNotificationManager biết cần add banner vào Window của Dialog nào đang mở
 * (nếu có), thay vì add vào Activity content — vì Dialog là 1 Window riêng luôn nổi trên
 * Window của Activity, add nhầm chỗ sẽ bị Dialog che mất.
 */
object CurrentDialogHolder {
    private val dialogs = mutableListOf<WeakReference<Dialog>>()

    fun register(dialog: Dialog) {
        dialogs.add(WeakReference(dialog))
    }

    /** Trả về Dialog đang hiển thị được mở gần đây nhất (trên cùng), nếu có. Tự dọn các entry đã chết/đã đóng. */
    fun getTopVisible(): Dialog? {
        for (i in dialogs.indices.reversed()) {
            val d = dialogs[i].get()
            if (d == null) {
                dialogs.removeAt(i)
                continue
            }
            if (d.isShowing) {
                return d
            } else {
                dialogs.removeAt(i)
            }
        }
        return null
    }
}
