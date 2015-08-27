package com.auriferous.atch.Users;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.AsyncTask;

import com.auriferous.atch.AtchApplication;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;

public class User {
    private static AtchApplication app;
    private static HashMap<String, User> userMap = new HashMap<>();


    private ParseUser user = null;
    private UserType userType = UserType.RANDOM;

    private Bitmap profPic = null;
    private Bitmap markerIcon = null;

    private ParseObject privateData = null;
    private MarkerOptions marker = null;
    private boolean loggedIn = false;


    public static void init(AtchApplication app){
        User.app = app;
    }

    public static User getUserFromMap(String parseId){
        if (!userMap.containsKey(parseId)) return null;
        return userMap.get(parseId);
    }


    public static User getOrCreateUser(ParseUser parseUser, UserType userType){
        User oldUser = getUserFromMap(parseUser.getObjectId());

        if(oldUser != null) {
            if(userType == UserType.FRIEND) {
                oldUser.setUserType(userType);
            } else if(userType == UserType.PENDING_YOU) {
                if(oldUser.getUserType() == UserType.PENDING_THEM){
                    oldUser.setUserType(UserType.FRIEND);
                } else if(oldUser.getUserType() != UserType.FRIEND){
                    oldUser.setUserType(userType);
                }
            } else if(userType == UserType.PENDING_THEM) {
                if(oldUser.getUserType() == UserType.PENDING_YOU){
                    oldUser.setUserType(UserType.FRIEND);
                } else if(oldUser.getUserType() != UserType.FRIEND){
                    oldUser.setUserType(userType);
                }
            } else if(userType == UserType.FACEBOOK_FRIEND) {
                if(oldUser.getUserType() == UserType.RANDOM) {
                    oldUser.setUserType(userType);
                }
            }

            return oldUser;
        }

        return new User(parseUser, userType);
    }
    private User(ParseUser parseUser, UserType userType){
        user = parseUser;
        this.userType = userType;
        setFacebookProfilePicture();

        userMap.put(user.getObjectId(), this);
    }


    public void setPrivateData(ParseObject privateData){
        this.privateData = privateData;
        updateOnlineStatus();
        createMarker();
    }
    private void updateOnlineStatus() {
        if (privateData == null) return;
        Date udAt = privateData.getUpdatedAt();
        if(udAt == null) return;
        Date udAtPlus5 = new Date(udAt.getTime() + (5 * 60 * 1000));
        loggedIn = udAtPlus5.after(new Date());
    }
    private void createMarker() {
        marker = null;
        if (privateData == null) return;
        LatLng loc = getLocation();
        if (loc == null) return;

        //todo change default marker
        if(profPic == null)
            marker = new MarkerOptions().position(loc)
                    .snippet(user.getObjectId());
        else
            marker = new MarkerOptions().position(loc)
                    .snippet(user.getObjectId())
                    .icon(BitmapDescriptorFactory.fromBitmap(markerIcon))
                    .anchor(.5f, .5f);

        app.updateView();
    }

    private void setFacebookProfilePicture(){
        final String fbid = user.getString("fbid");

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    URL imageURL = new URL("https://graph.facebook.com/" + fbid + "/picture?type=large");
                    profPic = getCircular(BitmapFactory.decodeStream(imageURL.openConnection().getInputStream()));
                    setMarkerIconBitmap();
                } catch (MalformedURLException mUE) {
                } catch (IOException iOE) {
                }
                return null;
            }
            @Override
            protected void onPostExecute(Void voids) {
                createMarker();
                if (app != null)
                    app.updateView();
            }
        };
        task.execute();
    }
    private void setMarkerIconBitmap(){
        //todo different size for different screens?
        markerIcon = Bitmap.createScaledBitmap(profPic, 170, 170, false);
    }
    private static Bitmap getCircular(Bitmap bm) {
        int radius = bm.getWidth();

        Bitmap bmOut = Bitmap.createBitmap(radius, radius, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmOut);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(0xff424242);

        Rect rect = new Rect(0, 0, radius, radius);
        RectF rectF = new RectF(rect);

        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(rectF.left + (rectF.width()/2), rectF.top + (rectF.height()/2), radius / 2, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bm, rect, rect, paint);

        return bmOut;
    }


    public ParseUser getUser() {
        return user;
    }
    public String getId(){
        return user.getObjectId();
    }
    public String getFullname(){
        return user.getString("fullname");
    }
    public String getUsername(){
        return user.getUsername();
    }

    public Bitmap getProfPic() {
        return profPic;
    }
    public MarkerOptions getMarker() {
        return marker;
    }
    public LatLng getLocation() {
        ParseGeoPoint loc = privateData.getParseGeoPoint("location");
        if (loc == null) return null;
        return new LatLng(loc.getLatitude(), loc.getLongitude());
    }

    public UserType getUserType() {
        return userType;
    }


    public void setUserType(UserType userType) {
        this.userType = userType;
        app.updateView();
    }


    public enum UserType {
        FRIEND, PENDING_YOU, PENDING_THEM, FACEBOOK_FRIEND, RANDOM;
    }
}
