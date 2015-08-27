package com.auriferous.atch.Activities;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;

import com.auriferous.atch.AtchApplication;
import com.auriferous.atch.BannerTouchView;
import com.auriferous.atch.Callbacks.SimpleCallback;
import com.auriferous.atch.Callbacks.VariableCallback;
import com.auriferous.atch.Callbacks.ViewUpdateCallback;
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

        String type = getIntent().getStringExtra("type");
        if (type != null && type.equals("message")) {
            String chatterPID;
            chatterPID = getIntent().getStringExtra("chatterParseId");
            if (chatterPID != null) {
                enableDisableBanner(true, chatterPID);
                banner.putAllTheWayUp();
                getIntent().removeExtra("type");
                getIntent().removeExtra("message");

                panTo(chatRecipient.getLocation());
            }
        }

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


    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
    }
    @Override
    public void onBackPressed(){
        if(bannerShowingAtAll){
            if(banner.allTheWayUp)
                banner.takeAllTheWayDown(false);
            else
                enableDisableBanner(false, null);
        }
        else
            super.onBackPressed();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }


    private void logOut() {
        Intent intent = new Intent(getApplication(), AtchAgreementActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }
    private void switchToFriends() {
        Intent intent = new Intent(getApplication(), ViewFriendsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
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
            map.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(pos, 13, 0, 0)), 700, null);
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
                enableDisableBanner(true, marker.getSnippet());
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


    private void enableDisableBanner(boolean enable, String chatterParseId){
        bannerShowingAtAll = enable;

        if(bannerShowingAtAll) {
            banner.setupBanner(findViewById(R.id.map_view).getHeight());
            map.setPadding(0, 0, 0, banner.titleBarHeight);
            findViewById(R.id.my_location_button).setPadding(10,10,10,10+banner.titleBarHeight);

            chatRecipient = User.getUserFromMap(chatterParseId);
            ((TextView) findViewById(R.id.fullname)).setText(chatRecipient.getFullname());

            refreshChatHistory();
        }
        else {
            banner.setVisibility(View.GONE);
            map.setPadding(0, 0, 0, 0);
            findViewById(R.id.my_location_button).setPadding(10, 10, 10, 10);
        }
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
        MessageListAdapter arrayAdapter = new MessageListAdapter(this, listView, messageList, ParseUser.getCurrentUser(), "No messages");
        listView.setAdapter(arrayAdapter);
    }
}
