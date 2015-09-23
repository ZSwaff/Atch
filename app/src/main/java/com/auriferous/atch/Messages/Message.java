package com.auriferous.atch.Messages;

import com.auriferous.atch.Users.Group;
import com.auriferous.atch.Users.User;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.Date;

public class Message {
    private ParseObject parseMessage;
    private Date sendDate = null;


    public Message(ParseObject parseMessage) {
        this.parseMessage = parseMessage;
    }
    public Message(String messageText, char decorationFlag, ParseUser fromUser) {
        parseMessage = new ParseObject("Message");
        parseMessage.put("messageText", messageText);
        parseMessage.put("decorationFlag", decorationFlag + "");
        parseMessage.put("fromUser", fromUser);
        sendDate = new Date();
    }

    public String getObjectId() {
        return parseMessage.getObjectId();
    }
    public String getMessageText() {
        return parseMessage.getString("messageText").trim();
    }
    public String getDecoratedMessageText(Group otherUsers, boolean fromCurrUser) {
        String originalText = getMessageText();
        switch (getDecorationFlag()){
            case 'h': {
                if(fromCurrUser)
                    return "You suggest that " + otherUsers.getNamesAsNiceList() + " come over";
                else
                    return User.getUserFromMap(getSenderId()).getFirstname() + " suggests that " + otherUsers.getSHeTheyPronoun() + " come over";
            }
            case 't': {
                if(fromCurrUser)
                    return "You suggest that you go to " + otherUsers.getNamesAsNiceList();
                else
                    return User.getUserFromMap(getSenderId()).getFirstname() + " suggests that you come over";
            }
            case 'r': {
                String responseText = originalText.split("_")[1];
                if(responseText.equals("atch"))
                    responseText += "!";
                if(fromCurrUser)
                    return "You are " + responseText;
                else
                    return otherUsers.getNamesAsNiceList() + " is " + responseText;
            }
            case 'n':
            default: {
                return originalText;
            }
        }
    }
    public char getDecorationFlag() {
        String flag = parseMessage.getString("decorationFlag");
        if(flag == null) return 'n';
        return flag.charAt(0);
    }
    public ParseUser getSender() {
        return parseMessage.getParseUser("fromUser");
    }
    public String getSenderId() {
        return parseMessage.getParseUser("fromUser").getObjectId();
    }
    public Date getSendDate() {
        if (sendDate != null) return sendDate;
        return parseMessage.getCreatedAt();
    }
}
