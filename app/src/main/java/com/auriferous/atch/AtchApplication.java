package com.auriferous.atch;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.location.Location;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import com.auriferous.atch.Callbacks.SimpleCallback;
import com.auriferous.atch.Callbacks.VariableCallback;
import com.auriferous.atch.Callbacks.ViewUpdateCallback;
import com.auriferous.atch.Users.User;
import com.auriferous.atch.Users.UserInfoGroup;
import com.auriferous.atch.Users.UserList;
import com.auriferous.atch.Users.UserListAdapter;
import com.facebook.FacebookSdk;
import com.google.android.gms.maps.MapsInitializer;
import com.parse.Parse;
import com.parse.ParseFacebookUtils;

import java.security.MessageDigest;
import java.util.Date;

//laptop release qgG7TnZ3y6x5EIBELHxeOp5l0+0=
//study mac release 28JFZmC2Z4KeVOdRPzxtEuX8UJM=

public class AtchApplication extends Application {
    private volatile Activity currentActivity = null;
    private volatile ViewUpdateCallback viewUpdateCallback = null;

    private volatile boolean isFriendListLoaded = false;
    private volatile SimpleCallback friendListLoadedCallback = null;

    private volatile Intent locationUpdateServiceIntent = null;
    private volatile Location currentLocation = null;
    private volatile Date lastUpdateTime = null;

    private volatile UserList friendsList = new UserList(User.UserType.FRIEND);

    private volatile boolean isLoggedIn = false;
    private volatile boolean isOnline = false;
    private volatile boolean isOnlineAndAppOpen = false;


    public Activity getCurrentActivity() {
        return currentActivity;
    }
    public void setCurrentActivity(Activity currentActivity) {
        this.currentActivity = currentActivity;
    }

    public void setFriendListLoadedCallback(SimpleCallback friendListLoadedCallback) {
        this.friendListLoadedCallback = friendListLoadedCallback;
    }
    public boolean isFriendListLoaded() {
        return isFriendListLoaded;
    }

    public void setViewUpdateCallback(ViewUpdateCallback viewUpdateCallback) {
        this.viewUpdateCallback = viewUpdateCallback;
    }
    public void updateView() {
        if (viewUpdateCallback != null){
            viewUpdateCallback.updateView();
        }
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }
    public void setIsLoggedIn(boolean isLoggedIn) {
        this.isLoggedIn = isLoggedIn;
    }
    public boolean isOnline() {
        return isOnline;
    }
    public void setIsOnline(boolean isOnline) {
        this.isOnline = isOnline;
    }
    public boolean isOnlineAndAppOpen() {
        return isOnlineAndAppOpen;
    }
    public void setIsOnlineAndAppOpen(boolean isOnlineAndAppOpen) {
        this.isOnlineAndAppOpen = isOnlineAndAppOpen;
    }

    public void startLocationUpdates(){
        if(locationUpdateServiceIntent != null) return;
        locationUpdateServiceIntent = new Intent(this, LocationUpdateService.class);
        startService(locationUpdateServiceIntent);
    }
    public void stopLocationUpdates(){
        if(locationUpdateServiceIntent == null) return;
        stopService(locationUpdateServiceIntent);
        locationUpdateServiceIntent = null;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }
    public void setCurrentLocation(Location mCurrentLocation) {
        this.currentLocation = mCurrentLocation;
    }

    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }
    public void setLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public UserList getFriendsList() {
        return friendsList;
    }
    public void addFriend(User newFriend){
        friendsList.addUser(newFriend);
    }
    public void removeFriend(User newEnemy){
        friendsList.removeUser(newEnemy);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        //logReleaseHashKey();

        AtchParsePushReceiver.init(this);
        AtchParsePushReceiver.cancelAllNotifications(this);

        UserInfoGroup infoGroup = UserInfoGroup.autoLoad(this);
        User.init(this, infoGroup);
        UserListAdapter.init(this);

        MapsInitializer.initialize(this);

        FacebookSdk.sdkInitialize(this);

        Parse.initialize(this, "P4g0harOzaQTi9g3QyEqGPI3HkiPJxxz4SJObhCE", "GpAM5yqJzbltLQENhwJt0cMbrVyM9q4aHR8O3k2s");
        ParseFacebookUtils.initialize(this);
    }

    private void logReleaseHashKey(){
        PackageInfo info;
        try {
            info = getPackageManager().getPackageInfo("com.auriferous.atch", PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md;
                md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String something = new String(Base64.encode(md.digest(), 0));
                Log.e("hash key", something);
            }
        }
        catch (Exception e) {
            Log.e("hash key", "error");
        }
    }

    //populates once results come in from Parse
    public void populateFriendList() {
        ParseAndFacebookUtils.getAllFriends(new VariableCallback<UserList>() {
            @Override
            public void done(UserList userList) {
                friendsList = userList;
                if (!isFriendListLoaded) {
                    if (friendListLoadedCallback != null)
                        friendListLoadedCallback.done();
                    friendListLoadedCallback = null;
                    isFriendListLoaded = true;
                }

                ParseAndFacebookUtils.updateFriendDataWithMostRecentLocations(friendsList, new SimpleCallback() {
                    @Override
                    public void done() {
                        updateView();
                    }
                });

                updateView();
            }
        });
    }
}