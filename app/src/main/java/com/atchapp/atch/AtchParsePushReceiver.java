package com.atchapp.atch;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.atchapp.atch.Activities.AddFriendsActivity;
import com.atchapp.atch.Activities.BaseFriendsActivity;
import com.atchapp.atch.Activities.LoginActivity;
import com.atchapp.atch.Activities.MapActivity;
import com.atchapp.atch.Activities.ViewFriendsActivity;
import com.atchapp.atch.Users.User;
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
            String pid = null;

            try {
                JSONObject cls = new JSONObject(intent.getStringExtra("com.parse.Data"));
                type = cls.optString("type", null);
                chatRecipientObjectId = cls.optString("chatterParseId", null);
                friendRecipientObjectId = cls.optString("frienderParseId", null);
                atchage = cls.optString("alert", null);
                pid = cls.optString("pid", null);
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
                    message = user.getUsername() + " accepted your friend request";
                } else if (type.equals("login")) {
                    user = User.getUserFromMap(pid);
                    message = user.getUsername() + " logged in";
                }
                ((BaseFriendsActivity)currActivity).createNotification(message, (user != null)?user.getRelativeColor():-1, getClassToOpen(type),
                        createIntent(context, getClassToOpen(type), intent, type, chatRecipientObjectId, friendRecipientObjectId, pid));
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

//        //reset certain parts of the message
//        if (pushData != null) {
//            action = pushData.optString("action", null);
//        }

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
//    protected Notification getNotification(Context context, Intent intent) {
//        JSONObject pushData = this.getPushData(intent);
//        if(pushData != null && (pushData.has("alert") || pushData.has("title"))) {
//            String title = pushData.optString("title", "Notification received.");
//            String alert = pushData.optString("alert", "Notification received.");
//            String tickerText = String.format(Locale.getDefault(), "%s: %s", new Object[]{title, alert});
//            Bundle extras = intent.getExtras();
//            Random random = new Random();
//            int contentIntentRequestCode = random.nextInt();
//            int deleteIntentRequestCode = random.nextInt();
//            String packageName = context.getPackageName();
//            Intent contentIntent = new Intent("com.parse.push.intent.OPEN");
//            contentIntent.putExtras(extras);
//            contentIntent.setPackage(packageName);
//            Intent deleteIntent = new Intent("com.parse.push.intent.DELETE");
//            deleteIntent.putExtras(extras);
//            deleteIntent.setPackage(packageName);
//            PendingIntent pContentIntent = PendingIntent.getBroadcast(context, contentIntentRequestCode, contentIntent, 134217728);
//            PendingIntent pDeleteIntent = PendingIntent.getBroadcast(context, deleteIntentRequestCode, deleteIntent, 134217728);
//            NotificationCompat.Builder parseBuilder = new NotificationCompat.Builder(context);
//            parseBuilder.setContentTitle(title).setContentText(alert).setTicker(tickerText).setSmallIcon(this.getSmallIconId(context, intent)).setLargeIcon(this.getLargeIcon(context, intent)).setContentIntent(pContentIntent).setDeleteIntent(pDeleteIntent).setAutoCancel(true).setDefaults(-1);
//            if(alert != null && alert.length() > 38) {
//                parseBuilder.setStyle((new NotificationCompat.Builder.BigTextStyle()).bigText(alert));
//            }
//
//            return parseBuilder.build();
//        } else {
//            return null;
//        }
//    }
//    private JSONObject getPushData(Intent intent) {
//        try {
//            return new JSONObject(intent.getStringExtra("com.parse.Data"));
//        } catch (JSONException var3) {
//            Log.e("xxxerr", "Unexpected JSONException when receiving push data: ", var3);
//            return null;
//        }
//    }

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
        String pid = null;

        try {
            JSONObject cls = new JSONObject(intent.getStringExtra("com.parse.Data"));
            type = cls.optString("type", null);
            chatRecipientObjectId = cls.optString("chatterParseId", null);
            friendRecipientObjectId = cls.optString("frienderParseId", null);
            pid = cls.optString("pid", null);
        }
        catch (JSONException var6) {}

        Intent activityIntent = createIntent(context, getClassToOpen(type), intent, type, chatRecipientObjectId, friendRecipientObjectId, pid);
        context.startActivity(activityIntent);
    }

    private Class getClassToOpen(String type){
        if(type.equals("message"))
            return MapActivity.class;
        else if(type.equals("friendRequest"))
            return AddFriendsActivity.class;
        else if(type.equals("friendAccept"))
            return ViewFriendsActivity.class;
        else if (type.equals("login"))
            return MapActivity.class;
        return null;
    }


    private Intent createIntent(Context context, Class resultClass, Intent oldIntent, String type, String chatterParseId, String frienderParseId, String pid) {
        Intent activityIntent = new Intent(context, resultClass);
        activityIntent.putExtras(oldIntent.getExtras());
        activityIntent.putExtra("type", type);
        activityIntent.putExtra("chatterParseId", (chatterParseId == null) ? pid : chatterParseId);
        activityIntent.putExtra("frienderParseId", frienderParseId);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activityIntent.addFlags(268435456);
        activityIntent.addFlags(67108864);

        return activityIntent;
    }
}
