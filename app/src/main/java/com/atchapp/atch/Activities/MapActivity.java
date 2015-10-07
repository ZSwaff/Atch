package com.atchapp.atch.Activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.atchapp.atch.ActionEditText;
import com.atchapp.atch.AtchApplication;
import com.atchapp.atch.Callbacks.SimpleCallback;
import com.atchapp.atch.Callbacks.VariableCallback;
import com.atchapp.atch.Callbacks.ViewUpdateCallback;
import com.atchapp.atch.GeneralUtils;
import com.atchapp.atch.Messages.Message;
import com.atchapp.atch.Messages.MessageList;
import com.atchapp.atch.Messages.MessageListAdapter;
import com.atchapp.atch.ParseAndFacebookUtils;
import com.atchapp.atch.R;
import com.atchapp.atch.UiElements.BannerTouchView;
import com.atchapp.atch.UiElements.SwipeRefreshLayoutBottom.SwipeRefreshLayoutBottom;
import com.atchapp.atch.Users.Group;
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
import java.util.HashMap;

public class MapActivity  extends BaseFriendsActivity {
    private GoogleMap map;
    private boolean locLoaded = false;
    private boolean splashScreenActive = true;

    private BannerTouchView banner;
    private boolean bannerShowingAtAll = false;
    private boolean pendingImmediateChat = false;

    private Group chatRecipients;
    private volatile MessageList messageList;

