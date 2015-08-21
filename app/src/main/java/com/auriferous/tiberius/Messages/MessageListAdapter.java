package com.auriferous.tiberius.Messages;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.auriferous.tiberius.ParseAndFacebookUtils;
import com.auriferous.tiberius.R;
import com.auriferous.tiberius.Users.User;
import com.auriferous.tiberius.Users.UserListAdapterSection;
import com.parse.ParseUser;

import java.util.ArrayList;

public class MessageListAdapter extends BaseAdapter {
    private final Context context;
    private MessageList messageList;
    private ParseUser currentUser;


    public MessageListAdapter(Context context, MessageList messageList, ParseUser currentUser) {
        this.context = context;
        this.messageList = messageList;
        this.currentUser = currentUser;
    }


    @Override
    public int getCount() {
        if(messageList == null || messageList.getAllMessages() == null) return 0;
        return messageList.getAllMessages().size();
    }
    @Override
    public Message getItem(int position) {
        if(messageList == null || messageList.getAllMessages() == null || position >= messageList.getAllMessages().size()) return null;
        return messageList.getAllMessages().get(position);
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(messageList == null || messageList.getAllMessages() == null || position >= messageList.getAllMessages().size()) return null;
        return createLabelView(messageList.getAllMessages().get(position), parent);
    }


    private View createLabelView(Message message, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView;
        if(message.getSender().getObjectId().equals(currentUser.getObjectId()))
            rowView = inflater.inflate(R.layout.chat_bubble_right, parent, false);
        else
            rowView = inflater.inflate(R.layout.chat_bubble_left, parent, false);

        TextView label = (TextView) rowView.findViewById(R.id.label);
        label.setText(message.getMessageText());

        return rowView;
    }
}