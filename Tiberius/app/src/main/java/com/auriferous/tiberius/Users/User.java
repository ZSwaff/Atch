package com.auriferous.tiberius.Users;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

public class User implements Serializable {
    private ParseUser user;
    private ParseObject privateData = null;

    Bitmap profPic = null;

    private MarkerOptions marker = null;


    public User(ParseUser parseUser){
        user = parseUser;
        setFacebookProfilePicture();
    }

    public void setFacebookProfilePicture(){
        if(user == null) return;
        String fbid = user.getString("fbid");
        if(fbid == null) return;

        //todo blocking?
        try {
            URL imageURL = new URL("https://graph.facebook.com/" + fbid + "/picture?type=large");
            profPic = BitmapFactory.decodeStream(imageURL.openConnection().getInputStream());
        }
        catch (MalformedURLException mUE) {}
        catch (IOException iOE) {}
    }

    public void setPrivateData(ParseObject privateData){
        this.privateData = privateData;
        createMarkerIfPossible();
    }
    public void createMarkerIfPossible() {
        marker = null;
        if (privateData == null) return;
        ParseGeoPoint loc = privateData.getParseGeoPoint("location");

        if(profPic == null)
            marker = new MarkerOptions().position(new LatLng(loc.getLatitude(), loc.getLongitude()));
        else
            marker = new MarkerOptions().position(new LatLng(loc.getLatitude(), loc.getLongitude()))
                .icon(BitmapDescriptorFactory.fromBitmap(profPic))
                .anchor(.5f,.5f);
    }

    public ParseUser getUser() {
        return user;
    }
    public Bitmap getProfPic() {
        return profPic;
    }
    public MarkerOptions getMarker() {
        return marker;
    }
    public String getFullname(){
        return user.getString("fullname");
    }
    public String getUsername(){
        return user.getUsername();
    }
    public String getId(){
        return user.getObjectId();
    }

    @Override
    public String toString() {
        return "";
    }
}
