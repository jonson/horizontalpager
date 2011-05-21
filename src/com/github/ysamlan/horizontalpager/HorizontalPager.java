/*
 * Modifications by Yoni Samlan; based on RealViewSwitcher, whose license is:
 *
 * Copyright (C) 2010 Marc Reichelt
 *
 * Work derived from Workspace.java of the Launcher application
 *  see http://android.git.kernel.org/?p=platform/packages/apps/Launcher.git
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.ysamlan.horizontalpager;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

/**
 * A view group that allows users to switch between multiple screens (layouts) in the same way as
 * the Android home screen (Launcher application).
 * <p>
 * You can add and remove views using the normal methods {@link ViewGroup#addView(View)},
 * {@link ViewGroup#removeView(View)} etc. You may want to listen for updates by calling
 * {@link HorizontalPager#setOnScreenSwitchListener(OnScreenSwitchListener)} in order to perform
 * operations once a new screen has been selected.
 *
 * Modifications from original version (ysamlan): Animate argument in setCurrentScreen and duration
 * in snapToScreen; onInterceptTouchEvent handling to support nesting a vertical Scrollview inside
 * the RealViewSwitcher; allowing snapping to a view even during an ongoing scroll; snap to
 * next/prev view on 25% scroll change; density-independent swipe sensitivity; width-independent
 * pager animation durations on scrolling to properly handle large screens without excessively
 * long animations.
 *
 * Other modifications:
 * (aveyD) Handle orientation changes properly and fully snap to the right position.
 *
 * @author Marc Reichelt, <a href="http://www.marcreichelt.de/">http://www.marcreichelt.de/</a>
 * @version 0.1.0
 */
public final class HorizontalPager extends AbstractPager {
    /**
     * Simple constructor to use when creating a view from code.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     */
    public HorizontalPager(final Context context) {
        super(context);
    }

    /**
     * Constructor that is called when inflating a view from XML. This is called
     * when a view is being constructed from an XML file, supplying attributes
     * that were specified in the XML file. This version uses a default style of
     * 0, so the only attribute values applied are those in the Context's Theme
     * and the given AttributeSet.
     *
     * <p>
     * The method onFinishInflate() will be called after all children have been
     * added.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @see #View(Context, AttributeSet, int)
     */
    public HorizontalPager(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    protected void onFirstLayout(int width, int height) {
    	scrollTo(mCurrentScreen * width, 0);
    }
    
    @Override
    protected void onLayoutWidthChanged() {
    	/*
		 * Recalculate the width and scroll to the right position to be sure we're in the right
		 * place in the event that we had a rotation that didn't result in an activity restart
		 * (code by aveyD). Without this you can end up between two pages after a rotation.
		 */
		Display display =
		        ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE))
		                .getDefaultDisplay();
		int displayWidth = display.getWidth();

		mNextScreen = Math.max(0, Math.min(getCurrentScreen(), getChildCount() - 1));
		final int newX = mNextScreen * displayWidth;
		final int delta = newX - getScrollX();

		mScroller.startScroll(getScrollX(), 0, delta, 0, 0);
    }
    
    @Override
	protected void onLayout(final boolean changed, final int l, final int t,
			final int r, final int b) {
			    int childLeft = 0;
			    final int count = getChildCount();
			
			    for (int i = 0; i < count; i++) {
			        final View child = getChildAt(i);
			        if (child.getVisibility() != View.GONE) {
			            final int childWidth = child.getMeasuredWidth();
			            child.layout(childLeft, 0, childLeft + childWidth, child.getMeasuredHeight());
			            childLeft += childWidth;
			        }
			    }
			}
    
    @Override
    protected boolean shouldInterceptMotionAction(int action) {
    	return mTouchState == TOUCH_STATE_HORIZONTAL_SCROLLING;
    }
    
    public void setCurrentScreen(final int currentScreen, final boolean animate) {
	    mCurrentScreen = Math.max(0, Math.min(currentScreen, getChildCount() - 1));
	    if (animate) {
	        snapToScreen(currentScreen, ANIMATION_SCREEN_SET_DURATION_MILLIS);
	    } else {
	        scrollTo(mCurrentScreen * getWidth(), 0);
	    }
	    invalidate();
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
		mLastMotionX = x;

		if (mScroller.isFinished()) {
		    mTouchState = TOUCH_STATE_REST;
		} else {
		    mTouchState = TOUCH_STATE_HORIZONTAL_SCROLLING;
		}
    }
    
    @Override
    protected void onTouchActionMove(float x, float y) {
    	final int xDiff = (int) Math.abs(x - mLastMotionX);
		boolean xMoved = xDiff > mTouchSlop;

		if (xMoved) {
		    // Scroll if the user moved far enough along the X axis
		    mTouchState = TOUCH_STATE_HORIZONTAL_SCROLLING;
		}

		if (mTouchState == TOUCH_STATE_HORIZONTAL_SCROLLING) {
		    // Scroll to follow the motion event
		    final int deltaX = (int) (mLastMotionX - x);
		    mLastMotionX = x;
		    final int scrollX = getScrollX();

		    if (deltaX < 0) {
		        if (scrollX > 0) {
		            scrollBy(Math.max(-scrollX, deltaX), 0);
		        }
		    } else if (deltaX > 0) {
		        final int availableToScroll =
		                getChildAt(getChildCount() - 1).getRight() - scrollX - getWidth();

		        if (availableToScroll > 0) {
		            scrollBy(Math.min(availableToScroll, deltaX), 0);
		        }
		    }
		}
    }
    
    @Override
    protected void snapToDestination() {
    	final int screenWidth = getWidth();
	    int scrollX = getScrollX();
	    int whichScreen = mCurrentScreen;
	    int deltaX = scrollX - (screenWidth * mCurrentScreen);
	
	    // Check if they want to go to the prev. screen
	    if ((deltaX < 0) && mCurrentScreen != 0
	            && ((screenWidth / FRACTION_OF_SCREEN_WIDTH_FOR_SWIPE) < -deltaX)) {
	        whichScreen--;
	        // Check if they want to go to the next screen
	    } else if ((deltaX > 0) && (mCurrentScreen + 1 != getChildCount())
	            && ((screenWidth / FRACTION_OF_SCREEN_WIDTH_FOR_SWIPE) < deltaX)) {
	        whichScreen++;
	    }
	
	    snapToScreen(whichScreen);
    }
    
    @Override
    protected void snapToScreen(int whichScreen, int duration) {
    	/*
	     * Modified by Yoni Samlan: Allow new snapping even during an ongoing scroll animation. This
	     * is intended to make HorizontalPager work as expected when used in conjunction with a
	     * RadioGroup used as "tabbed" controls. Also, make the animation take a percentage of our
	     * normal animation time, depending how far they've already scrolled.
	     */
	    mNextScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
	    final int newX = mNextScreen * getWidth();
	    final int delta = newX - getScrollX();
	
	    if (duration < 0) {
	         // E.g. if they've scrolled 80% of the way, only animation for 20% of the duration
	        mScroller.startScroll(getScrollX(), 0, delta, 0, (int) (Math.abs(delta)
	                / (float) getWidth() * ANIMATION_SCREEN_SET_DURATION_MILLIS));
	    } else {
	        mScroller.startScroll(getScrollX(), 0, delta, 0, duration);
	    }
	
	    invalidate();
    	
    }
    
}
