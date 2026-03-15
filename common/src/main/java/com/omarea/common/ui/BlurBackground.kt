package com.omarea.common.ui

import android.app.Activity
import android.app.Dialog
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.graphics.scale
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build

class BlurBackground(private val activity: Activity) {
    private var dialogBg: ImageView? = null
    private var originalW = 0
    private var originalH = 0
    private var mHandler: Handler = Handler(Looper.getMainLooper())

    private fun captureScreen(activity: Activity): Bitmap {
        activity.window.decorView.destroyDrawingCache() //先清理屏幕绘制缓存(重要)
        activity.window.decorView.isDrawingCacheEnabled = true
        var bmp: Bitmap = activity.window.decorView.drawingCache
        //获取原图尺寸
        originalW = bmp.width
        originalH = bmp.height
        //对原图进行缩小，提高下一步高斯模糊的效率
        bmp = bmp.scale(originalW / 4, originalH / 4, false)
        return bmp
    }

    private fun asyncRefresh(`in`: Boolean) {
        //淡出淡入效果的实现
        if (`in`) {    //淡入效果
            Thread {
                var i = 0
                while (i < 256) {
                    refreshUI(i) //在UI线程刷新视图
                    try {
                        Thread.sleep(4)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    i += 5
                }
            }.start()
        } else {    //淡出效果
            Thread {
                var i = 255
                while (i >= 0) {
                    refreshUI(i) //在UI线程刷新视图
                    try {
                        Thread.sleep(4)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    i -= 5
                }
                //当淡出效果完毕后发送消息给mHandler把对话框背景设为不可见
                mHandler.sendEmptyMessage(0)
            }.start()
        }
    }

    private fun runOnUiThread(runnable: Runnable) {
        mHandler.post(runnable)
    }

    private fun refreshUI(i: Int) {
        runOnUiThread { dialogBg?.imageAlpha = i }
    }

    private fun hideBlur() {
        //把对话框背景隐藏
        asyncRefresh(false)
        System.gc()
    }

    private fun blur(bitmap: Bitmap?): Bitmap? {
        //使用RenderScript对图片进行高斯模糊处理
        val output = bitmap?.let { Bitmap.createBitmap(it) } // 创建输出图片
        val rs: RenderScript = RenderScript.create(activity) // 构建一个RenderScript对象
        val gaussianBlue: ScriptIntrinsicBlur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs)) //
        // 创建高斯模糊脚本
        val allIn: Allocation = Allocation.createFromBitmap(rs, bitmap) // 开辟输入内存
        val allOut: Allocation = Allocation.createFromBitmap(rs, output) // 开辟输出内存
        val radius = 10f //设置模糊半径
        gaussianBlue.setRadius(radius) // 设置模糊半径，范围0f<radius<=25f
        gaussianBlue.setInput(allIn) // 设置输入内存
        gaussianBlue.forEach(allOut) // 模糊编码，并将内存填入输出内存
        allOut.copyTo(output) // 将输出内存编码为Bitmap，图片大小必须注意
        rs.destroy()
        //rs.releaseAllContexts(); // 关闭RenderScript对象，API>=23则使用rs.releaseAllContexts()
        return output
    }

private fun handleBlur() {
    dialogBg?.run {
        var bp: Bitmap? = captureScreen(activity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ blur bằng GPU
            bp = bp?.scale(originalW, originalH, false)
            setImageBitmap(bp)

            setRenderEffect(
                RenderEffect.createBlurEffect(
                    40f,
                    40f,
                    Shader.TileMode.CLAMP
                )
            )

        } else {
            // Android 11 trở xuống
            bp = blur(bp)
            bp = bp?.scale(originalW, originalH, false)
            setImageBitmap(bp)
        }
        visibility = View.VISIBLE
        asyncRefresh(true)
    }
}

    fun setScreenBgLight(dialog: Dialog) {
        val window: Window? = dialog.window
        val lp: WindowManager.LayoutParams
        if (window != null) {
            lp = window.attributes
            lp.dimAmount = 0.5f
            window.attributes = lp
        }
        handleBlur()
    }

}