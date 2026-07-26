package com.tool.tree.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.OverScroller
import kotlin.math.abs
import kotlin.math.max

/**
 * SwipePager - thay thế androidx.viewpager2.widget.ViewPager2.
 *
 * Lý do viết riêng thay vì dùng ViewPager2:
 * Tool-Tree chỉ có đúng 4 tab CỐ ĐỊNH, và trước đây offscreenPageLimit đã được set = 4,
 * nghĩa là ViewPager2 vốn đã giữ TẤT CẢ trang trong bộ nhớ, không hề recycle. Vậy toàn bộ
 * bộ máy RecyclerView bên trong ViewPager2 (đo đạc/layout qua LayoutManager riêng, tạo-huỷ
 * ViewHolder, nested-scrolling, nhiều tầng listener/Adapter) chỉ tạo thêm chi phí xử lý mỗi
 * khung hình mà không mang lại lợi ích recycle nào cả - đây là nguyên nhân chính gây giật.
 *
 * SwipePager tự layout các trang cạnh nhau (giống ViewPager thế hệ 1) và tự xử lý
 * chạm/fling bằng OverScroller - ít tầng trung gian hơn nên phản hồi nhanh và mượt hơn.
 */
class SwipePager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    interface OnPageChangeListener {
        fun onPageSelected(position: Int) {}
        fun onPageScrolled(position: Int, offset: Float) {}
    }

    /**
     * Cho phép áp hiệu ứng chuyển trang tuỳ ý (fade, scale, depth...), tương tự
     * ViewPager.PageTransformer thế hệ 1. [position] là vị trí tương đối của trang so với
     * trang đang được chọn: 0 = đang hiển thị đầy đủ, -1 = lùi hẳn 1 trang bên trái,
     * 1 = lùi hẳn 1 trang bên phải.
     */
    interface PageTransformer {
        fun transformPage(page: View, position: Float)
    }

    private val pages = ArrayList<View>()
    private var listener: OnPageChangeListener? = null
    private var pageTransformer: PageTransformer? = null

    var currentItem: Int = 0
        private set

    private val scroller = OverScroller(context, DecelerateInterpolator(1.4f))
    private var velocityTracker: VelocityTracker? = null
    private val minFlingVelocity: Int
    private val maxFlingVelocity: Int

    // Giảm touch slop so với mặc định hệ thống để cử chỉ vuốt được nhận diện sớm hơn,
    // dứt khoát hơn ngay từ những pixel di chuyển đầu tiên (thay cho hack reflection cũ
    // từng áp dụng lên RecyclerView nội bộ của ViewPager2).
    private val touchSlop: Int

    private var initialX = 0f
    private var initialY = 0f
    private var lastX = 0f
    private var isDragging = false

    // Vị trí đang chọn được yêu cầu trước khi View có kích thước (vd gọi lúc khởi tạo,
    // trước lần layout đầu tiên) - sẽ được áp dụng ngay khi layout xong.
    private var pendingItem: Int? = null

    init {
        val vc = ViewConfiguration.get(context)
        minFlingVelocity = vc.scaledMinimumFlingVelocity
        maxFlingVelocity = vc.scaledMaximumFlingVelocity
        touchSlop = (vc.scaledTouchSlop * 0.6f).toInt()
    }

    fun setOnPageChangeListener(l: OnPageChangeListener) {
        listener = l
    }

    fun setPageTransformer(transformer: PageTransformer?) {
        pageTransformer = transformer
        applyPageTransform()
    }

    /**
     * Áp hiệu ứng transform lên từng trang dựa theo vị trí hiện tại của scrollX.
     * Gọi lại mỗi khi scrollX đổi (kéo tay, fling, settle) hoặc khi layout/thêm trang.
     */
    private fun applyPageTransform() {
        if (width == 0) return
        val transformer = pageTransformer
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val position = (child.left - scrollX).toFloat() / width
            if (transformer != null) {
                transformer.transformPage(child, position)
            } else {
                // Không có transformer -> đảm bảo trang luôn ở trạng thái gốc
                child.alpha = 1f
                child.scaleX = 1f
                child.scaleY = 1f
                child.translationX = 0f
            }
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        applyPageTransform()
    }

    fun addPage(view: View) {
        pages.add(view)
        addView(view)
        requestLayout()
        applyPageTransform()
    }

    fun getPageCount(): Int = pages.size

    fun setCurrentItem(position: Int, smoothScroll: Boolean = true) {
        if (pages.isEmpty()) return
        val target = position.coerceIn(0, pages.size - 1)

        if (width == 0) {
            // Chưa layout xong -> ghi nhận, đợi onLayout căn lại
            pendingItem = target
            if (currentItem != target) {
                currentItem = target
                listener?.onPageSelected(target)
            }
            return
        }

        val targetX = target * width
        scroller.abortAnimation()
        if (smoothScroll) {
            scroller.startScroll(scrollX, 0, targetX - scrollX, 0, 300)
            postInvalidateOnAnimation()
        } else {
            scrollTo(targetX, 0)
        }

        if (currentItem != target) {
            currentItem = target
            listener?.onPageSelected(target)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val childWidthSpec = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY)
        val childHeightSpec = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
        for (i in 0 until childCount) {
            getChildAt(i).measure(childWidthSpec, childHeightSpec)
        }
        setMeasuredDimension(w, h)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val w = r - l
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val left = i * w
            child.layout(left, 0, left + w, b - t)
        }
        if (changed && w > 0) {
            val target = pendingItem ?: currentItem
            pendingItem = null
            scrollTo(target * w, 0)
        }
        applyPageTransform()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            isDragging = false
            return false
        }
        if (action != MotionEvent.ACTION_DOWN && isDragging) return true

        if (action == MotionEvent.ACTION_DOWN) {
            initialX = ev.x
            initialY = ev.y
            lastX = ev.x
            isDragging = false
            if (!scroller.isFinished) scroller.abortAnimation()
        } else if (action == MotionEvent.ACTION_MOVE) {
            val dx = ev.x - initialX
            val dy = ev.y - initialY
            if (abs(dx) > touchSlop && abs(dx) > abs(dy)) {
                isDragging = true
                lastX = ev.x
                parent?.requestDisallowInterceptTouchEvent(true)
            }
        }
        return isDragging
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (pages.size <= 1) return false

        if (velocityTracker == null) velocityTracker = VelocityTracker.obtain()
        velocityTracker?.addMovement(ev)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!scroller.isFinished) scroller.abortAnimation()
                initialX = ev.x
                initialY = ev.y
                lastX = ev.x
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isDragging) {
                    val dx = ev.x - initialX
                    val dy = ev.y - initialY
                    if (abs(dx) > touchSlop && abs(dx) > abs(dy)) isDragging = true
                }
                if (isDragging) {
                    val dx = lastX - ev.x
                    lastX = ev.x
                    val maxScroll = max(0, (pages.size - 1) * width)
                    val newScrollX = (scrollX + dx).coerceIn(0f, maxScroll.toFloat())
                    scrollTo(newScrollX.toInt(), 0)
                    notifyScrolled()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    velocityTracker?.computeCurrentVelocity(1000, maxFlingVelocity.toFloat())
                    settle(velocityTracker?.xVelocity ?: 0f)
                }
                isDragging = false
                velocityTracker?.recycle()
                velocityTracker = null
            }
        }
        return true
    }

    private fun settle(velocityX: Float) {
        if (width == 0 || pages.isEmpty()) return
        val page = width
        val current = scrollX / page
        val fraction = (scrollX % page).toFloat() / page

        // VelocityTracker: velocityX < 0 nghĩa là ngón tay đang di chuyển sang trái
        // (vuốt để xem trang KẾ TIẾP); velocityX > 0 nghĩa là vuốt sang phải (trang trước).
        val target = when {
            abs(velocityX) > minFlingVelocity -> if (velocityX < 0) current + 1 else current
            fraction > 0.5f -> current + 1
            else -> current
        }
        setCurrentItem(target.coerceIn(0, pages.size - 1), true)
    }

    private fun notifyScrolled() {
        if (width == 0 || pages.isEmpty()) return
        val position = (scrollX / width).coerceIn(0, pages.size - 1)
        val offset = (scrollX % width).toFloat() / width
        listener?.onPageScrolled(position, offset)
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            notifyScrolled()
            postInvalidateOnAnimation()
        } else if (width > 0 && pages.isNotEmpty()) {
            val settled = (scrollX / width).coerceIn(0, pages.size - 1)
            if (settled != currentItem) {
                currentItem = settled
                listener?.onPageSelected(settled)
            }
        }
    }
}
