package com.auriferous.tiberius.Activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.auriferous.tiberius.AtchApplication;
import com.auriferous.tiberius.LocationUpdateService;
import com.auriferous.tiberius.R;
import com.parse.ParseInstallation;
import com.parse.ParseUser;

public class AtchAgreementActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_atch_agreement);

        ((AtchApplication)getApplication()).populateFriendList();

        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.put("userId", ParseUser.getCurrentUser().getObjectId());
        installation.saveInBackground();
    }

    @Override
    public void onBackPressed() {}


    public void engageApp(View view){
        startService(new Intent(this, LocationUpdateService.class));

        startActivity(new Intent(getApplicationContext(), MapActivity.class));
    }
}
