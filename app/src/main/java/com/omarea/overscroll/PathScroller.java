package com.omarea.overscroll;

import android.graphics.Path;
import android.graphics.PathMeasure;
import android.util.Log;
import android.view.animation.AnimationUtils;

/**
 * A Scroller that can compute the "value - time" by a Path that extends from
 * <code>Point</code> <code>(0, ±infinite)</code> to <code>(1, ±infinite)</code>
 * . The x coordinate along the <code>Path</code> is the "time" and the y
 * coordinate is the "value".
 * 
 * <p>
 * The <code>Path</code> must not have gaps in the x direction and must not loop
 * back on itself such that there can be two points sharing the same x
 * coordinate. It is alright to have a disjoint line in the vertical direction:
 * </p>
 * <p>
 * <blockquote>
 * 
 * <pre>
 *     Path path = new Path();
 *     path.moveTo(0f, ...);
 *     path.lineTo(0.25f, 0.25f);
 *     path.moveTo(0.25f, 0.5f);
 *     path.lineTo(1f, ...);
 * </pre>
 * 
 * </blockquote>
 * </p>
 * 
 * The core idea is from
 * <code>android.support.v4.view.animation.PathInterpolatorCompat</code>
 * 
 * @author Mixiaoxiao 2016/9/1
 */
public class PathScroller {

	private static final String LOG_TAG = "PathScroller";
	private static final boolean DEBUG = false;
	private long mStartTime;
	private int mDuration;
	private boolean mFinished;
	private int mCurrValue;
	private float mValueFactor;
	private PathPointsHolder mPathPointsHolder;

	public PathScroller() {
		super();
	}

	/**
	 * Start scrolling.
	 * 
	 * @param valueFactor
	 *            The "Factor" to scale the "value", @see
	 *            <code>PathPointsHolder</code>
	 * @param duration
	 *            Duration of the scroll in milliseconds.
	 * @param pathPointsHolder
	 * @see <code>PathPointsHolder</code>
	 */
	public void start(float valueFactor, int duration, PathPointsHolder pathPointsHolder) {
		mValueFactor = valueFactor;
		mDuration = duration;
		mFinished = false;
		mPathPointsHolder = pathPointsHolder;
		mStartTime = AnimationUtils.currentAnimationTimeMillis();
	}

	/**
	 * Returns whether the scroller has finished scrolling.
	 * 
	 * @return True if the scroller has finished scrolling, false otherwise.
	 */
	public boolean isFinished() {
		return mFinished;
	}

	/**
	 * Call this when you want to know the new location("value"). If it returns
	 * true, the animation is not yet finished.
	 */
	public boolean computeScrollOffset() {
		if (mFinished) {
			return false;
		}
		final long timePassed = AnimationUtils.currentAnimationTimeMillis() - mStartTime;
		float timePassedPercent = timePassed * 1f / mDuration;
		if (timePassed >= mDuration) {
			mFinished = true;
			timePassedPercent = 1f;
		}
		mCurrValue = Math.round(mValueFactor * mPathPointsHolder.getY(timePassedPercent));
		return true;
	}

	/**
	 * Returns the current "value" in the scroll.
	 * 
	 * @return The new "value" as an absolute distance from the origin.
	 */
	public int getCurrValue() {
		return mCurrValue;
	}

	/**
	 * use {@link #getCurrValue()} instead
	 */
	public int getCurrY() {
		return mCurrValue;
	}

	/**
	 * Stops the animation.
	 */
	public void abortAnimation() {
		mFinished = true;

	}

	/**
	 * PathPointsHolder Creating a PathPointsHolder may cost some time and memory. You are
	 * recommended to create a static PathPointsHolder and reuse it by different
	 * <code>valueFactor</code>
	 */
	public static class PathPointsHolder {

		// This governs how accurate the approximation of the Path is.
		private static final float PRECISION = 0.002f;
		private final float[] mX;// x coordinates in the line
		private final float[] mY;// y coordinates in the line

		/**
		 * PathPointsHolder x∈[0,1],y∈[-infinite,+infinite]
		 * @param path
		 *            A Path that extends from <code>Point</code>
		 *            <code>(0, ±infinite)</code> to <code>(1, ±infinite)</code>
		 *            . The x coordinate along the <code>Path</code> is the
		 *            "time" and the y coordinate is the "value".
		 */
		public PathPointsHolder(Path path) {
			super();
			// The "approximate" method in SDKv21 is "@hide", oh, shit
			// path.approximate(float acceptableError);
			final long initStart = AnimationUtils.currentAnimationTimeMillis(); 
			final PathMeasure pathMeasure = new PathMeasure(path, false /* forceClosed */);

			final float pathLength = pathMeasure.getLength();
			final int numPoints = (int) (pathLength / PRECISION) + 1;

			mX = new float[numPoints];
			mY = new float[numPoints];

			final float[] position = new float[2];
			for (int i = 0; i < numPoints; ++i) {
				final float distance = (i * pathLength) / (numPoints - 1);
				pathMeasure.getPosTan(distance, position, null /* tangent */);

				mX[i] = position[0];
				mY[i] = position[1];
			}
			long usedTime = AnimationUtils.currentAnimationTimeMillis() - initStart;
			if(DEBUG){
				Log.d(LOG_TAG, "initPath cost time ->" + usedTime + "ms");
			}
			
		}

		float getY(float x) {
			if (x <= 0.0f) {
				return mY[0];// return 0.0f;
			} else if (x >= 1.0f) {
				return mY[mY.length - 1];// return 1.0f;
			}

			// Do a binary search for the correct x to interpolate between.
			int startIndex = 0;
			int endIndex = mX.length - 1;
			while (endIndex - startIndex > 1) {
				int midIndex = (startIndex + endIndex) / 2;
				if (x < mX[midIndex]) {
					endIndex = midIndex;
				} else {
					startIndex = midIndex;
				}
			}

			final float xRange = mX[endIndex] - mX[startIndex];
			if (xRange == 0) {
				return mY[startIndex];
			}

			final float tInRange = x - mX[startIndex];
			final float fraction = tInRange / xRange;

			final float startY = mY[startIndex];
			final float endY = mY[endIndex];

			return startY + (fraction * (endY - startY));
		}
	}

}
