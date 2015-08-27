package com.auriferous.atch.Messages;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.auriferous.atch.R;
import com.parse.ParseUser;

import java.text.SimpleDateFormat;

public class MessageListAdapter extends BaseAdapter {
    private final Context context;
    private ListView listView;

    private MessageList messageList;
    private ParseUser currentUser;
    private String emptyMessage;


    public MessageListAdapter(Context context, ListView listView, MessageList messageList, ParseUser currentUser, String emptyMessage) {
        this.context = context;
        this.listView = listView;
        this.messageList = messageList;
        this.currentUser = currentUser;
        this.emptyMessage = emptyMessage;
    }


    @Override
    public int getCount() {
        if(messageList == null || messageList.getAllMessages() == null || messageList.getAllMessages().size() == 0) return 1;
        return messageList.getAllMessages().size();
    }
    @Override
    public Message getItem(int position) {
        if(messageList == null || messageList.getAllMessages() == null || messageList.getAllMessages().size() == 0 || position >= messageList.getAllMessages().size()) return null;
        return messageList.getAllMessages().get(position);
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(messageList == null || messageList.getAllMessages() == null || messageList.getAllMessages().size() == 0)
            return createFullscreenLabelView(parent);
        if (position >= messageList.getAllMessages().size()) return null;

        final View chatBubble = createLabelView(messageList.getAllMessages().get(position), parent);
        chatBubble.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView date = (TextView)chatBubble.findViewById(R.id.date);
                if(date.getVisibility() == View.GONE){
                    int h = v.getMeasuredHeight();
                    date.setVisibility(View.VISIBLE);
                    v.invalidate();
                    listView.scrollBy(0, h - v.getMeasuredHeight());
                }
                else{
                    int h = v.getMeasuredHeight();
                    date.setVisibility(View.GONE);
                    v.invalidate();
                    listView.scrollBy(0, h - v.getMeasuredHeight());
                }
            }
        });
        return chatBubble;
    }

    private View createFullscreenLabelView(ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.fullscreen_label, parent, false);

        TextView label = (TextView) rowView.findViewById(R.id.label);
        label.setText(emptyMessage);

        return rowView;
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
        TextView date = (TextView) rowView.findViewById(R.id.date);
        SimpleDateFormat formatter = new SimpleDateFormat("h:mm a");
        date.setText(formatter.format(message.getSendDate()));

        return rowView;
    }
}