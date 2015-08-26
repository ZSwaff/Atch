package com.auriferous.atch.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.auriferous.atch.Users.User;
import com.auriferous.atch.Users.UserListAdapter;
import com.auriferous.atch.AtchApplication;
import com.auriferous.atch.Callbacks.ViewUpdateCallback;
import com.auriferous.atch.R;
import com.auriferous.atch.Users.UserListAdapterSection;
import com.auriferous.atch.Users.UserList;

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

        if (id == R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        if (id == R.id.switch_to_add_friends) {
            Intent intent = new Intent(getApplicationContext(), AddFriendsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void fillListView() {
        UserList friends = ((AtchApplication) getApplication()).getFriendsList();

        ListView listView = (ListView) findViewById(R.id.listview);
        UserListAdapter arrayAdapter = new UserListAdapter(this, new UserListAdapterSection("Friends", friends), "No friends yet", (UserListAdapter)listView.getAdapter());
        listView.setAdapter(arrayAdapter);
    }
}
