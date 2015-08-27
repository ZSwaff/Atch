package com.auriferous.atch;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.auriferous.atch.Activities.AddFriendsActivity;
import com.auriferous.atch.Activities.MapActivity;
import com.auriferous.atch.Activities.ViewFriendsActivity;
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
            if(currActivity != null && chatRecipientObjectId != null && currActivity instanceof MapActivity && ((MapActivity)currActivity).isChattingWithPerson(chatRecipientObjectId))
                ((MapActivity)currActivity).refreshChatHistory();
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

        if(type.equals("message")) {
            if (chatRecipientObjectId != null) {
                Intent activityIntent = new Intent(context, MapActivity.class);
                activityIntent.putExtras(intent.getExtras());
                activityIntent.putExtra("type", "message");
                activityIntent.putExtra("chatterParseId", chatRecipientObjectId);
                activityIntent.addFlags(268435456);
                activityIntent.addFlags(67108864);
                activityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
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
                activityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                context.startActivity(activityIntent);
            }
        }
        else if(type.equals("friendAccept")) {
            if (friendRecipientObjectId != null) {
                app.populateFriendList();

                Intent activityIntent = new Intent(context, ViewFriendsActivity.class);
                activityIntent.putExtras(intent.getExtras());
                activityIntent.putExtra("frienderParseId", friendRecipientObjectId);
                activityIntent.addFlags(268435456);
                activityIntent.addFlags(67108864);
                activityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                context.startActivity(activityIntent);
            }
        }
    }
}
