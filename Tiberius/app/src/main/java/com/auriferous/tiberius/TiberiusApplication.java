package com.auriferous.tiberius;

import android.app.Application;
import android.util.Log;

import com.auriferous.tiberius.Friends.User;
import com.auriferous.tiberius.Friends.UserList;
import com.facebook.FacebookSdk;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class TiberiusApplication extends Application {
    private UserList friendsList = new UserList();
    private UserList facebookFriendsList = new UserList();

    public UserList getFacebookFriendsList() {
        return facebookFriendsList;
    }
    public UserList getFriendsList() {
        return friendsList;
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

                if(vUC != null)
                    vUC.updateView();
            }
        });
    }
    //populates once results come in from Parse
    public void populateFriendList(JSONArray listFromFacebook, final ViewUpdateCallback vUC){
        synchronized (friendsList) {friendsList = new UserList(); }
        //TODO finish

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

                if(vUC != null)
                    vUC.updateView();
            }
        });
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        FacebookSdk.sdkInitialize(getApplicationContext());
        Parse.initialize(getApplicationContext(), "P4g0harOzaQTi9g3QyEqGPI3HkiPJxxz4SJObhCE", "GpAM5yqJzbltLQENhwJt0cMbrVyM9q4aHR8O3k2s");
        ParseFacebookUtils.initialize(getApplicationContext());
    }
}