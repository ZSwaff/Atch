package com.auriferous.atch;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;

public class InAppNotificationView extends RelativeLayout {
    private InputMethodManager imm;

    public int barHeight = 70;
    private float slop;

    private MarginLayoutParams layoutParams;
    public boolean down = false;

    private float lastY = 0, lastX;
    private int activePointerId = -1;
    boolean panned = false;


    public InAppNotificationView(Context context) {
        this(context, null, 0);
    }
    public InAppNotificationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public InAppNotificationView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        barHeight = GeneralUtils.convertDpToPixel(barHeight, context);

        imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
    }


    public void setupBanner(){
        setVisibility(View.VISIBLE);

        layoutParams = (MarginLayoutParams)getLayoutParams();
        layoutParams.setMargins(0, getBottomHeight(), 0, 0);
        requestLayout();
    }
    public void takeAllTheWayDown(){
        animate(layoutParams.topMargin, getBottomHeight());
    }
    public void putAllTheWayUp(){
        animate(layoutParams.topMargin, 0);
    }

    private void animate(int start, int finish) {
        ValueAnimator animator = ValueAnimator.ofInt(start, finish);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int paddingAmount = (Integer) valueAnimator.getAnimatedValue();

                if (paddingAmount == getBottomHeight())
                    down = false;
                if (paddingAmount == 0)
                    down = true;
                layoutParams.topMargin = paddingAmount;

                layoutParams.bottomMargin = - layoutParams.topMargin;

                setLayoutParams(layoutParams);
                invalidate();
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animator.setDuration(300);
        animator.start();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        imm.hideSoftInputFromWindow(getWindowToken(), 0);
        layoutParams = (MarginLayoutParams) getLayoutParams();

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                lastX = event.getX();
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
                    down = true;
                }
                if(layoutParams.topMargin >= getBottomHeight()) {
                    layoutParams.topMargin = getBottomHeight();
                    down = false;
                }

                layoutParams.bottomMargin = - layoutParams.topMargin;

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

                if(!down)
                    animate(layoutParams.topMargin, 0);
                else
                    animate(layoutParams.topMargin, getBottomHeight());
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
        return 5;
    }
}