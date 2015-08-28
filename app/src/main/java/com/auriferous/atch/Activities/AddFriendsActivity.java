package com.auriferous.atch.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SearchView;

import com.auriferous.atch.Callbacks.ViewUpdateCallback;
import com.auriferous.atch.Users.UserListAdapter;
import com.auriferous.atch.Callbacks.VariableCallback;
import com.auriferous.atch.ParseAndFacebookUtils;
import com.auriferous.atch.R;
import com.auriferous.atch.Users.UserListAdapterSection;
import com.auriferous.atch.Users.User;
import com.auriferous.atch.Users.UserList;

import java.util.ArrayList;

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
        ParseAndFacebookUtils.getUsersWhoHaveRequestedToFriendCurrentUser(new VariableCallback<UserList>() {
            @Override
            public void done(UserList list) {
                usersWhoSentFriendRequests = list;
                updateCurrentView();
            }
        });
        ParseAndFacebookUtils.getUsersWhoCurrentUserHasRequestedToFriend(new VariableCallback<UserList>() {
            @Override
            public void done(UserList userList) {
                //done only to populate the user hashmap
            }
        });
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
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);

        overridePendingTransition(0, 0);
    }
    @Override
    public void onBackPressed(){
        Intent intent = new Intent(getApplication(), ViewFriendsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("back", true);
        startActivity(intent);
        overridePendingTransition(0,0);
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

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    //populates once results come in from Facebook, then Parse
    private void populateFacebookFriendList(){
        ParseAndFacebookUtils.getAllFacebookFriends(new VariableCallback<UserList>(){
            @Override
            public void done(UserList list) {
                facebookFriends = list;
                fillListView();
            }

        });
    }
    //populates once results come in from Parse
    private void populateSearchResults(){
        ParseAndFacebookUtils.getUsersWithMatchingUsernameOrFullname(searchTerm, new VariableCallback<UserList>() {
            @Override
            public void done(UserList list) {
                searchResults = list;
                fillListView();
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
