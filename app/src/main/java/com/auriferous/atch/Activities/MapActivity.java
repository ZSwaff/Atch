package com.auriferous.atch.Activities;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.auriferous.atch.AtchApplication;
import com.auriferous.atch.BannerTouchView;
import com.auriferous.atch.Callbacks.SimpleCallback;
import com.auriferous.atch.Callbacks.VariableCallback;
import com.auriferous.atch.Callbacks.ViewUpdateCallback;
import com.auriferous.atch.GeneralUtils;
import com.auriferous.atch.Messages.MessageList;
import com.auriferous.atch.Messages.MessageListAdapter;
import com.auriferous.atch.ParseAndFacebookUtils;
import com.auriferous.atch.R;
import com.auriferous.atch.Users.Group;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.ArrayList;

public class MapActivity  extends BaseFriendsActivity {
    private GoogleMap map;
    private boolean locLoaded = false;

    private BannerTouchView banner;
    private boolean bannerShowingAtAll = false;
    private boolean pendingImmediateChat = false;

    private Group chatRecipients;
    private volatile ParseObject messageHistory;
    private volatile MessageList messageList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        setupViews();

        setupMapIfNeeded();
    }
    private void setupViews() {
        banner = (BannerTouchView)findViewById(R.id.map_banner);
        banner.findViewById(R.id.body).getBackground().setAlpha(239);

        final View mapView = findViewById(R.id.map_view);
        ViewTreeObserver vto = mapView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                banner.setHeight(mapView.getHeight());
                banner.setMyLocButton((ImageButton) mapView.findViewById(R.id.my_location_button));
                ViewTreeObserver obs = mapView.getViewTreeObserver();
                obs.removeOnGlobalLayoutListener(this);

                if (pendingImmediateChat) {
                    pendingImmediateChat = false;
                    immediateChat();
                }
            }
        });

        EditText messageBox = (EditText) findViewById(R.id.message_box);
        messageBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage();
                    return true;
                }
                return false;
            }
        });


        findViewById(R.id.log_out_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logOut();
            }
        });
        GeneralUtils.addButtonEffect(findViewById(R.id.log_out_button));
        findViewById(R.id.friends_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchToFriends();
            }
        });
        GeneralUtils.addButtonEffect(findViewById(R.id.friends_button));
        findViewById(R.id.my_location_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findMyLocation(700);
            }
        });
        GeneralUtils.addButtonEffect(findViewById(R.id.my_location_button));


        findViewById(R.id.send_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });
        GeneralUtils.addButtonEffect(findViewById(R.id.send_button));
        findViewById(R.id.meet_here).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessageMeetHere();
            }
        });
        GeneralUtils.addButtonEffect(findViewById(R.id.meet_here));
        findViewById(R.id.meet_there).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessageMeetThere();
            }
        });
        GeneralUtils.addButtonEffect(findViewById(R.id.meet_there));
    }

    @Override
    protected void onResume() {
        super.onResume();

        setViewUpdateCallback(new ViewUpdateCallback() {
            @Override
            public void updateView() {
                addMarkers();
                fillListView();
            }
        });
        addMarkers();

        immediateChat();

        if(bannerShowingAtAll)
            banner.findViewById(R.id.title_bar).setBackgroundColor(chatRecipients.getColor());

        banner.requestFocus();

        fillListView();
    }

    public void immediateChat(){
        if(!banner.isHeightInitialized) {
            pendingImmediateChat = true;
            return;
        }

        String type = getIntent().getStringExtra("type");
        if (type != null) {
            if (type.equals("message")) {
                final String chatterPID = getIntent().getStringExtra("chatterParseId");
                if (chatterPID != null) {
                    chatRecipients = Group.getOrCreateGroup(chatterPID, app.getFriendsList().getAllGroups());

                    int countDownTime = 500;
                    if(getIntent().getBooleanExtra("direct", false))
                        countDownTime = 1;
                    (new CountDownTimer(countDownTime, countDownTime){
                        @Override
                        public void onTick(long millisUntilFinished) {}
                        @Override
                        public void onFinish() {
                            enableBanner(-400, chatterPID, new SimpleCallback() {
                                @Override
                                public void done() {
                                    banner.putAllTheWayUp(-600);
                                }
                            });
                        }
                    }).start();

                    getIntent().removeExtra("type");
                    getIntent().removeExtra("message");

                    if(chatRecipients != null && chatRecipients.getId() >= 0)
                        panTo(chatRecipients.getLocation(), 700);
                }
            }
            else if (type.equals("zoom")) {
                final String chatterPID = getIntent().getStringExtra("chatterParseId");
                if (chatterPID != null) {
                    if(chatterPID.startsWith("group"))
                        chatRecipients = Group.getGroupFromNumber(chatterPID, app.getFriendsList().getAllGroups());
                    else
                        chatRecipients = Group.getOrCreateGroup(chatterPID, app.getFriendsList().getAllGroups());

                    (new CountDownTimer(500, 500){
                        @Override
                        public void onTick(long millisUntilFinished) {}
                        @Override
                        public void onFinish() {
                            enableBanner(-400, chatterPID, null);
                        }
                    }).start();

                    getIntent().removeExtra("type");
                    getIntent().removeExtra("message");

                    if(chatRecipients != null && chatRecipients.getId() >= 0)
                        panTo(chatRecipients.getLocation(), 700);
                }
            }
        }
    }


    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);

        if(intent.getBooleanExtra("back", false))
            overridePendingTransition(R.anim.slide_right_in, R.anim.slide_right_out);
        intent.removeExtra("back");

        setIntent(intent);
    }
    @Override
    public void onBackPressed(){
        if(bannerShowingAtAll){
            if (banner.allTheWayUp) banner.takeAllTheWayDown();
            else disableBanner();
        }
        else logOut();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }


    private void logOut() {
        Intent intent = new Intent(getApplication(), AtchAgreementActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("back", true);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_up_in, R.anim.slide_up_out);
    }
    private void switchToFriends() {
        Intent intent = new Intent(getApplication(), ViewFriendsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_left_in, R.anim.slide_left_out);
    }
    private void findMyLocation(int speed) {
        Location myLoc = map.getMyLocation();
        if(myLoc == null) {
            locLoaded = false;
            return;
        }
        panTo(new LatLng(myLoc.getLatitude(), myLoc.getLongitude()), speed);
    }
    private void panTo(LatLng pos, int speed){
        if(pos != null) {
            if(speed <= 0)
                map.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(pos, 16, 0, 0)));
            else
                map.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(pos, 16, 0, 0)), speed, null);
        }
    }


    private void setupMapIfNeeded() {
        if (map == null)
            map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();

        map.setMyLocationEnabled(true);
        map.setIndoorEnabled(false);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setTiltGesturesEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setRotateGesturesEnabled(false);
        map.getUiSettings().setCompassEnabled(false);

        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker) {
                enableBanner(marker.getSnippet(), null);
                return true;
            }
        });
        map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                findMyLocation(0);
            }
        });
        map.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                if(!locLoaded){
                    locLoaded = true;

                    panTo(new LatLng(location.getLatitude(), location.getLongitude()), 0);

                    (new CountDownTimer(200, 200){
                        @Override
                        public void onTick(long millisUntilFinished) {}
                        @Override
                        public void onFinish() {
                            final RelativeLayout splashScreen = (RelativeLayout)findViewById(R.id.splash_screen);
                            ((AtchApplication)(getApplication())).setSetupCompleteCallback(new SimpleCallback() {
                                @Override
                                public void done() {
                                    splashScreen.setVisibility(View.GONE);
                                }
                            });
                        }
                    }).start();
                }
            }
        });

        addMarkers();
    }
    private void addMarkers() {
        map.clear();
        AtchApplication app = (AtchApplication)(getApplication());
        ArrayList<Group> friendGroups = app.getFriendsList().getAllGroups();

        for (Group group : friendGroups) {
            MarkerOptions marker = group.getMarker();
            if(marker != null)
                map.addMarker(marker);
        }
    }


    private void sendMessage() {
        EditText messageBox = (EditText) findViewById(R.id.message_box);
        String newMessage = messageBox.getText().toString();
        if(newMessage.isEmpty()) return;
        ParseAndFacebookUtils.sendMessage(messageHistory, newMessage, 'n', new SimpleCallback() {
            @Override
            public void done() {
                refreshChatHistory();
            }
        });
        messageBox.setText("");
    }
    private void sendMessageMeetHere() {
        ParseAndFacebookUtils.sendMessage(messageHistory, "Meet here", 'h', new SimpleCallback() {
            @Override
            public void done() {
                refreshChatHistory();
            }
        });
    }
    private void sendMessageMeetThere() {
        ParseAndFacebookUtils.sendMessage(messageHistory, "Meet there", 't', new SimpleCallback() {
            @Override
            public void done() {
                refreshChatHistory();
            }
        });
    }

    private void enableBanner(final String chatterParseId, final SimpleCallback callback){
        enableBanner(300, chatterParseId, callback);
    }
    private void enableBanner(int speed, final String chatterParseId, final SimpleCallback callback){
        bannerShowingAtAll = true;

        if(banner.partiallyUp){
            if(callback != null)
                callback.done();
        }

        if(chatterParseId.startsWith("group")){
            int groupId = Integer.parseInt(chatterParseId.substring(6));
            for(Group group : app.getFriendsList().getAllGroups()){
                if(group.getId() == groupId){
                    chatRecipients = group;
                    break;
                }

            }
        }
        else
            chatRecipients = Group.getOrCreateGroup(chatterParseId, app.getFriendsList().getAllGroups());

        final Context context = this;
        banner.setupBanner(speed, new VariableCallback<Integer>() {
            @Override
            public void done(Integer i) {
                map.setPadding(0, 0, 0, i - banner.shadowHeight);
                View myLocButton = findViewById(R.id.my_location_button);
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) myLocButton.getLayoutParams();
                layoutParams.bottomMargin = i - banner.shadowHeight + GeneralUtils.convertDpToPixel(10, context);
                myLocButton.invalidate();

                if (callback != null && i == banner.titleBarHeight)
                    callback.done();
            }
        }, this, chatRecipients.getColor());

        refreshChatHistory();
    }
    private void disableBanner(){
        bannerShowingAtAll = false;

        final Context context = this;
        banner.removeBanner(new VariableCallback<Integer>() {
            @Override
            public void done(Integer i) {
                map.setPadding(0, 0, 0, i - banner.shadowHeight);
                View myLocButton = findViewById(R.id.my_location_button);
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)myLocButton.getLayoutParams();
                int tenDpInPx = GeneralUtils.convertDpToPixel(10, context);
                layoutParams.bottomMargin = i - banner.shadowHeight + tenDpInPx;
                if(layoutParams.bottomMargin < tenDpInPx)
                    layoutParams.bottomMargin = tenDpInPx;
                myLocButton.invalidate();
            }
        });
    }


    //chat functions
    public boolean isChattingWithPerson(String chatterObjId){
        return (banner.allTheWayUp && chatRecipients != null && chatRecipients.containsAll(chatterObjId.split("_")));
    }

    public void refreshChatHistory(){
        final MapActivity activity = this;
        ParseAndFacebookUtils.getOrCreateMessageHistory(chatRecipients.getUserIds(), new VariableCallback<ParseObject>() {
            @Override
            public void done(ParseObject messageHistory) {
                activity.messageHistory = messageHistory;
                ParseAndFacebookUtils.getAllMessagesFromHistory(messageHistory, new VariableCallback<MessageList>() {
                    @Override
                    public void done(MessageList messageList) {
                        activity.messageList = messageList;
                        fillListView();
                    }
                });
            }
        });
    }
    private void fillListView() {
        if (chatRecipients != null) {
            ((TextView)banner.findViewById(R.id.fullname)).setText(chatRecipients.getNames());
            ((ImageView)banner.findViewById(R.id.prof_pic)).setImageBitmap(chatRecipients.getGroupImage());
            banner.findViewById(R.id.title_bar).setBackgroundColor(chatRecipients.getColor());
        }

        ListView listView = (ListView) findViewById(R.id.listview);
        MessageListAdapter arrayAdapter = new MessageListAdapter(this, listView, messageHistory, messageList, ParseUser.getCurrentUser(), chatRecipients, "No messages", (MessageListAdapter) listView.getAdapter(), new SimpleCallback() {
            @Override
            public void done() {
                refreshChatHistory();
            }
        });
        listView.setAdapter(arrayAdapter);
    }
}
