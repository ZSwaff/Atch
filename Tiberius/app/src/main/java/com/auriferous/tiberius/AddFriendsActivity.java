package com.auriferous.tiberius;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.auriferous.tiberius.Callbacks.ListUserCallback;
import com.auriferous.tiberius.Callbacks.ViewUpdateCallback;
import com.auriferous.tiberius.Users.User;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseUser;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class AddFriendsActivity extends AppCompatActivity {
    volatile ArrayList<User> usersWhoSentFriendRequests = new ArrayList<>();

    boolean searchMode = false;
    String searchTerm = "a";
    //todo update search term

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friends);

        //TODO move this to login finished area
        GraphRequest request = GraphRequest.newMyFriendsRequest(
                AccessToken.getCurrentAccessToken(),
                new GraphRequest.GraphJSONArrayCallback() {
                    @Override
                    public void onCompleted(JSONArray array, GraphResponse response) {
                        ((AtchApplication) getApplication()).populateFacebookFriendList(array, new ViewUpdateCallback() {
                            @Override
                            public void updateView() {
                                fillListView();
                            }
                        });
                    }
                });
        request.executeAsync();

        MyParseFacebookUtils.getUsersWhoHaveRequestedToFriendCurrentUser(new ListUserCallback() {
            @Override
            public void done(ArrayList<User> list) {
                usersWhoSentFriendRequests = list;
                fillListView();
            }
        });

        fillListView();

        MenuItem searchBar = (MenuItem) findViewById(R.id.search);
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

        if (id == R.id.search) {
            searchMode = !searchMode;
            fillListView();
        }

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private synchronized void fillListView() {
        if(searchMode) fillListViewSearch();
        else fillListViewNormal();
    }
    private synchronized void fillListViewNormal() {
        ArrayList<User> users = ((AtchApplication) getApplication()).getFacebookFriendsList().getAllUsers();
        ListView listView = (ListView) findViewById(R.id.listview);

        AddFriendScreenListAdapter arrayAdapter = new AddFriendScreenListAdapter(this, "Pending requests", usersWhoSentFriendRequests, "Facebook friends", users);

        listView.setAdapter(arrayAdapter);
    }
    private synchronized void fillListViewSearch() {
        final ListView listView = (ListView) findViewById(R.id.listview);
        final Context context = this;

        MyParseFacebookUtils.getUsersWithMatchingUsernameOrFullname(searchTerm, new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> list, ParseException e) {
                ArrayList<User> usersFound = new ArrayList<>();
                for(ParseUser user : list) usersFound.add(new User(user));

                //todo filter out the fb friends
                AddFriendScreenListAdapter arrayAdapter = new AddFriendScreenListAdapter(context, "Facebook friends", new ArrayList<User>(), "All users", usersFound);
                listView.setAdapter(arrayAdapter);
            }
        });
    }

}
