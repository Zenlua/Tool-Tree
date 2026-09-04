package com.omarea.common.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.widget.ListPopupWindow
import java.lang.ref.WeakReference

/**
 * Nền kính mờ cho ListPopupWindow (popup menu "⋮", spinner dropdown...) khi app đang ở
 * "chế độ ảnh nền" (ThemeModeState.isImageBackgroundMode()) và blur không bị tắt
 * (DialogHelper.disableBlurBg == false). Xem SpinnerPopupHelper.buildPopupBackground - nơi
 * duy nhất khởi tạo class này.
 *
 * KHÔNG thể chụp/crop ảnh ngay lúc khởi tạo: popup.setBackgroundDrawable() luôn được gọi
 * TRƯỚC popup.show(), lúc đó popup.listView còn null và popup CHƯA có toạ độ thật trên màn
 * hình. Vì vậy việc chụp/crop được hoãn tới lần draw() ĐẦU TIÊN - khi đó hệ thống đã đo đạc +
 * định vị xong cửa sổ popup nên popup.listView tồn tại và getLocationOnScreen() cho toạ độ
 * đúng.
 *
 * Nguồn ảnh: dùng lại cache blur wallpaper toàn màn hình đã có sẵn
 * (FastBlurUtility.getPageBlurBackground - CHỈ đọc cache BlurEngine.blurBitmap, KHÔNG tự
 * chụp/blur lại) rồi crop đúng vùng popup - vừa nhanh vừa khớp với nền mờ đã hiển thị ở những
 * nơi khác trong app (thanh top/bottom bar, trang mới mở...).
 *
 * Nếu chụp/crop thất bại (cache chưa sẵn sàng, activity đang đóng...) thì vẽ fallbackContent
 * (nền rounded solid) thay thế - không bao giờ để trống.
 */
class BlurPopupBackgroundDrawable(
    activity: Activity,
    private val popup: ListPopupWindow,
    private val cornerRadiusPx: Float,
    private val fallbackContent: Drawable
) : Drawable() {

    private val activityRef = WeakReference(activity)
    private var captured = false
    private var croppedBitmap: Bitmap? = null

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val clipPath = Path()
    private val location = IntArray(2)

    override fun draw(canvas: Canvas) {
        if (!captured) {
            captured = true
            captureCrop()
        }

        val bounds = bounds
        if (bounds.width() <= 0 || bounds.height() <= 0) return

        clipPath.reset()
        clipPath.addRoundRect(RectF(bounds), cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
        val saveCount = canvas.save()
        canvas.clipPath(clipPath)

        val bitmap = croppedBitmap
        if (bitmap != null && !bitmap.isRecycled) {
            canvas.drawBitmap(bitmap, bounds.left.toFloat(), bounds.top.toFloat(), bitmapPaint)
        } else {
            fallbackContent.bounds = bounds
            fallbackContent.draw(canvas)
        }

        canvas.restoreToCount(saveCount)
    }

    private fun captureCrop() {
        val activity = activityRef.get() ?: return
        if (activity.isFinishing || activity.isDestroyed) return

        val listView = popup.listView ?: return
        val width = listView.width
        val height = listView.height
        if (width <= 0 || height <= 0) return

        listView.getLocationOnScreen(location)

        val fullBlur = FastBlurUtility.getPageBlurBackground(activity) ?: return
        try {
            val left = location[0].coerceIn(0, fullBlur.width)
            val top = location[1].coerceIn(0, fullBlur.height)
            val right = (left + width).coerceIn(left, fullBlur.width)
            val bottom = (top + height).coerceIn(top, fullBlur.height)
            if (right > left && bottom > top) {
                croppedBitmap = Bitmap.createBitmap(fullBlur, left, top, right - left, bottom - top)
            }
        } catch (_: Exception) {
            // giữ nguyên croppedBitmap = null -> draw() sẽ rơi xuống fallbackContent
        } finally {
            if (!fullBlur.isRecycled) {
                fullBlur.recycle()
            }
        }
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Deprecated("Deprecated in Java", ReplaceWith("PixelFormat.TRANSLUCENT"))
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
