package com.auriferous.atch.Activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.auriferous.atch.AtchApplication;
import com.auriferous.atch.LocationUpdateService;
import com.auriferous.atch.R;

public class AtchAgreementActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_atch_agreement);
    }
    @Override
    protected void onResume() {
        super.onResume();

        AtchApplication app = (AtchApplication)getApplication();
        app.stopLocationUpdates();
        app.populateFriendList();
    }


    @Override
    public void onBackPressed() {}


    public void engageApp(View view){
        ((AtchApplication)getApplication()).startLocationUpdates();

        startActivity(new Intent(getApplication(), MapActivity.class));
    }
}