    private HashMap<String, String> messagesInProgress = new HashMap<>();


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
        messageBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                messagesInProgress.put(chatRecipients.getIdsInString(null), s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {
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
                sendMessageMeetHereOrThere('h');
            }
        });
        GeneralUtils.addButtonEffect(findViewById(R.id.meet_here));
        findViewById(R.id.meet_there).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessageMeetHereOrThere('t');
            }
        });
        GeneralUtils.addButtonEffect(findViewById(R.id.meet_there));

        SwipeRefreshLayoutBottom swipeRefreshObj = (SwipeRefreshLayoutBottom) findViewById(R.id.swipe_refresh_layout);
        swipeRefreshObj.setColorSchemeColors(GeneralUtils.generateNewColors(30));
        swipeRefreshObj.setOnRefreshListener(new SwipeRefreshLayoutBottom.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshChatHistory();
            }
        });

        int bgColor = GeneralUtils.generateNewColor();
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xffffffff, bgColor});
        gd.setCornerRadius(0f);
        findViewById(R.id.splash_screen).setBackground(gd);
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

        if (chatRecipients != null)
            chatRecipients = Group.getOrCreateGroup(chatRecipients.getIdsInString(null), app.getFriendsList().getAllGroups());

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
            } else if (type.equals("zoom") || type.equals("login")) {
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
        locLoaded = false;

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
                Group crBefore = chatRecipients;
                enableBanner(marker.getSnippet(), null);
                if (chatRecipients.equals(crBefore))
                    panTo(chatRecipients.getLocation(), 400);
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
                                    splashScreenActive = false;
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
        ParseAndFacebookUtils.sendMessage(messageList.getMessageHistory(), newMessage, 'n', new SimpleCallback() {
            @Override
            public void done() {
                refreshChatHistory();
            }
        });
        messagesInProgress.put(chatRecipients.getIdsInString(null), "");
        messageBox.setText("");

        addFakeMessageToChatHistory(newMessage, 'n');
    }
    private void sendMessageMeetHereOrThere(char flag) {
        String messageText = "Meet ";
        if (flag == 'h') messageText += "here";
        if (flag == 't') messageText += "there";
        ParseAndFacebookUtils.sendMessage(messageList.getMessageHistory(), messageText, flag, new SimpleCallback() {
            @Override
            public void done() {
                refreshChatHistory();
            }
        });

        addFakeMessageToChatHistory(messageText, flag);
    }
    private void addFakeMessageToChatHistory(String messageText, char decorationFlag) {
        messageList.addFakeMessage(messageText, decorationFlag, ParseUser.getCurrentUser());
        ((MessageListAdapter) ((ListView) findViewById(R.id.listview)).getAdapter()).notifyDataSetChanged();
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
        if (!chatterParseId.startsWith("group") || chatRecipients == null)
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
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) myLocButton.getLayoutParams();
                int tenDpInPx = GeneralUtils.convertDpToPixel(10, context);
                layoutParams.bottomMargin = i - banner.shadowHeight + tenDpInPx;
                if (layoutParams.bottomMargin < tenDpInPx)
                    layoutParams.bottomMargin = tenDpInPx;
                myLocButton.invalidate();
            }
        });
    }


    //chat functions
    public boolean isSplashScreenActive() {
        return splashScreenActive;
    }
    public boolean isChattingWithPerson(String chatterObjId){
        return (banner.allTheWayUp && chatRecipients != null && chatRecipients.matchesAll(chatterObjId.split("_")));
    }

    public void refreshChatHistory(){
        if (chatRecipients == null) return;

        fillListView();
        ParseAndFacebookUtils.getOrCreateMessageHistory(chatRecipients.getUserIds(), new VariableCallback<ParseObject>() {
            @Override
            public void done(ParseObject messageHistory) {
                ((AtchApplication) getApplication()).refreshMessageList(chatRecipients, messageHistory, new SimpleCallback() {
                    @Override
                    public void done() {
                        SwipeRefreshLayoutBottom swipeRefreshObj = (SwipeRefreshLayoutBottom) findViewById(R.id.swipe_refresh_layout);
                        swipeRefreshObj.setRefreshing(false);
                        swipeRefreshObj.setColorSchemeColors(GeneralUtils.generateNewColors(30));
                        fillListView();
                    }
                });
            }
        });
    }
    private void fillListView() {
        if (chatRecipients == null) return;

        messageList = ((AtchApplication) getApplication()).getMessageList(chatRecipients);
        ((TextView) banner.findViewById(R.id.fullname)).setText(chatRecipients.getNames());
        ((ImageView) banner.findViewById(R.id.prof_pic)).setImageBitmap(chatRecipients.getGroupImage(false));
        banner.findViewById(R.id.title_bar).setBackgroundColor(chatRecipients.getColor());
        ActionEditText editText = (ActionEditText) findViewById(R.id.message_box);
        int start = editText.getSelectionStart(), end = editText.getSelectionEnd();
        editText.setText(messagesInProgress.get(chatRecipients.getIdsInString(null)));
        editText.setSelection(start, end);

        ListView listView = (ListView) findViewById(R.id.listview);
        String firstMessageId = null;
        int scrollFromTop = 0;
        if (listView.getAdapter() != null && listView.getCount() > 0) {
            if (!(listView.getLastVisiblePosition() == listView.getCount() - 1 && listView.getChildAt(listView.getChildCount() - 1).getBottom() <= listView.getHeight())) {
                Message firstMessage = (Message) listView.getItemAtPosition(listView.getFirstVisiblePosition());
                if (firstMessage != null) {
                    View v = listView.getChildAt(0);
                    if (v != null) {
                        scrollFromTop = v.getTop() - listView.getPaddingTop();
                        firstMessageId = firstMessage.getObjectId();
                    }
                }
            }
        }

        if (messageList != null) {
            MessageListAdapter arrayAdapter = new MessageListAdapter(this, listView, messageList.getMessageHistory(), messageList, ParseUser.getCurrentUser(), chatRecipients, "No messages", new SimpleCallback() {
                @Override
                public void done() {
                    refreshChatHistory();
                }
            });
            listView.setAdapter(arrayAdapter);
        }

        boolean scrolled = false;
        if (firstMessageId != null) {
            for (int i = listView.getCount() - 1; i >= 0; i--) {
                Message item = (Message) listView.getItemAtPosition(i);
                if (item != null && item.getObjectId() != null && item.getObjectId().equals(firstMessageId)) {
                    listView.setSelectionFromTop(i, scrollFromTop);
                    scrolled = true;
                    break;
                }
            }
        }
        if (!scrolled) {
            int scrollTo = listView.getCount() - 1;
            if (scrollTo != -1)
                listView.setSelectionFromTop(scrollTo, scrollFromTop);
        }
    }
}
