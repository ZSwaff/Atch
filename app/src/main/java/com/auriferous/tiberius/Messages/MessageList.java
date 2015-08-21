package com.auriferous.tiberius.Messages;

import com.parse.ParseObject;

import java.util.ArrayList;
import java.util.List;

public class MessageList {
    private ArrayList<Message> messages = new ArrayList<>();


    public MessageList() {}
    public MessageList(List<ParseObject> messages) {
        for(ParseObject pObj : messages)
            this.messages.add(new Message(pObj));
    }


    public ArrayList<Message> getAllMessages() {
        return messages;
    }
    public void setAllMessages(ArrayList<Message> messages) {
        this.messages = messages;
    }
}
