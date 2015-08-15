package com.auriferous.tiberius;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class SignUpActivity extends Activity {

    private AutoCompleteTextView mUsernameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mUsernameView = (AutoCompleteTextView) findViewById(R.id.username);

        Button mSignInButton = (Button) findViewById(R.id.sign_up_button);
        mSignInButton.setOnClickListener(new View.OnClickListener() {
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

    public void attemptSignUp() {
        final boolean accountWUNCreated = accountIsAlreadyCreatedWithUsername(ParseUser.getCurrentUser());
        Log.d("Here2", "account created: " + accountWUNCreated);

        if (ParseUser.getCurrentUser() != null && ParseFacebookUtils.isLinked(ParseUser.getCurrentUser()) && accountWUNCreated)
            startActivity(new Intent(getApplicationContext(), AtchAgreementActivity.class));

        mUsernameView.setError(null);

        final String username = mUsernameView.getText().toString();

        View focusView = null;
        if (isBadUsername(username, focusView)) {
            focusView.requestFocus();
            Log.d("Here3", "bad username");
            return;
        }

        ParseFacebookUtils.logInWithReadPermissionsInBackground(this, MyParseFacebookUtils.permissions, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException err) {
                if (user != null) {
                    Log.d("Here4", "user not null");
                    if (user.isNew() || !accountWUNCreated) {
                        GraphRequest request = GraphRequest.newMeRequest(
                                AccessToken.getCurrentAccessToken(),
                                new GraphRequest.GraphJSONObjectCallback() {
                                    @Override
                                    public void onCompleted(JSONObject object, GraphResponse response) {
                                        try {
                                            ParseUser.getCurrentUser().put("fbid", object.getString("id"));
                                            //TODO
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
            focusView = mUsernameView;
            return true;
        } else if (isUsernameTaken(username)) {
            mUsernameView.setError(getString(R.string.error_taken_username));
            focusView = mUsernameView;
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
        return user.get("usernameSet").equals("t");
    }
}
