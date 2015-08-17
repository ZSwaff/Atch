package com.auriferous.tiberius.ReadingWriting;

import com.auriferous.tiberius.Friends.UserList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class Writer
{
    public static void writeToTextFile(File file, String message)
    {
        try {
            FileWriter fileWriter = new FileWriter(file, true);
            BufferedWriter out = new BufferedWriter(fileWriter);
            out.write(message);
            out.close(); fileWriter.close();

        } catch (IOException e) {}
    }
    public static void writeToTextFile(File root, String fileName, String message)
    {
        if (!root.exists())
            root.mkdirs();
        File file = new File(root, fileName+(fileName.contains(".txt")?"":".txt"));
        writeToTextFile(file, message);
    }

    public static void writeFriendList(File file, UserList list){
        try{
            FileOutputStream fOut = new FileOutputStream(file);
            ObjectOutputStream oOS = new ObjectOutputStream(fOut);
            oOS.writeObject(list);
            oOS.close();
            fOut.close();
        } catch(IOException ex){
            ex.printStackTrace();
        }
    }
}
