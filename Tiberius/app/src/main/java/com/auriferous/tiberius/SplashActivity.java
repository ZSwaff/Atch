package com.auriferous.tiberius;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //if the user is already logged in, skip this screen
//        if (ParseUser.getCurrentUser() != null && ParseFacebookUtils.isLinked(ParseUser.getCurrentUser())) {
//            startActivity(new Intent(getApplicationContext(), AtchAgreementActivity.class));
//        }

        Button mSignUpButton = (Button) findViewById(R.id.sign_up_switch_button);
        mSignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), SignUpActivity.class));
            }
        });
        Button mLogInButton = (Button) findViewById(R.id.log_in_button);
        mLogInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogIn();
            }
        });
    }

    private void attemptLogIn() {
        ParseFacebookUtils.logInWithReadPermissionsInBackground(this, MyParseFacebookUtils.permissions, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException err) {
                Log.d("Here1",err.getMessage());
                if (user != null) {
                    if (user.isNew())
                        startActivity(new Intent(getApplicationContext(), SignUpActivity.class));
                    else
                        startActivity(new Intent(getApplicationContext(), AtchAgreementActivity.class));
                }
            }
        });
    }
}
