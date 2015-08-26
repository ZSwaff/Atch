package com.auriferous.atch;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;

import com.auriferous.atch.Callbacks.FuncCallback;

public class BannerTouchView extends RelativeLayout {
    public int titleBarHeight = 70;
    private int windowHeight;

    InputMethodManager imm;
    ViewGroup.MarginLayoutParams layoutParams;

    private float lastY = 0;
    private int activePointerId = -1;

    private float slop;
    private boolean panned = false;

    private boolean allTheWayUp = false;


    public BannerTouchView(Context context) {
        this(context, null, 0);
    }
    public BannerTouchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public BannerTouchView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        titleBarHeight = (int)convertDpToPixel(titleBarHeight, context);

        slop = ViewConfiguration.get(context).getScaledTouchSlop();
        imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }


    public void setupBanner(int height){
        setVisibility(View.VISIBLE);

        windowHeight = height;

        layoutParams = (ViewGroup.MarginLayoutParams)getLayoutParams();
        layoutParams.setMargins(0, getBottomHeight(), 0, 0);
        requestLayout();
    }
    public void takeAllTheWayDown(){
        ValueAnimator animator = ValueAnimator.ofInt(layoutParams.topMargin, getBottomHeight());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int paddingAmount = (Integer) valueAnimator.getAnimatedValue();
                if (paddingAmount == getBottomHeight()) {
                    allTheWayUp = false;
                    setVisibility(GONE);
                }
                layoutParams.topMargin = paddingAmount;
                if(getBottomHeight() - layoutParams.topMargin < 400)
                    layoutParams.bottomMargin = (getBottomHeight() - layoutParams.topMargin) - 400;
                else
                    layoutParams.bottomMargin = 0;
                setLayoutParams(layoutParams);
                invalidate();
            }
        });
        animator.setDuration(200);
        animator.start();
    }
    public void putAllTheWayUp(){
        ValueAnimator animator = ValueAnimator.ofInt(layoutParams.topMargin, 0);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int paddingAmount = (Integer) valueAnimator.getAnimatedValue();
                if(paddingAmount == 0)
                    allTheWayUp = true;
                layoutParams.topMargin = paddingAmount;
                if(getBottomHeight() - layoutParams.topMargin < 400)
                    layoutParams.bottomMargin = (getBottomHeight() - layoutParams.topMargin) - 400;
                else
                    layoutParams.bottomMargin = 0;
                setLayoutParams(layoutParams);
                invalidate();
            }
        });
        animator.setDuration(200);
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
                if(layoutParams.topMargin <= 0) {
                    layoutParams.topMargin = 0;
                    allTheWayUp = true;
                }
                if(layoutParams.topMargin >= getBottomHeight()) {
                    layoutParams.topMargin = getBottomHeight();
                    allTheWayUp = false;
                }
                if(getBottomHeight() - layoutParams.topMargin < 400)
                    layoutParams.bottomMargin = (getBottomHeight() - layoutParams.topMargin) - 400;
                else
                    layoutParams.bottomMargin = 0;

                setLayoutParams(layoutParams);
                invalidate();

                if(Math.abs(layoutParams.topMargin - getBottomHeight()) > slop)
                    panned = true;

                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                activePointerId = -1;

                if(layoutParams.topMargin == getBottomHeight() && panned) return true;
                if(layoutParams.topMargin == 0 && panned) return true;

                ValueAnimator animator = ValueAnimator.ofInt(((MarginLayoutParams) getLayoutParams()).topMargin, 0);
                if(allTheWayUp)
                    animator = ValueAnimator.ofInt(((MarginLayoutParams) getLayoutParams()).topMargin, getBottomHeight());

                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        int paddingAmount = (Integer) valueAnimator.getAnimatedValue();
                        if(paddingAmount == 0)
                            allTheWayUp = true;
                        if(paddingAmount == getBottomHeight())
                            allTheWayUp = false;
                        layoutParams.topMargin = paddingAmount;
                        if(getBottomHeight() - layoutParams.topMargin < 400)
                            layoutParams.bottomMargin = (getBottomHeight() - layoutParams.topMargin) - 400;
                        else
                            layoutParams.bottomMargin = 0;

                        setLayoutParams(layoutParams);
                        invalidate();
                    }
                });
                animator.setDuration(200);
                animator.start();

                break;
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

    public int getBottomHeight() {
        return (windowHeight - titleBarHeight);
    }

    public float convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }
}