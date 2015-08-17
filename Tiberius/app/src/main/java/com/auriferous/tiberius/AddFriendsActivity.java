package com.auriferous.tiberius;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.auriferous.tiberius.Friends.User;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;

import org.json.JSONArray;

import java.util.ArrayList;

public class AddFriendsActivity extends AppCompatActivity {
    volatile ArrayList<User> usersWhoSentFriendRequests = new ArrayList<User>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friends);

        GraphRequest request = GraphRequest.newMyFriendsRequest(
                AccessToken.getCurrentAccessToken(),
                new GraphRequest.GraphJSONArrayCallback() {
                    @Override
                    public void onCompleted(JSONArray array, GraphResponse response) {
                        ((TiberiusApplication) getApplication()).populateFacebookFriendList(array, new ViewUpdateCallback() {
                            @Override
                            public void updateView() {
                                fillListView();
                            }
                        });
                    }
                });
        request.executeAsync();

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... voids) {
                usersWhoSentFriendRequests = MyParseFacebookUtils.getUsersWhoHaveRequestedToFriendCurrentUser();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        fillListView();
                    }
                });
                return null;
            }
        };
        task.execute();

        fillListView();
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(getApplicationContext(), MapActivity.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_friends, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private synchronized void fillListView() {
        ArrayList<User> users = ((TiberiusApplication) getApplication()).getFacebookFriendsList().getAllUsers();
        ListView listView = (ListView) findViewById(R.id.listview);

        AddFriendScreenListAdapter arrayAdapter = new AddFriendScreenListAdapter(this, usersWhoSentFriendRequests, users);

        listView.setAdapter(arrayAdapter);
    }
}
