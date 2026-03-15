package com.omarea.common.ui

import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.graphics.scale

class BlurBackground(private val activity: Activity) {

    var dialogBg: ImageView? = null

    private var originalW = 0
    private var originalH = 0

    private fun captureScreen(activity: Activity): Bitmap? {
        val decorView = activity.window.decorView
        decorView.destroyDrawingCache()
        decorView.isDrawingCacheEnabled = true

        val bmp = decorView.drawingCache ?: return null

        originalW = bmp.width
        originalH = bmp.height

        return bmp.scale(originalW / 4, originalH / 4, false)
    }

    private fun asyncRefresh(isIn: Boolean) {
        val start = if (isIn) 0 else 255
        val end = if (isIn) 255 else 0

        ValueAnimator.ofInt(start, end).apply {
            duration = 200
            addUpdateListener {
                dialogBg?.imageAlpha = it.animatedValue as Int
            }
            start()
        }
    }

    private fun blur(bitmap: Bitmap?): Bitmap? {
        bitmap ?: return null

        val output = Bitmap.createBitmap(bitmap)
        val rs = RenderScript.create(activity)

        val blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))

        val allIn = Allocation.createFromBitmap(rs, bitmap)
        val allOut = Allocation.createFromBitmap(rs, output)

        blur.setRadius(10f)
        blur.setInput(allIn)
        blur.forEach(allOut)

        allOut.copyTo(output)

        rs.destroy()

        return output
    }

    private fun handleBlur() {
        dialogBg?.run {

            var bp = captureScreen(activity) ?: return

            bp = blur(bp)
            bp = bp?.scale(originalW, originalH, false)

            setImageBitmap(bp)

            visibility = View.VISIBLE

            asyncRefresh(true)
        }
    }

    fun setScreenBgLight(dialog: Dialog) {
        val window: Window? = dialog.window

        window?.let {
            val lp = it.attributes
            lp.dimAmount = 0.5f
            it.attributes = lp
        }

        handleBlur()
    }
}