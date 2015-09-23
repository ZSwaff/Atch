package com.atchapp.atch.Users;

import java.util.ArrayList;

public class UserListAdapterSection {
    private String label;
    private UserList users = null;
    private ArrayList<Group> groups = null;


    public UserListAdapterSection(String label, UserList users){
        this.label = label;
        this.users = users;
    }
    public UserListAdapterSection(String label, ArrayList<Group> groups){
        this.label = label;
        this.groups = groups;
    }


    public boolean isUsers() {
        return (users != null);
    }
    public int size(){
        if(users != null)
            return users.getAllUsers().size();
        return groups.size();
    }
    public UserList getUsers() {
        return users;
    }
    public ArrayList<Group> getGroups() {
        return groups;
    }
    public String getLabel() {
        return label;
    }
}
