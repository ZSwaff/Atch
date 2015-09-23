package com.auriferous.atch.Users;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.auriferous.atch.Activities.MapActivity;
import com.auriferous.atch.AtchApplication;
import com.auriferous.atch.GeneralUtils;
import com.auriferous.atch.ParseAndFacebookUtils;
import com.auriferous.atch.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class UserListAdapter extends BaseAdapter {
    private static AtchApplication app;

    private final Context context;
    private ArrayList<UserListAdapterSection> sections;
    private String emptyMessage;

    private int leftPadding = 10;

    private HashSet<String> viewsAccessed = new HashSet<>();
    private HashMap<String, View> allViews = new HashMap<>();


    public UserListAdapter(Context context, UserListAdapterSection section, String emptyMessage, UserListAdapter oldAdapter) {
        this(context, new ArrayList<>(Arrays.asList(new UserListAdapterSection[]{section})), emptyMessage, oldAdapter);
    }


    public UserListAdapter(Context context, ArrayList<UserListAdapterSection> sections, String emptyMessage, UserListAdapter oldAdapter) {
        this.context = context;
        this.sections = sections;
        this.emptyMessage = emptyMessage;

        leftPadding = GeneralUtils.convertDpToPixel(leftPadding, context);

        if(oldAdapter != null)
            allViews = oldAdapter.allViews;
    }
    public static void init(AtchApplication app) {
        UserListAdapter.app = app;
    }
    @Override
    public int getCount() {
        int count = 0;
        for(UserListAdapterSection section : sections) {
            int size = section.size();
            if(size != 0 && section.getLabel() != null) count++;
            count += size;
        }
        if(count == 0) return 1;
        return count;
    }
    @Override
    public Object getItem(int position) {
        for(UserListAdapterSection section : sections){
            ArrayList parts = section.getGroups();
            if(section.isUsers())
                parts = section.getUsers().getAllUsers();

            if (parts.size() == 0) continue;
            if (section.getLabel() != null) {
                if (position == 0) return null;
                position--;
            }

            if (position < parts.size())
                return parts.get(position);
            position -= parts.size();
        }
        return null;
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        for(UserListAdapterSection section : sections){
            ArrayList parts = section.getGroups();
            if(section.isUsers())
                parts = section.getUsers().getAllUsers();

            if (parts.size() == 0) continue;
            if(section.getLabel() != null) {
                if (position == 0)
                    return createLabelView(section.getLabel(), parent);
                position--;
            }

            if(position < parts.size()) {
                if(section.isUsers()) {
                    User user = (User)parts.get(position);
                    String uid = user.getId();
                    if (!viewsAccessed.contains(uid)) {
                        viewsAccessed.add(uid);
                        if (allViews.containsKey(uid))
                            return allViews.get(uid);
                    }
                    View v = createUserView(user, parent);
                    allViews.put(uid, v);
                    return v;
                }
                else
                    return createGroupView((Group)parts.get(position), parent);
            }
            position -= parts.size();
        }
        if (position == 0)
            return createFullscreenLabelView(parent);
        return null;
    }


    private View createFullscreenLabelView(ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.fullscreen_label, parent, false);

        TextView label = (TextView) rowView.findViewById(R.id.label);
        label.setText(emptyMessage);

        return rowView;
    }
    private View createLabelView(String text, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.label, parent, false);

        TextView label = (TextView) rowView.findViewById(R.id.label);
        label.setText(text);

        return rowView;
    }

    private View createUserView(final User user, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        int layout = 0;
        switch(user.getUserType()){
            case FRIEND:
                layout = R.layout.friend_list_item;
                break;
            case PENDING_YOU:
                layout = R.layout.pending_you_list_item;
                break;
            case PENDING_THEM:
                layout = R.layout.pending_them_list_item;
                break;
            case FACEBOOK_FRIEND:
            case RANDOM:
                layout = R.layout.random_list_item;
                break;
        }
        View rowView = inflater.inflate(layout, parent, false);

        TextView fullname = (TextView) rowView.findViewById(R.id.fullname);
        TextView username = (TextView) rowView.findViewById(R.id.username);
        TextView score = (TextView) rowView.findViewById(R.id.score);

        fullname.setText(user.getFullname());
        username.setText(user.getUsername());
        if(score != null)
            score.setText(""+user.getCheckinCount());


        Bitmap profPic = user.getProfPic();
        if(profPic != null) {
            ImageView imageView = (ImageView) rowView.findViewById(R.id.prof_pic);
            imageView.setImageBitmap(profPic);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    user.setNewColor();
                }
            });
            imageView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    user.resetToLastColor();
                    return true;
                }
            });
        }

        ImageButton actionButton = null;

        final String uid = user.getId();
        ImageButton chatButton = (ImageButton) rowView.findViewById(R.id.chat_button);
        if(chatButton != null) {
            actionButton = chatButton;
            chatButton.setImageBitmap(user.getChatIcon());
            chatButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent(context, MapActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    intent.putExtra("type", "message");
                    intent.putExtra("chatterParseId", user.getId());
                    intent.putExtra("back", true);
                    app.getCurrentActivity().startActivity(intent);
                    app.getCurrentActivity().overridePendingTransition(R.anim.slide_right_in, R.anim.slide_right_out);
                }
            });
        }
        ImageButton friendButton = (ImageButton) rowView.findViewById(R.id.friend_button);
        if(friendButton != null) {
            actionButton = friendButton;
            friendButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ParseAndFacebookUtils.sendFriendRequest(uid);
                    user.setUserType(User.UserType.PENDING_THEM);
                    app.updateView();
                }
            });
        }
        ImageButton acceptButton = (ImageButton) rowView.findViewById(R.id.accept_button);
        if(acceptButton != null) {
            actionButton = acceptButton;
            acceptButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ParseAndFacebookUtils.acceptFriendRequest(uid);
                    user.setUserType(User.UserType.FRIEND);
                    app.addFriend(user);
                    app.updateView();
                }
            });
        }
        ImageButton fakeButton = (ImageButton) rowView.findViewById(R.id.fake_button);
        if(fakeButton != null){
            actionButton = fakeButton;
            fakeButton.setEnabled(false);
            fakeButton.setClickable(false);
        }


        Button unfriendButton = (Button) rowView.findViewById(R.id.unfriend_button);
        if(unfriendButton != null) {
            GeneralUtils.addButtonEffect(unfriendButton);
            unfriendButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ParseAndFacebookUtils.unfriendFriend(uid);
                    user.setUserType(User.UserType.RANDOM);
                    app.removeFriend(user);
                    app.updateView();
                }
            });
        }
        Button rejectButton = (Button) rowView.findViewById(R.id.reject_button);
        if(rejectButton != null) {
            GeneralUtils.addButtonEffect(rejectButton);
            rejectButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ParseAndFacebookUtils.rejectFriendRequest(uid);
                    user.setUserType(User.UserType.RANDOM);
                    app.updateView();
                }
            });
        }
        Button cancelButton = (Button) rowView.findViewById(R.id.cancel_button);
        if(cancelButton != null) {
            GeneralUtils.addButtonEffect(cancelButton);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ParseAndFacebookUtils.cancelFriendRequest(uid);
                    user.setUserType(User.UserType.RANDOM);
                    app.updateView();
                }
            });
        }

        initListener(user, actionButton, rowView);
        return rowView;
    }
    private void initListener(final User user, final ImageButton actionButton, final View rowView) {
        if(actionButton == null) return;

        final Context context = this.context;
        rowView.setOnTouchListener(new View.OnTouchListener(){
            public boolean allTheWayLeft = false;
            float slop = ViewConfiguration.get(context).getScaledTouchSlop();
            int initialX = 0;
            boolean newEvent = true;

            @Override
            public boolean onTouch(final View view, MotionEvent event) {
                final View viewToMove = view.findViewById(R.id.main_view);
                final ViewGroup buttonGroup = (ViewGroup)view.findViewById(R.id.button_view);
                if (viewToMove == null || buttonGroup == null) return true;
                final Button swipeButton = (Button) buttonGroup.getChildAt(0);
                int sButtonWidth = swipeButton.getMeasuredWidth();

                int currentX = (int) event.getX();
                int offset = currentX - initialX;
                if(offset > sButtonWidth)
                    offset = sButtonWidth;
                if(-offset > sButtonWidth)
                    offset = -sButtonWidth;

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN: {
                        initialX = (int) event.getX();
                        newEvent = true;

                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        if (!allTheWayLeft && offset < -slop) {
                            viewToMove.setPadding(offset + leftPadding, 0, 0, 0);
                            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) viewToMove.getLayoutParams();
                            params.setMargins(0, 0, -offset, 0);
                            viewToMove.requestLayout();

                            if (offset == -sButtonWidth)
                                allTheWayLeft = true;

                            swipeButton.setEnabled(allTheWayLeft);
                            actionButton.setEnabled(!allTheWayLeft);
                        }

                        if (Math.abs(offset) > slop)
                            newEvent = false;

                        break;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL: {
                        if (allTheWayLeft && offset > -slop) {
                            allTheWayLeft = false;
                            swipeButton.setEnabled(allTheWayLeft);
                            actionButton.setEnabled(!allTheWayLeft);
                        }
                        else if (Math.abs(offset) < slop) {
                            if (user.getUserType() == User.UserType.FRIEND) {
                                Intent intent = new Intent(context, MapActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                intent.putExtra("type", "zoom");
                                intent.putExtra("chatterParseId", user.getId());
                                intent.putExtra("back", true);
                                app.getCurrentActivity().startActivity(intent);
                                app.getCurrentActivity().overridePendingTransition(R.anim.slide_right_in, R.anim.slide_right_out);
                            }
                        }

                        if (!allTheWayLeft) {
                            ValueAnimator animator = ValueAnimator.ofInt(viewToMove.getPaddingLeft(), leftPadding);
                            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                    int paddingAmount = (Integer) valueAnimator.getAnimatedValue();
                                    viewToMove.setPadding(paddingAmount, 0, 0, 0);
                                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) viewToMove.getLayoutParams();
                                    params.setMargins(0, 0, -paddingAmount + leftPadding, 0);
                                    viewToMove.requestLayout();
                                }
                            });
                            animator.setDuration(150);
                            animator.start();
                        }
                    }
                }
                return true;
            }
        });
    }

    private View createGroupView(final Group group, ViewGroup parent){
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.group_list_item, parent, false);

        TextView names = (TextView) rowView.findViewById(R.id.names);
        names.setText(group.getNames());

        Bitmap groupImage = group.getGroupImage(true);
        if(groupImage != null) {
            ImageView imageView = (ImageView) rowView.findViewById(R.id.group_pic);
            imageView.setImageBitmap(groupImage);
        }

        rowView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Intent intent = new Intent(context, MapActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.putExtra("type", "zoom");
                intent.putExtra("chatterParseId", "group_" + group.getId());
                intent.putExtra("back", true);
                if(app == null || app.getCurrentActivity() == null)
                    return true;
                app.getCurrentActivity().startActivity(intent);
                app.getCurrentActivity().overridePendingTransition(R.anim.slide_right_in, R.anim.slide_right_out);
                return true;
            }
        });

        return rowView;
    }
}