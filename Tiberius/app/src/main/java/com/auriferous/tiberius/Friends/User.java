package com.auriferous.tiberius.Friends;

import com.parse.ParseObject;
import com.parse.ParseUser;

import java.io.Serializable;

public class User implements Serializable {
    private ParseUser user;
    private ParseObject privateData;

    public User(ParseUser parseUser){
        user = parseUser;
    }

    public ParseUser getUser() {
        return user;
    }

    public void setPrivateData(ParseObject privateData){
        this.privateData = privateData;
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
