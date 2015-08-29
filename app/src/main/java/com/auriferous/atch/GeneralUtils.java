package com.auriferous.atch;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.util.Log;

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
}
