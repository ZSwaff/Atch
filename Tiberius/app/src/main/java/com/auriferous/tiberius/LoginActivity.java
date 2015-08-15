package com.auriferous.tiberius;

import android.app.Activity;
import android.content.Intent;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;

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

import java.util.Arrays;
import java.util.List;


public class LoginActivity extends Activity {
    private AutoCompleteTextView mUsernameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ParseUser currUser = ParseUser.getCurrentUser();
        final boolean accountWUNCreated = (currUser == null)?false:accountIsAlreadyCreatedWithUsername(currUser);

        if (currUser != null && ParseFacebookUtils.isLinked(currUser) &&
                isUsernameTaken(currUser.getUsername()) && accountWUNCreated) {
            startActivity(new Intent(getApplicationContext(), AtchAgreementActivity.class));
        }

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

        mUsernameView = (AutoCompleteTextView) findViewById(R.id.username);

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

    public void attemptLogin() {
        ParseFacebookUtils.logInWithReadPermissionsInBackground(this, MyParseFacebookUtils.permissions, new LogInCallback() {
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

    public void attemptSignUp() {
        ParseUser currUser = ParseUser.getCurrentUser();
        final boolean accountWUNCreated = (currUser == null)?false:accountIsAlreadyCreatedWithUsername(currUser);

        mUsernameView.setError(null);
        final String username = mUsernameView.getText().toString();

        View focusView = mUsernameView;;
        if (currUser != null && ParseFacebookUtils.isLinked(currUser) &&
                isUsernameTaken(currUser.getUsername()) && accountWUNCreated) {
            mUsernameView.setError(getString(R.string.error_account_created));
            focusView.requestFocus();
            return;
        }
        if (isBadUsername(username, focusView)) {
            focusView.requestFocus();
            return;
        }

        ParseFacebookUtils.logInWithReadPermissionsInBackground(this, MyParseFacebookUtils.permissions, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException err) {
                if (user != null) {
                    if (user.isNew() || !accountWUNCreated) {
                        GraphRequest request = GraphRequest.newMeRequest(
                                AccessToken.getCurrentAccessToken(),
                                new GraphRequest.GraphJSONObjectCallback() {
                                    @Override
                                    public void onCompleted(JSONObject object, GraphResponse response) {
                                        try {
                                            ParseUser.getCurrentUser().put("fbid", object.getString("id"));
                                            ParseUser.getCurrentUser().put("fullname", object.getString("name"));
                                            ParseUser.getCurrentUser().saveInBackground();
                                        } catch (JSONException e) {}
                                    }
                                });
                        request.executeAsync();

                        user.setUsername(username);
                        user.put("usernameSet", "t");
                        user.saveInBackground();
                    }

                    //switch to the atch agreement activity
                    startActivity(new Intent(getApplicationContext(), AtchAgreementActivity.class));
                }
            }
        });
    }

    private boolean isBadUsername(String username, View focusView){
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_username_required));
            return true;
        } else if (isUsernameTaken(username)) {
            mUsernameView.setError(getString(R.string.error_taken_username));
            return true;
        }
        return false;
    }

    private boolean isUsernameTaken(String username) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo("username", username);
        try {
            List<ParseUser> users = query.find();
            if (users.isEmpty()) return false;
        } catch (ParseException e) {}
        return true;
    }

    private boolean accountIsAlreadyCreatedWithUsername(ParseUser user){
        if(user == null) return false;
        if(user.get("usernameSet") == null) return false;
        return user.get("usernameSet").equals("t");
    }


    private void switchViews(boolean switchToSignUp) {
        LinearLayout oldLayout = (LinearLayout) findViewById(switchToSignUp?R.id.buttons_layout:R.id.sign_up_layout);
        oldLayout.setVisibility(View.GONE);
        LinearLayout newLayout = (LinearLayout) findViewById(switchToSignUp?R.id.sign_up_layout:R.id.buttons_layout);
        newLayout.setVisibility(View.VISIBLE);
    }
}