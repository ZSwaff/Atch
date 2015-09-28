package com.atchapp.atch.Activities;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.TextView;

import com.atchapp.atch.ActionEditText;
import com.atchapp.atch.AtchApplication;
import com.atchapp.atch.Callbacks.SimpleCallback;
import com.atchapp.atch.Callbacks.VariableCallback;
import com.atchapp.atch.ParseAndFacebookUtils;
import com.atchapp.atch.R;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;

public class LoginActivity extends FragmentActivity {
    private GoogleMap map;
    private CallbackManager callbackManager;

    private boolean signUpScreen = false;
    private boolean loggingIn = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ((AtchApplication)getApplication()).setIsLoggedIn(false);

        //if the user is already logged in, bypass this screen
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


        setupMap();

        setupViews();
    }
    private void setupMap() {
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
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(mapBounds, 0), 5000, null);
                }
            });
        }
    }
    private void setupViews() {
        final View view = (findViewById(R.id.buttons_layout));
        ViewTreeObserver vto = view.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                map.setPadding(0, 0, 0, findViewById(R.id.log_in_button).getMeasuredHeight() + findViewById(R.id.sign_up_switch_button).getMeasuredHeight());
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


        final ActionEditText usernameView = (ActionEditText) findViewById(R.id.username);
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
                String currentUsername = s.toString();
                isValidUsername(currentUsername, null);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
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
        ParseAndFacebookUtils.getParseUserFromFbid(fbid, new VariableCallback<ParseUser>() {
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
        final String username = ((ActionEditText) findViewById(R.id.username)).getText().toString();
        isValidUsername(username, new SimpleCallback() {
            @Override
            public void done() {
                ParseFacebookUtils.logInWithReadPermissionsInBackground(activity, ParseAndFacebookUtils.permissions, new LogInCallback() {
                    @Override
                    public void done(ParseUser parseUser, ParseException e) {
                        ParseAndFacebookUtils.setFacebookInfoAboutCurrentUser(username);

                        proceedToAtchAgreement();
                    }
                });
            }
        });
    }
    private void isValidUsername(String username, final SimpleCallback calledIfValid) {
        if (TextUtils.isEmpty(username)) {
            updateUsernameFeedback(getString(R.string.error_username_required));
            return;
        }
        if(!username.matches("\\w*")){
            updateUsernameFeedback(getString(R.string.error_invalid_characters));
            return;
        }
        if (username.length() >= 20) {
            updateUsernameFeedback(getString(R.string.error_too_long));
            return;
        }
        ParseAndFacebookUtils.getParseUserFromUsername(username, new VariableCallback<ParseUser>() {
            @Override
            public void done(ParseUser parseUser) {
                if (parseUser != null) {
                    updateUsernameFeedback(getString(R.string.error_taken_username));
                } else {
                    updateUsernameFeedback(getString(R.string.valid_username));
                    if (calledIfValid != null)
                        calledIfValid.done();
                }
            }
        });
    }
    private void updateUsernameFeedback(String message) {
        if (message == null || message.isEmpty()) {
            findViewById(R.id.username_feedback_area).setVisibility(View.GONE);
        } else {
            findViewById(R.id.username_feedback_area).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.username_feedback)).setText(message);
        }
    }


    private void switchViews() {
        RelativeLayout oldLayout = (RelativeLayout) findViewById(signUpScreen?R.id.buttons_layout:R.id.sign_up_layout);
        oldLayout.setVisibility(View.GONE);
        RelativeLayout newLayout = (RelativeLayout) findViewById(signUpScreen?R.id.sign_up_layout:R.id.buttons_layout);
        newLayout.setVisibility(View.VISIBLE);

        if (signUpScreen)
            map.setPadding(0, 0, 0, findViewById(R.id.log_in_button).getMeasuredHeight() + findViewById(R.id.sign_up_switch_button).getMeasuredHeight());
        else
            map.setPadding(0, 0, 0, findViewById(R.id.username).getMeasuredHeight() + findViewById(R.id.sign_up_button).getMeasuredHeight());
    }
    private void proceedToAtchAgreement(){
        ((AtchApplication)getApplication()).setIsLoggedIn(true);

        Intent intent = new Intent(getApplication(), AtchAgreementActivity.class);
        finish();
        startActivity(intent);
        overridePendingTransition(R.anim.slide_down_in, R.anim.slide_down_out);
    }
}