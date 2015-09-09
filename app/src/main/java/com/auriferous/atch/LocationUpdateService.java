package com.auriferous.atch;

import com.auriferous.atch.Callbacks.SimpleCallback;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import java.util.Date;

public class LocationUpdateService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    GoogleApiClient googleApiClient;
    LocationRequest locationRequest;


    @Override
    public void onCreate() {
        super.onCreate();

        buildGoogleApiClient();
        createLocationRequest();
        googleApiClient.connect();
    }
    protected void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }
    protected void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(60000);
        locationRequest.setFastestInterval(60000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onDestroy() {
        googleApiClient.disconnect();

        super.onDestroy();
    }


    @Override
    public void onConnected(Bundle connectionHint) {
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }
    @Override
    public void onConnectionSuspended(int code) {
        Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onLocationChanged(Location location) {
        final AtchApplication app = (AtchApplication)getApplication();
        app.setCurrentLocation(location);
        app.setLastUpdateTime(new Date());

        ParseAndFacebookUtils.updateMyLocation(location);

        ParseAndFacebookUtils.updateFriendDataWithMostRecentLocations(app.getFriendsList(), new SimpleCallback() {
            @Override
            public void done() {
                app.updateView();
            }
        });
    }


    @Override
    public IBinder onBind(Intent intent)
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
