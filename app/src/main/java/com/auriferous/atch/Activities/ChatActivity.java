package com.auriferous.atch.Activities;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.auriferous.atch.Callbacks.FuncCallback;
import com.auriferous.atch.Callbacks.ViewUpdateCallback;
import com.auriferous.atch.Messages.MessageList;
import com.auriferous.atch.Messages.MessageListAdapter;
import com.auriferous.atch.ParseAndFacebookUtils;
import com.auriferous.atch.R;
import com.auriferous.atch.Users.User;
import com.parse.FunctionCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;

public class ChatActivity extends BaseFriendsActivity {
    private User chatRecipient;
    private volatile ParseObject messageHistory;
    private volatile MessageList messageList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        if(getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);

        chatRecipient = User.getUserFromMap(getIntent().getStringExtra("chatterParseId"));

        setupChatHistory();

        EditText messageBox = (EditText) findViewById(R.id.message_box);
        messageBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage(null);
                    return true;
                }
                return false;
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
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void sendMessage(View view) {
        EditText messageBox = (EditText) findViewById(R.id.message_box);
        String newMessage = messageBox.getText().toString();
        ParseAndFacebookUtils.sendMessage(messageHistory, newMessage, new FuncCallback<Object>() {
            @Override
            public void done(Object o) {
                setupChatHistory();
            }
        });
        messageBox.setText("");
    }


    public String getChatterObjectId(){
        return chatRecipient.getId();
    }
    public void setupChatHistory(){
        final ChatActivity chatActivity = this;
        ParseAndFacebookUtils.getOrCreateMessageHistory(chatRecipient.getId(), new FunctionCallback<ParseObject>() {
            @Override
            public void done(ParseObject messageHistory, ParseException e) {
                chatActivity.messageHistory = messageHistory;
                ParseAndFacebookUtils.getAllMessagesFromHistory(messageHistory, new FuncCallback<MessageList>() {
                    @Override
                    public void done(MessageList messageList) {
                        chatActivity.messageList = messageList;
                        fillListView();
                    }
                });
            }
        });
    }
    private void fillListView() {
        MessageListAdapter arrayAdapter = new MessageListAdapter(this, messageList, ParseUser.getCurrentUser(), "No messages");

        ListView listView = (ListView) findViewById(R.id.listview);
        listView.setAdapter(arrayAdapter);
    }
}
