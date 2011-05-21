package com.github.ysamlan.horizontalpager;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Scroller;

public abstract class AbstractPager extends ViewGroup {

	protected static final int ANIMATION_SCREEN_SET_DURATION_MILLIS = 500;
	protected static final int FRACTION_OF_SCREEN_WIDTH_FOR_SWIPE = 4;
	protected static final int INVALID_SCREEN = -1;
	protected static final int SNAP_VELOCITY_DIP_PER_SECOND = 600;
	protected static final int VELOCITY_UNIT_PIXELS_PER_SECOND = 1000;
	protected static final int TOUCH_STATE_REST = 0;
	protected static final int TOUCH_STATE_HORIZONTAL_SCROLLING = 1;
	protected static final int TOUCH_STATE_VERTICAL_SCROLLING = -1;
	protected int mCurrentScreen;
	protected int mDensityAdjustedSnapVelocity;
	protected boolean mFirstLayout = true;
	protected float mLastMotionX;
	protected float mLastMotionY;
	protected OnScreenSwitchListener mOnScreenSwitchListener;
	protected int mMaximumVelocity;
	protected int mNextScreen = INVALID_SCREEN;
	protected Scroller mScroller;
	protected int mTouchSlop;
	protected int mTouchState = TOUCH_STATE_REST;
	protected VelocityTracker mVelocityTracker;
	protected int mLastSeenLayoutWidth = -1;
	protected int mLastSeenLayoutHeight = -1;

	/**
	 * Listener for the event that the HorizontalPager switches to a new view.
	 */
	public static interface OnScreenSwitchListener {
	    /**
	     * Notifies listeners about the new screen. Runs after the animation completed.
	     *
	     * @param screen The new screen index.
	     */
	    void onScreenSwitched(int screen);
	}

	public AbstractPager(Context context) {
		super(context);
		init();
	}

	public AbstractPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	/**
	 * Sets up the scroller and touch/fling sensitivity parameters for the pager.
	 */
	protected void init() {
	    mScroller = new Scroller(getContext());
	
	    // Calculate the density-dependent snap velocity in pixels
	    DisplayMetrics displayMetrics = new DisplayMetrics();
	    ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
	            .getMetrics(displayMetrics);
	    mDensityAdjustedSnapVelocity =
	            (int) (displayMetrics.density * SNAP_VELOCITY_DIP_PER_SECOND);
	
	    final ViewConfiguration configuration = ViewConfiguration.get(getContext());
	    mTouchSlop = configuration.getScaledTouchSlop();
	    mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
	}
	
	protected abstract void onFirstLayout(int width, int height);
	
	@Override
	public void computeScroll() {
	    if (mScroller.computeScrollOffset()) {
	        scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
	        postInvalidate();
	    } else if (mNextScreen != INVALID_SCREEN) {
	        mCurrentScreen = Math.max(0, Math.min(mNextScreen, getChildCount() - 1));
	
	        // Notify observer about screen change
	        if (mOnScreenSwitchListener != null) {
	            mOnScreenSwitchListener.onScreenSwitched(mCurrentScreen);
	        }
	
	        mNextScreen = INVALID_SCREEN;
	    }
	}

	@Override
	protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
	    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	
	    final int width = MeasureSpec.getSize(widthMeasureSpec);
	    final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
	    if (widthMode != MeasureSpec.EXACTLY) {
	        throw new IllegalStateException("ViewSwitcher can only be used in EXACTLY mode.");
	    }
	
	    final int height = MeasureSpec.getSize(heightMeasureSpec);
	    final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
	    if (heightMode != MeasureSpec.EXACTLY) {
	        throw new IllegalStateException("ViewSwitcher can only be used in EXACTLY mode.");
	    }
	
	    // The children are given the same width and height as the workspace
	    final int count = getChildCount();
	    for (int i = 0; i < count; i++) {
	        getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
	    }
	
	    if (mFirstLayout) {
	    	onFirstLayout(width, height);
	        mFirstLayout = false;
	    }
	
