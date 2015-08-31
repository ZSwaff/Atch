package com.auriferous.atch.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.auriferous.atch.Users.UserListAdapter;
import com.auriferous.atch.AtchApplication;
import com.auriferous.atch.Callbacks.ViewUpdateCallback;
import com.auriferous.atch.R;
import com.auriferous.atch.Users.UserListAdapterSection;
import com.auriferous.atch.Users.UserList;

import java.util.ArrayList;

public class ViewFriendsActivity extends BaseFriendsActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_friends);
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
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);

        if(intent.getBooleanExtra("back", false))
            overridePendingTransition(0, 0);
        else
            overridePendingTransition(R.anim.slide_left_in, R.anim.slide_left_out);
    }
    @Override
    public void onBackPressed(){
        Intent intent = new Intent(getApplication(), MapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("back", true);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_right_in, R.anim.slide_right_out);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_view_friends, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        if (id == R.id.switch_to_add_friends){
            Intent intent = new Intent(getApplication(), AddFriendsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        }

        return super.onOptionsItemSelected(item);
    }


    private void fillListView() {
        UserList friends = ((AtchApplication) getApplication()).getFriendsList();

        ArrayList<UserListAdapterSection> sections = new ArrayList<>();
        sections.add(new UserListAdapterSection("Currently hanging", friends.getAllGroupsWithMoreThanOnePerson()));
        sections.add(new UserListAdapterSection("All friends", friends));
        ListView listView = (ListView) findViewById(R.id.listview);
        UserListAdapter arrayAdapter = new UserListAdapter(this, sections, "No friends yet", (UserListAdapter)listView.getAdapter());
        listView.setAdapter(arrayAdapter);
    }
}
