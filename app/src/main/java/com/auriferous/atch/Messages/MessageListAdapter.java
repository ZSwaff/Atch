package com.auriferous.atch.Messages;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.auriferous.atch.R;
import com.auriferous.atch.Users.Group;
import com.parse.ParseUser;

import java.text.SimpleDateFormat;

public class MessageListAdapter extends BaseAdapter {
    private final Context context;
    private ListView listView;

    private MessageList messageList;
    private ParseUser primaryUser;
    private Group recipientUsers;
    private String emptyMessage;


    public MessageListAdapter(Context context, ListView listView, MessageList messageList, ParseUser primaryUser, Group recipientUsers, String emptyMessage, MessageListAdapter oldAdapter) {
        this.context = context;
        this.listView = listView;
        this.messageList = messageList;
        this.primaryUser = primaryUser;
        this.recipientUsers = recipientUsers;
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

        Message thisMessage = messageList.getAllMessages().get(position);
        View chatView = null;
        switch(thisMessage.getDecorationFlag()){
            case 'n': {
                chatView = createNormalChat(thisMessage, parent);

                if(!thisMessage.getSender().getObjectId().equals(primaryUser.getObjectId())){
                    View background = chatView.findViewById(R.id.background);
                    background.setBackgroundResource(R.drawable.chat_left_background);
                    GradientDrawable bg = (GradientDrawable)background.getBackground();
                    bg.setColor(recipientUsers.getLighterColor(thisMessage.getSenderId()));
                }

                final View finalChatView = chatView;
                finalChatView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        TextView date = (TextView)finalChatView.findViewById(R.id.date);
                        final int initalHeight = v.getMeasuredHeight();

                        if(date.getVisibility() == View.GONE)
                            date.setVisibility(View.VISIBLE);
                        else
                            date.setVisibility(View.GONE);

                        ViewTreeObserver vto = v.getViewTreeObserver();
                        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                listView.scrollBy(0, v.getMeasuredHeight() - initalHeight);
                                ViewTreeObserver obs = v.getViewTreeObserver();
                                obs.removeOnGlobalLayoutListener(this);
                            }
                        });
                    }
                });
                break;
            }
            case 'h': {
                chatView = createMeetHereChat(thisMessage, parent);
                chatView.setBackgroundColor(recipientUsers.getLighterColor(thisMessage.getSenderId()));
                break;
            }
            case 't': {
                chatView = createMeetThereChat(thisMessage, parent);
                chatView.setBackgroundColor(recipientUsers.getLighterColor(thisMessage.getSenderId()));
                break;
            }
        }

        return chatView;
    }

    private View createFullscreenLabelView(ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.fullscreen_label, parent, false);

        TextView label = (TextView) rowView.findViewById(R.id.label);
        label.setText(emptyMessage);

        return rowView;
    }
    private View createNormalChat(Message message, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView;
        if(message.getSender().getObjectId().equals(primaryUser.getObjectId()))
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
    private View createMeetHereChat(Message message, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView;
        if(message.getSender().getObjectId().equals(primaryUser.getObjectId()))
            rowView = inflater.inflate(R.layout.meet_here_sent, parent, false);
        else
            rowView = inflater.inflate(R.layout.meet_there_sent, parent, false);

        TextView label = (TextView) rowView.findViewById(R.id.message);
        label.setText(message.getMessageText());
        TextView date = (TextView) rowView.findViewById(R.id.date);
        SimpleDateFormat formatter = new SimpleDateFormat("h:mm a");
        date.setText(formatter.format(message.getSendDate()));

        return rowView;
    }
    private View createMeetThereChat(Message message, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView;
        if(message.getSender().getObjectId().equals(primaryUser.getObjectId()))
            rowView = inflater.inflate(R.layout.meet_there_sent, parent, false);
        else
            rowView = inflater.inflate(R.layout.meet_there_received, parent, false);

        TextView label = (TextView) rowView.findViewById(R.id.message);
        label.setText(message.getMessageText());
        TextView date = (TextView) rowView.findViewById(R.id.date);
        SimpleDateFormat formatter = new SimpleDateFormat("h:mm a");
        date.setText(formatter.format(message.getSendDate()));

        return rowView;
    }
}