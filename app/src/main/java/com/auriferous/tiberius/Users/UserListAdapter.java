package com.auriferous.tiberius.Users;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.auriferous.tiberius.ParseAndFacebookUtils;
import com.auriferous.tiberius.R;

import java.util.ArrayList;

public class UserListAdapter extends BaseAdapter {
    private final Context context;
    private ArrayList<UserListAdapterSection> sections;


    public UserListAdapter(Context context, UserListAdapterSection section) {
        this.context = context;
        sections = new ArrayList<>();
        sections.add(section);
    }
    public UserListAdapter(Context context, ArrayList<UserListAdapterSection> sections) {
        this.context = context;
        this.sections = sections;
    }


    @Override
    public int getCount() {
        int count = 0;
        for(UserListAdapterSection section : sections) {
            int size = section.getUsers().getAllUsers().size();
                if(size != 0) count++;
            count += size;
        }
        return count;
    }
    @Override
    public User getItem(int position) {
        for(UserListAdapterSection section : sections){
            ArrayList<User> users = section.getUsers().getAllUsers();

            if (users.size() == 0) continue;
            if (position == 0) return null;
            position--;

            if(position < users.size())
                return users.get(position);
            position -= users.size();
        }
        return null;
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        for(UserListAdapterSection section : sections){
            ArrayList<User> users = section.getUsers().getAllUsers();

            if (users.size() == 0) continue;
            if (position == 0)
                return createLabelView(section.getLabel(), parent);
            position--;

            if(position < users.size())
                return createUserView(users.get(position), parent);
            position -= users.size();
        }
        return null;
    }


    private View createLabelView(String text, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.label, parent, false);

        TextView label = (TextView) rowView.findViewById(R.id.label);
        label.setText(text);

        return rowView;
    }

    private View createUserView(User user, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        int layout = 0;
        switch(user.getUserType()){
            case FRIEND:
                layout = R.layout.friend_list_item;
                break;
            case PENDING_YOU:
                layout = R.layout.pending_you_list_item;
                break;
            case PENDING_THEM:
                layout = R.layout.pending_them_list_item;
                break;
            case FACEBOOK_FRIEND:
                layout = R.layout.facebook_friend_list_item;
                break;
            case RANDOM:
                layout = R.layout.random_list_item;
                break;
        }
        View rowView = inflater.inflate(layout, parent, false);

        TextView fullname = (TextView) rowView.findViewById(R.id.fullname);
        TextView username = (TextView) rowView.findViewById(R.id.username);

        fullname.setText(user.getFullname());
        username.setText(user.getUsername());

        final String uid = user.getId();

        Button friendButton = (Button) rowView.findViewById(R.id.friend_button);
        if(friendButton != null)
            friendButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ParseAndFacebookUtils.sendFriendRequest(uid);
                }
            });

        Button acceptButton = (Button) rowView.findViewById(R.id.accept_button);
        if(acceptButton != null)
            acceptButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ParseAndFacebookUtils.acceptFriendRequest(uid);
                }
            });

        Bitmap profPic = user.getProfPic();
        if(profPic != null) {
            ImageView imageView = (ImageView) rowView.findViewById(R.id.prof_pic);
            imageView.setImageBitmap(profPic);
        }

        return rowView;
    }
}