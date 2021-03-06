package com.atchapp.atch.Activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.atchapp.atch.AtchApplication;
import com.atchapp.atch.AtchParsePushReceiver;
import com.atchapp.atch.Callbacks.SimpleCallback;
import com.atchapp.atch.Callbacks.ViewUpdateCallback;
import com.atchapp.atch.R;
import com.atchapp.atch.UiElements.InAppNotificationView;

public abstract class BaseFriendsActivity extends AppCompatActivity{
    protected AtchApplication app;
    private InAppNotificationView notif = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (AtchApplication)this.getApplication();
    }
    @Override
    protected void onResume() {
        super.onResume();

        if(!app.isOnline()) {
            Intent intent = new Intent(getApplication(), AtchAgreementActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.putExtra("back", true);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_up_in, R.anim.slide_up_out);
        }

        app.setCurrentActivity(this);
        app.setIsOnlineAndAppOpen(true);
        app.deactivateLogoutAlarm();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        AtchParsePushReceiver.cancelAllNotifications(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        clearReferences();
    }
    @Override
    protected void onDestroy() {
        clearReferences();
        super.onDestroy();
    }

    private void clearReferences(){
        Activity currActivity = app.getCurrentActivity();
        if (currActivity != null && currActivity.equals(this)) {
            app.setIsOnlineAndAppOpen(false);
            app.setCurrentActivity(null);
            app.activateLogoutAlarm();
            setViewUpdateCallback(null);
        }
        if(currActivity == null || !(currActivity instanceof BaseFriendsActivity))
            app.setIsOnlineAndAppOpen(false);
    }


    public void createNotification(String message, int color, final Class activityToStart, final Intent startInfo){
        final ViewGroup baseLayout = ((ViewGroup) getWindow().getDecorView().getRootView());
        notif = (InAppNotificationView) baseLayout.findViewById(R.id.notif);
        if (notif == null) {
            View notifLayout = View.inflate(this, R.layout.in_app_notif, baseLayout);

            int statusBarHeight = -1;
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0)
                statusBarHeight = getResources().getDimensionPixelSize(resourceId);

            notif = (InAppNotificationView) notifLayout.findViewById(R.id.notif);
            if (color != -1)
                notif.setBackgroundColor(color);
            notif.setUpperMargin(statusBarHeight);
        }

        final Activity thisAct = this;
        notif.setCallbacks(new SimpleCallback() {
            @Override
            public void done() {
                if (activityToStart.isInstance(thisAct)) {
                    startInfo.putExtra("direct", true);
                    onNewIntent(startInfo);
                    onResume();
                } else
                    startActivity(startInfo);
            }
        });
        ((TextView) notif.findViewById(R.id.message)).setText(message);

        notif.deployDown();
    }

    protected void setViewUpdateCallback(ViewUpdateCallback viewUpdateCallback) {
        app.setViewUpdateCallback(viewUpdateCallback);
    }
}
