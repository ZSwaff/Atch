package com.auriferous.atch.Users;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;

public class UserInfoGroup implements Serializable{
    public static final String INFO_GROUP_SAVE_NAME = "userinfogroupsave";

    private HashMap<String, Integer> colorToUserMap = new HashMap<>();

    private UserInfoGroup() {}
    public UserInfoGroup(HashMap<String, User> userMap){
        for(String key : userMap.keySet()){
            User user = userMap.get(key);
            colorToUserMap.put(user.getId(), user.getRelativeColor());
        }
    }
    public UserInfoGroup(File file){
        UserInfoGroup info;
        try{
            FileInputStream fIn = new FileInputStream(file);
            ObjectInputStream oIS = new ObjectInputStream(fIn);
            info = (UserInfoGroup) oIS.readObject();
            oIS.close();
            fIn.close();
            colorToUserMap = info.colorToUserMap;
        }catch(IOException e){
            e.printStackTrace();
        }catch(ClassNotFoundException e){
            e.printStackTrace();
        }
    }


    public int getColor(String uid){
        if(colorToUserMap.containsKey(uid))
            return colorToUserMap.get(uid);
        return -1;
    }

    public void writeUserInfoGroup(File file){
        try{
            FileOutputStream fOut = new FileOutputStream(file);
            ObjectOutputStream oOS = new ObjectOutputStream(fOut);
            oOS.writeObject(this);
            oOS.close();
            fOut.close();
        } catch(IOException ex){
            ex.printStackTrace();
        }
    }

    public static void autoSave(Context context, HashMap<String, User> userMap){
        UserInfoGroup infoGroup = new UserInfoGroup(userMap);
        File file = new File(context.getFilesDir(), INFO_GROUP_SAVE_NAME);
        infoGroup.writeUserInfoGroup(file);
    }
    public static UserInfoGroup autoLoad(Context context){
        File storedInfo = new File(context.getFilesDir(), INFO_GROUP_SAVE_NAME);
        if(storedInfo.exists()){
            return new UserInfoGroup(storedInfo);
        }
        return new UserInfoGroup();
    }
}
