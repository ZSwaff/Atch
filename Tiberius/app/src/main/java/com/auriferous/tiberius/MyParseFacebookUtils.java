package com.auriferous.tiberius;

import android.location.Location;

import com.auriferous.tiberius.Callbacks.ListUserCallback;
import com.auriferous.tiberius.Users.User;
import com.auriferous.tiberius.Users.UserList;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyParseFacebookUtils {
    public static List<String> permissions = Arrays.asList("public_profile", "user_friends");

    //not blocking any more
    private static void getParseUserFromId(String parseId, final GetCallback<ParseUser> callback){
        ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
        userQuery.whereEqualTo("objectId", parseId);
        userQuery.getFirstInBackground(callback);
    }

    public static void sendFriendRequest(String targetParseId) {
        getParseUserFromId(targetParseId, new GetCallback<ParseUser>() {
            @Override
            public void done(final ParseUser targetUser, ParseException e) {
                if (targetUser == null) return;

                //todo implement the below code on the cloud which checks if duplicate
                ParseQuery<ParseObject> query = ParseQuery.getQuery("FriendRequest");
                query.whereEqualTo("toUser", targetUser);
                query.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> list, ParseException e) {
                        if (list.isEmpty()) {
                            ParseObject friendRequest = new ParseObject("FriendRequest");
                            friendRequest.put("fromUser", ParseUser.getCurrentUser());
                            friendRequest.put("toUser", targetUser);
                            friendRequest.put("state", "requested");
                            friendRequest.saveInBackground();
                        }
                    }
                });

            }
        });
    }
    public static void acceptFriendRequest(String senderParseId) {
        getParseUserFromId(senderParseId, new GetCallback<ParseUser>() {
            @Override
            public void done(final ParseUser senderUser, ParseException e) {
                if (senderUser == null) return;

                ParseQuery<ParseObject> query = ParseQuery.getQuery("FriendRequest");
                query.whereEqualTo("fromUser", senderUser);
                query.getFirstInBackground(new GetCallback<ParseObject>() {
                    @Override
                    public void done(ParseObject friendRequest, ParseException e) {
                        friendRequest.put("state", "accepted");
                        friendRequest.saveInBackground();
                    }
                });
            }
        });
    }

    public static void getUsersWithMatchingUsernameOrFullname(String strQuery, final FindCallback<ParseUser> callback){
        ParseQuery<ParseUser> usernameQuery = ParseUser.getQuery();
        usernameQuery.whereContains("username", strQuery);

        ParseQuery<ParseUser> fullnameQuery = ParseUser.getQuery();
        fullnameQuery.whereContains("fullname", strQuery);

        List<ParseQuery<ParseUser>> queries = new ArrayList<ParseQuery<ParseUser>>();
        queries.add(usernameQuery);
        queries.add(fullnameQuery);

        ParseQuery<ParseUser> mainQuery = ParseQuery.or(queries);
        mainQuery.orderByAscending("fullname");
        mainQuery.findInBackground(callback);
    }

    public static void getPendingFriendRequestsToCurrentUser(final FindCallback<ParseObject> callback) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("FriendRequest");
        query.whereEqualTo("toUser", ParseUser.getCurrentUser());
        query.whereEqualTo("state", "requested");
        query.findInBackground(callback);
    }
    public static void getUsersWhoHaveRequestedToFriendCurrentUser(final ListUserCallback callback) {
        getPendingFriendRequestsToCurrentUser(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> requests, ParseException e) {
                ArrayList<String> userObjIds = new ArrayList<String>();
                for (ParseObject req : requests)
                    userObjIds.add(req.getParseObject("fromUser").getObjectId());

                ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
                userQuery.whereContainedIn("objectId", userObjIds);
                userQuery.orderByAscending("fullname");
                userQuery.findInBackground(new FindCallback<ParseUser>() {
                    @Override
                    public void done(List<ParseUser> users, ParseException e) {
                        if (callback == null) return;
                        ArrayList<User> ret = new ArrayList<User>();
                        for(ParseUser pUsr : users)
                            ret.add(new User(pUsr));
                        callback.done(ret);
                    }
                });
            }
        });
    }

    public static void updateMyLocation(Location location){
        ParseUser currentUser = ParseUser.getCurrentUser();

        final ParseGeoPoint loc = new ParseGeoPoint(location.getLatitude(),location.getLongitude());

        ParseQuery<ParseObject> query = ParseQuery.getQuery("FriendData");
        query.whereEqualTo("user", currentUser);
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject parseObject, ParseException e) {
                parseObject.put("location",loc);
                parseObject.saveInBackground();
            }
        });
    }

    //TODO so this blocks. it's very complicated where it is, but long story short it shouldn't
    public static void updateWithMostRecentLocations(UserList friends){
        ArrayList<ParseUser> friendPUsers = new ArrayList<ParseUser>();
        synchronized (friends){
            for(User friend : friends.getAllUsers())
                friendPUsers.add(friend.getUser());
        }

        ParseQuery<ParseObject> userQuery = ParseQuery.getQuery("FriendData");
        userQuery.whereContainedIn("user", friendPUsers);
        try{
            List<ParseObject> dataList = userQuery.find();
            synchronized (friends) {
                for (ParseObject privateDatum : dataList) {
                    friends.addDataToUnknownUser(privateDatum);
                }

            }
        } catch (ParseException e) {}
    }
}
