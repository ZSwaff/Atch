package com.auriferous.atch.Activities;

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SearchView;

import com.auriferous.atch.Callbacks.ViewUpdateCallback;
import com.auriferous.atch.Users.UserListAdapter;
import com.auriferous.atch.Callbacks.FuncCallback;
import com.auriferous.atch.ParseAndFacebookUtils;
import com.auriferous.atch.R;
import com.auriferous.atch.Users.UserListAdapterSection;
import com.auriferous.atch.Users.User;
import com.auriferous.atch.Users.UserList;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AddFriendsActivity extends BaseFriendsActivity {
    private volatile UserList usersWhoSentFriendRequests = new UserList(User.UserType.PENDING_YOU);
    private volatile UserList facebookFriends = new UserList(User.UserType.FACEBOOK_FRIEND);
    private volatile UserList searchResults = new UserList(User.UserType.RANDOM);

    private boolean searchMode = false;
    private String searchTerm = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friends);
        if(getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);

        populateFacebookFriendList();
        ParseAndFacebookUtils.getUsersWhoHaveRequestedToFriendCurrentUser(new FuncCallback<UserList>() {
            @Override
            public void done(UserList list) {
                usersWhoSentFriendRequests = list;
                updateCurrentView();
            }
        });
        ParseAndFacebookUtils.getUsersWhoCurrentUserHasRequestedToFriend(new FuncCallback<UserList>() {
            @Override
            public void done(UserList userList) {
                //done only to populate the user hashmap
                return;
            }
        });

        setViewUpdateCallback(new ViewUpdateCallback() {
            @Override
            public void updateView() {
                fillListView();
            }
        });
        fillListView();
    }
    @Override
    protected void onResume() {
        super.onResume();
        setViewUpdateCallback(new ViewUpdateCallback() {
            @Override
            public void updateView() {
                fillListView();
            }
        });
        fillListView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_friends, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setIconifiedByDefault(false);
        searchView.setIconified(false);
        searchView.setQueryHint("Search by name or username");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchTerm = newText;
                searchMode = !searchTerm.equals("");
                if(searchMode) populateSearchResults();
                fillListView();
                return true;
            }
        });
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    //populates once results come in from Facebook, then Parse
    private void populateFacebookFriendList(){
        GraphRequest request = GraphRequest.newMyFriendsRequest(
                AccessToken.getCurrentAccessToken(),
                new GraphRequest.GraphJSONArrayCallback() {
                    @Override
                    public void onCompleted(JSONArray array, GraphResponse response) {
                        facebookFriends = new UserList(User.UserType.FACEBOOK_FRIEND);

                        ArrayList<String> fbids = new ArrayList<>();

                        for (int i = 0; i < array.length(); i++) {
                            try {
                                JSONObject obj = array.getJSONObject(i);
                                fbids.add(obj.getString("id"));
                            } catch (JSONException e) {}
                        }

                        ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
                        userQuery.whereContainedIn("fbid", fbids);
                        userQuery.orderByAscending("fullname");
                        userQuery.findInBackground(new FindCallback<ParseUser>() {
                            @Override
                            public void done(List<ParseUser> list, ParseException e) {
                                for (ParseUser user : list)
                                    facebookFriends.addUser(User.getOrCreateUser(user, User.UserType.FACEBOOK_FRIEND));

                                updateCurrentView();
                            }
                        });
                    }
                });
        request.executeAsync();
    }
    //populates once results come in from Parse
    private void populateSearchResults(){
        ParseAndFacebookUtils.getUsersWithMatchingUsernameOrFullname(searchTerm, new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> list, ParseException e) {
                if (list == null) return;
                searchResults = new UserList(list, User.UserType.RANDOM);
                searchResults.sortByPriorityForSearch();
            }
        });
    }


    private void fillListView() {
        if(searchMode) fillListViewSearch();
        else fillListViewNormal();
    }
    private void fillListViewNormal() {
        ArrayList<UserListAdapterSection> sections = new ArrayList<>();
        sections.add(new UserListAdapterSection("Pending requests", usersWhoSentFriendRequests));
        sections.add(new UserListAdapterSection("Facebook friends", facebookFriends));

        ListView listView = (ListView) findViewById(R.id.listview);
        UserListAdapter arrayAdapter = new UserListAdapter(this, sections, "No suggestions", (UserListAdapter)listView.getAdapter());
        listView.setAdapter(arrayAdapter);
    }
    private void fillListViewSearch() {
        ListView listView = (ListView) findViewById(R.id.listview);
        UserListAdapter arrayAdapter = new UserListAdapter(this, new UserListAdapterSection("Search results", searchResults), "No search results", (UserListAdapter)listView.getAdapter());
        listView.setAdapter(arrayAdapter);
    }
}
