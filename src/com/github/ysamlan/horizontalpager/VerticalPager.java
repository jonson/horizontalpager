package com.github.ysamlan.horizontalpager;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class VerticalPager extends AbstractPager {

	public VerticalPager(Context context) {
		super(context);
	}

	public VerticalPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onFirstLayout(int width, int height) {
		scrollTo(0, mCurrentScreen * height);
	}

	@Override
    protected void onLayout(final boolean changed, final int l, final int t, final int r,
            final int b) {
        int childTop = 0;
        final int count = getChildCount();

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                final int childHeight = child.getMeasuredHeight();
                child.layout(0, childTop, child.getMeasuredWidth(), childTop + childHeight);
                childTop += childHeight;
            }
        }
    }
	
	@Override
	protected boolean shouldInterceptMotionAction(int action) {
		return mTouchState == TOUCH_STATE_VERTICAL_SCROLLING; 
	}
	
	@Override
	protected void onTouchActionDown(float x, float y) {
		/*
		 * If being flinged and user touches, stop the fling. isFinished will be false if
		 * being flinged.
		 */
		if (!mScroller.isFinished()) {
		    mScroller.abortAnimation();
		}

		// Remember where the motion event started
		mLastMotionY = y;

		if (mScroller.isFinished()) {
		    mTouchState = TOUCH_STATE_REST;
		} else {
		    mTouchState = TOUCH_STATE_VERTICAL_SCROLLING;
		}
	}
	
	@Override
	protected void onTouchActionMove(float x, float y) {
		final int yDiff = (int) Math.abs(y - mLastMotionY);
        boolean yMoved = yDiff > mTouchSlop;

        if (yMoved) {
            // Scroll if the user moved far enough along the X axis
            mTouchState = TOUCH_STATE_VERTICAL_SCROLLING;
        }

        if (mTouchState == TOUCH_STATE_VERTICAL_SCROLLING) {
            // Scroll to follow the motion event
            final int deltaY = (int) (mLastMotionY - y);
            mLastMotionY = y;
            final int scrollY = getScrollY();

            if (deltaY < 0) {
                if (scrollY > 0) {
                    scrollBy(0, Math.max(-scrollY, deltaY));
                }
            } else if (deltaY > 0) {
                final int availableToScroll =
                        getChildAt(getChildCount() - 1).getBottom() - scrollY - getHeight();

                if (availableToScroll > 0) {
                    scrollBy(0, Math.min(availableToScroll, deltaY));
                }
            }
        }
	}
	
	@Override
	protected void snapToDestination() {
		
		final int screenHeight = getHeight();
        int scrollY = getScrollY();
        int whichScreen = mCurrentScreen;
        int deltaY = scrollY - (screenHeight * mCurrentScreen);

        // Check if they want to go to the prev. screen
        if ((deltaY < 0) && mCurrentScreen != 0
                && ((screenHeight / FRACTION_OF_SCREEN_WIDTH_FOR_SWIPE) < -deltaY)) {
            whichScreen--;
            // Check if they want to go to the next screen
        } else if ((deltaY > 0) && (mCurrentScreen + 1 != getChildCount())
                && ((screenHeight / FRACTION_OF_SCREEN_WIDTH_FOR_SWIPE) < deltaY)) {
            whichScreen++;
        }

        snapToScreen(whichScreen);
	}
	
	@Override
	protected void snapToScreen(int whichScreen, int duration) {
		
		/*
         * Modified by Yoni Samlan: Allow new snapping even during an ongoing scroll animation. This
         * is intended to make HorizontalPager work as expected when used in conjunction with a
         * RadioGroup used as "tabbed" controls.
         */
        mNextScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
        final int newY = mNextScreen * getHeight();
        final int delta = newY - getScrollY();

        if (duration < 0) {
            mScroller.startScroll(0, getScrollY(), 0, delta, Math.abs(delta) * 2);
        } else {
            mScroller.startScroll(0, getScrollY(), 0, delta, duration);
        }

        invalidate();
		
	}
	
	@Override
	public void setCurrentScreen(int currentScreen, boolean animate) {
		mCurrentScreen = Math.max(0, Math.min(currentScreen, getChildCount() - 1));
        if (animate) {
            snapToScreen(currentScreen, ANIMATION_SCREEN_SET_DURATION_MILLIS);
        } else {
            scrollTo(0, mCurrentScreen * getHeight());
        }
        invalidate();
	}

}
