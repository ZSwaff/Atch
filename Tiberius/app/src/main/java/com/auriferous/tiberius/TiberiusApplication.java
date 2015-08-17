package com.auriferous.tiberius;

import android.app.Application;
import android.location.Location;
import android.util.Log;

import com.auriferous.tiberius.Friends.User;
import com.auriferous.tiberius.Friends.UserList;
import com.facebook.FacebookSdk;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;


public class TiberiusApplication extends Application {
    private Location mCurrentLocation;
    private DateFormat mLastUpdateTime;

    private UserList friendsList = new UserList();
    private UserList facebookFriendsList = new UserList();


    public Location getCurrentLocation() {
        synchronized (mCurrentLocation) {
            return mCurrentLocation;
        }
    }

    public void setCurrentLocation(Location mCurrentLocation) {
        synchronized (mCurrentLocation) {
            this.mCurrentLocation = mCurrentLocation;
        }
    }

    public DateFormat getLastUpdateTime() {
        synchronized (mLastUpdateTime) {
            return mLastUpdateTime;
        }
    }

    public void setLastUpdateTime(DateFormat mLastUpdateTime) {
        synchronized (mLastUpdateTime) {
            this.mLastUpdateTime = mLastUpdateTime;
        }
    }


    public UserList getFacebookFriendsList() {
        return facebookFriendsList;
    }
    public UserList getFriendsList() {
        return friendsList;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        FacebookSdk.sdkInitialize(getApplicationContext());
        Parse.initialize(getApplicationContext(), "P4g0harOzaQTi9g3QyEqGPI3HkiPJxxz4SJObhCE", "GpAM5yqJzbltLQENhwJt0cMbrVyM9q4aHR8O3k2s");
        ParseFacebookUtils.initialize(getApplicationContext());

        populateFriendList(null);
    }

    //populates once results come in from Parse
    public void populateFacebookFriendList(JSONArray listFromFacebook, final ViewUpdateCallback vUC){
        synchronized (facebookFriendsList) {facebookFriendsList = new UserList(); }

        ArrayList<String> fbids = new ArrayList<String>();

        for (int i = 0; i < listFromFacebook.length(); i++) {
            try {
                JSONObject obj = listFromFacebook.getJSONObject(i);
                fbids.add(obj.getString("id"));
            } catch (JSONException e) {}
        }

        ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
        userQuery.whereContainedIn("fbid", fbids);
        userQuery.orderByAscending("fullname");
        userQuery.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> list, ParseException e) {
                synchronized (facebookFriendsList) {
                    for (ParseUser user : list)
                        facebookFriendsList.addUser(new User(user));
                }

                if (vUC != null)
                    vUC.updateView();
            }
        });
    }
    //populates once results come in from Parse
    public void populateFriendList(final ViewUpdateCallback vUC){
        synchronized (friendsList) {friendsList = new UserList(); }

        ParseQuery<ParseObject> roleQuery = ParseQuery.getQuery("Role");
        roleQuery.whereEqualTo("name", "friendsOf_" + ParseUser.getCurrentUser());
        roleQuery.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject role, ParseException e) {
                if (role == null) return;
                ParseRelation<ParseUser> relation = role.getRelation("users");
                ParseQuery<ParseUser> friendQuery = relation.getQuery();
                friendQuery.orderByAscending("fullname");
                friendQuery.findInBackground(new FindCallback<ParseUser>() {
                    @Override
                    public void done(List<ParseUser> list, ParseException e) {
                        synchronized (friendsList) {
                            for (ParseUser user : list)
                                friendsList.addUser(new User(user));
                        }

                        if (vUC != null)
                            vUC.updateView();
                    }
                });
            }
        });
    }
}