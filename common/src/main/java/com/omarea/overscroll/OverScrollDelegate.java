package com.omarea.overscroll;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;

import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.omarea.overscroll.PathScroller.PathPointsHolder;

public class OverScrollDelegate {

	public interface OverScrollable {
		int superComputeVerticalScrollExtent();

		int superComputeVerticalScrollOffset();

		int superComputeVerticalScrollRange();

		void superOnTouchEvent(MotionEvent event);

		void superDraw(Canvas canvas);

		boolean superAwakenScrollBars();

		boolean superOverScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX,
                                  int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent);

		View getOverScrollableView();

		OverScrollDelegate getOverScrollDelegate();
	}

	public static abstract class OverScrollStyle {

		public static final float DEFAULT_DRAW_TRANSLATE_RATE = 0.36f;
		public static final float SYSTEM_DENSITY = Resources.getSystem().getDisplayMetrics().density;

		/**
		 * Transform canvas before draw content, for example, do
		 * canvas.translate
		 */
		public void transformOverScrollCanvas(float offsetY, Canvas canvas, View view) {
			final int translateY = Math.round(offsetY * DEFAULT_DRAW_TRANSLATE_RATE);
			canvas.translate(0, translateY);
		}

		/**
		 * Draw overscroll effect(e.g. logo) at top, the direction of offsetY is
		 * same as TouchEvent
		 */
		public void drawOverScrollTop(float offsetY, Canvas canvas, View view) {
		}

		/**
		 * Draw overscroll effect(e.g. logo) at bottom, the direction of offsetY
		 * is same as TouchEvent
		 */
		public void drawOverScrollBottom(float offsetY, Canvas canvas, View view) {
		}

		/**
		 * To get the "real bottom" when overscroll bottom, that is height +
		 * getScrollY
		 **/
		public final int getOverScrollViewCanvasBottom(View view) {
			return view.getHeight() + view.getScrollY();
		}

		public static int dp2px(int dp) {
			return (int) (dp * SYSTEM_DENSITY + 0.5f);
		}
	}

	private static final OverScrollStyle sDefaultStyle = new OverScrollStyle() {
	};

	private static final PathPointsHolder sDragBackPathPointsHolder = buildDragBackPathPointsHolder();
	private static final PathPointsHolder sFlingBackPathPointsHolder = buildFlingBackPathPointsHolder();

	private static PathPointsHolder buildDragBackPathPointsHolder() {
		final float startY = 1f;
		Path path = new Path();
//		path.moveTo(0, startY);
//		path.cubicTo(0.15f, 0.11f, 0.56f, 0.10f, 1f, 0f);
		path.moveTo(0, startY);
		path.cubicTo(0.11f, 0.11f, 0.36f, 0.05f, 1f, 0f);
		return new PathPointsHolder(path);
	}

	private static PathPointsHolder buildFlingBackPathPointsHolder() {
		final float baseOverY = 1f; 
		Path path = new Path();
		path.moveTo(0f, 0f);
//		path.cubicTo(0.1f, overY * 0.6f, 0.15f, overY * 1.0f, 0.30f, overY);
//		path.cubicTo(0.44f, overY * 1.00f, 0.58f, overY * 0.02f, 1f, 0f);
		path.cubicTo(0.05f, baseOverY * 0.8f, 0.09f, baseOverY * 1.20f, 0.21f, baseOverY * 0.88f);
		path.cubicTo(0.48f, baseOverY * 0.10f, 0.72f, baseOverY * 0.02f, 1f, 0f);
		return new PathPointsHolder(path);
	}

	static final String LOG_TAG = "OverScrollDelegate";

	public static final int OS_NONE = 0;// OS = "OverScroll"
	public static final int OS_DRAG_TOP = 1;
	public static final int OS_DRAG_BOTTOM = 2;
	public static final int OS_SPRING_BACK = 3;
	public static final int OS_FLING = 4;

	private static final int DRAG_BACK_DURATION = 420;
	private static final int FLING_BACK_DURATION = 550;

	private static final int INVALID_POINTER = -1;

	private int mState = OS_NONE;

	private final int mTouchSlop;
	private float mLastMotionY;
	// private float mLastMotionX;
	private float mOffsetY;
	private int mActivePointerId = INVALID_POINTER;

	private final PathScroller mScroller;

	private final View mView;
	private final OverScrollable mOverScrollable;
	private boolean mEnableDragOverScroll = true;
	private boolean mEnableFlingOverScroll = true;
	private OverScrollStyle mStyle;

	// private final Path mPath = new Path();

	public OverScrollDelegate(OverScrollable overScrollable) {
		this.mView = overScrollable.getOverScrollableView();
		if (mView instanceof RecyclerView) {
			// In RecyclerView, we need to override the method absorbGlows
			// to get the velocity of fling overscroll,
			// but if OVER_SCROLL_NEVER, this method will not be called.
			// See RecyclerView$ViewFlinger.run();
			mView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
		} else {
			mView.setOverScrollMode(View.OVER_SCROLL_NEVER);
		}
		this.mOverScrollable = overScrollable;
		Context context = mView.getContext();
		mScroller = new PathScroller();
		ViewConfiguration configuration = ViewConfiguration.get(context);
		// TouchSlop() / 2 to make TouchSlop "more sensible"
		mTouchSlop = configuration.getScaledTouchSlop() / 2;// 8dp/2
		mStyle = sDefaultStyle;
	}

	// ===========================================================
	// Customization
	// ===========================================================
	public void setOverScrollStyle(OverScrollStyle style) {
		if (style == null) {
			throw new IllegalArgumentException("OverScrollStyle should NOT be NULL!");
		}
		mStyle = style;
	}

	/**
	 * Enable drag/fling-overscroll
	 * 
	 * @param dragOverScroll
	 * @param flingOverScroll
	 */
	public void setOverScrollType(boolean dragOverScroll, boolean flingOverScroll) {
		mEnableDragOverScroll = dragOverScroll;
		mEnableFlingOverScroll = flingOverScroll;
	}

	// ===========================================================
	// Delegate
	// ===========================================================
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (mEnableDragOverScroll) {
			return onInterceptTouchEventInternal(event);
		}
		return false;
	}

	public boolean onTouchEvent(MotionEvent event) {
		if (mEnableDragOverScroll) {
			return onTouchEventInternal(event);
		}
		return false;
	}

	/**
	 * In RecyclerView, overScrollBy does not work. Call absorbGlows instead of
	 * this method. If super.overScrollBy return true and isTouchEvent, means
	 * current scroll is fling-overscroll, we use the deltaY to compute
	 * velocityY.
	 */
	public boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY,
			int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
		maxOverScrollX = maxOverScrollY = 0;
		final boolean overScroll = mOverScrollable.superOverScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX,
				scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent);
		if (!mEnableFlingOverScroll) {
			return overScroll;
		}
		if (!overScroll) {
			if (mState == OS_FLING) {
				log("warning, overScroll=flase BUT mState=OS_FLING");
			}
		} else {// overScroll = true;
			if (!isTouchEvent) {
				// isTouchEvent=false, means fling by the scroller of View
				if (mState != OS_FLING) {
					log("deltaY->" + deltaY);
					// Compute the velocity by the deltaY
					final int velocityY = -(int) (deltaY / 0.0166666f);
					onAbsorb(velocityY);
				}
			}
		}
		return overScroll;
	}

	/**
	 * In RecyclerView, overScrollBy does not work. Call absorbGlows instead of
	 * this method.
	 */
	public void recyclerViewAbsorbGlows(int velocityX, int velocityY) {
		log("recyclerViewAbsorbGlows velocityY->" + velocityY);
		if (mEnableFlingOverScroll) {
			// The direction mOffsetY is same as TouchEvent,
			// and the direction of "velocityY" is same as scroll,
			// so we need to reverse it.
			if (velocityY != 0) {
				onAbsorb(-velocityY);
			}

		}
	}

	private void onAbsorb(final int velocityY) {
		// offset the start of fling 1px
		mOffsetY = velocityY > 0 ? -1 : 1;
		final float overY = velocityY * 0.07f;
		log("velocityY->" + velocityY + " overY->" + overY);
		mScroller.start(overY, FLING_BACK_DURATION, sFlingBackPathPointsHolder);
		setState(OS_FLING);
		mView.invalidate();
	} 

	public void draw(Canvas canvas) {
		if (mState == OS_NONE) {
			mOverScrollable.superDraw(canvas);
		} else {
			if (mState == OS_SPRING_BACK || mState == OS_FLING) {
				if (mScroller.computeScrollOffset()) {
					mOffsetY = mScroller.getCurrY();
				} else {
					mOffsetY = 0;
					setState(OS_NONE);
				}
                mView.postInvalidateOnAnimation();
			}
			final int sc = canvas.save();
			mStyle.transformOverScrollCanvas(mOffsetY, canvas, mView);
			mOverScrollable.superDraw(canvas);
			canvas.restoreToCount(sc);

			final int sc1 = canvas.save();
			if (mOffsetY > 0) {// top
				mStyle.drawOverScrollTop(mOffsetY, canvas, mView);
			} else if (mOffsetY < 0) {// bottom
				mStyle.drawOverScrollBottom(mOffsetY, canvas, mView);
			}
			canvas.restoreToCount(sc1);
		}
	}

	// ===========================================================
	// Internal
	// ===========================================================

	private void setState(int newState) {
		if (mState != newState) {
			mState = newState;
			String newStateName = "";
			if (mState == OS_NONE) {
				newStateName = "OS_NONE";
			} else if (mState == OS_DRAG_TOP) {
				newStateName = "OS_TOP";
			} else if (mState == OS_DRAG_BOTTOM) {
				newStateName = "OS_BOTTOM";
			} else if (mState == OS_SPRING_BACK) {
				newStateName = "OS_SPRING_BACK";
			} else if (mState == OS_FLING) {
				newStateName = "OS_FLING";
			}
			log("setState->" + newStateName);
		}
	}

	private boolean isOsTop() {
		return mState == OS_DRAG_TOP;
	}

	private boolean isOsBottom() {
		return mState == OS_DRAG_BOTTOM;
	}

	private boolean isOsDrag() {
		return mState == OS_DRAG_TOP || mState == OS_DRAG_BOTTOM;
	}

	private boolean onInterceptTouchEventInternal(MotionEvent event) {
		final int action = event.getActionMasked();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			log("onInterceptTouchEvent -> ACTION_DOWN");
			mLastMotionY = event.getY();
			// mLastMotionX = ev.getX();
			mActivePointerId = event.getPointerId(0);
			// If OS_FLING, we do not Intercept and allow the scroller to finish
			if (mState == OS_SPRING_BACK) {
				if (mScroller.computeScrollOffset()) {
					mOffsetY = mScroller.getCurrY();
					mScroller.abortAnimation();
					if (mOffsetY == 0) {
						setState(OS_NONE);
					} else {
						setState(mOffsetY > 0 ? OS_DRAG_TOP : OS_DRAG_BOTTOM);
					}
					mView.invalidate();
				}
			}
			break;
		case MotionEvent.ACTION_MOVE:
			// log("onInterceptTouchEvent -> ACTION_MOVE " + ev.toString());
			if (mActivePointerId == INVALID_POINTER) {
				Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
				break;
			}
			final int pointerIndex = event.findPointerIndex(mActivePointerId);
			if (pointerIndex == -1) {
				Log.e(LOG_TAG, "Invalid pointerId=" + mActivePointerId + " in onInterceptTouchEvent");
				break;
			}
			final float y = event.getY(pointerIndex);
			// final float x = ev.getX(pointerIndex);
			final float yDiff = y - mLastMotionY;
			// final float xDiff = x - mLastMotionX;
			mLastMotionY = y;
			if (!isOsDrag()) {
				boolean canScrollUp, canScrollDown;
				final int offset = mOverScrollable.superComputeVerticalScrollOffset();
				final int range = mOverScrollable.superComputeVerticalScrollRange()
						- mOverScrollable.superComputeVerticalScrollExtent();
				if (range == 0) {
					canScrollDown = canScrollUp = false;
				} else {
					canScrollUp = offset > 0;
					canScrollDown = offset < (range - 1);
				}
				if (canScrollUp && canScrollDown) {
					break;
				}
				// mLastMotionX = x;
				if ((Math.abs(yDiff) > mTouchSlop)) {
					boolean isOs = false;
					if (!canScrollUp && yDiff > 0) {
						setState(OS_DRAG_TOP);
						isOs = true;
					} else if (!canScrollDown && yDiff < 0) {
						setState(OS_DRAG_BOTTOM);
						isOs = true;
					}
					if (isOs) {
						// Cancel the "real touch of View"
						MotionEvent fakeCancelEvent = MotionEvent.obtain(event);
						fakeCancelEvent.setAction(MotionEvent.ACTION_CANCEL);
						mOverScrollable.superOnTouchEvent(fakeCancelEvent);
						fakeCancelEvent.recycle();
						mOverScrollable.superAwakenScrollBars();

						final ViewParent parent = mView.getParent();
						if (parent != null) {
							parent.requestDisallowInterceptTouchEvent(true);
						}
					}
				}
			}

			break;

		case MotionEvent.ACTION_POINTER_UP:
			onSecondaryPointerUp(event);
			break;

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			mActivePointerId = INVALID_POINTER;
			break;
		}
		return isOsDrag();
	}

	private boolean onTouchEventInternal(MotionEvent event) {
		// log("onTouchEvent->" + ev.toString());
		final int action = event.getActionMasked();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			log("onTouchEvent -> ACTION_DOWN");
			mLastMotionY = event.getY();
			// mLastMotionX = ev.getX();
			mActivePointerId = event.getPointerId(0);
			// ACTION_DOWN is hanled in InterceptTouchEvent
			break;
		case MotionEvent.ACTION_MOVE: {
			// log("onTouchEvent -> ACTION_MOVE "+ ev.toString());
			if (mActivePointerId == INVALID_POINTER) {
				Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
				break;
			}
			final int pointerIndex = MotionEventCompat.findPointerIndex(event, mActivePointerId);
			if (pointerIndex < 0) {
				Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
				break;
			}
			final float y = event.getY(pointerIndex);
			// final float x = ev.getX(pointerIndex);
			final float yDiff = y - mLastMotionY;
			// final float xDiff = x - mLastMotionX;
			mLastMotionY = y;
			if (!isOsDrag()) {
				boolean canScrollUp, canScrollDown;
				final int offset = mOverScrollable.superComputeVerticalScrollOffset();
				final int range = mOverScrollable.superComputeVerticalScrollRange()
						- mOverScrollable.superComputeVerticalScrollExtent();
				if (range == 0) {
					canScrollDown = canScrollUp = false;
				} else {
					canScrollUp = offset > 0;
					canScrollDown = offset < (range - 1);
				}
				if (canScrollUp && canScrollDown) {
					break;
				}
				// mLastMotionX = x;
				// In TouchEvent, if can not UP or Down and yDiff > 1px,
				// we start drag overscroll
				if ((Math.abs(yDiff) >= 1f)) {// mTouchSlop
					boolean isOs = false;
					if (!canScrollUp && yDiff > 0) {
						setState(OS_DRAG_TOP);
						isOs = true;
					} else if (!canScrollDown && yDiff < 0) {
						setState(OS_DRAG_BOTTOM);
						isOs = true;
					}
					if (isOs) {
						// Cancel the "real touch of View"
						MotionEvent fakeCancelEvent = MotionEvent.obtain(event);
						fakeCancelEvent.setAction(MotionEvent.ACTION_CANCEL);
						mOverScrollable.superOnTouchEvent(fakeCancelEvent);
						fakeCancelEvent.recycle();
						mOverScrollable.superAwakenScrollBars();

						final ViewParent parent = mView.getParent();
						if (parent != null) {
							parent.requestDisallowInterceptTouchEvent(true);
						}
					}
				}
			}
			if (isOsDrag()) {
				// mLastMotionX = MotionEventCompat.getX(ev, pointerIndex);
				mOffsetY += yDiff;
				if (isOsTop()) {// mDragOffsetY should > 0
					if (mOffsetY <= 0) {
						setState(OS_NONE);
						mOffsetY = 0;
						// return to "touch real view"
						MotionEvent fakeDownEvent = MotionEvent.obtain(event);
						fakeDownEvent.setAction(MotionEvent.ACTION_DOWN);
						mOverScrollable.superOnTouchEvent(fakeDownEvent);
						fakeDownEvent.recycle();
					}
				} else if (isOsBottom()) {// mDragOffsetY should < 0
					if (mOffsetY >= 0) {
						setState(OS_NONE);
						mOffsetY = 0;
						// return to "touch real view"
						MotionEvent fakeDownEvent = MotionEvent.obtain(event);
						fakeDownEvent.setAction(MotionEvent.ACTION_DOWN);
						mOverScrollable.superOnTouchEvent(fakeDownEvent);
						fakeDownEvent.recycle();
					}
				}
				mView.invalidate();
			}
			break;
		}
		case MotionEventCompat.ACTION_POINTER_DOWN: {
			final int index = event.getActionIndex();
			mLastMotionY = event.getY(index);
			// mLastMotionX = MotionEventCompat.getX(ev, index);
			mActivePointerId = event.getPointerId(index);
			break;
		}

		case MotionEventCompat.ACTION_POINTER_UP: {
			onSecondaryPointerUp(event);
			final int index = event.findPointerIndex(mActivePointerId);
			if (index != -1) {
				mLastMotionY = event.getY(index);
				// mLastMotionX = MotionEventCompat.getX(ev, index);
			}
			break;
		}
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL: {
			if (mOffsetY != 0f) {
				// Sping back to 0
				final int startScrollY = Math.round(mOffsetY);
				// mScroller.startScroll(0, startScrollY, 0, -startScrollY,
				// SPRING_BACK_DURATION);
				// mPath.reset();
				// mPath.moveTo(0f, startScrollY);
				// mPath.lineTo(1f, 0);
				// mScroller.start(1f, SPRING_BACK_DURATION, mPath);
				mScroller.start(startScrollY, DRAG_BACK_DURATION, sDragBackPathPointsHolder);
				setState(OS_SPRING_BACK);
				mView.invalidate();
			}
			mActivePointerId = INVALID_POINTER;
		}
		}
		return isOsDrag();
	}

	private void onSecondaryPointerUp(MotionEvent ev) {
		final int pointerIndex = ev.getActionIndex();
		final int pointerId = ev.getPointerId(pointerIndex);
		if (pointerId == mActivePointerId) {
			// This was our active pointer going up. Choose a new
			// active pointer and adjust accordingly.
			final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
			mActivePointerId = ev.getPointerId(newPointerIndex);
		}
	}

	private void log(String msg) {
		Log.d(LOG_TAG, msg);
	}

}
