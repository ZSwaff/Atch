package com.auriferous.atch;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.RelativeLayout;

import com.auriferous.atch.Callbacks.SimpleCallback;

public class InAppNotificationView extends RelativeLayout {
    public int barHeight = 90;
    public int upperMargin = 0;
    private float slop;

    private MarginLayoutParams layoutParams;

    private float lastY = 0;
    private int activePointerId = -1;
    private boolean panned = false;

    private SimpleCallback onClickCallback;
    private SimpleCallback destroyCallback;
    private Handler rescindViewHandler;
    private Runnable rescindView;


    public InAppNotificationView(Context context) {
        this(context, null, 0);
    }
    public InAppNotificationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public InAppNotificationView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        rescindViewHandler = new Handler();
        rescindView = new Runnable() {
            @Override
            public void run() {
                returnUp();
            }
        };

        barHeight = GeneralUtils.convertDpToPixel(barHeight, context);
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setCallbacks(SimpleCallback onClickCallback, SimpleCallback destroyCallback){
        this.onClickCallback = onClickCallback;
        this.destroyCallback = destroyCallback;

        layoutParams = (MarginLayoutParams)getLayoutParams();
        layoutParams.setMargins(0, -barHeight, 0, 0);
        requestLayout();
    }
    public void setUpperMargin(int upperMargin){
        this.upperMargin = upperMargin;
    }

    public void deployDown(){
        animate(barHeight, -upperMargin, true);
        rescindViewHandler.postDelayed(rescindView, 3500);
    }
    public void returnUp(){
        animate(layoutParams.bottomMargin, barHeight, false);
    }

    private void animate(int start, int finish, boolean deploying) {
        ValueAnimator animator = ValueAnimator.ofInt(start, finish);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int paddingAmount = (Integer) valueAnimator.getAnimatedValue();

                layoutParams.bottomMargin = paddingAmount;
                layoutParams.topMargin = -layoutParams.bottomMargin;

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
        animator.setDuration(Math.abs((500*Math.abs(finish-start))/(barHeight+upperMargin)));
        animator.start();

        if(!deploying){
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {}
                @Override
                public void onAnimationEnd(Animator animation) {
                    destroyCallback.done();
                }
                @Override
                public void onAnimationCancel(Animator animation) {}
                @Override
                public void onAnimationRepeat(Animator animation) {}
            });
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        layoutParams = (MarginLayoutParams) getLayoutParams();

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                rescindViewHandler.removeCallbacks(rescindView);

                lastY = event.getY();
                panned = false;

                activePointerId = event.getPointerId(0);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = event.findPointerIndex(activePointerId);
                final float currentY = event.getY(pointerIndex);
                final float dy = currentY - lastY;
                layoutParams.bottomMargin = -upperMargin - (int)dy;

                if(layoutParams.bottomMargin < -upperMargin)
                    layoutParams.bottomMargin = -upperMargin;
                if(layoutParams.bottomMargin > barHeight - upperMargin)
                    layoutParams.bottomMargin = barHeight - upperMargin;

                layoutParams.topMargin = -layoutParams.bottomMargin;
                setLayoutParams(layoutParams);
                invalidate();

                if(Math.abs(layoutParams.bottomMargin + upperMargin) > slop)
                    panned = true;

                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                activePointerId = -1;

                if(Math.abs(layoutParams.bottomMargin + upperMargin) < slop) {
                    if(panned)
                        deployDown();
                    else {
                        returnUp();
                        onClickCallback.done();
                    }
                }
                else
                    returnUp();
            }
            case MotionEvent.ACTION_POINTER_UP: {
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
}