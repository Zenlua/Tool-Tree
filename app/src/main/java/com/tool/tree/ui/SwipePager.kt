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
 * Tối ưu hóa hiệu năng cho số lượng tab cố định, loại bỏ hoàn toàn chi phí RecyclerView.
 * Đã tích hợp chống khựng (drop frame) và hiệu ứng cuộn quá mép (rubber-band overscroll).
 */
class SwipePager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    interface OnPageChangeListener {
        fun onPageSelected(position: Int) {}
        fun onPageScrolled(position: Int, offset: Float) {}
    }

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

    private val touchSlop: Int

    private var initialX = 0f
    private var initialY = 0f
    private var lastX = 0f
    private var isDragging = false

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

    private fun applyPageTransform() {
        val transformer = pageTransformer ?: return
        if (width == 0) return
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val position = (child.left - scrollX).toFloat() / width
            transformer.transformPage(child, position)
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
            velocityTracker?.recycle()
            velocityTracker = null
            return false
        }
        if (action != MotionEvent.ACTION_DOWN && isDragging) return true

        if (velocityTracker == null) velocityTracker = VelocityTracker.obtain()
        velocityTracker?.addMovement(ev)

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
                    var dx = lastX - ev.x
                    lastX = ev.x
                    
                    val maxScroll = max(0, (pages.size - 1) * width).toFloat()

                    // Tạo lực cản khi vuốt vượt biên (Rubber-band damping)
                    if (scrollX < 0 && dx < 0) {
                        dx *= 0.35f
                    } else if (scrollX > maxScroll && dx > 0) {
                        dx *= 0.35f
                    }

                    val newScrollX = scrollX + dx
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
        val maxScroll = max(0, (pages.size - 1) * width)

        // Nảy về vị trí chuẩn nếu đang vượt mép trái/phải
        if (scrollX < 0) {
            setCurrentItem(0, true)
            return
        } else if (scrollX > maxScroll) {
            setCurrentItem(pages.size - 1, true)
            return
        }

        val page = width
        val current = scrollX / page
        val fraction = (scrollX % page).toFloat() / page

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
        } else if (!isDragging && width > 0 && pages.isNotEmpty()) {
            // Chỉ phát sự kiện đổi page khi đã dừng di chuyển hoàn toàn
            val settled = (scrollX / width).coerceIn(0, pages.size - 1)
            if (settled != currentItem) {
                currentItem = settled
                listener?.onPageSelected(settled)
            }
        }
    }
}