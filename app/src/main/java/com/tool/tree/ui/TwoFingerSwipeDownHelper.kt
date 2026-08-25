package com.tool.tree.ui

import android.app.Activity
import android.view.MotionEvent

/**
 * Phát hiện cử chỉ dùng 2 ngón tay cùng vuốt xuống ở bất kỳ đâu trên màn hình - dùng để bật/tắt
 * hiện các mục có hide = true (xem ActionListFragment/PageLayoutRender). Không can thiệp/chặn sự
 * kiện chạm (luôn để children xử lý bình thường - cuộn danh sách, bấm nút...) vì đây chỉ là 1 cử
 * chỉ "phụ" phát hiện song song, không phải thao tác chính của trang.
 *
 * Cách dùng: gọi dispatchTouchEvent() từ Activity.dispatchTouchEvent() (TRƯỚC hay sau super đều
 * được vì không bao giờ return true/chặn sự kiện).
 *
 * Theo dõi khoảng cách di chuyển xuống của ĐÚNG 2 ngón đầu tiên chạm xuống (pointer thứ 1 và thứ
 * 2) kể từ lúc ngón thứ 2 vừa đặt xuống - nếu cả 2 cùng di chuyển xuống đủ xa (trung bình cộng
 * >= TRIGGER_DISTANCE_DP) thì coi là xác nhận cử chỉ, gọi onTrigger() đúng 1 lần cho tới khi cử
 * chỉ kết thúc (nhấc bớt ngón/nhấc hết/hủy). Nếu có ngón thứ 3 chạm thêm vào giữa chừng thì huỷ
 * theo dõi (tránh nhầm với các cử chỉ đa chạm khác như pinch 3 ngón).
 */
class TwoFingerSwipeDownHelper(
    activity: Activity,
    private val onTrigger: () -> Unit
) {
    companion object {
        private const val TRIGGER_DISTANCE_DP = 60f
    }

    private val triggerDistancePx = TRIGGER_DISTANCE_DP * activity.resources.displayMetrics.density

    private var pointerId1 = -1
    private var pointerId2 = -1
    private var startY1 = 0f
    private var startY2 = 0f
    private var triggered = false

    var enabled = true

    fun dispatchTouchEvent(ev: MotionEvent) {
        if (!enabled) return

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                reset()
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (ev.pointerCount == 2) {
                    pointerId1 = ev.getPointerId(0)
                    pointerId2 = ev.getPointerId(1)
                    startY1 = ev.getY(0)
                    startY2 = ev.getY(1)
                    triggered = false
                } else {
                    // Có ngón thứ 3 trở lên -> không phải cử chỉ 2 ngón đơn thuần, huỷ theo dõi
                    reset()
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (triggered || pointerId1 == -1 || ev.pointerCount != 2) return
                val idx1 = ev.findPointerIndex(pointerId1)
                val idx2 = ev.findPointerIndex(pointerId2)
                if (idx1 == -1 || idx2 == -1) return

                val dy1 = ev.getY(idx1) - startY1
                val dy2 = ev.getY(idx2) - startY2
                val avgDy = (dy1 + dy2) / 2f

                if (avgDy >= triggerDistancePx) {
                    triggered = true
                    onTrigger()
                }
            }

            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                reset()
            }
        }
    }

    private fun reset() {
        pointerId1 = -1
        pointerId2 = -1
        triggered = false
    }
}
