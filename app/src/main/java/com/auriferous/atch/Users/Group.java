package com.auriferous.atch.Users;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

public class Group {
    private static final float maxDistanceInMetersForGroup = 100;
    private static final int rakishAngle = 20;

    private int id = -1;
    private ArrayList<User> usersInGroup;
    private LatLng location;
    private Bitmap groupImage = null;
    private MarkerOptions marker = null;


    private Group() {
        usersInGroup = new ArrayList<>();
    }
    private Group(User user) {
        usersInGroup = new ArrayList<>();
        usersInGroup.add(user);
    }
    public static ArrayList<Group> getGroups(UserList users) {
        ArrayList<User> allUsers = new ArrayList<>();
        for(User user : users.getAllUsers())
            if(user.getLocation() != null)
                allUsers.add(user);

        Group[] allGroups = new Group[allUsers.size()];
        for(int i = 0; i < allUsers.size(); i++)
            allGroups[i] = new Group(allUsers.get(i));

        for(int i = 0; i < allUsers.size(); i++){
            for(int j = i + 1; j < allUsers.size(); j++){
                if(allGroups[i] == allGroups[j]) continue;

                if(allUsers.get(i).getDistanceInMetersFrom(allUsers.get(j)) > maxDistanceInMetersForGroup) continue;

                allGroups[i].addUsersFrom(allGroups[j]);
                for(int k = 0; k < allUsers.size(); k++)
                    if(allGroups[k] == allGroups[j])
                        allGroups[k] = allGroups[i];
            }
        }

        HashSet<Group> set = new HashSet<>();
        Collections.addAll(set, allGroups);

        ArrayList<Group> ret = new ArrayList<>();
        int counter = 0;
        for(Group group : set){
            group.id = counter;
            counter++;
            group.recalcLocation();
            group.makeImage();
            group.setupMarker();
            ret.add(group);
        }
        return ret;
    }
    public static Group getGroupFromNumber(String groupNumberString, ArrayList<Group> allFriendListGroups) {
        int groupId = Integer.parseInt(groupNumberString.substring(6));

        for(Group g : allFriendListGroups)
            if(g.getId() == groupId) return g;

        return null;
    }
    public static Group getOrCreateGroup(String chatterIds, ArrayList<Group> allFriendListGroups) {
        for(Group g : allFriendListGroups)
            if (g.matchesAll(chatterIds.trim().split("_"))) return g;

        Group newGroup = new Group();
        for(String chatterId : chatterIds.trim().split("_")){
            User currUser = User.getUserFromMap(chatterId);
            if (currUser == null) return null;
            newGroup.usersInGroup.add(currUser);
        }

        newGroup.recalcLocation();
        newGroup.makeImage();
        newGroup.setupMarker();
        return newGroup;
    }
    private void addUsersFrom(Group otherGroup) {
        usersInGroup.addAll(otherGroup.usersInGroup);
    }


    public int size(){
        return usersInGroup.size();
    }
    public boolean contains(User user){
        return usersInGroup.contains(user);
    }
    public boolean contains(String userId){
        for(User user : usersInGroup)
            if(user.getId().equals(userId)) return true;
        return false;
    }
    public boolean matchesAll(String[] userIds) {
        for (int i = 0; i < userIds.length; i++) {
            String chatterId = userIds[i];
            for (int j = 0; j < i; j++)
                if (userIds[j].equals(chatterId)) return false;
            if (!contains(chatterId)) return false;
        }
        return (userIds.length == usersInGroup.size());
    }
    public Bitmap getGroupImage() {
        return groupImage;
    }
    public MarkerOptions getMarker() {
        return marker;
    }
    public LatLng getLocation() {
        return location;
    }
    public String getNames(){
        String names = "";

        for(User user : usersInGroup)
            names += ", " + user.getFirstname();

        return names.substring(2);
    }
    public String getIdsInString(String extraId) {
        ArrayList<String> ids = new ArrayList<>();
        if (extraId != null)
            ids.add(extraId);
        for (User user : usersInGroup)
            ids.add(user.getId());
        Collections.sort(ids);
        String ret = "";
        for (String id : ids)
            ret += "_" + id;
        return ret.substring(1);
    }
    public String getNamesAsNiceList(){
        String names = "";

        for(int i = 0; i < usersInGroup.size(); i++){
            User user = usersInGroup.get(i);
            String demar = ", ";
            if(i == 0)
                demar = "";
            else if(i == usersInGroup.size() - 1){
                if(usersInGroup.size() == 2)
                    demar = " and ";
                else
                    demar = ", and ";
            }
            names += demar + user.getFirstname();
        }

        return names;
    }
    public String getSHeTheyPronoun(){
        if(usersInGroup.size() == 1)
            return usersInGroup.get(0).getSHePronoun();
        return "they";
    }
    public int getId(){
        return id;
    }
    public ArrayList<String> getUserIds(){
        ArrayList<String> ret = new ArrayList<>();
        for(User user : usersInGroup)
            ret.add(user.getId());
        return ret;
    }
    public int getColor() {
        if (usersInGroup.size() == 1)
            return usersInGroup.get(0).getRelativeColor();
        return 0xff333333;
    }
    public int getLighterColor(String userId){
        for(User user : usersInGroup)
            if(user.getId().equals(userId))
                return user.getLighterColor();
        return -1;
    }


