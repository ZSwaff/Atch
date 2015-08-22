package com.auriferous.atch.Messages;

import com.parse.ParseObject;
import com.parse.ParseUser;

public class Message {
    private ParseObject parseMessage;


    public Message(ParseObject parseMessage) {
        this.parseMessage = parseMessage;
    }


    public ParseObject getParseMessage() {
        return parseMessage;
    }
    public void setParseMessage(ParseObject parseMessage) {
        this.parseMessage = parseMessage;
    }

    public String getMessageText() {
        return parseMessage.getString("messageText");
    }
    public ParseUser getSender() {
        return parseMessage.getParseUser("fromUser");
    }
}
