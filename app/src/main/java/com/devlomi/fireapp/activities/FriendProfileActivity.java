package com.devlomi.fireapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.devlomi.fireapp.R;
import com.devlomi.fireapp.model.UserInfo;

import de.hdodenhof.circleimageview.CircleImageView;

public class FriendProfileActivity extends AppCompatActivity {

    private UserInfo user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_profile);

        user = (UserInfo) getIntent().getSerializableExtra("FRIEND");

        if( user == null ) return;

        getSupportActionBar().setTitle(user.getName());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //
        CircleImageView photo = findViewById(R.id.friendprofile_image);
        TextView content = findViewById(R.id.friendprofile_content);
        TextView emailText = findViewById(R.id.email_text);
        TextView genderText = findViewById(R.id.gender_text);
        TextView aboutText = findViewById(R.id.about_text);
        TextView phoneText = findViewById(R.id.phone_text);
        TextView birthText = findViewById(R.id.birth_text);

        if( user.getPhoto() != null ) {

            Glide.with(this)
                    .asBitmap()
                    .load(Uri.parse(user.getPhoto()))
                    .into(photo);

        }
        if( user.getSurname() != null )
            content.setText(user.getName()+ " " + user.getSurname());
        else
            content.setText(user.getName());

        if( user.getEmail() != null )
            emailText.setText(user.getEmail());
        if( user.getStatus() != null )
            aboutText.setText(user.getStatus());
        if( user.getBirthDate() != null )
            birthText.setText(user.getBirthDate());
        phoneText.setText(user.getPhone());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.menu_friend, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackPressed();
        return super.onOptionsItemSelected(item);
    }
}
