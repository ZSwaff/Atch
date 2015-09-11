package com.auriferous.atch.Activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.auriferous.atch.AtchApplication;
import com.auriferous.atch.AtchParsePushReceiver;
import com.auriferous.atch.ParseAndFacebookUtils;
import com.auriferous.atch.R;
import com.facebook.login.LoginManager;
import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseUser;

public class AtchAgreementActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_atch_agreement);

        ParseAndFacebookUtils.setupParseInstallation();
        AtchParsePushReceiver.cancelAllNotifications(this);

        AtchApplication app = (AtchApplication) getApplication();
        app.populateFriendList();
        app.populateFacebookFriendList();
        app.populatePendingLists();

        findViewById(R.id.engage_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                engageApp();
            }
        });
        findViewById(R.id.log_out_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logAllTheWayOut();
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();

        ((AtchApplication)getApplication()).logout();
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);

        if(intent.getBooleanExtra("back", false))
            overridePendingTransition(R.anim.slide_up_in, R.anim.slide_up_out);
    }
    @Override
    public void onBackPressed() {}


    public void logAllTheWayOut() {
        deregisterParseInstallation();

        LoginManager.getInstance().logOut();

        ParseUser user = ParseUser.getCurrentUser();
        ParseUser.logOutInBackground(new LogOutCallback() {
            @Override
            public void done(ParseException e) {
                Intent intent = new Intent(getApplication(), LoginActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_up_in, R.anim.slide_up_out);
            }
        });
    }
    private void deregisterParseInstallation() {
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.remove("userId");
        installation.saveInBackground();
    }


    public void engageApp(){
        ((AtchApplication)getApplication()).setIsOnline(true);
        ((AtchApplication)getApplication()).startLocationUpdates();

        ParseAndFacebookUtils.sendLoginNotifications();

        Intent intent = new Intent(getApplication(), MapActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_down_in, R.anim.slide_down_out);
    }
}
