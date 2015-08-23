package com.auriferous.atch;

import android.content.Context;
import android.content.Intent;

import com.auriferous.atch.Activities.AddFriendsActivity;
import com.auriferous.atch.Activities.ChatActivity;
import com.parse.ParseAnalytics;
import com.parse.ParsePushBroadcastReceiver;

import org.json.JSONException;
import org.json.JSONObject;

public class AtchParsePushReceiver extends ParsePushBroadcastReceiver {
    protected void onPushOpen(Context context, Intent intent) {
        ParseAnalytics.trackAppOpenedInBackground(intent);
        String chatRecipientObjectId = null;
        String friendRecipientObjectId = null;

        try {
            JSONObject cls = new JSONObject(intent.getStringExtra("com.parse.Data"));
            chatRecipientObjectId = cls.optString("chatterParseId", null);
            friendRecipientObjectId = cls.optString("frienderParseId", null);
        }
        catch (JSONException var6) {}

        if(chatRecipientObjectId != null) {
            Intent activityIntent = new Intent(context, ChatActivity.class);
            activityIntent.putExtras(intent.getExtras());
            activityIntent.putExtra("chatterParseId", chatRecipientObjectId);
            activityIntent.addFlags(268435456);
            activityIntent.addFlags(67108864);
            context.startActivity(activityIntent);
        }
        if(friendRecipientObjectId != null) {
            Intent activityIntent = new Intent(context, AddFriendsActivity.class);
            activityIntent.putExtras(intent.getExtras());
            activityIntent.putExtra("frienderParseId", friendRecipientObjectId);
            activityIntent.addFlags(268435456);
            activityIntent.addFlags(67108864);
            context.startActivity(activityIntent);
        }
    }
}
