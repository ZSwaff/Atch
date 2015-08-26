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
import android.widget.ListView;
import android.widget.TextView;

import com.auriferous.atch.AtchApplication;
import com.auriferous.atch.BannerTouchView;
import com.auriferous.atch.Callbacks.FuncCallback;
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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FunctionCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.ArrayList;

public class MapActivity  extends BaseFriendsActivity {
    private GoogleMap map;
    private BannerTouchView banner;
    private boolean locLoaded = false;

    private boolean inChat = false;
    private User chatRecipient;
    private volatile ParseObject messageHistory;
    private volatile MessageList messageList;


    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        banner = (BannerTouchView)findViewById(R.id.map_banner);

        EditText messageBox = (EditText) findViewById(R.id.message_box);
        messageBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage(null);
                    return true;
                }
                return false;
            }
        });

        setViewUpdateCallback(new ViewUpdateCallback() {
            @Override
            public void updateView() {
                addMarkers();
                fillListView();
            }
        });
        setUpMapIfNeeded();
        fillListView();
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
        fillListView();
    }
    @Override
    protected void onRestart() {
        super.onRestart();

        String type = getIntent().getStringExtra("type");
        String chatterPID;
        if (type != null && type.equals("message")) {
            chatterPID = getIntent().getStringExtra("chatterParseId");
            if (chatterPID != null) {
                enableDisableBanner(true, chatterPID);
                banner.putAllTheWayUp();
                getIntent().removeExtra("type");
                getIntent().removeExtra("message");

                panTo(chatRecipient.getLocation());
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public void onBackPressed(){
        if(inChat){
            inChat = false;
            enableDisableBanner(false, null);
        }
        else
            super.onBackPressed();
    }


    public void logOut(View view) {
        startActivity(new Intent(getApplicationContext(), AtchAgreementActivity.class));
    }
    public void switchToFriends(View view) {
        Intent intent = new Intent(getApplicationContext(), ViewFriendsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }
    public void findMyLocation(View view) {
        Location myLoc = map.getMyLocation();
        if(myLoc == null) {
            locLoaded = false;
            return;
        }
        panTo(new LatLng(myLoc.getLatitude(), myLoc.getLongitude()));
    }
    public void panTo(LatLng pos){
        if(pos != null)
            map.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(pos, 13, 0, 0)), 700, null);
    }


    private void enableDisableBanner(boolean enable, String chatterParseId){
        inChat = enable;

        if(inChat) {
            //todo pan to user if offscreen or at edge

            banner.setupBanner(findViewById(R.id.map_view).getHeight());
            map.setPadding(0, 0, 0, banner.titleBarHeight);
            findViewById(R.id.my_location_button).setPadding(10,10,10,10+banner.titleBarHeight);

            chatRecipient = User.getUserFromMap(chatterParseId);
            ((TextView) findViewById(R.id.fullname)).setText(chatRecipient.getFullname());

            setupChatHistory();
        }
        else {
            banner.takeAllTheWayDown();
            map.setPadding(0, 0, 0, 0);
            findViewById(R.id.my_location_button).setPadding(10, 10, 10, 10);
        }
    }

    private void setUpMapIfNeeded() {
        if (map == null) {
            map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        }
        if (map != null) {
            setUpMap();
        }
    }
    private void setUpMap() {
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
                String type = getIntent().getStringExtra("type");
                String chatterPID;
                if (type != null && type.equals("message")) {
                    chatterPID = getIntent().getStringExtra("chatterParseId");
                    if (chatterPID != null) {
                        enableDisableBanner(true, chatterPID);
                        banner.putAllTheWayUp();
                        getIntent().removeExtra("type");
                        getIntent().removeExtra("message");

                        panTo(chatRecipient.getLocation());
                    }
                } else
                    findMyLocation(null);
            }
        });
        map.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                if(!locLoaded){
                    locLoaded = true;
                    findMyLocation(null);
                }
            }
        });

        addMarkers();
    }
    public void addMarkers() {
        map.clear();
        AtchApplication app = (AtchApplication)(getApplication());
        ArrayList<User> friends = app.getFriendsList().getAllUsers();

        for (User friend : friends) {
            MarkerOptions marker = friend.getMarker();
            if(marker != null)
                map.addMarker(marker);
        }
    }

    //old marker zoom in on friends idea
    /*
    public void addMarkers() {
        map.clear();
        AtchApplication app = (AtchApplication)(getApplication());
        ArrayList<User> friends = app.getFriendsList().getAllUsers();

        final double minRadialDist = .008; //in degrees
        final double maxRadius = 8000; //in meters

        double lat = 37.427325;
        double lng = -122.169882;
        Location userLocation = app.getCurrentLocation();
        if(userLocation != null) {
            lat = userLocation.getLatitude();
            lng = userLocation.getLongitude();
        }
        LatLngBounds mapBounds = new LatLngBounds(new LatLng(lat-minRadialDist, lng-minRadialDist), new LatLng(lat+minRadialDist, lng+minRadialDist));

        //todo sort by distance first, then stop expanding if we get too many
        for (User friend : friends) {
            MarkerOptions marker = friend.getMarker();
            if(marker != null) {
                map.addMarker(marker);
                LatLngBounds newMapBounds = mapBounds.including(marker.getPosition());
                LatLng sw = newMapBounds.southwest, cent = newMapBounds.getCenter();
                float[] results = new float[1];
                Location.distanceBetween(sw.latitude,sw.longitude,cent.latitude,cent.longitude,results);
                float radialDist = results[0];
                if(radialDist < maxRadius)
                    mapBounds = newMapBounds;
            }
        }

        try {
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(mapBounds, 0), 500, null);
        }
        catch (IllegalStateException iSE) {}
    }
    * */



    public void sendMessage(View view) {
        EditText messageBox = (EditText) findViewById(R.id.message_box);
        String newMessage = messageBox.getText().toString();
        if(newMessage.isEmpty()) return;
        ParseAndFacebookUtils.sendMessage(messageHistory, newMessage, new FuncCallback<Object>() {
            @Override
            public void done(Object o) {
                setupChatHistory();
            }
        });
        messageBox.setText("");
    }
    public void sendMessageMeetHere(View view) {
        ParseAndFacebookUtils.sendMessage(messageHistory, "Meet here", new FuncCallback<Object>() {
            @Override
            public void done(Object o) {
                setupChatHistory();
            }
        });
    }
    public void sendMessageMeetThere(View view) {
        ParseAndFacebookUtils.sendMessage(messageHistory, "Meet there", new FuncCallback<Object>() {
            @Override
            public void done(Object o) {
                setupChatHistory();
            }
        });
    }

    public boolean isChattingWithPerson(String chatterObjId){
        boolean ret = (inChat && chatRecipient != null && chatRecipient.getId().equals(chatterObjId));
        if(ret) ;
            //todo notify somehow
        return ret;
    }
    public void setupChatHistory(){
        final MapActivity activity = this;
        ParseAndFacebookUtils.getOrCreateMessageHistory(chatRecipient.getId(), new FunctionCallback<ParseObject>() {
            @Override
            public void done(ParseObject messageHistory, ParseException e) {
                activity.messageHistory = messageHistory;
                ParseAndFacebookUtils.getAllMessagesFromHistory(messageHistory, new FuncCallback<MessageList>() {
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
        MessageListAdapter arrayAdapter = new MessageListAdapter(this, messageList, ParseUser.getCurrentUser(), "No messages");

        ListView listView = (ListView) findViewById(R.id.listview);
        listView.setAdapter(arrayAdapter);
    }
}
