package com.auriferous.tiberius;

import android.location.Location;
import android.util.Log;

import com.auriferous.tiberius.Callbacks.FuncCallback;
import com.auriferous.tiberius.Messages.MessageList;
import com.auriferous.tiberius.Users.User;
import com.auriferous.tiberius.Users.UserList;
import com.parse.FindCallback;
import com.parse.FunctionCallback;
import com.parse.GetCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ParseAndFacebookUtils {
    public static final List<String> permissions = Arrays.asList("public_profile", "user_friends");


    private static void getParseUserFromId(String parseId, final GetCallback<ParseUser> callback){
        ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
        userQuery.whereEqualTo("objectId", parseId);
        userQuery.getFirstInBackground(callback);
    }
    public static void getUsersWithMatchingUsernameOrFullname(String strQuery, final FindCallback<ParseUser> callback){
        ParseQuery<ParseUser> usernameQuery = ParseUser.getQuery();
        usernameQuery.whereContains("username", strQuery);

        ParseQuery<ParseUser> fullnameQuery = ParseUser.getQuery();
        fullnameQuery.whereContains("fullname", strQuery);

        List<ParseQuery<ParseUser>> queries = new ArrayList<>();
        queries.add(usernameQuery);
        queries.add(fullnameQuery);

        ParseQuery<ParseUser> mainQuery = ParseQuery.or(queries);
        mainQuery.whereNotEqualTo("objectId", ParseUser.getCurrentUser().getObjectId());
        mainQuery.orderByAscending("fullname");
        mainQuery.findInBackground(callback);
    }


    public static void sendFriendRequest(String targetParseId) {
        final ParseUser targetUser = User.getUserFromMap(targetParseId).getUser();
        if (targetUser == null) {
            Log.e("xxx", "sendFriendRequest(...) failed, requested User not in userMap");
        }

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
    public static void acceptFriendRequest(String senderParseId) {
        final ParseUser targetUser = User.getUserFromMap(senderParseId).getUser();
        if (targetUser == null) {
            Log.e("xxx", "acceptFriendRequest(...) failed, requested User not in userMap");
        }

        ParseQuery<ParseObject> query = ParseQuery.getQuery("FriendRequest");
        query.whereEqualTo("fromUser", targetUser);
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject friendRequest, ParseException e) {
                friendRequest.put("state", "accepted");
                friendRequest.saveInBackground();
            }
        });
    }


    public static void getPendingFriendRequestsToCurrentUser(final FindCallback<ParseObject> callback) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("FriendRequest");
        query.whereEqualTo("toUser", ParseUser.getCurrentUser());
        query.whereEqualTo("state", "requested");
        query.findInBackground(callback);
    }
    public static void getUsersWhoHaveRequestedToFriendCurrentUser(final FuncCallback<UserList> callback) {
        getPendingFriendRequestsToCurrentUser(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> requests, ParseException e) {
                ArrayList<String> userObjIds = new ArrayList<>();
                for (ParseObject req : requests)
                    userObjIds.add(req.getParseObject("fromUser").getObjectId());

                ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
                userQuery.whereContainedIn("objectId", userObjIds);
                userQuery.orderByAscending("fullname");
                userQuery.findInBackground(new FindCallback<ParseUser>() {
                    @Override
                    public void done(List<ParseUser> users, ParseException e) {
                        callback.done(new UserList(users, User.UserType.PENDING_YOU));
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
    public static void updateFriendDataWithMostRecentLocations(final UserList friends){
        ParseQuery<ParseObject> userQuery = ParseQuery.getQuery("FriendData");
        userQuery.whereNotEqualTo("user", ParseUser.getCurrentUser());
        userQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                for (ParseObject privateDatum : parseObjects) {
                    friends.addDataToUnknownUser(privateDatum);
                }
            }
        });
    }


    public static void getOrCreateMessageHistory(String recipientParseId, FunctionCallback<ParseObject> callback){
        ArrayList<String> recipientsParseIds = new ArrayList<>();
        recipientsParseIds.add(recipientParseId);
        getOrCreateMessageHistory(recipientsParseIds, callback);
    }
    public static void getOrCreateMessageHistory(ArrayList<String> recipientsParseIds, FunctionCallback<ParseObject> callback){
        recipientsParseIds.add(ParseUser.getCurrentUser().getObjectId());
        JSONArray userIds = convertArray(recipientsParseIds);

        HashMap<String, Object> params = new HashMap<>();
        params.put("userIds", userIds);
        ParseCloud.callFunctionInBackground("getOrCreateMessageHistory", params, callback);
    }


    public static void sendMessage(final ParseObject messageHistory, final String messageText){
        HashMap<String, Object> params = new HashMap<>();
        params.put("messageText", messageText);
        params.put("messageHistoryId", messageHistory.getObjectId());
        ParseCloud.callFunctionInBackground("sendMessage", params);
    }
    public static void sendMessage(final ParseObject messageHistory, final String messageText, final FuncCallback<Object> callback){
        HashMap<String, Object> params = new HashMap<>();
        params.put("messageText", messageText);
        params.put("messageHistoryId", messageHistory.getObjectId());
        ParseCloud.callFunctionInBackground("sendMessage", params, new FunctionCallback<Object>() {
            @Override
            public void done(Object o, ParseException e) {
                if(callback != null)
                    callback.done(o);
            }
        });
    }
    public static void getAllMessagesFromHistory(final ParseObject messageHistory, final FuncCallback<MessageList> callback){
        ArrayList<String> messageList = (ArrayList<String>)messageHistory.get("messageList");

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Message");
        query.whereContainedIn("objectId", messageList);
        query.orderByAscending("createdAt");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> messages, ParseException e) {
                callback.done(new MessageList(messages));
            }
        });
    }


    public static JSONArray convertArray(ArrayList<String> list){
        JSONArray ret = new JSONArray();
        for(int i = 0; i < list.size(); i++){
            ret.put(list.get(i));
        }
        return ret;
    }
    public static ArrayList<String> convertArray(JSONArray arr){
        ArrayList<String> ret = new ArrayList<>();
        for(int i = 0; i < arr.length(); i++){
            try {
                ret.add((String) arr.get(i));
            }
            catch (JSONException jE) {}
        }
        return ret;
    }
}
