package com.atchapp.atch.Users;

import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class UserList{
    private ArrayList<User> users = new ArrayList<>();
    private User.UserType userType = User.UserType.RANDOM;

    private ArrayList<Group> friendGroups = new ArrayList<>();


    public UserList(User.UserType userType) {
        this.userType = userType;
    }
    public UserList(List<ParseUser> users, User.UserType userType) {
        this.userType = userType;
        for(ParseUser pUsr : users)
            this.users.add(User.getOrCreateUser(pUsr, userType));
    }
    public UserList(User.UserType userType, ArrayList<User> users) {
        this.userType = userType;
        this.users = users;
    }


    public UserList getOnline(){
        ArrayList<User> onlineUsers = new ArrayList<>();
        for(User user : users)
            if(user.isLoggedIn())
                onlineUsers.add(user);
        return new UserList(userType, onlineUsers);
    }
    public UserList getOffline(){
        ArrayList<User> offlineUsers = new ArrayList<>();
        for(User user : users)
            if(!user.isLoggedIn())
                offlineUsers.add(user);
        return new UserList(userType, offlineUsers);
    }

    public ArrayList<User> getAllUsers() {
        return users;
    }
    public ArrayList<Group> getAllGroups() {
        return friendGroups;
    }
    public ArrayList<Group> getAllGroupsWithMoreThanOnePerson() {
        ArrayList<Group> ret = new ArrayList<>();
        for(Group group : friendGroups)
            if(group.size() > 1)
                ret.add(group);
        return ret;
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


    public void updateFriendGroups(){
        friendGroups = Group.getGroups(this);
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
