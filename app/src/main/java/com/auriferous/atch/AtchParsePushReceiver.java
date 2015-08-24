package com.auriferous.atch;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.auriferous.atch.Activities.AddFriendsActivity;
import com.auriferous.atch.Activities.ChatActivity;
import com.auriferous.atch.Activities.ViewFriendsActivity;
import com.parse.ParseAnalytics;
import com.parse.ParsePushBroadcastReceiver;

import org.json.JSONException;
import org.json.JSONObject;

public class AtchParsePushReceiver extends ParsePushBroadcastReceiver {
    public static final int NOTIFICATION_ID = 492304;

    static AtchApplication app = null;


    static void init(AtchApplication app) {
        AtchParsePushReceiver.app = app;
    }

    public static void cancelAllNotifications(Context context){
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }


    @Override
    protected void onPushReceive(Context context, Intent intent) {
        if(app != null) {
            app.updateView();

            String chatRecipientObjectId = null;
            try {
                JSONObject cls = new JSONObject(intent.getStringExtra("com.parse.Data"));
                chatRecipientObjectId = cls.optString("chatterParseId", null);
            }
            catch (JSONException jE) {}

            Activity currActivity = app.getCurrentActivity();
            if(currActivity != null) {
                if(currActivity instanceof ChatActivity){
                    if((chatRecipientObjectId != null && ((ChatActivity)currActivity).getChatterObjectId().equals(chatRecipientObjectId)))
                        ((ChatActivity)currActivity).setupChatHistory();
                    else
                        setupAndDeliverNotification(context, intent);
                }
                else
                    setupAndDeliverNotification(context, intent);
            }
            else
                setupAndDeliverNotification(context, intent);
        }
        else
            setupAndDeliverNotification(context, intent);
    }

    private void setupAndDeliverNotification(Context context, Intent intent) {
        JSONObject pushData = null;

        try {
            pushData = new JSONObject(intent.getStringExtra("com.parse.Data"));
        } catch (JSONException jE) {}

        String action = null;
        if (pushData != null) {
            action = pushData.optString("action", null);
        }

        if (action != null) {
            Bundle notification = intent.getExtras();
            Intent broadcastIntent = new Intent();
            broadcastIntent.putExtras(notification);
            broadcastIntent.setAction(action);
            broadcastIntent.setPackage(context.getPackageName());
            context.sendBroadcast(broadcastIntent);
        }

        Notification notification = this.getNotification(context, intent);
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
        String type = null;

        String chatRecipientObjectId = null;
        String friendRecipientObjectId = null;

        try {
            JSONObject cls = new JSONObject(intent.getStringExtra("com.parse.Data"));
            chatRecipientObjectId = cls.optString("chatterParseId", null);
            friendRecipientObjectId = cls.optString("frienderParseId", null);
            type = cls.optString("type", null);
        }
        catch (JSONException var6) {}

        if(type.equals("message")) {
            if (chatRecipientObjectId != null) {
                Intent activityIntent = new Intent(context, ChatActivity.class);
                activityIntent.putExtras(intent.getExtras());
                activityIntent.putExtra("chatterParseId", chatRecipientObjectId);
                activityIntent.addFlags(268435456);
                activityIntent.addFlags(67108864);
                context.startActivity(activityIntent);
            }
        }
        else if(type.equals("friendRequest")) {
            if (friendRecipientObjectId != null) {
                Intent activityIntent = new Intent(context, AddFriendsActivity.class);
                activityIntent.putExtras(intent.getExtras());
                activityIntent.putExtra("frienderParseId", friendRecipientObjectId);
                activityIntent.addFlags(268435456);
                activityIntent.addFlags(67108864);
                context.startActivity(activityIntent);
            }
        }
        else if(type.equals("friendAccept")) {
            if (friendRecipientObjectId != null) {
                Intent activityIntent = new Intent(context, ViewFriendsActivity.class);
                activityIntent.putExtras(intent.getExtras());
                activityIntent.putExtra("frienderParseId", friendRecipientObjectId);
                activityIntent.addFlags(268435456);
                activityIntent.addFlags(67108864);
                context.startActivity(activityIntent);
            }
        }
    }
}
