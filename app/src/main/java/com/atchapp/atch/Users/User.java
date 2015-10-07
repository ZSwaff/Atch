package com.atchapp.atch.Users;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Location;
import android.os.AsyncTask;

import com.atchapp.atch.AtchApplication;
import com.atchapp.atch.GeneralUtils;
import com.atchapp.atch.R;
import com.google.android.gms.maps.model.LatLng;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Stack;

public class User implements Comparable<User> {
    private static AtchApplication app;
    private static HashMap<String, User> userCache = new HashMap<>();
    private static UserInfoSaveable infoGroup = null;


    private ParseUser user = null;
    private UserType userType = UserType.RANDOM;

    private int relativeColor = Color.argb(256, 0, 0, 0);
    private Stack<Integer> oldColors = new Stack<>();
    private int lighterColor = Color.argb(256, 0, 0, 0);
    private Bitmap rawFbPic = null;
    private Bitmap profPic = null;
    private Bitmap markerIcon = null;
    private Bitmap chatIcon = null;

    private ParseObject privateData = null;
    private boolean loggedIn = false;


    private User(ParseUser parseUser, UserType userType) {
        user = parseUser;
        this.userType = userType;

        userCache.put(user.getObjectId(), this);

        int color = infoGroup.getColor(user.getObjectId());
        if (color != -1)
            relativeColor = color;
        else {
            relativeColor = GeneralUtils.generateNewColor();
            UserInfoSaveable.autoSave(app, User.getUserCache());
        }

        lighterColor = GeneralUtils.getLighter(relativeColor);

        setChatIcon();
        setFacebookProfilePicture();
    }
    public static void init(AtchApplication app, UserInfoSaveable infoGroup){
        User.app = app;
        User.infoGroup = infoGroup;
    }
    public static User getUserFromCache(String parseId) {
        if (!userCache.containsKey(parseId)) return null;
        return userCache.get(parseId);
    }
    public static User getOrCreateUser(ParseUser parseUser, UserType userType){
        User oldUser = getUserFromCache(parseUser.getObjectId());

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
            app.updateView();

            return oldUser;
        }

        return new User(parseUser, userType);
    }
    public static HashMap<String, User> getUserCache() {
        return userCache;
    }
    public static void resetCache() {
        userCache = new HashMap<>();
        infoGroup = UserInfoSaveable.autoLoad(app);
    }
    public static void resetAllCachedTypes() {
        for (User user : userCache.values())
            user.setUserType(UserType.RANDOM);
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
        canvas.drawCircle(rectF.left + (rectF.width() / 2), rectF.top + (rectF.height() / 2), radius / 2, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bm, rect, rect, paint);

        return bmOut;
    }
    private static Bitmap getCircularWithColor(Bitmap bm, int color) {
        int borderWidth = 14;
        float radius = bm.getWidth();

        Bitmap bmOut = Bitmap.createBitmap((int) radius + 2 * borderWidth, (int) radius + 2 * borderWidth, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmOut);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawCircle(radius / 2 + borderWidth, radius / 2 + borderWidth, radius / 2 + borderWidth, paint);

        paint = new Paint();
        paint.setAntiAlias(true);
        canvas.drawBitmap(getCircular(bm), borderWidth, borderWidth, null);

        return bmOut;
    }

    public void setPrivateData(ParseObject privateData){
        this.privateData = privateData;
        updateOnlineStatus();
    }
    private void updateOnlineStatus() {
        boolean initialStatus = loggedIn;
        if (privateData == null || privateData.getUpdatedAt() == null || getLocation() == null)
            loggedIn = false;
        Date udAtPlus5 = new Date(privateData.getUpdatedAt().getTime() + (5 * 60 * 1000));
        loggedIn = udAtPlus5.after(new Date());
    }
    private void setFacebookProfilePicture(){
        final String fbid = user.getString("fbid");

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    URL imageURL = new URL("https://graph.facebook.com/" + fbid + "/picture?width=200&height=200");
                    rawFbPic = BitmapFactory.decodeStream(imageURL.openConnection().getInputStream());
                }
                catch (MalformedURLException mUE) {}
                catch (IOException iOE) {}
                return null;
            }
            @Override
            protected void onPostExecute(Void voids) {
                setProfPics();
                app.callbackIfReady(5);
            }
        };
        task.execute();
    }
    private void setProfPics(){
        if(rawFbPic != null) {
            profPic = getCircularWithColor(rawFbPic, relativeColor);
            setMarkerIconBitmap();
        }
        if (app != null)
            app.updateView();
    }
    private void setMarkerIconBitmap(){
        markerIcon = Bitmap.createScaledBitmap(profPic, 200, 200, false);

        for(Group group : app.getFriendsList().getAllGroups())
            if(group.contains(this))
                group.resetImage();
    }
    private void setChatIcon() {
        Bitmap leftBubble = BitmapFactory.decodeResource(app.getResources(), R.drawable.left_chat_bubble_white);
        Bitmap rightBubble = BitmapFactory.decodeResource(app.getResources(), R.drawable.right_chat_bubble);
        chatIcon = GeneralUtils.layerImagesRecolorForeground(leftBubble, rightBubble, relativeColor);
    }

    public int getRelativeColor() {
        return relativeColor;
    }
    public int getLighterColor() {
        return lighterColor;
    }
    public void setNewColor() {
        oldColors.push(relativeColor);
        relativeColor = GeneralUtils.generateNewColor();
        lighterColor = GeneralUtils.getLighter(relativeColor);
        setProfPics();
        setChatIcon();
        UserInfoSaveable.autoSave(app, User.getUserCache());
    }
    public void resetToLastColor() {
        if (oldColors.empty()) return;
        relativeColor = oldColors.pop();
        lighterColor = GeneralUtils.getLighter(relativeColor);
        setProfPics();
        setChatIcon();
        UserInfoSaveable.autoSave(app, User.getUserCache());
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
    public String getFirstname() {
        String firstname = user.getString("firstname");
        if(firstname != null) return firstname;
        return getUsername();
    }
    public int getCheckinCount(){
        return user.getInt("checkinCount");
    }
    public String getSHePronoun(){
        String gender = getGender();
        if (getGender() == null) return "they";
        if (getGender().equals("male")) return "he";
        if (getGender().equals("female")) return "she";
        return "they";
    }
    public String getGender() {
        return user.getString("gender");
    }


    public boolean isLoggedIn() {
        return loggedIn;
    }
    public Bitmap getProfPic() {
        return profPic;
    }
    public Bitmap getMarkerIcon() {
        return markerIcon;
    }
    public Bitmap getChatIcon() {
        return chatIcon;
    }
    public LatLng getLocation() {
        if (privateData == null) return null;
        ParseGeoPoint loc = privateData.getParseGeoPoint("location");
        if (loc == null || !isLoggedIn()) return null;
        return new LatLng(loc.getLatitude(), loc.getLongitude());
    }
    public float getDistanceInMetersFrom(User otherUser){
        LatLng myLoc = getLocation();
        LatLng otherLoc = otherUser.getLocation();
        float[] results = new float[5];
        Location.distanceBetween(myLoc.latitude, myLoc.longitude, otherLoc.latitude, otherLoc.longitude, results);
        return results[0];
    }

    public UserType getUserType() {
        return userType;
    }
    public void setUserType(UserType userType) {
        this.userType = userType;
    }


    public int compareTo(User otherUser) {
        return getFullname().compareTo(otherUser.getFullname());
    }

    public enum UserType {
        FRIEND, PENDING_YOU, PENDING_THEM, FACEBOOK_FRIEND, RANDOM
    }
}
