package com.atchapp.atch;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.location.Location;
import android.os.CountDownTimer;
import android.util.Base64;
import android.util.Log;

import com.atchapp.atch.Callbacks.SimpleCallback;
import com.atchapp.atch.Callbacks.TwoVariableCallback;
import com.atchapp.atch.Callbacks.VariableCallback;
import com.atchapp.atch.Callbacks.ViewUpdateCallback;
import com.atchapp.atch.Messages.MessageList;
import com.atchapp.atch.Users.Group;
import com.atchapp.atch.Users.User;
import com.atchapp.atch.Users.UserInfoSaveable;
import com.atchapp.atch.Users.UserList;
import com.atchapp.atch.Users.UserListAdapter;
import com.facebook.FacebookSdk;
import com.google.android.gms.maps.MapsInitializer;
import com.parse.Parse;
import com.parse.ParseFacebookUtils;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

//laptop  OYjIaRgMAlt7n5rwAlrxEN3XMf8=
//desktop OYTDmaBNFPsf+lOOtQO1qU+67pk=

public class AtchApplication extends Application {
    private volatile Activity currentActivity = null;
    private volatile ViewUpdateCallback viewUpdateCallback = null;

    private volatile boolean isSetupComplete = false;
    //0 is friendList, 1 is facebook friends, 2 is pending you, 3 is pending them, 4 is locations, 5 is prof pics
    private volatile int[] loadingPhasesComplete = {1, 1, 1, 1, 1, -1};
    private volatile SimpleCallback setupCompleteCallback = null;

    private volatile Intent locationUpdateServiceIntent = null;
    private volatile Location currentLocation = null;
    private volatile Date lastUpdateTime = null;

    private volatile UserList friendsList = new UserList(User.UserType.FRIEND);
    private volatile UserList usersWhoSentFriendRequests = new UserList(User.UserType.PENDING_YOU);
    private volatile UserList facebookFriends = new UserList(User.UserType.FACEBOOK_FRIEND);
    private volatile HashMap<String, MessageList> allMessageLists = new HashMap<>();

    private volatile CountDownTimer logoutAlarm = null;
    private volatile boolean isLoggedIn = false;
    private volatile boolean isOnline = false;
    private volatile boolean isOnlineAndAppOpen = false;


