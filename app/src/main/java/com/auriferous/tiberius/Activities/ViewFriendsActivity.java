package com.auriferous.tiberius.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.auriferous.tiberius.Users.UserListAdapter;
import com.auriferous.tiberius.AtchApplication;
import com.auriferous.tiberius.Callbacks.ViewUpdateCallback;
import com.auriferous.tiberius.R;
import com.auriferous.tiberius.Users.UserListAdapterSection;
import com.auriferous.tiberius.Users.UserList;

public class ViewFriendsActivity extends BaseFriendsActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_friends);
        if(getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);

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
        getMenuInflater().inflate(R.menu.menu_view_friends, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.home || id == R.id.switch_to_map) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        if (id == R.id.switch_to_add_friends) {
            startActivity(new Intent(getApplicationContext(), AddFriendsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void fillListView() {
        UserList friends = ((AtchApplication) getApplication()).getFriendsList();
        UserListAdapter arrayAdapter = new UserListAdapter(this, new UserListAdapterSection("Friends", friends));

        ListView listView = (ListView) findViewById(R.id.listview);
        listView.setAdapter(arrayAdapter);
    }
}
