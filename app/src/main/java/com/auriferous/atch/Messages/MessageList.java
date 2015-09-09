package com.auriferous.atch.Messages;

import com.parse.ParseObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MessageList {
    private ArrayList<Message> messages = new ArrayList<>();
    private ArrayList<Message> nonResponseMessages = new ArrayList<>();


    public MessageList(List<ParseObject> messages) {
        this(messages, false);
    }
    public MessageList(List<ParseObject> messages, boolean shouldReverse) {
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


    public ArrayList<Message> getAllNonResponseMessages() {
        return nonResponseMessages;
    }

    public Message getResponse(String originalId) {
        for(Message message : messages)
            if (message.getDecorationFlag() == 'r' && message.getMessageText().startsWith(originalId))
                return message;
        return null;
    }
}
