package com.auriferous.tiberius;

import android.support.annotation.NonNull;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class MyParseFacebookUtils {
    public static List<String> permissions = Arrays.asList("public_profile", "user_friends");

    //blocking sorry
    private static ParseUser getUserFromId(String parseId){
        ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
        userQuery.whereEqualTo("objectId", parseId);
        try {
            List<ParseUser> users = userQuery.find();
            if(!users.isEmpty())
                return users.get(0);
        } catch (ParseException e) {}
        return null;
    }

    public static void sendFriendRequest(String targetParseId) {
        //gets user from id
        final ParseUser targetUser = getUserFromId(targetParseId);
        if(targetUser == null) return;

        //checks if duplicate
        ParseQuery<ParseObject> query = ParseQuery.getQuery("FriendRequest");
        query.whereEqualTo("toUser", targetUser);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                if(list.isEmpty()){
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
        //gets user from id
        final ParseUser senderUser = getUserFromId(senderParseId);
        if(senderUser == null) return;

        //checks if duplicate
        ParseQuery<ParseObject> query = ParseQuery.getQuery("FriendRequest");
        query.whereEqualTo("fromUser", senderUser);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                if(!list.isEmpty()){
                    ParseObject friendRequest = list.get(0);
                    friendRequest.put("state", "accepted");
                    friendRequest.saveInBackground();
                }
            }
        });
    }

    //should be called in background, probably
    public static List<ParseObject> getPendingFriendRequestsToCurrentUser() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("FriendRequest");
        query.whereEqualTo("toUser", ParseUser.getCurrentUser());
        query.whereEqualTo("state", "accepted");
        try {
            List<ParseObject> requests = query.find();
            return requests;
        } catch (ParseException e) {}
        return new ArrayList<ParseObject>();
    }
}