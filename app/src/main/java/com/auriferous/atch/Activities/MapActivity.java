package com.auriferous.atch.Activities;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
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
import com.auriferous.atch.Users.User;
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

    private User chatRecipient;
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
        banner.getBackground().setAlpha(242);

        final View mapView = findViewById(R.id.map_view);
        ViewTreeObserver vto = mapView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                banner.setHeight(mapView.getHeight());
                ViewTreeObserver obs = mapView.getViewTreeObserver();
                obs.removeOnGlobalLayoutListener(this);

                if(pendingImmediateChat){
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
        findViewById(R.id.friends_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchToFriends();
            }
        });
        findViewById(R.id.my_location_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findMyLocation();
            }
        });


        findViewById(R.id.send_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });
        findViewById(R.id.meet_here).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessageMeetHere();
            }
        });
        findViewById(R.id.meet_there).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessageMeetThere();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        immediateChat();

        setViewUpdateCallback(new ViewUpdateCallback() {
            @Override
            public void updateView() {
                addMarkers();
                fillListView();
            }
        });
        addMarkers();
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
                String chatterPID;
                chatterPID = getIntent().getStringExtra("chatterParseId");
                if (chatterPID != null) {
                    enableBanner(chatterPID, new SimpleCallback() {
                        @Override
                        public void done() {
                            banner.putAllTheWayUp();
                        }
                    });
                    getIntent().removeExtra("type");
                    getIntent().removeExtra("message");

                    if(chatRecipient != null)
                        panTo(chatRecipient.getLocation());
                }
            }
            else if (type.equals("zoom")) {
                String chatterPID;
                chatterPID = getIntent().getStringExtra("chatterParseId");
                if (chatterPID != null) {
                    enableBanner(chatterPID, null);
                    getIntent().removeExtra("type");
                    getIntent().removeExtra("message");

                    panTo(chatRecipient.getLocation());
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
    private void findMyLocation() {
        Location myLoc = map.getMyLocation();
        if(myLoc == null) {
            locLoaded = false;
            return;
        }
        panTo(new LatLng(myLoc.getLatitude(), myLoc.getLongitude()));
    }
    private void panTo(LatLng pos){
        if(pos != null)
            map.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(pos, 12, 0, 0)), 700, null);
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
                findMyLocation();
            }
        });
        map.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                if(!locLoaded){
                    locLoaded = true;
                    findMyLocation();
                }
            }
        });

        addMarkers();
    }
    private void addMarkers() {
        map.clear();
        AtchApplication app = (AtchApplication)(getApplication());
        ArrayList<User> friends = app.getFriendsList().getAllUsers();

        for (User friend : friends) {
            MarkerOptions marker = friend.getMarker();
            if(marker != null)
                map.addMarker(marker);
        }
    }


    private void sendMessage() {
        EditText messageBox = (EditText) findViewById(R.id.message_box);
        String newMessage = messageBox.getText().toString();
        if(newMessage.isEmpty()) return;
        ParseAndFacebookUtils.sendMessage(messageHistory, newMessage, new SimpleCallback() {
            @Override
            public void done() {
                refreshChatHistory();
            }
        });
        messageBox.setText("");
    }
    private void sendMessageMeetHere() {
        ParseAndFacebookUtils.sendMessage(messageHistory, "Meet here", new SimpleCallback() {
            @Override
            public void done() {
                refreshChatHistory();
            }
        });
    }
    private void sendMessageMeetThere() {
        ParseAndFacebookUtils.sendMessage(messageHistory, "Meet there", new SimpleCallback() {
            @Override
            public void done() {
                refreshChatHistory();
            }
        });
    }


    private void enableBanner(final String chatterParseId, final SimpleCallback callback){
        bannerShowingAtAll = true;

        if(banner.partiallyUp){
            if(callback != null)
                callback.done();
        }

        final Context context = this;
        banner.setupBanner(new VariableCallback<Integer>() {
            @Override
            public void done(Integer i) {
                map.setPadding(0, 0, 0, i);
                View myLocButton = findViewById(R.id.my_location_button);
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) myLocButton.getLayoutParams();
                layoutParams.bottomMargin = i + GeneralUtils.convertDpToPixel(10, context);
                myLocButton.invalidate();

                if (callback != null && i == banner.titleBarHeight)
                    callback.done();
            }
        });

        if(app.isFriendListLoaded()) {
            chatRecipient = User.getUserFromMap(chatterParseId);

            ((TextView) banner.findViewById(R.id.fullname)).setText(chatRecipient.getFullname());
            refreshChatHistory();
        }
        else {
            app.setFriendListLoadedCallback(new SimpleCallback() {
                @Override
                public void done() {
                    chatRecipient = User.getUserFromMap(chatterParseId);

                    ((TextView) banner.findViewById(R.id.fullname)).setText(chatRecipient.getFullname());
                    refreshChatHistory();
                }
            });
        }
    }
    private void disableBanner(){
        bannerShowingAtAll = false;

        final Context context = this;
        banner.removeBanner(new VariableCallback<Integer>() {
            @Override
            public void done(Integer i) {
                map.setPadding(0, 0, 0, i);
                View myLocButton = findViewById(R.id.my_location_button);
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)myLocButton.getLayoutParams();
                layoutParams.bottomMargin = i + GeneralUtils.convertDpToPixel(10, context);
                myLocButton.invalidate();
            }
        });
    }


    //chat functions
    public boolean isChattingWithPerson(String chatterObjId){
        boolean ret = (bannerShowingAtAll && chatRecipient != null && chatRecipient.getId().equals(chatterObjId));
        if(ret) ;
            //todo notify somehow
        return ret;
    }

    public void refreshChatHistory(){
        final MapActivity activity = this;
        ParseAndFacebookUtils.getOrCreateMessageHistory(chatRecipient.getId(), new VariableCallback<ParseObject>() {
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
        ListView listView = (ListView) findViewById(R.id.listview);
        MessageListAdapter arrayAdapter = new MessageListAdapter(this, listView, messageList, ParseUser.getCurrentUser(), "No messages", (MessageListAdapter)listView.getAdapter());
        listView.setAdapter(arrayAdapter);
    }
}
