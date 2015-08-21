package com.auriferous.tiberius.Activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.auriferous.tiberius.LocationUpdateService;
import com.auriferous.tiberius.R;

public class AtchAgreementActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_atch_agreement);

    }

    @Override
    public void onBackPressed() {}


    public void engageApp(View view){
        startService(new Intent(this, LocationUpdateService.class));

        startActivity(new Intent(getApplicationContext(), MapActivity.class));
    }
}
