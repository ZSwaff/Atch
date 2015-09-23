package com.auriferous.atch;

import android.location.Location;
import android.util.Log;

import com.auriferous.atch.Callbacks.SimpleCallback;
import com.auriferous.atch.Callbacks.TwoVariableCallback;
import com.auriferous.atch.Callbacks.VariableCallback;
import com.auriferous.atch.Messages.MessageList;
import com.auriferous.atch.Users.User;
import com.auriferous.atch.Users.UserList;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.parse.FindCallback;
import com.parse.FunctionCallback;
import com.parse.GetCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseRole;
import com.parse.ParseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ParseAndFacebookUtils {
    public static final List<String> permissions = Arrays.asList("public_profile", "user_friends");


    public static void setupParseInstallation() {
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.put("userId", ParseUser.getCurrentUser().getObjectId());
        installation.saveInBackground();
        ParsePush.subscribeInBackground("global");
    }

    //note that the callback here gets called both when the friends are retrieved and when those friends' locations are
    public static void getAllFriends(final VariableCallback<UserList> callback){
        final UserList friendsList = new UserList(User.UserType.FRIEND);

        ParseQuery<ParseRole> roleQuery = ParseRole.getQuery();
        roleQuery.whereEqualTo("name", "friendsOf_" + ParseUser.getCurrentUser().getObjectId());
        roleQuery.getFirstInBackground(new GetCallback<ParseRole>() {
            @Override
            public void done(ParseRole role, ParseException e) {
                if (role == null) return;
                ParseRelation<ParseUser> relation = role.getRelation("users");
                ParseQuery<ParseUser> friendQuery = relation.getQuery();
                friendQuery.orderByAscending("fullname");
                friendQuery.findInBackground(new FindCallback<ParseUser>() {
                    @Override
                    public void done(List<ParseUser> list, ParseException e) {
                        for (ParseUser user : list) {
                            friendsList.addUser(User.getOrCreateUser(user, User.UserType.FRIEND));
                        }

                        callback.done(friendsList);
                    }
                });
            }
        });
    }

    public static void getParseUserFromFbid(String fbid, final VariableCallback<ParseUser> callback){
        ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
        userQuery.whereEqualTo("fbid", fbid);
        userQuery.getFirstInBackground(new GetCallback<ParseUser>() {
            @Override
            public void done(ParseUser parseUser, ParseException e) {
                if(callback != null)
                    callback.done(parseUser);
            }
        });
    }
    public static void getParseUserFromUsername(String username, final VariableCallback<ParseUser> callback){
        ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
        userQuery.whereEqualTo("username", username);
        userQuery.getFirstInBackground(new GetCallback<ParseUser>() {
            @Override
            public void done(ParseUser parseUser, ParseException e) {
                if (callback != null)
                    callback.done(parseUser);
            }
        });
    }
    public static void getUsersWithMatchingUsernameOrFullname(String strQuery, final VariableCallback<UserList> callback){
        strQuery = strQuery.toLowerCase();

        ParseQuery<ParseUser> usernameQuery = ParseUser.getQuery();
        usernameQuery.whereContains("queryUsername", strQuery);

        ParseQuery<ParseUser> fullnameQuery = ParseUser.getQuery();
        fullnameQuery.whereContains("queryFullname", strQuery);

        List<ParseQuery<ParseUser>> queries = new ArrayList<>();
        queries.add(usernameQuery);
        queries.add(fullnameQuery);

        ParseQuery<ParseUser> mainQuery = ParseQuery.or(queries);
        mainQuery.whereNotEqualTo("objectId", ParseUser.getCurrentUser().getObjectId());
        mainQuery.orderByAscending("fullname");
        mainQuery.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> list, ParseException e) {
                if (list == null) return;
                UserList searchResults = new UserList(list, User.UserType.RANDOM);
                searchResults.sortByPriorityForSearch();
                callback.done(searchResults);
            }
        });
    }


    public static void sendFriendRequest(String targetParseId) {
        final ParseUser targetUser = User.getUserFromMap(targetParseId).getUser();
        if (targetUser == null) {
            Log.e("xxxerr", "sendFriendRequest(...) failed, requested User not in userMap");
        }

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
    public static void cancelFriendRequest(String targetParseId) {
        final ParseUser targetUser = User.getUserFromMap(targetParseId).getUser();
        if (targetUser == null) {
            Log.e("xxxerr", "cancelFriendRequest(...) failed, requested User not in userMap");
        }

        ParseQuery<ParseObject> query = ParseQuery.getQuery("FriendRequest");
        query.whereEqualTo("toUser", targetUser);
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject parseObject, ParseException e) {
                HashMap<String, Object> params = new HashMap<>();
                params.put("friendRequestId", parseObject.getObjectId());
                ParseCloud.callFunctionInBackground("cancelFriendRequest", params);
            }
        });
    }

    public static void acceptFriendRequest(String senderParseId) {
        final ParseUser targetUser = User.getUserFromMap(senderParseId).getUser();
        if (targetUser == null) {
            Log.e("xxxerr", "acceptFriendRequest(...) failed, requested User not in userMap");
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
    public static void rejectFriendRequest(String senderParseId) {
        final ParseUser targetUser = User.getUserFromMap(senderParseId).getUser();
        if (targetUser == null) {
            Log.e("xxxerr", "rejectFriendRequest(...) failed, requested User not in userMap");
        }

        ParseQuery<ParseObject> query = ParseQuery.getQuery("FriendRequest");
        query.whereEqualTo("fromUser", targetUser);
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject friendRequest, ParseException e) {
                friendRequest.put("state", "rejected");
                friendRequest.saveInBackground();
            }
        });
    }
    public static void unfriendFriend(String uid){
        final ParseUser targetUser = User.getUserFromMap(uid).getUser();
        if (targetUser == null) {
            Log.e("xxxerr", "unfriendFriend(...) failed, requested User not in userMap");
        }

        HashMap<String, Object> params = new HashMap<>();
        params.put("friendId", uid);
        ParseCloud.callFunctionInBackground("deleteFriend", params);
    }

    public static void getUsersWhoCurrentUserHasRequestedToFriend(final VariableCallback<UserList> callback) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("FriendRequest");
        query.whereEqualTo("fromUser", ParseUser.getCurrentUser());
        query.whereEqualTo("state", "requested");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> requests, ParseException e) {
                ArrayList<String> userObjIds = new ArrayList<>();
                for (ParseObject req : requests)
                    userObjIds.add(req.getParseObject("toUser").getObjectId());

                ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
                userQuery.whereContainedIn("objectId", userObjIds);
                userQuery.findInBackground(new FindCallback<ParseUser>() {
                    @Override
                    public void done(List<ParseUser> users, ParseException e) {
                        callback.done(new UserList(users, User.UserType.PENDING_THEM));
                    }
                });
            }
        });
    }
    public static void getUsersWhoHaveRequestedToFriendCurrentUser(final VariableCallback<UserList> callback) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("FriendRequest");
        query.whereEqualTo("toUser", ParseUser.getCurrentUser());
        query.whereEqualTo("state", "requested");
        query.findInBackground(new FindCallback<ParseObject>() {
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


    public static void sendLoginNotifications() {
        ParseCloud.callFunctionInBackground("sendLoginNotifications", null, new FunctionCallback<Object>() {
            @Override
            public void done(Object o, ParseException e) {
            }
        });
    }
    public static void updateMyLocation(Location location){
        ParseUser currentUser = ParseUser.getCurrentUser();
        final ParseGeoPoint loc = (location != null) ? new ParseGeoPoint(location.getLatitude(),location.getLongitude()) : null;

        if(loc != null) {
            currentUser.increment("checkinCount");
            currentUser.saveInBackground();
        }

        ParseQuery<ParseObject> query = ParseQuery.getQuery("FriendData");
        query.whereEqualTo("user", currentUser);
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject parseObject, ParseException e) {
                if (parseObject == null) return;
                if (loc == null)
                    parseObject.remove("location");
                else
                    parseObject.put("location", loc);
                parseObject.saveInBackground();
            }
        });
    }
    public static void updateFriendDataWithMostRecentLocations(final UserList friends, final SimpleCallback callback){
        ParseQuery<ParseObject> userQuery = ParseQuery.getQuery("FriendData");
        userQuery.whereNotEqualTo("user", ParseUser.getCurrentUser());
        userQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (parseObjects == null) return;
                for (ParseObject privateDatum : parseObjects)
                    friends.addDataToUnknownUser(privateDatum);

                friends.updateFriendGroups();

                if (callback != null)
                    callback.done();
            }
        });
    }


    public static void getOrCreateMessageHistory(ArrayList<String> recipientsParseIds, final VariableCallback<ParseObject> callback){
        recipientsParseIds.add(ParseUser.getCurrentUser().getObjectId());
        JSONArray userIds = convertArray(recipientsParseIds);

        HashMap<String, Object> params = new HashMap<>();
        params.put("userIds", userIds);
        ParseCloud.callFunctionInBackground("getOrCreateMessageHistory", params, new FunctionCallback<ParseObject>() {
            @Override
            public void done(ParseObject messageHistory, ParseException e) {
                callback.done(messageHistory);
            }
        });
    }


    public static void sendMessage(final ParseObject messageHistory, final String messageText, final char decorationFlag, final SimpleCallback callback){
        HashMap<String, Object> params = new HashMap<>();
        params.put("messageText", messageText.trim());
        params.put("decorationFlag", decorationFlag+"");
        params.put("messageHistoryId", messageHistory.getObjectId());
        ParseCloud.callFunctionInBackground("sendMessage", params, new FunctionCallback<Object>() {
            @Override
            public void done(Object o, ParseException e) {
                if(callback != null)
                    callback.done();
            }
        });
    }

    public static void getAllMessagesFromAllLists(final TwoVariableCallback<String, MessageList> callback) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("MessageHistory");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                if (list == null)
                    Log.e("xxxerr", "getAllMessagesFromAllLists(...) failed:" + e.getMessage());
                else {
                    for (ParseObject messageHistory : list) {
                        String mHName = messageHistory.getString("name");
                        final String userIds = mHName.substring(mHName.indexOf('_') + 1);
                        getMessagesFromHistory(messageHistory, new VariableCallback<MessageList>() {
                            @Override
                            public void done(MessageList messageList) {
                                callback.done(userIds, messageList);
                            }
                        });
                    }
                }
            }
        });
    }
    public static void getMessagesFromHistory(final ParseObject messageHistory, final VariableCallback<MessageList> callback) {
        ArrayList<String> messageList = (ArrayList<String>)messageHistory.get("messageList");

        if(messageList != null) {
            ParseQuery<ParseObject> query = ParseQuery.getQuery("Message");
            query.whereContainedIn("objectId", messageList);
            query.orderByDescending("createdAt");
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> messages, ParseException e) {
                    MessageList ret = new MessageList(messageHistory, messages, true);
                    ret.setMessageHistory(messageHistory);
                    callback.done(ret);
                }
            });
        }
        else {
            callback.done(new MessageList(messageHistory, new ArrayList<ParseObject>()));
        }
    }



    public static void getAllFacebookFriends(final VariableCallback<UserList> callback){
        GraphRequest request = GraphRequest.newMyFriendsRequest(
                AccessToken.getCurrentAccessToken(),
                new GraphRequest.GraphJSONArrayCallback() {
                    @Override
                    public void onCompleted(JSONArray array, GraphResponse response) {
                        final UserList facebookFriends = new UserList(User.UserType.FACEBOOK_FRIEND);

                        ArrayList<String> fbids = new ArrayList<>();

                        for (int i = 0; i < array.length(); i++) {
                            try {
                                JSONObject obj = array.getJSONObject(i);
                                fbids.add(obj.getString("id"));
                            } catch (JSONException e) {}
                        }

                        ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
                        userQuery.whereContainedIn("fbid", fbids);
                        userQuery.orderByAscending("fullname");
                        userQuery.findInBackground(new FindCallback<ParseUser>() {
                            @Override
                            public void done(List<ParseUser> list, ParseException e) {
                                for (ParseUser user : list)
                                    facebookFriends.addUser(User.getOrCreateUser(user, User.UserType.FACEBOOK_FRIEND));

                                callback.done(facebookFriends);
                            }
                        });
                    }
                });
        request.executeAsync();
    }
    public static void setFacebookInfoAboutCurrentUser(final String username){
        GraphRequest request = GraphRequest.newMeRequest(
                AccessToken.getCurrentAccessToken(),
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        try {
                            ParseUser currUser = ParseUser.getCurrentUser();
                            currUser.setUsername(username);
                            currUser.put("checkinCount", 0);
                            currUser.put("queryUsername", username.toLowerCase());
                            currUser.put("firstname", object.getString("first_name"));
                            currUser.put("fbid", object.getString("id"));
                            currUser.put("fullname", object.getString("name"));
                            currUser.put("queryFullname", object.getString("name").toLowerCase());
                            currUser.put("gender", object.getString("gender"));
                            currUser.saveInBackground();
                        } catch (JSONException e) {}
                    }
                });
        request.executeAsync();
    }



    public static JSONArray convertArray(ArrayList<String> list){
        JSONArray ret = new JSONArray();
        for(int i = 0; i < list.size(); i++){
            ret.put(list.get(i));
        }
        return ret;
    }
}
