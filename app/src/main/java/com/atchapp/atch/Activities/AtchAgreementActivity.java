package com.atchapp.atch.Activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;

import com.atchapp.atch.AtchApplication;
import com.atchapp.atch.AtchParsePushReceiver;
import com.atchapp.atch.GeneralUtils;
import com.atchapp.atch.ParseAndFacebookUtils;
import com.atchapp.atch.R;
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

        findViewById(R.id.root).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                engageApp();
            }
        });
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

        if (ParseUser.getCurrentUser() == null)
            logAllTheWayOut();

        int bgColor = GeneralUtils.generateNewColor();
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{bgColor, 0x00ffffff});
        gd.setCornerRadius(0f);
        findViewById(R.id.imageView).setBackground(gd);

        ParseAndFacebookUtils.setupParseInstallation();
        AtchParsePushReceiver.cancelAllNotifications(this);

        AtchApplication app = (AtchApplication) getApplication();
        app.logout();
        app.refreshAppData();
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

        LoginManager loginManager = LoginManager.getInstance();
        if(loginManager != null)
            loginManager.logOut();

        if (ParseUser.getCurrentUser() == null) {
            switchToLoginActivity();
            return;
        }

        ParseUser.logOutInBackground(new LogOutCallback() {
            @Override
            public void done(ParseException e) {
                switchToLoginActivity();
            }
        });
    }
    private void deregisterParseInstallation() {
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        if(installation == null) return;
        installation.remove("userId");
        installation.saveInBackground();
    }
    private void switchToLoginActivity() {
        Intent intent = new Intent(getApplication(), LoginActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_up_in, R.anim.slide_up_out);
    }


    public void engageApp(){
        AtchApplication app = (AtchApplication) getApplication();
        app.setIsOnline(true);
        app.startLocationUpdates();

        ParseAndFacebookUtils.sendLoginNotifications();

        Intent intent = new Intent(getApplication(), MapActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_down_in, R.anim.slide_down_out);
    }
}
