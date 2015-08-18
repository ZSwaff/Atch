package com.auriferous.tiberius;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.auriferous.tiberius.Users.User;

import java.util.ArrayList;

public class AddFriendScreenListAdapter extends BaseAdapter {
    private final Context context;

    private String topLabel = "";
    private final ArrayList<User> topHalf;
    private String bottomLabel = "";
    private final ArrayList<User> bottomHalf;

    public AddFriendScreenListAdapter(Context context, String topLabel, ArrayList<User> topHalf, String bottomLabel, ArrayList<User> bottomHalf) {
        this.context = context;
        this.topLabel = topLabel;
        this.topHalf = topHalf;
        this.bottomLabel = bottomLabel;
        this.bottomHalf = bottomHalf;
    }

    public int getCount() {
        return topHalf.size() + bottomHalf.size() + ((topHalf.size() == 0)?1:3);
    }

    public User getItem(int position) {
        if(topHalf.size() != 0){
            if(position == 0) return null;
            position--;
            if(position < topHalf.size()) topHalf.get(position);
            position -= topHalf.size();
            if(position == 0) return null;
            position--;
        }
        if(position == 0) return null;
        position--;
        return bottomHalf.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        if(topHalf.size() != 0) {
            if (position == 0)
                return createLabelView(topLabel, parent);
            position--;
            if (position < topHalf.size())
                return createUserView(topHalf.get(position), R.layout.pending_request_list_item, parent);
            position -= topHalf.size();
            if (position == 0) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                return inflater.inflate(R.layout.divider, parent, false);
            }
            position--;
        }
        if(position == 0)
            return createLabelView(bottomLabel, parent);
        position--;
        if(position < bottomHalf.size())
            return createUserView(bottomHalf.get(position), R.layout.facebook_friend_list_item, parent);
        return null;
    }

    private View createLabelView(String text, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.label, parent, false);

        TextView label = (TextView) rowView.findViewById(R.id.label);
        label.setText(text);

        return rowView;
    }

    private View createUserView(User user, int layout, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
                    MyParseFacebookUtils.sendFriendRequest(uid);
                }
            });

        Button acceptButton = (Button) rowView.findViewById(R.id.accept_button);
        if(acceptButton != null)
            acceptButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    MyParseFacebookUtils.acceptFriendRequest(uid);
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