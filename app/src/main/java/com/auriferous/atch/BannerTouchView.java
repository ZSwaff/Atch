package com.auriferous.atch;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.auriferous.atch.Activities.MapActivity;
import com.auriferous.atch.Callbacks.VariableCallback;

public class BannerTouchView extends RelativeLayout {
    private InputMethodManager imm;
    private Window window;
    private int color;
    private ImageButton myLocButton = null;

    public int titleBarHeight = 75;
    public int shadowHeight = 5;
    private int windowHeight;
    public boolean isHeightInitialized = false;
    private float slop;

    private ViewGroup.MarginLayoutParams layoutParams;
    public boolean allTheWayUp = false;
    public boolean partiallyUp = false;

    private float lastY = 0;
    private int activePointerId = -1;
    private boolean panned = false;


    public BannerTouchView(Context context) {
        this(context, null, 0);
    }
    public BannerTouchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public BannerTouchView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        titleBarHeight = GeneralUtils.convertDpToPixel(titleBarHeight, context);
        shadowHeight = GeneralUtils.convertDpToPixel(shadowHeight, context);

        imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setHeight(int height) {
        windowHeight = height;
        isHeightInitialized = true;
    }
    public void setMyLocButton(ImageButton myLocButton){
        this.myLocButton = myLocButton;
    }

    public void setupBanner(VariableCallback<Integer> callback, Activity activity, int color){
        setupBanner(300, callback, activity, color);
    }
    public void setupBanner(int speed, VariableCallback<Integer> callback, Activity activity, int color){
        if(partiallyUp) return;
        partiallyUp = true;
        setVisibility(View.VISIBLE);

        window = activity.getWindow();
        this.color = color;

        layoutParams = (ViewGroup.MarginLayoutParams)getLayoutParams();
        layoutParams.setMargins(0, getBottomHeight() + titleBarHeight, 0, 0);
        animate(layoutParams.topMargin, getBottomHeight(), speed, callback);
    }
    public void putAllTheWayUp(){
        putAllTheWayUp(300);
    }
    public void putAllTheWayUp(int speed){
        animate(layoutParams.topMargin, -shadowHeight, speed, null);
    }
    public void takeAllTheWayDown(){
        animate(layoutParams.topMargin, getBottomHeight(), 300, null);
    }
    public void removeBanner(VariableCallback<Integer> callback){
        partiallyUp = false;
        animate(layoutParams.topMargin, getBottomHeight() + titleBarHeight, 300, callback);
    }

    private void animate(int start, int finish, int speed, final VariableCallback<Integer> callback) {
        ValueAnimator animator = ValueAnimator.ofInt(start, finish);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int paddingAmount = (Integer) valueAnimator.getAnimatedValue();

                if (paddingAmount == getBottomHeight())
                    allTheWayUp = false;
                if (paddingAmount == getBottomHeight() + titleBarHeight && !partiallyUp)
                    setVisibility(View.GONE);
                if (paddingAmount == -shadowHeight)
                    allTheWayUp = true;
                layoutParams.topMargin = paddingAmount;

                layoutParams.bottomMargin = -layoutParams.topMargin - shadowHeight;

                setLayoutParams(layoutParams);
                invalidate();

                if (myLocButton != null && layoutParams.topMargin < getBottomHeight())
                    myLocButton.setAlpha(1f - ((float) (getBottomHeight() - layoutParams.topMargin)) / ((float) getBottomHeight()));

                updateWindow(layoutParams.topMargin);

                if (callback != null)
                    callback.done(windowHeight - paddingAmount);
            }
        });
        if(speed < 0)
            animator.setDuration(-speed);
        else
            animator.setDuration(Math.abs((speed*Math.abs(finish-start))/getBottomHeight()));
        animator.start();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        imm.hideSoftInputFromWindow(getWindowToken(), 0);
        layoutParams = (ViewGroup.MarginLayoutParams) getLayoutParams();

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                panned = false;
                lastY = event.getY();

                activePointerId = event.getPointerId(0);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = event.findPointerIndex(activePointerId);
                final float currentY = event.getY(pointerIndex);
                final float dy = currentY - lastY;

                layoutParams.topMargin += dy;
                if(layoutParams.topMargin <= -shadowHeight) {
                    layoutParams.topMargin = -shadowHeight;
                    allTheWayUp = true;
                }
                if(layoutParams.topMargin >= getBottomHeight()) {
                    layoutParams.topMargin = getBottomHeight();
                    allTheWayUp = false;
                }

                layoutParams.bottomMargin = - layoutParams.topMargin - shadowHeight;

                setLayoutParams(layoutParams);
                invalidate();

                if(myLocButton != null && layoutParams.topMargin < getBottomHeight())
                    myLocButton.setAlpha(1f - ((float)(getBottomHeight()-layoutParams.topMargin))/((float)getBottomHeight()));

                updateWindow(layoutParams.topMargin);

                if(!allTheWayUp && Math.abs(layoutParams.topMargin - getBottomHeight()) > slop)
                    panned = true;
                if(allTheWayUp && layoutParams.topMargin > slop)
                    panned = true;

                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                activePointerId = -1;

                if(layoutParams.topMargin == getBottomHeight() && panned) return true;
                if(layoutParams.topMargin == -shadowHeight && panned) return true;

                if(!allTheWayUp)
                    animate(layoutParams.topMargin, -shadowHeight, 300, null);
                else
                    animate(layoutParams.topMargin, getBottomHeight(), 300, null);
            }
            case MotionEvent.ACTION_POINTER_UP:
            {
                final int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = event.getPointerId(pointerIndex);

                if(pointerId == activePointerId) {
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    lastY = event.getY(newPointerIndex);
                    activePointerId = event.getPointerId(newPointerIndex);
                }
                break;
            }
        }
        return true;
    }

    private void updateWindow(int topMargin){
        if(Build.VERSION.SDK_INT < 21) return;

        //todo revisit
//        if(topMargin == -shadowHeight) {
//            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//            window.setStatusBarColor(color);
//        }
//        else {
//            window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//        }
    }

    public int getBottomHeight() {
        return (windowHeight - titleBarHeight);
    }
}