    public void resetImage(){
        makeImage();
        setupMarker();
    }
    private void recalcLocation() {
        Collections.sort(usersInGroup, new Comparator<User>() {
            @Override
            public int compare(User user1, User user2){
                return user1.getUsername().compareTo(user2.getUsername());
            }
        });

        //todo might misbehave around the hemisphere boundaries
        int numUsers = usersInGroup.size();
        if(numUsers == 1 && usersInGroup.get(0).getLocation() == null) {
            location = null;
            return;
        }

        double centerLat = 0, centerLng = 0;
        for(User user : usersInGroup){
            LatLng userLoc = user.getLocation();
            centerLat += userLoc.latitude/((float)numUsers);
            centerLng += userLoc.longitude/((float)numUsers);
        }
        location = new LatLng(centerLat, centerLng);
    }
    private void makeImage() {
        final double singleRadius = 100;

        if(usersInGroup.size() == 0) return;
        if(usersInGroup.size() == 1) {
            groupImage = usersInGroup.get(0).getMarkerIcon();
            return;
        }

        double outerRadius = 2d * singleRadius;
        double innerRadius = 0;
        double middleRadius = singleRadius;
        double k = usersInGroup.size();
        double alpha = 360d/k;

        if(k !=  2){
            middleRadius = singleRadius / Math.sin(Math.toRadians(alpha/2d));
            outerRadius = middleRadius + singleRadius;
            innerRadius = Math.sqrt(middleRadius * middleRadius - singleRadius * singleRadius);
        }

        Bitmap newImage = Bitmap.createBitmap((int)outerRadius*2 + 10, (int)outerRadius*2 + 10, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newImage);

        //draw pie pieces
        for(int i = 0; i < usersInGroup.size(); i++) {
            User user = usersInGroup.get(i);
            int color = user.getRelativeColor();

            double specAlpha = alpha * i;

            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            canvas.drawArc(new RectF(5, 5, (int)outerRadius*2 + 5, (int)outerRadius*2 + 5), (float) (specAlpha - alpha - 90 + rakishAngle), (float) alpha, true, paint);
        }

        //clear center
        Paint paint2 = new Paint();
        paint2.setAntiAlias(true);
        paint2.setStyle(Paint.Style.FILL);
        paint2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawCircle((float)outerRadius + 5, (float)outerRadius + 5, (float)innerRadius, paint2);

        //draw prof pics
        for(int i = 0; i < usersInGroup.size(); i++) {
            User user = usersInGroup.get(i);
            Bitmap markerIcon = user.getMarkerIcon();
            if(markerIcon == null) continue;
            Bitmap profPic = Bitmap.createScaledBitmap(markerIcon, (int) singleRadius * 2, (int) singleRadius * 2, false);

            double specAlpha = alpha * i + rakishAngle;
            double centerX = middleRadius * Math.sin(Math.toRadians(specAlpha));
            double centerY = - middleRadius * Math.cos(Math.toRadians(specAlpha));

            Paint paint = new Paint();
            paint.setAntiAlias(true);
            canvas.drawBitmap(profPic, (int)(outerRadius + centerX - singleRadius) + 5, (int)(outerRadius + centerY - singleRadius) + 5, null);
        }

        groupImage = newImage;
    }
    private void setupMarker() {
        if(usersInGroup.size() == 0) return;

        int width = 200 + usersInGroup.size() * 30;

        if(groupImage != null)
            marker = new MarkerOptions().position(location)
                    .snippet("group " + getId())
                    .icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(groupImage, width, width, false)))
                    .anchor(.5f, .5f);
        else
            marker = null;
    }
}
