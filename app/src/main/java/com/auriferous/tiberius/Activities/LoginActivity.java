package com.auriferous.tiberius.Activities;

import android.app.Activity;
import android.content.Intent;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;

import com.auriferous.tiberius.AtchApplication;
import com.auriferous.tiberius.ParseAndFacebookUtils;
import com.auriferous.tiberius.R;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends Activity {
    private AutoCompleteTextView usernameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ParseUser currUser = ParseUser.getCurrentUser();
        if (accountIsAlreadyCreatedWithUsername(currUser) && ParseFacebookUtils.isLinked(currUser) && isLoggedIn())
            startActivity(new Intent(getApplicationContext(), AtchAgreementActivity.class));

        Button mSignUpSwitchButton = (Button) findViewById(R.id.sign_up_switch_button);
        mSignUpSwitchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchViews(true);
            }
        });
        Button mLogInButton = (Button) findViewById(R.id.log_in_button);
        mLogInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        usernameView = (AutoCompleteTextView) findViewById(R.id.username);

        Button mSignUpButton = (Button) findViewById(R.id.sign_up_button);
        mSignUpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptSignUp();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ParseFacebookUtils.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        switchViews(false);
    }


    private void attemptLogin() {
        ParseFacebookUtils.logInWithReadPermissionsInBackground(this, ParseAndFacebookUtils.permissions, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException err) {
                if (user != null) {
                    if (user.isNew() || !accountIsAlreadyCreatedWithUsername(user))
                        switchViews(true);
                    else
                        startActivity(new Intent(getApplicationContext(), AtchAgreementActivity.class));
                }
            }
        });
    }
    private void attemptSignUp() {
        usernameView.setError(null);
        View focusView = usernameView;

        final String username = usernameView.getText().toString();

        ParseUser usernameUser = getUserWithUsername(username);

        if (isBadUsername(username)) {
            focusView.requestFocus();
            return;
        }
        if (usernameUser != null) {
            //todo maybe make message for if you're the user who took the username
            usernameView.setError(getString(R.string.error_taken_username));
            focusView.requestFocus();
            return;
        }

        ParseFacebookUtils.logInWithReadPermissionsInBackground(this, ParseAndFacebookUtils.permissions, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException err) {
                if (user != null) {
                    if (user.isNew() || !accountIsAlreadyCreatedWithUsername(user)) {
                        GraphRequest request = GraphRequest.newMeRequest(
                                AccessToken.getCurrentAccessToken(),
                                new GraphRequest.GraphJSONObjectCallback() {
                                    @Override
                                    public void onCompleted(JSONObject object, GraphResponse response) {
                                        try {
                                            ParseUser.getCurrentUser().put("fbid", object.getString("id"));
                                            ParseUser.getCurrentUser().put("fullname", object.getString("name"));
                                            ParseUser.getCurrentUser().put("usernameSet", "t");
                                            ParseUser.getCurrentUser().setUsername(username);
                                            ParseUser.getCurrentUser().saveInBackground();
                                        } catch (JSONException e) {}
                                    }
                                });
                        request.executeAsync();
                    }

                    //switch to the atch agreement activity
                    startActivity(new Intent(getApplicationContext(), AtchAgreementActivity.class));
                }
            }
        });
    }

    private boolean isBadUsername(String username){
        if (TextUtils.isEmpty(username)) {
            usernameView.setError(getString(R.string.error_username_required));
            return true;
        }
        return false;
    }
    //todo blocking
    private ParseUser getUserWithUsername(String username) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo("username", username);
        try {
            return query.getFirst();
        } catch (ParseException e) {}
        return null;
    }
    private boolean accountIsAlreadyCreatedWithUsername(ParseUser user){
        return (user != null && user.get("usernameSet") != null && user.get("usernameSet").equals("t"));
    }

    public boolean isLoggedIn() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        //todo reexamine
        return (accessToken != null && !accessToken.isExpired()/* && accessToken.getPermissions().size() == ParseAndFacebookUtils.permissions.size()*/);
    }

    private void switchViews(boolean switchToSignUp) {
        LinearLayout oldLayout = (LinearLayout) findViewById(switchToSignUp?R.id.buttons_layout:R.id.sign_up_layout);
        oldLayout.setVisibility(View.GONE);
        LinearLayout newLayout = (LinearLayout) findViewById(switchToSignUp?R.id.sign_up_layout:R.id.buttons_layout);
        newLayout.setVisibility(View.VISIBLE);
    }
}