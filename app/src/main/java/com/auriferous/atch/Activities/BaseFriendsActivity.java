package com.auriferous.atch.Activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.auriferous.atch.AtchApplication;
import com.auriferous.atch.AtchParsePushReceiver;
import com.auriferous.atch.Callbacks.SimpleCallback;
import com.auriferous.atch.Callbacks.ViewUpdateCallback;
import com.auriferous.atch.InAppNotificationView;
import com.auriferous.atch.R;

public abstract class BaseFriendsActivity extends AppCompatActivity{
    protected AtchApplication app;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (AtchApplication)this.getApplication();
    }
    @Override
    protected void onResume() {
        super.onResume();
        app.setCurrentActivity(this);
        app.setIsOnlineAndAppOpen(true);
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
            setViewUpdateCallback(null);
        }
        else if(!(currActivity instanceof BaseFriendsActivity))
            app.setIsOnlineAndAppOpen(false);
    }


    public void createNotification(String message, int color, final Class activityToStart, final Intent startInfo){
        final ViewGroup baseLayout = ((ViewGroup)getWindow().getDecorView().getRootView());
        final View notifLayout = View.inflate(this, R.layout.in_app_notif, baseLayout);

        int statusBarHeight = -1;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if(resourceId > 0)
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);

        InAppNotificationView notif = (InAppNotificationView)notifLayout.findViewById(R.id.notif);
        if(color != -1)
            notif.setBackgroundColor(color);
        notif.setUpperMargin(statusBarHeight);

        final Activity thisAct = this;
        notif.setCallbacks(new SimpleCallback() {
            @Override
            public void done() {
                if (activityToStart.isInstance(thisAct)) {
                    startInfo.putExtra("direct", true);
                    onNewIntent(startInfo);
                    onResume();
                }
                else
                    startActivity(startInfo);
            }
        }, new SimpleCallback() {
            @Override
            public void done() {
                View v = baseLayout.findViewById(R.id.notif_root);
                while (v != null) {
                    baseLayout.removeView(v);
                    v = baseLayout.findViewById(R.id.notif_root);
                }
                baseLayout.removeView(notifLayout);
            }
        });
        ((TextView)notif.findViewById(R.id.message)).setText(message);
        notif.deployDown();
    }

    protected void setViewUpdateCallback(ViewUpdateCallback viewUpdateCallback) {
        app.setViewUpdateCallback(viewUpdateCallback);
    }
    protected void updateCurrentView() {
        app.updateView();
    }
}
