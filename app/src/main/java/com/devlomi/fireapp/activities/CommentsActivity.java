package com.devlomi.fireapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.devlomi.fireapp.R;
import com.devlomi.fireapp.adapters.CommentRecyclerViewAdapter;
import com.devlomi.fireapp.model.Comment;
import com.devlomi.fireapp.model.Post;
import com.devlomi.fireapp.model.Posts;
import com.devlomi.fireapp.model.realms.User;
import com.devlomi.fireapp.utils.FireConstants;
import com.devlomi.fireapp.utils.FireManager;
import com.devlomi.fireapp.utils.KeyboardHelper;
import com.devlomi.fireapp.utils.PostManager;
import com.devlomi.fireapp.utils.RealmHelper;
import com.devlomi.fireapp.utils.SharedPreferencesManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText;

public class CommentsActivity extends AppCompatActivity {

    private int currentApiVersion;
    private CommentRecyclerViewAdapter mAdapter;
    private List<Comment> mComments;
    private HashMap<String,Object> hashComments;
    private Posts mPost;
    private boolean isSaved = false;

    private EmojiconEditText msg_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        if( getSupportActionBar() != null )
            getSupportActionBar().hide();

        // get comments from post
        mPost =(Posts) getIntent().getSerializableExtra("POST");

        // message
        Button send_view = findViewById(R.id.send_btn);
        //send_view.setVisibility(View.INVISIBLE);
        send_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if( msg_text.getText().length() < 2 ) {
                    Toast.makeText(CommentsActivity.this, "You must type at least 2 letters" ,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // hide keyboard
                KeyboardHelper.hideSoftKeyboard(CommentsActivity.this, msg_text);

                User user = RealmHelper.getInstance().getUser(FireManager.getUid());

                Comment comment = new Comment();
                comment.setPhotoUrl(user.getThumbImg());
                comment.setContent(msg_text.getText().toString());
                comment.setUserName(SharedPreferencesManager.getUserName());
                comment.setTime(String.format("%d", System.currentTimeMillis()));

                HashMap<String,Object> curComments = new HashMap<>();
                curComments.put("commentPhoto", comment.getPhotoUrl());
                curComments.put("commentName", comment.getUserName());
                curComments.put("commentText", comment.getContent());
                curComments.put("commentTime", comment.getTime());

                setHashComments(curComments);

                convertToList();
                sortComments();
                mAdapter.notifyDataSetChanged();

                msg_text.setText("");
            }
        });

        // click like image
        ImageView like_click_image = findViewById(R.id.like_click_image);
        like_click_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //
//                if( mPost != null ) {
//
//                    if( mPost.getPostUid().equals(FireManager.getUid()) ) return;
//
//                    PostManager.updateLike(mPost);
//                }
            }
        });

        // show like text
        TextView like_show_text = findViewById(R.id.like_show_text);
        like_show_text.setText(Integer.toString(mPost.getPostLikes()));

        msg_text = findViewById(R.id.msg_text);
        KeyboardHelper.openSoftKeyboard(this, msg_text.findFocus());
        msg_text.requestFocus();
        msg_text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if( count > 0 )
                    send_view.setVisibility(View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        msg_text.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                //KeyboardHelper.openSoftKeyboard(CommentsActivity.this, msg_text.findFocus());

            }
        });

        //
        mComments = new ArrayList<>();
        hashComments = mPost.getPostComments();
        if( hashComments == null ) {

        } else {
            convertToList();
            sortComments();
        }

        RecyclerView comment_recyclerview = findViewById(R.id.comment_recyclerview);
        mAdapter = new CommentRecyclerViewAdapter(this, mComments);
        comment_recyclerview.setAdapter(mAdapter);
        comment_recyclerview.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        mAdapter.notifyDataSetChanged();
    }

    private void setHashComments(HashMap<String,Object> comment) {

        if( hashComments == null || hashComments.size() == 0 ) {

            if( hashComments == null ) {
                hashComments = new HashMap<>();

                String sub_key = String.format("%d", System.currentTimeMillis());
                hashComments.put(sub_key, comment);
            }
        } else {
            String sub_key = String.format("%d", System.currentTimeMillis());
            hashComments.put(sub_key, comment);
        }

        mPost.setPostComments(hashComments);

        FireConstants.postsRef.child(mPost.getPostId()).child("postComments")
                .updateChildren(hashComments).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                isSaved = true;
            }
        });

//        PostManager.updatePost(mPost);
    }

    private void convertToList() {

        mComments.clear();
        for (Map.Entry<String, Object> entry : hashComments.entrySet()) {

            Comment comment = new Comment();

            HashMap<String,Object> hashComment = (HashMap<String,Object>)entry.getValue();
            for (Map.Entry<String, Object> subentry : hashComment.entrySet()) {

                if( subentry.getKey().equals("commentName") )
                    comment.setUserName((String)subentry.getValue());
                else if( subentry.getKey().equals("commentPhoto") )
                    comment.setPhotoUrl((String)subentry.getValue());
                else if( subentry.getKey().equals("commentText") )
                    comment.setContent((String)subentry.getValue());
                else
                    comment.setTime((String)subentry.getValue());

            }
            mComments.add(comment);
        }
    }

    private void sortComments() {
        Collections.sort(mComments, new Comparator<Comment>() {
            @Override
            public int compare(Comment o1, Comment o2) {
                return o2.getTime().compareTo(o1.getTime());
            }
        });
    }

    @Override
    public void onBackPressed() {

        if( isSaved )
            setResult();

        super.onBackPressed();
    }

    private void setResult() {
        Intent intent = new Intent();
        intent.putExtra("postt", mPost);
        setResult(1123, intent);
    }
}
