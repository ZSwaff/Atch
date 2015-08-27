package com.auriferous.atch.Messages;

import com.parse.ParseObject;

import java.util.ArrayList;
import java.util.List;

public class MessageList {
    private ArrayList<Message> messages = new ArrayList<>();


    public MessageList(List<ParseObject> messages) {
        this(messages, false);
    }
    public MessageList(List<ParseObject> messages, boolean shouldReverse) {
        if(!shouldReverse){
            for(ParseObject pObj : messages)
                this.messages.add(new Message(pObj));
        }
        else {
            for(int i = messages.size() - 1; i >= 0; i--)
                this.messages.add(new Message(messages.get(i)));
        }
    }


    public ArrayList<Message> getAllMessages() {
        return messages;
    }
}
