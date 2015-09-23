package com.atchapp.atch.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SearchView;

import com.atchapp.atch.AtchApplication;
import com.atchapp.atch.Callbacks.VariableCallback;
import com.atchapp.atch.Callbacks.ViewUpdateCallback;
import com.atchapp.atch.ParseAndFacebookUtils;
import com.atchapp.atch.R;
import com.atchapp.atch.Users.Group;
import com.atchapp.atch.Users.User;
import com.atchapp.atch.Users.UserList;
import com.atchapp.atch.Users.UserListAdapter;
import com.atchapp.atch.Users.UserListAdapterSection;

import java.util.ArrayList;

public class AddFriendsActivity extends BaseFriendsActivity {
    private volatile UserList searchResults = new UserList(User.UserType.RANDOM);

    private boolean searchMode = false;
    private String searchTerm = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friends);
        if(getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);
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

        AtchApplication app = (AtchApplication) getApplication();
        app.populateFacebookFriendList();
        app.populatePendingLists();
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
        overridePendingTransition(0, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_friends, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setIconifiedByDefault(false);
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
                if (searchMode) populateSearchResults();
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
        AtchApplication app = (AtchApplication) getApplication();

        ArrayList<UserListAdapterSection> sections = new ArrayList<>();
        sections.add(new UserListAdapterSection("Pending requests", app.getUsersWhoSentFriendRequests()));
        sections.add(new UserListAdapterSection("Facebook friends", app.getFacebookFriends()));

        ListView listView = (ListView) findViewById(R.id.listview);
        fillListViewGivenAdapter(new UserListAdapter(this, sections, "No suggestions", (UserListAdapter) listView.getAdapter()));
    }
    private void fillListViewSearch() {
        ListView listView = (ListView) findViewById(R.id.listview);
        fillListViewGivenAdapter(new UserListAdapter(this, new UserListAdapterSection("Search results", searchResults), "No results", (UserListAdapter) listView.getAdapter()));
    }

    private void fillListViewGivenAdapter(UserListAdapter arrayAdapter) {
        ListView listView = (ListView) findViewById(R.id.listview);
        String firstItemId = null;
        int scrollFromTop = 0;
        if (listView.getAdapter() != null && listView.getCount() > 0) {
            Object firstObject = listView.getItemAtPosition(listView.getFirstVisiblePosition());
            if (firstObject != null) {
                View v = listView.getChildAt(0);
                scrollFromTop = (v == null) ? 0 : (v.getTop() - listView.getPaddingTop());
                if (firstObject instanceof User)
                    firstItemId = ((User) firstObject).getId();
                if (firstObject instanceof Group)
                    firstItemId = ((Group) firstObject).getIdsInString(null);
            }
        }

        listView.setAdapter(arrayAdapter);

        if (firstItemId != null) {
            for (int i = listView.getCount() - 1; i >= 0; i--) {
                Object currObject = listView.getItemAtPosition(i);
                boolean isCorrect = false;
                if (currObject instanceof User)
                    if (((User) currObject).getId().equals(firstItemId))
                        isCorrect = true;
                if (currObject instanceof Group)
                    if (((Group) currObject).getIdsInString(null).equals(firstItemId))
                        isCorrect = true;

                if (isCorrect) {
                    listView.setSelectionFromTop(i, scrollFromTop);
                    break;
                }
            }
        }
    }
}
