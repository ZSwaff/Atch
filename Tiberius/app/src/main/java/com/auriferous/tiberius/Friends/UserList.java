package com.auriferous.tiberius.Friends;

import com.parse.ParseUser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserList implements Serializable {
    private ArrayList<User> users = new ArrayList<User>();


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

    @Override
    public String toString() {
        String res = "";
        for(User user : users)
            res += "\n" + user.toString();
        return res.substring(1);
    }
}
