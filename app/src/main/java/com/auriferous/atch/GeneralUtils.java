package com.auriferous.atch;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

public class GeneralUtils {
    public static int convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return (int)px;
    }

    public static int generateNewColor(){
        Random rnd = new Random();
        while(true){
            int r = rnd.nextInt(256), g = rnd.nextInt(256), b = rnd.nextInt(256);
            if (Math.abs(r-g) < 20 && b < 50) continue;
            if (Math.abs(r - g) < 20 && Math.abs(r-b) < 20) continue;
            if (r + g + b < 100) continue;
            if (r + g + b > 550) continue;
            if (r + g  > 430 && b < 120) continue;
            return Color.argb(255, r, g, b);
        }
    }
    public static int getLighter(int oldColor) {
        float[] hsv = new float[3];
        Color.colorToHSV(oldColor, hsv);
        hsv[1] = hsv[1]/2f;
        hsv[2] = 1f - ((1f - hsv[2])/4f);
        return Color.HSVToColor(hsv);
    }

    public static Bitmap reColorImage(Bitmap image, int newColor) {
        int width = image.getWidth();
        int height = image.getHeight();

        Bitmap newImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newImage);

        Paint paint = new Paint();
        ColorFilter filter = new LightingColorFilter(newColor, 0);
        paint.setColorFilter(filter);
        canvas.drawBitmap(image, 0, 0, paint);

        return newImage;
    }

    public static Bitmap layerImagesRecolorForeground(Bitmap foreground, Bitmap background, int newColor) {
        int width = background.getWidth();
        int height = background.getHeight();
        if(width != foreground.getWidth() || height != foreground.getHeight()) return null;

        Bitmap newImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newImage);
        canvas.drawBitmap(background, 0, 0, null);

        Paint paint = new Paint();
        ColorFilter filter = new LightingColorFilter(newColor, 0);
        paint.setColorFilter(filter);
        canvas.drawBitmap(foreground, 0, 0, paint);

        return newImage;
    }

    public static void addButtonEffect(View button){
        button.setOnTouchListener(new View.OnTouchListener() {
            boolean isOver = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        isOver = true;
                        v.getBackground().setColorFilter(0x33333333, PorterDuff.Mode.SRC_ATOP);
                        v.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        int x = (int)event.getX();
                        int y = (int)event.getY();
                        if(isOver && (x < 0 || y < 0 || x > v.getWidth() || y > v.getHeight())){
                            isOver = false;
                            v.getBackground().clearColorFilter();
                            v.invalidate();
                        }
                        break;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL: {
                        isOver = false;
                        v.getBackground().clearColorFilter();
                        v.invalidate();
                        break;
                    }
                }
                return false;
            }
        });
    }
}
