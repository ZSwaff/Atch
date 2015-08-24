package com.auriferous.atch.Activities;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import com.auriferous.atch.AtchApplication;
import com.auriferous.atch.Callbacks.ViewUpdateCallback;
import com.auriferous.atch.LocationUpdateService;
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
        addMarkers();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }


    public void logOut(View view) {
        ((AtchApplication)getApplication()).stopLocationUpdates();

        startActivity(new Intent(getApplicationContext(), AtchAgreementActivity.class));
    }
    public void switchToFriends(View view) {
        startActivity(new Intent(getApplicationContext(), ViewFriendsActivity.class));
    }
    public void findMyLocation(View view) {
        try {
            Location myLoc = map.getMyLocation();
            if(myLoc != null)
                map.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(myLoc.getLatitude(), myLoc.getLongitude()), 13, 0, 0)), 700, null);
        }
        catch (IllegalStateException iSE) {}
        map.getMyLocation();
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

        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker) {
                Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
                intent.putExtra("chatterParseId", marker.getSnippet());
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

        final double minRadialDist = .04; //in degrees
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
}
