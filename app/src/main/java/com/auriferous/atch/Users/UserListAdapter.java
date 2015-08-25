package com.auriferous.atch.Users;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.auriferous.atch.Activities.ChatActivity;
import com.auriferous.atch.AtchApplication;
import com.auriferous.atch.ParseAndFacebookUtils;
import com.auriferous.atch.R;

import java.util.ArrayList;

public class UserListAdapter extends BaseAdapter {
    private static final int ITEM_LEFT_PADDING = 16;

    private static AtchApplication app;

    private final Context context;
    private ArrayList<UserListAdapterSection> sections;
    private String emptyMessage;


    public static void init(AtchApplication app){
        UserListAdapter.app = app;
    }


    public UserListAdapter(Context context, UserListAdapterSection section, String emptyMessage) {
        this.context = context;
        sections = new ArrayList<>();
        sections.add(section);
        this.emptyMessage = emptyMessage;
    }
    public UserListAdapter(Context context, ArrayList<UserListAdapterSection> sections, String emptyMessage) {
        this.context = context;
        this.sections = sections;
        this.emptyMessage = emptyMessage;
    }


    @Override
    public int getCount() {
        int count = 0;
        for(UserListAdapterSection section : sections) {
            int size = section.getUsers().getAllUsers().size();
                if(size != 0) count++;
            count += size;
        }
        if(count == 0) return 1;
        return count;
    }
    @Override
    public User getItem(int position) {
        for(UserListAdapterSection section : sections){
            ArrayList<User> users = section.getUsers().getAllUsers();

            if (users.size() == 0) continue;
            if (position == 0) return null;
            position--;

            if(position < users.size())
                return users.get(position);
            position -= users.size();
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
            ArrayList<User> users = section.getUsers().getAllUsers();

            if (users.size() == 0) continue;
            if (position == 0)
                return createLabelView(section.getLabel(), parent);
            position--;

            if(position < users.size())
                return createUserView(users.get(position), parent);
            position -= users.size();
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

        fullname.setText(user.getFullname());
        username.setText(user.getUsername());

        final String uid = user.getId();

        Button friendButton = (Button) rowView.findViewById(R.id.friend_button);
        if(friendButton != null)
            friendButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ParseAndFacebookUtils.sendFriendRequest(uid);
                    user.setUserType(User.UserType.PENDING_THEM);
                    app.updateView();
                }
            });

        final Button acceptButton = (Button) rowView.findViewById(R.id.accept_button);
        if(acceptButton != null)
            acceptButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ParseAndFacebookUtils.acceptFriendRequest(uid);
                    user.setUserType(User.UserType.FRIEND);
                    app.addFriend(user);
                    app.updateView();
                }
            });


        final Button unfriendButton = (Button) rowView.findViewById(R.id.unfriend_button);
        if(unfriendButton != null)
            unfriendButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ParseAndFacebookUtils.unfriendFriend(uid);
                    user.setUserType(User.UserType.RANDOM);
                    app.removeFriend(user);
                    app.updateView();
                }
            });
        final Button rejectButton = (Button) rowView.findViewById(R.id.reject_button);
        if(rejectButton != null)
            rejectButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ParseAndFacebookUtils.rejectFriendRequest(uid);
                    user.setUserType(User.UserType.RANDOM);
                    app.updateView();
                }
            });
        final Button cancelButton = (Button) rowView.findViewById(R.id.cancel_button);
        if(cancelButton != null)
            cancelButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ParseAndFacebookUtils.cancelFriendRequest(uid);
                    user.setUserType(User.UserType.RANDOM);
                    app.updateView();
                }
            });


        Bitmap profPic = user.getProfPic();
        if(profPic != null) {
            ImageView imageView = (ImageView) rowView.findViewById(R.id.prof_pic);
            imageView.setImageBitmap(profPic);
        }

        initListener(rowView);
        return rowView;
    }
    private void initListener(final View rowView) {
        rowView.setOnTouchListener(new View.OnTouchListener() {
            final float slop = ViewConfiguration.get(context).getScaledTouchSlop();

            boolean buttonEnabled = false;
            int initialX = 0;
            boolean newEvent = true;

            @Override
            public boolean onTouch(final View view, MotionEvent event) {
                final View viewToMove = view.findViewById(R.id.main_view);
                if (viewToMove == null) return true;
                final Button swipeButton = (Button) ((ViewGroup)view.findViewById(R.id.button_view)).getChildAt(0);
                int sButtonWidth = swipeButton.getMeasuredWidth();

                int currentX = (int) event.getX();
                int offset = currentX - initialX;
                if(offset > sButtonWidth)
                    offset = sButtonWidth;
                if(-offset > sButtonWidth)
                    offset = -sButtonWidth;

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    initialX = (int) event.getX();
                    newEvent = true;
                    if (!buttonEnabled)
                        viewToMove.setPadding((int) convertDpToPixel(ITEM_LEFT_PADDING), 0, 0, 0);
                }
                else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (buttonEnabled && offset > -slop) {
                        buttonEnabled = false;
                        swipeButton.setEnabled(buttonEnabled);
                    }

                    if (!buttonEnabled && offset < -slop) {
                        viewToMove.setPadding(offset + (int) convertDpToPixel(ITEM_LEFT_PADDING), 0, 0, 0);
                        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) viewToMove.getLayoutParams();
                        params.setMargins(0, 0, -offset, 0);
                        viewToMove.requestLayout();

                        if (offset == -sButtonWidth) {
                            buttonEnabled = true;
                        }

                        swipeButton.setEnabled(buttonEnabled);
                    }

                    if (Math.abs(offset) > slop)
                        newEvent = false;
                }
                else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    if(!buttonEnabled) {
                        resetRow(viewToMove);
                    }
                }
                return true;
            }
        });
    }

    private void resetRow(final View viewToMove){
        ValueAnimator animator = ValueAnimator.ofInt(viewToMove.getPaddingLeft(), (int) convertDpToPixel(ITEM_LEFT_PADDING));
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int paddingAmount = (Integer) valueAnimator.getAnimatedValue();
                viewToMove.setPadding(paddingAmount, 0, 0, 0);
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) viewToMove.getLayoutParams();
                params.setMargins(0, 0, -paddingAmount, 0);
                viewToMove.requestLayout();
            }
        });
        animator.setDuration(150);
        animator.start();
    }

    public float convertDpToPixel(float dp){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }
}