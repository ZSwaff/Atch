package com.atchapp.atch.Messages;

import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MessageList {
    private ParseObject messageHistory = null;
    private ArrayList<Message> messages = new ArrayList<>();
    private ArrayList<Message> nonResponseMessages = new ArrayList<>();


    public MessageList(ParseObject messageHistory, List<ParseObject> messages) {
        this(messageHistory, messages, false);
    }
    public MessageList(ParseObject messageHistory, List<ParseObject> messages, boolean shouldReverse) {
        this.messageHistory = messageHistory;

        for(ParseObject pObj : messages) {
            Message nextMessage = new Message(pObj);
            this.messages.add(nextMessage);
            if(nextMessage.getDecorationFlag() != 'r')
                nonResponseMessages.add(nextMessage);
        }

        if(shouldReverse) {
            Collections.reverse(messages);
            Collections.reverse(nonResponseMessages);
        }
    }


    public void addFakeMessage(String messageText, char decorationFlag, ParseUser fromUser) {
        Message newMessage = new Message(messageText, decorationFlag, fromUser);
        messages.add(newMessage);
        if (decorationFlag != 'r')
            nonResponseMessages.add(newMessage);
    }


    public ParseObject getMessageHistory() {
        return messageHistory;
    }
    public void setMessageHistory(ParseObject messageHistory) {
        this.messageHistory = messageHistory;
    }

    public int getUnreadCount(ParseUser parseUser) {
        Date lastCheckinDate = getLastCheckin(parseUser);
        if (lastCheckinDate == null) return 0;

        int unreadCount = 0;
        for (Message message : messages) {
            Date messageDate = message.getSendDate();
            if (messageDate.after(lastCheckinDate)) unreadCount++;
        }
        return unreadCount;
    }
    public Date getLastCheckin(ParseUser parseUser) {
        int index = -1;
        String[] userIds = messageHistory.getString("name").substring(12).split("_");
        for (int i = 0; i < userIds.length; i++)
            if (userIds[i].equals(parseUser.getObjectId())) {
                index = i;
                break;
            }
        if (index == -1) return null;

        List<Date> lastCheckins = messageHistory.getList("lastCheckins");
        if (lastCheckins == null) return null;
        Object ret = lastCheckins.get(index);
        if (ret instanceof String) return null;
        return (Date) ret;
    }

    public ArrayList<Message> getAllNonResponseMessages() {
        return nonResponseMessages;
    }
    public ArrayList<Message> getResponses(String originalId) {
        if (originalId == null) return new ArrayList<>();
        ArrayList<Message> ret = new ArrayList<>();
        for(Message message : messages)
            if (message.getDecorationFlag() == 'r' && message.getMessageText().startsWith(originalId))
                ret.add(message);
        return ret;
    }
}
