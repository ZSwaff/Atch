package com.auriferous.tiberius.Users;

import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class UserList {
    private ArrayList<User> users = new ArrayList<>();
    private User.UserType userType = User.UserType.RANDOM;


    public UserList(User.UserType userType) {
        this.userType = userType;
    }
    public UserList(List<ParseUser> users, User.UserType userType) {
        this.userType = userType;
        for(ParseUser pUsr : users)
            this.users.add(User.getOrCreateUser(pUsr, userType));
    }


    public ArrayList<User> getAllUsers() {
        return users;
    }
    public void setAllUsers(ArrayList<User> users) {
        this.users = users;
    }

    public void addUser(User user) {
        users.add(user);
    }
    public void addAllUsers(List<User> users) {
        for (User user : users)
            this.users.add(user);
    }

    public void addDataToUnknownUser(ParseObject privateDatum){
        ParseUser goalUser = privateDatum.getParseUser("user");
        String goalId = goalUser.getObjectId();

        for(User user : users){
            if(user.getId().equals(goalId))
                user.setPrivateData(privateDatum);
        }
    }


    @Override
    public String toString() {
        String res = "";
        for(User user : users)
            res += "\n" + user.toString();
        return res.substring(1);
    }
}
