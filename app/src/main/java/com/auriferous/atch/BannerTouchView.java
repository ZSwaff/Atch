package com.auriferous.atch;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.auriferous.atch.Callbacks.FuncCallback;

public class BannerTouchView extends RelativeLayout {
    private int titleBarHeight;
    private int windowHeight;

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

        slop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setupBanner(){
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        windowHeight = size.y;

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)getLayoutParams();
        params.setMargins(0, getBottomHeight(), 0, 0);
        requestLayout();

        titleBarHeight = findViewById(R.id.title_bar).getHeight();
    }

    public void takeDown(){
        ValueAnimator animator = ValueAnimator.ofInt(0, getBottomHeight());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int paddingAmount = (Integer) valueAnimator.getAnimatedValue();
                if (paddingAmount == 0) {
                    allTheWayUp = false;
                    setVisibility(GONE);
                }
                layoutParams.topMargin = paddingAmount;
                setLayoutParams(layoutParams);
                invalidate();
            }
        });
        animator.setDuration(300);
        animator.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(allTheWayUp) return true;

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
                if(layoutParams.topMargin > getBottomHeight())
                    layoutParams.topMargin = getBottomHeight();

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

                ValueAnimator animator = ValueAnimator.ofInt(((MarginLayoutParams) getLayoutParams()).topMargin, 0);
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        int paddingAmount = (Integer) valueAnimator.getAnimatedValue();
                        if(paddingAmount == 0)
                            allTheWayUp = true;
                        layoutParams.topMargin = paddingAmount;
                        setLayoutParams(layoutParams);
                        invalidate();
                    }
                });
                animator.setDuration(300);
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
}