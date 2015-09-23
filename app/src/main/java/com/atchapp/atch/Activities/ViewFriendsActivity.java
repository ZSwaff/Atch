package com.atchapp.atch.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.atchapp.atch.AtchApplication;
import com.atchapp.atch.Callbacks.ViewUpdateCallback;
import com.atchapp.atch.R;
import com.atchapp.atch.Users.Group;
import com.atchapp.atch.Users.User;
import com.atchapp.atch.Users.UserList;
import com.atchapp.atch.Users.UserListAdapter;
import com.atchapp.atch.Users.UserListAdapterSection;

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
        sections.add(new UserListAdapterSection("Together", friends.getAllGroupsWithMoreThanOnePerson()));
        sections.add(new UserListAdapterSection("Online", friends.getOnline()));
        sections.add(new UserListAdapterSection("Offline", friends.getOffline()));

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

        UserListAdapter arrayAdapter = new UserListAdapter(this, sections, "No friends yet", (UserListAdapter)listView.getAdapter());
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
