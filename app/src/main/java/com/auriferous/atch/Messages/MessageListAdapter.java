package com.auriferous.atch.Messages;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.auriferous.atch.Callbacks.SimpleCallback;
import com.auriferous.atch.ParseAndFacebookUtils;
import com.auriferous.atch.R;
import com.auriferous.atch.Users.Group;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class MessageListAdapter extends BaseAdapter {
    private final Context context;
    private ListView listView;

    private ParseObject messageHistory;
    private MessageList messageList;
    private ParseUser primaryUser;
    private Group recipientUsers;
    private String emptyMessage;
    private SimpleCallback viewRefreshCallback;


    public MessageListAdapter(Context context, ListView listView, ParseObject messageHistory, MessageList messageList, ParseUser primaryUser, Group recipientUsers, String emptyMessage, SimpleCallback viewRefreshCallback) {
        this.context = context;
        this.listView = listView;
        this.messageHistory = messageHistory;
        this.messageList = messageList;
        this.primaryUser = primaryUser;
        this.recipientUsers = recipientUsers;
        this.emptyMessage = emptyMessage;
        this.viewRefreshCallback = viewRefreshCallback;
    }


    @Override
    public int getCount() {
        if(messageList == null || messageList.getAllNonResponseMessages() == null || messageList.getAllNonResponseMessages().size() == 0) return 1;
        return messageList.getAllNonResponseMessages().size();
    }
    @Override
    public Message getItem(int position) {
        if(messageList == null || messageList.getAllNonResponseMessages() == null || messageList.getAllNonResponseMessages().size() == 0 || position >= messageList.getAllNonResponseMessages().size()) return null;
        return messageList.getAllNonResponseMessages().get(position);
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(messageList == null || messageList.getAllNonResponseMessages() == null || messageList.getAllNonResponseMessages().size() == 0)
            return createFullscreenLabelView(parent);
        if (position >= messageList.getAllNonResponseMessages().size()) return null;

        Message thisMessage = messageList.getAllNonResponseMessages().get(position);
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
                                if (!(listView.getLastVisiblePosition() == listView.getAdapter().getCount() - 1 && listView.getChildAt(listView.getChildCount() - 1).getBottom() <= listView.getHeight())) {
                                    listView.scrollListBy(v.getMeasuredHeight() - initalHeight);
                                }
                                ViewTreeObserver obs = v.getViewTreeObserver();
                                obs.removeOnGlobalLayoutListener(this);
                            }
                        });
                    }
                });
                break;
            }
            case 'h':
            case 't': {
                chatView = createMeetHereOrThereChat(thisMessage, parent);
                break;
            }
            case 'r':
            default: {
                break;
            }
        }

        return chatView;
    }
    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        listView.setSelection(getCount() - 1);
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
    private View createMeetHereOrThereChat(final Message message, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView;
        boolean fromCurrUser = message.getSender().getObjectId().equals(primaryUser.getObjectId());

        if(message.getDecorationFlag() == 'h') {
            if (fromCurrUser)
                rowView = inflater.inflate(R.layout.meet_here_sent, parent, false);
            else
                rowView = inflater.inflate(R.layout.meet_here_received, parent, false);
        }
        else {
            if (fromCurrUser)
                rowView = inflater.inflate(R.layout.meet_there_sent, parent, false);
            else
                rowView = inflater.inflate(R.layout.meet_there_received, parent, false);
        }

        if(!fromCurrUser)
            rowView.findViewById(R.id.inner_view).setBackgroundColor(recipientUsers.getLighterColor(message.getSenderId()));

        TextView label = (TextView) rowView.findViewById(R.id.message);
        label.setText(message.getDecoratedMessageText(recipientUsers, fromCurrUser));
        TextView date = (TextView) rowView.findViewById(R.id.date);
        SimpleDateFormat formatter = new SimpleDateFormat("h:mm a");
        date.setText(formatter.format(message.getSendDate()));

        ArrayList<Message> responses = messageList.getResponses(message.getObjectId());
        boolean existsResponseFromCurrentUser = false;
        for (Message response : responses) {
            boolean thisMessageFromCurrUser = false;
            if (response.getSender().getObjectId().equals(ParseUser.getCurrentUser().getObjectId())) {
                existsResponseFromCurrentUser = true;
                thisMessageFromCurrUser = true;
            }

            LinearLayout responseArea = (LinearLayout) rowView.findViewById(R.id.response_layout);
            View responseView = inflater.inflate(R.layout.response, responseArea, true);
            TextView responseLabel = (TextView) responseView.findViewById(R.id.response_message);
            responseLabel.setText(response.getDecoratedMessageText(recipientUsers, thisMessageFromCurrUser));
            TextView responseDate = (TextView) responseView.findViewById(R.id.response_date);
            responseDate.setText(formatter.format(response.getSendDate()));
        }
        if (!existsResponseFromCurrentUser) {
            Button okButton = (Button) rowView.findViewById(R.id.ok_button);
            if(okButton != null)
                okButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        ParseAndFacebookUtils.sendMessage(messageHistory, message.getObjectId() + "_atch", 'r', viewRefreshCallback);
                        messageList.addFakeMessage(message.getObjectId() + "_atch", 'r', ParseUser.getCurrentUser());
                        ((MessageListAdapter) listView.getAdapter()).notifyDataSetChanged();
                    }
                });
            Button busyButton = (Button) rowView.findViewById(R.id.busy_button);
            if(busyButton != null)
                busyButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        ParseAndFacebookUtils.sendMessage(messageHistory, message.getObjectId() + "_busy", 'r', viewRefreshCallback);
                        messageList.addFakeMessage(message.getObjectId() + "_busy", 'r', ParseUser.getCurrentUser());
                        ((MessageListAdapter) listView.getAdapter()).notifyDataSetChanged();
                    }
                });
        } else {
            RelativeLayout buttonArea = (RelativeLayout)rowView.findViewById(R.id.response_options);
            if(buttonArea != null)
                buttonArea.setVisibility(View.GONE);
        }

        return rowView;
    }
}