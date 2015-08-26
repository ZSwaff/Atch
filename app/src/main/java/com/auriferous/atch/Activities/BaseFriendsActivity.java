package com.auriferous.atch.Activities;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.auriferous.atch.AtchApplication;
import com.auriferous.atch.AtchParsePushReceiver;
import com.auriferous.atch.Callbacks.ViewUpdateCallback;

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
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        AtchParsePushReceiver.cancelAllNotifications(this);
    }

    @Override
    protected void onPause() {
        clearReferences();
        super.onPause();
    }
    @Override
    protected void onDestroy() {
        clearReferences();
        super.onDestroy();
    }

    private void clearReferences(){
        Activity currActivity = app.getCurrentActivity();
        if (currActivity != null && currActivity.equals(this)) {
            app.setCurrentActivity(null);
            setViewUpdateCallback(null);
        }
    }


    protected void setViewUpdateCallback(ViewUpdateCallback viewUpdateCallback) {
        app.setViewUpdateCallback(viewUpdateCallback);
    }
    protected void updateCurrentView() {
        app.updateView();
    }
}