    public Activity getCurrentActivity() {
        return currentActivity;
    }
    public void setCurrentActivity(Activity currentActivity) {
        this.currentActivity = currentActivity;
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

    public void logout() {
        stopLocationUpdates();
        setIsOnline(false);
        ParseAndFacebookUtils.updateMyLocation(null);
    }
    public void activateLogoutAlarm() {
        logoutAlarm = new CountDownTimer(30 * 60 * 1000, 30 * 60 * 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }
            @Override
            public void onFinish() {
                logout();
            }
        };
        logoutAlarm.start();
    }
    public void deactivateLogoutAlarm() {
        if(logoutAlarm != null)
            logoutAlarm.cancel();
        logoutAlarm = null;
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

    public MessageList getMessageList(Group chatGroup) {
        String chatGroupIds = chatGroup.getIdsInString(ParseUser.getCurrentUser().getObjectId());
        if (allMessageLists.containsKey(chatGroupIds))
            return allMessageLists.get(chatGroupIds);
        return new MessageList(null, new ArrayList<ParseObject>());
    }
    public void refreshMessageList(final Group chatGroup, ParseObject messageHistory, final SimpleCallback callback) {
        final String chatGroupIds = chatGroup.getIdsInString(ParseUser.getCurrentUser().getObjectId());
        ParseAndFacebookUtils.getMessagesFromHistory(messageHistory, new VariableCallback<MessageList>() {
            @Override
            public void done(MessageList messageList) {
                allMessageLists.put(chatGroupIds, messageList);

                int unreadCount = messageList.getUnreadCount(ParseUser.getCurrentUser());
                chatGroup.setUnreadCount(unreadCount);

                if (callback != null)
                    callback.done();
            }
        });
    }

    public UserList getUsersWhoSentFriendRequests() {
        return usersWhoSentFriendRequests;
    }
    public UserList getFacebookFriends() {
        return facebookFriends;
    }
    public HashMap<String, MessageList> getAllMessageLists() {
        return allMessageLists;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        //logReleaseHashKey();

        AtchParsePushReceiver.init(this);
        AtchParsePushReceiver.cancelAllNotifications(this);

        UserInfoSaveable infoGroup = UserInfoSaveable.autoLoad(this);
        User.init(this, infoGroup);
        Group.init(this);
        UserListAdapter.init(this);

        MapsInitializer.initialize(this);

        FacebookSdk.sdkInitialize(this);

        Parse.initialize(this, "P4g0harOzaQTi9g3QyEqGPI3HkiPJxxz4SJObhCE", "GpAM5yqJzbltLQENhwJt0cMbrVyM9q4aHR8O3k2s");
        ParseFacebookUtils.initialize(this);
    }

    private void logReleaseHashKey(){
        PackageInfo info;
        try {
            info = getPackageManager().getPackageInfo("com.atchapp.atch", PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md;
                md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String something = new String(Base64.encode(md.digest(), 0));
                Log.e("xxx hash key", something);
            }
        }
        catch (Exception e) {
            Log.e("xxxerr hash key", "error");
        }
    }



    public boolean isSetupComplete() {
        return isSetupComplete;
    }
    public void setSetupCompleteCallback(SimpleCallback setupCompleteCallback) {
        if(isSetupComplete) {
            if(setupCompleteCallback != null)
                setupCompleteCallback.done();
            this.setupCompleteCallback = null;
            return;
        }

        this.setupCompleteCallback = setupCompleteCallback;
    }
    public synchronized void callbackIfReady(int switchIndex) {
        if (!isSetupComplete) {
            if(loadingPhasesComplete[switchIndex] > 0)
                loadingPhasesComplete[switchIndex]--;

            for (int i = 0; i < loadingPhasesComplete.length; i++)
                if (loadingPhasesComplete[i] != 0) return;

            if (setupCompleteCallback != null)
                setupCompleteCallback.done();
            setupCompleteCallback = null;
            isSetupComplete = true;
        }
    }

    //populates once results come in from Parse
    public void populateFriendList() {
        ParseAndFacebookUtils.getAllFriends(new VariableCallback<UserList>() {
            @Override
            public void done(UserList userList) {
                friendsList = userList;
                if (!isSetupComplete) {
                    loadingPhasesComplete[5] = friendsList.getAllUsers().size();
                    callbackIfReady(0);
                }

                ParseAndFacebookUtils.updateFriendDataWithMostRecentLocations(friendsList, new SimpleCallback() {
                    @Override
                    public void done() {
                        if (!isSetupComplete)
                            callbackIfReady(4);
                        updateView();
                    }
                });

                updateView();
            }
        });
    }
    //populates once results come in from Facebook, then Parse
    public void populateFacebookFriendList(){
        ParseAndFacebookUtils.getAllFacebookFriends(new VariableCallback<UserList>() {
            @Override
            public void done(UserList list) {
                facebookFriends = list;
                if (!isSetupComplete)
                    callbackIfReady(1);

                updateView();
            }

        });
    }
    //populates once results come in from Parse
    public void populatePendingLists(){
        ParseAndFacebookUtils.getUsersWhoHaveRequestedToFriendCurrentUser(new VariableCallback<UserList>() {
            @Override
            public void done(UserList list) {
                usersWhoSentFriendRequests = list;

                if (!isSetupComplete)
                    callbackIfReady(2);

                updateView();
            }
        });
        ParseAndFacebookUtils.getUsersWhoCurrentUserHasRequestedToFriend(new VariableCallback<UserList>() {
            @Override
            public void done(UserList userList) {
                if (!isSetupComplete)
                    callbackIfReady(3);
            }
        });
    }
    //populates once results come in from Parse
    public void populateMessageLists() {
        allMessageLists = new HashMap<>();
        ParseAndFacebookUtils.getAllMessagesFromAllLists(new TwoVariableCallback<String, MessageList>() {
            @Override
            public void done(String userIds, MessageList messageList) {
                allMessageLists.put(userIds, messageList);
                int unreadCount = messageList.getUnreadCount(ParseUser.getCurrentUser());
                for (Group group : friendsList.getAllGroups()) {
                    if (group.getIdsInString(ParseUser.getCurrentUser().getObjectId()).equals(userIds)) {
                        group.setUnreadCount(unreadCount);
                        break;
                    }
                }
            }
        });
    }
}