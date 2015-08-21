package com.auriferous.tiberius.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.auriferous.tiberius.AtchApplication;
import com.auriferous.tiberius.Callbacks.ViewUpdateCallback;
import com.auriferous.tiberius.R;
import com.auriferous.tiberius.Users.User;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class MapActivity  extends BaseFriendsActivity {
    private GoogleMap map;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        setViewUpdateCallback(new ViewUpdateCallback() {
            @Override
            public void updateView() {
                addMarkers();
            }
        });
        setUpMapIfNeeded();
    }
    @Override
    protected void onResume() {
        super.onResume();
        setViewUpdateCallback(new ViewUpdateCallback() {
            @Override
            public void updateView() {
                addMarkers();
            }
        });
        setUpMapIfNeeded();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.switch_to_friend_screen) {
            startActivity(new Intent(getApplicationContext(), ViewFriendsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
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

        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker) {
                Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
                intent.putExtra("userParseId", marker.getSnippet());
                startActivity(intent);
                return true;
            }
        });

        addMarkers();
    }

    public void addMarkers(){
        map.clear();
        AtchApplication app = (AtchApplication)(getApplication());
        ArrayList<User> friends = app.getFriendsList().getAllUsers();
        for (User friend : friends) {
            MarkerOptions marker = friend.getMarker();
            if(marker != null)
                map.addMarker(marker);
        }
    }
}
