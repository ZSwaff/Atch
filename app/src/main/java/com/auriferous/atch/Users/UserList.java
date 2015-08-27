package com.auriferous.atch.Users;

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

    public void addUser(User user) {
        if (!users.contains(user))
            users.add(user);
    }
    public void removeUser(User user) {
        users.remove(user);
    }

    public void addDataToUnknownUser(ParseObject privateDatum){
        ParseUser goalUser = privateDatum.getParseUser("user");
        String goalId = goalUser.getObjectId();

        for(User user : users){
            if(user.getId().equals(goalId))
                user.setPrivateData(privateDatum);
        }
    }


    public void sortByPriorityForSearch() {
        ArrayList<User> friends = new ArrayList<>();
        ArrayList<User> pendingYou = new ArrayList<>();
        ArrayList<User> fbFriends = new ArrayList<>();
        ArrayList<User> other = new ArrayList<>();

        for (User user : users) {
            switch (user.getUserType()){
                case FRIEND:
                    friends.add(user);
                    break;
                case PENDING_YOU:
                    pendingYou.add(user);
                    break;
                case FACEBOOK_FRIEND:
                    fbFriends.add(user);
                    break;
                default:
                    other.add(user);
                    break;
            }
        }

        users = friends;
        users.addAll(pendingYou);
        users.addAll(fbFriends);
        users.addAll(other);
    }
}
