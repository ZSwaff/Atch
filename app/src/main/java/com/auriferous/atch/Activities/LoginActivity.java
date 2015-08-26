package com.auriferous.atch.Activities;

import android.app.Activity;
import android.content.Intent;

import android.graphics.Rect;
import android.graphics.drawable.LayerDrawable;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.auriferous.atch.ActionEditText;
import com.auriferous.atch.AtchParsePushReceiver;
import com.auriferous.atch.Callbacks.FuncCallback;
import com.auriferous.atch.ParseAndFacebookUtils;
import com.auriferous.atch.R;
import com.auriferous.atch.Users.User;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.ParseUser;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends FragmentActivity {
    private GoogleMap map;
    private ActionEditText usernameView;
    private CallbackManager callbackManager;

    private boolean signUpScreen = false;
    private boolean loggingIn = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        if (ParseUser.getCurrentUser() != null)
            proceedToAtchAgreement();


        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                if (!loggingIn) {
                    loggingIn = true;
                    checkParseForFbAccount(loginResult.getAccessToken());
                }
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(FacebookException exception) {
                Log.e("xxxerr", exception.toString());
            }
        });


        if (map == null) {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            map = mapFragment.getMap();
            map.setIndoorEnabled(false);
            map.getUiSettings().setMapToolbarEnabled(false);
            map.getUiSettings().setTiltGesturesEnabled(false);
            map.getUiSettings().setCompassEnabled(false);
            map.getUiSettings().setRotateGesturesEnabled(false);

            map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    double lat = 37.427325;
                    double lng = -122.169882;
                    LatLngBounds mapBounds = new LatLngBounds(new LatLng(lat-.015, lng-.015), new LatLng(lat+.015, lng+.015));
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(mapBounds, 0), 2000, null);
                }
            });
        }

        final View view = (findViewById(R.id.buttons_layout));
        ViewTreeObserver vto = view.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                map.setPadding(0, 0, 0, findViewById(R.id.log_in_button).getMeasuredHeight()+findViewById(R.id.sign_up_switch_button).getMeasuredHeight());
                ViewTreeObserver obs = view.getViewTreeObserver();
                obs.removeOnGlobalLayoutListener(this);
            }
        });


        Button signUpSwitchButton = (Button) findViewById(R.id.sign_up_switch_button);
        signUpSwitchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signUpScreen = true;
                switchViews();
            }
        });
        Button logInButton = (Button) findViewById(R.id.log_in_button);
        logInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });


        usernameView = (ActionEditText) findViewById(R.id.username);
        usernameView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    attemptSignUp();
                    return true;
                }
                return false;
            }
        });
        usernameView.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                usernameView.setError(null);
                String currentUsername = s.toString();
                if(!currentUsername.matches("\\w*")){
                    usernameView.setError(getString(R.string.error_invalid_characters));
                    usernameView.requestFocus();
                }
                if(currentUsername.length() > 20){
                    usernameView.setError(getString(R.string.error_too_long));
                    usernameView.requestFocus();
                }
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

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
        callbackManager.onActivityResult(requestCode, resultCode, data);
        ParseFacebookUtils.onActivityResult(requestCode, resultCode, data);
    }
    @Override
    public void onBackPressed() {
        if(signUpScreen){
            signUpScreen = false;
            switchViews();
        }
        else
            super.onBackPressed();
    }


    private void attemptLogin() {
        LoginManager.getInstance().logInWithReadPermissions(this, ParseAndFacebookUtils.permissions);
    }
    private void checkParseForFbAccount(final AccessToken token) {
        final Activity activity = this;
        String fbid = token.getUserId();
        ParseAndFacebookUtils.getParseUserFromFbid(fbid, new FuncCallback<ParseUser>() {
            @Override
            public void done(ParseUser parseUser) {
                if (parseUser != null) {
                    ParseFacebookUtils.logInWithReadPermissionsInBackground(activity, ParseAndFacebookUtils.permissions, new LogInCallback() {
                        @Override
                        public void done(ParseUser parseUser, ParseException e) {
                            proceedToAtchAgreement();
                        }
                    });
                } else {
                    signUpScreen = true;
                    switchViews();
                }
            }
        });
    }


    private void attemptSignUp() {
        final Activity activity = this;
        final String username = usernameView.getText().toString();
        isValidUsername(new FuncCallback<Boolean>() {
            @Override
            public void done(Boolean aBoolean) {
                ParseFacebookUtils.logInWithReadPermissionsInBackground(activity, ParseAndFacebookUtils.permissions, new LogInCallback() {
                    @Override
                    public void done(ParseUser parseUser, ParseException e) {
                        setupParseInstallation();
                        GraphRequest request = GraphRequest.newMeRequest(
                                AccessToken.getCurrentAccessToken(),
                                new GraphRequest.GraphJSONObjectCallback() {
                                    @Override
                                    public void onCompleted(JSONObject object, GraphResponse response) {
                                        try {
                                            ParseUser.getCurrentUser().setUsername(username);
                                            ParseUser.getCurrentUser().put("queryUsername", username.toLowerCase());
                                            ParseUser.getCurrentUser().put("fbid", object.getString("id"));
                                            ParseUser.getCurrentUser().put("fullname", object.getString("name"));
                                            ParseUser.getCurrentUser().put("queryFullname", object.getString("name").toLowerCase());
                                            ParseUser.getCurrentUser().saveInBackground();
                                        } catch (JSONException e) {
                                        }
                                    }
                                });
                        request.executeAsync();

                        proceedToAtchAgreement();
                    }
                });
            }
        });
    }
    private void isValidUsername(final FuncCallback<Boolean> calledIfValid){
        usernameView.setError(null);
        final ActionEditText focusView = usernameView;
        final String username = usernameView.getText().toString();

        if (TextUtils.isEmpty(username)) {
            focusView.setError(getString(R.string.error_username_required));
            focusView.requestFocus();
            return;
        }
        if(!username.matches("\\w*")){
            usernameView.setError(getString(R.string.error_invalid_characters));
            usernameView.requestFocus();
            return;
        }
        if(username.length() > 20){
            usernameView.setError(getString(R.string.error_too_long));
            usernameView.requestFocus();
            return;
        }
        ParseAndFacebookUtils.getParseUserFromUsername(username, new FuncCallback<ParseUser>() {
            @Override
            public void done(ParseUser parseUser) {
                if(parseUser != null){
                    focusView.setError(getString(R.string.error_taken_username));
                    focusView.requestFocus();
                }
                else{
                    calledIfValid.done(true);
                }
            }
        });
    }


    private void switchViews() {
        RelativeLayout oldLayout = (RelativeLayout) findViewById(signUpScreen?R.id.buttons_layout:R.id.sign_up_layout);
        oldLayout.setVisibility(View.GONE);
        RelativeLayout newLayout = (RelativeLayout) findViewById(signUpScreen?R.id.sign_up_layout:R.id.buttons_layout);
        newLayout.setVisibility(View.VISIBLE);
    }
    private void setupParseInstallation() {
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.put("userId", ParseUser.getCurrentUser().getObjectId());
        installation.saveInBackground();
        ParsePush.subscribeInBackground("global");
    }
    private void proceedToAtchAgreement(){
        setupParseInstallation();
        AtchParsePushReceiver.cancelAllNotifications(this);
        startActivity(new Intent(getApplicationContext(), AtchAgreementActivity.class));
    }
}