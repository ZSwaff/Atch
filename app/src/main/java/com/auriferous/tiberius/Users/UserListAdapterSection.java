package com.auriferous.tiberius.Users;

public class UserListAdapterSection {
    private String label;
    private UserList users;


    public UserListAdapterSection(String label, UserList users){
        this.label = label;
        this.users = users;
    }

    public String getLabel() {
        return label;
    }
    public UserList getUsers() {
        return users;
    }
}
