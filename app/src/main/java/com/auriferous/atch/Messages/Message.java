package com.auriferous.atch.Messages;

import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.Date;

public class Message {
    private ParseObject parseMessage;


    public Message(ParseObject parseMessage) {
        this.parseMessage = parseMessage;
    }


    public String getMessageText() {
        return parseMessage.getString("messageText").trim();
    }
    public char getDecorationFlag() {
        String flag = parseMessage.getString("decorationFlag");
        if(flag == null) return 'n';
        return flag.charAt(0);
    }
    public ParseUser getSender() {
        return parseMessage.getParseUser("fromUser");
    }
    public Date getSendDate() {
        return parseMessage.getCreatedAt();
    }
}
