package com.auriferous.tiberius.Friends;

import com.parse.ParseUser;

import java.io.Serializable;

public class User implements Serializable {
    private ParseUser user;

    public User(ParseUser parseUser){
        user = parseUser;
    }

    public String getFullname(){
        return user.getString("fullname");
    }
    public String getUsername(){
        return user.getUsername();
    }
    public String getId(){
        return user.getObjectId();
    }

    @Override
    public String toString() {
        return "";
    }
}
