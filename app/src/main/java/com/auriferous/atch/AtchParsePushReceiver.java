package com.auriferous.atch;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.auriferous.atch.Activities.AddFriendsActivity;
import com.auriferous.atch.Activities.BaseFriendsActivity;
import com.auriferous.atch.Activities.LoginActivity;
import com.auriferous.atch.Activities.MapActivity;
import com.auriferous.atch.Activities.ViewFriendsActivity;
import com.auriferous.atch.Users.User;
import com.parse.ParseAnalytics;
import com.parse.ParsePushBroadcastReceiver;

import org.json.JSONException;
import org.json.JSONObject;

public class AtchParsePushReceiver extends ParsePushBroadcastReceiver {
    private static final int NOTIFICATION_ID = 492304;

    private static AtchApplication app = null;


    public static void init(AtchApplication app) {
        AtchParsePushReceiver.app = app;
    }

    public static void cancelAllNotifications(Context context){
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }


    @Override
    protected void onPushReceive(final Context context, final Intent intent) {
        if(app != null) {
            app.updateView();

            String type = null;
            String chatRecipientObjectId = null;
            String friendRecipientObjectId = null;
            String atchage = null;

            try {
                JSONObject cls = new JSONObject(intent.getStringExtra("com.parse.Data"));
                type = cls.optString("type", null);
                chatRecipientObjectId = cls.optString("chatterParseId", null);
                friendRecipientObjectId = cls.optString("frienderParseId", null);
                atchage = cls.optString("alert", null);
            } catch (JSONException var6) {
            }

            Activity currActivity = app.getCurrentActivity();
            if (currActivity != null && chatRecipientObjectId != null && currActivity instanceof MapActivity && ((MapActivity) currActivity).isChattingWithPerson(chatRecipientObjectId)){
                ((MapActivity) currActivity).refreshChatHistory();
            }
            else if(app.isOnlineAndAppOpen() && currActivity instanceof BaseFriendsActivity){
                User user = null;
                String message ="";
                if(type.equals("message")) {
                    user = User.getUserFromMap(chatRecipientObjectId);
                    if(atchage != null)
                        message = atchage;
                    else
                        message = "new atchage from " + user.getUsername();
                }
                else if(type.equals("friendRequest")) {
                    user = User.getUserFromMap(friendRecipientObjectId);
                    message = "Friend request from " + user.getUsername();
                }
                else if(type.equals("friendAccept")) {
                    user = User.getUserFromMap(friendRecipientObjectId);
                    message = user.getUsername() + "accepted your friend request";
                }
                ((BaseFriendsActivity)currActivity).createNotification(message, (user != null)?user.getRelativeColor():-1, getClassToOpen(type),
                        createIntent(context, getClassToOpen(type), intent, type, chatRecipientObjectId, friendRecipientObjectId));
            }
            else {
                setupAndDeliverNotification(context, intent);
            }
        }
        else {
            setupAndDeliverNotification(context, intent);
        }
    }
    private void setupAndDeliverNotification(Context context, Intent intent) {
        JSONObject pushData = null;
        try {
            pushData = new JSONObject(intent.getStringExtra("com.parse.Data"));
        } catch (JSONException jE) {}

        String action = null;
        if (pushData != null)
            action = pushData.optString("action", null);

        if (action != null) {
            Bundle notification = intent.getExtras();
            Intent broadcastIntent = new Intent();
            broadcastIntent.putExtras(notification);
            broadcastIntent.setAction(action);
            broadcastIntent.setPackage(context.getPackageName());
            context.sendBroadcast(broadcastIntent);
        }

        Notification notification = getNotification(context, intent);
        if(context != null && notification != null) {
            NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

            try {
                nm.notify(NOTIFICATION_ID, notification);
            } catch (SecurityException var6) {
                notification.defaults = 5;
                nm.notify(NOTIFICATION_ID, notification);
            }
        }
    }

    @Override
    protected void onPushOpen(Context context, Intent intent) {
        ParseAnalytics.trackAppOpenedInBackground(intent);

        if(!app.isSetupComplete()){
            Intent activityIntent = new Intent(context, LoginActivity.class);
            activityIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activityIntent.addFlags(268435456);
            activityIntent.addFlags(67108864);
            context.startActivity(activityIntent);
            return;
        }

        String type = null;
        String chatRecipientObjectId = null;
        String friendRecipientObjectId = null;

        try {
            JSONObject cls = new JSONObject(intent.getStringExtra("com.parse.Data"));
            type = cls.optString("type", null);
            chatRecipientObjectId = cls.optString("chatterParseId", null);
            friendRecipientObjectId = cls.optString("frienderParseId", null);
        }
        catch (JSONException var6) {}

        Intent activityIntent = createIntent(context, getClassToOpen(type), intent, type, chatRecipientObjectId, friendRecipientObjectId);
        context.startActivity(activityIntent);
    }

    private Class getClassToOpen(String type){
        if(type.equals("message"))
            return MapActivity.class;
        else if(type.equals("friendRequest"))
            return AddFriendsActivity.class;
        else if(type.equals("friendAccept"))
            return ViewFriendsActivity.class;
        return null;
    }



    private Intent createIntent(Context context, Class resultClass, Intent oldIntent, String type, String chatterParseId, String frienderParseId) {
        Intent activityIntent = new Intent(context, resultClass);
        activityIntent.putExtras(oldIntent.getExtras());
        activityIntent.putExtra("type", type);
        activityIntent.putExtra("chatterParseId", chatterParseId);
        activityIntent.putExtra("frienderParseId", frienderParseId);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activityIntent.addFlags(268435456);
        activityIntent.addFlags(67108864);

        return activityIntent;
    }
}