	    else if (width != mLastSeenLayoutWidth) { // Width has changed
	        onLayoutWidthChanged();
	    }
	    
	    else if (height != mLastSeenLayoutHeight) {
	    	onLayoutHeightChanged();
	    }
	
	    mLastSeenLayoutWidth   = width;
	    mLastSeenLayoutHeight  = height;
	}
	
	@Override
	public boolean onInterceptTouchEvent(final MotionEvent ev) {
	    /*
	     * By Yoni Samlan: Modified onInterceptTouchEvent based on standard ScrollView's
	     * onIntercept. The logic is designed to support a nested vertically scrolling view inside
	     * this one; once a scroll registers for X-wise scrolling, handle it in this view and don't
	     * let the children, but once a scroll registers for y-wise scrolling, let the children
	     * handle it exclusively.
	     */
	    final int action = ev.getAction();
	    boolean intercept = false;
	
	    switch (action) {
	        case MotionEvent.ACTION_MOVE:
	        	
	            /*
	             * If we're in a horizontal scroll event, take it (intercept further events). But if
	             * we're mid-vertical-scroll, don't even try; let the children deal with it. If we
	             * haven't found a scroll event yet, check for one.
	             */
	            if (mTouchState == TOUCH_STATE_HORIZONTAL_SCROLLING ||
	            		mTouchState == TOUCH_STATE_VERTICAL_SCROLLING) {
	                /*
	                 * We've already started a horizontal scroll; set intercept to true so we can
	                 * take the remainder of all touch events in onTouchEvent.
	                 */
	            	intercept = shouldInterceptMotionAction(action);
	            } else { // We haven't picked up a scroll event yet; check for one.
	
	                /*
	                 * If we detected a horizontal scroll event, start stealing touch events (mark
	                 * as scrolling). Otherwise, see if we had a vertical scroll event -- if so, let
	                 * the children handle it and don't look to intercept again until the motion is
	                 * done.
	                 */
	
	                final float x = ev.getX();
	                final int xDiff = (int) Math.abs(x - mLastMotionX);
	                boolean xMoved = xDiff > mTouchSlop;
	                final float y = ev.getY();
	                final int yDiff = (int) Math.abs(y - mLastMotionY);
	                boolean yMoved = yDiff > mTouchSlop;
	
	                if (xMoved) {
	                    // Scroll if the user moved far enough along the X axis
	                    mTouchState = TOUCH_STATE_HORIZONTAL_SCROLLING;
	                    mLastMotionX = x;
	                }                
	
	                if (yMoved) {
	                    mTouchState = TOUCH_STATE_VERTICAL_SCROLLING;
	                    mLastMotionY = y;
	                }
	            }
	
	            break;
	        case MotionEvent.ACTION_CANCEL:
	        case MotionEvent.ACTION_UP:
	            // Release the drag.
	            mTouchState = TOUCH_STATE_REST;
	            break;
	        case MotionEvent.ACTION_DOWN:
	            /*
	             * No motion yet, but register the coordinates so we can check for intercept at the
	             * next MOVE event.
	             */
	            mLastMotionY = ev.getY();
	            mLastMotionX = ev.getX();
	            break;
	        default:
	            break;
	        }
	
	    return intercept;
	}
	
	protected abstract boolean shouldInterceptMotionAction(int action);

	protected void onLayoutWidthChanged() {
	}
	
	protected void onLayoutHeightChanged() {
	}
	
	@Override
	public boolean onTouchEvent(final MotionEvent ev) {
	
	    if (mVelocityTracker == null) {
	        mVelocityTracker = VelocityTracker.obtain();
	    }
	    mVelocityTracker.addMovement(ev);
	
	    final int action = ev.getAction();
	    final float x = ev.getX();
	    final float y = ev.getY();
	    
	    switch (action) {
	        case MotionEvent.ACTION_DOWN:
	        	onTouchActionDown(x, y);
	            break;
	        case MotionEvent.ACTION_MOVE:
	        	onTouchActionMove(x, y);
	            break;
	        case MotionEvent.ACTION_UP:
	        	onTouchActionUp();
	            break;
	        case MotionEvent.ACTION_CANCEL:
	            mTouchState = TOUCH_STATE_REST;
	            break;
	        default:
	            break;
	    }
	
	    return true;
	}

	protected void onTouchActionUp() {
		if (mTouchState == TOUCH_STATE_HORIZONTAL_SCROLLING) {
		    final VelocityTracker velocityTracker = mVelocityTracker;
		    velocityTracker.computeCurrentVelocity(VELOCITY_UNIT_PIXELS_PER_SECOND,
		            mMaximumVelocity);
		    int velocityX = (int) velocityTracker.getXVelocity();

		    if (velocityX > mDensityAdjustedSnapVelocity && mCurrentScreen > 0) {
		        // Fling hard enough to move left
		        snapToScreen(mCurrentScreen - 1);
		    } else if (velocityX < -mDensityAdjustedSnapVelocity
		            && mCurrentScreen < getChildCount() - 1) {
		        // Fling hard enough to move right
		        snapToScreen(mCurrentScreen + 1);
		    } else {
		        snapToDestination();
		    }

		    if (mVelocityTracker != null) {
		        mVelocityTracker.recycle();
		        mVelocityTracker = null;
		    }
		} else if (mTouchState == TOUCH_STATE_VERTICAL_SCROLLING) {
			final VelocityTracker velocityTracker = mVelocityTracker;
            velocityTracker.computeCurrentVelocity(VELOCITY_UNIT_PIXELS_PER_SECOND,
                    mMaximumVelocity);
            int velocityY = (int) velocityTracker.getYVelocity();

            if (velocityY > mDensityAdjustedSnapVelocity && mCurrentScreen > 0) {
                // Fling hard enough to move left
                snapToScreen(mCurrentScreen - 1);
            } else if (velocityY < -mDensityAdjustedSnapVelocity
                    && mCurrentScreen < getChildCount() - 1) {
                // Fling hard enough to move right
                snapToScreen(mCurrentScreen + 1);
            } else {
                snapToDestination();
            }

            if (mVelocityTracker != null) {
                mVelocityTracker.recycle();
                mVelocityTracker = null;
            }
		}

		mTouchState = TOUCH_STATE_REST;
	}

	protected abstract void onTouchActionMove(final float x, final float y);

	protected abstract void onTouchActionDown(final float x, final float y);
	

	/**
	 * Returns the index of the currently displayed screen.
	 *
	 * @return The index of the currently displayed screen.
	 */
	public int getCurrentScreen() {
	    return mCurrentScreen;
	}

	/**
	 * Sets the current screen.
	 *
	 * @param currentScreen The new screen.
	 * @param animate True to smoothly scroll to the screen, false to snap instantly
	 */
	public abstract void setCurrentScreen(final int currentScreen, final boolean animate);

	/**
	 * Sets the {@link OnScreenSwitchListener}.
	 *
	 * @param onScreenSwitchListener The listener for switch events.
	 */
	public void setOnScreenSwitchListener(final OnScreenSwitchListener onScreenSwitchListener) {
	    mOnScreenSwitchListener = onScreenSwitchListener;
	}

	/**
	 * Snaps to the screen we think the user wants (the current screen for very small movements; the
	 * next/prev screen for bigger movements).
	 */
	protected abstract void snapToDestination();

	/**
	 * Snap to a specific screen, animating automatically for a duration proportional to the
	 * distance left to scroll.
	 *
	 * @param whichScreen Screen to snap to
	 */
	protected void snapToScreen(final int whichScreen) {
	    snapToScreen(whichScreen, -1);
	}

	/**
	 * Snaps to a specific screen, animating for a specific amount of time to get there.
	 *
	 * @param whichScreen Screen to snap to
	 * @param duration -1 to automatically time it based on scroll distance; a positive number to
	 *            make the scroll take an exact duration.
	 */
	protected abstract void snapToScreen(final int whichScreen, final int duration);

}