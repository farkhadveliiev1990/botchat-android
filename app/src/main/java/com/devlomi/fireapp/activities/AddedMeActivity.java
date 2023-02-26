package com.devlomi.fireapp.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.devlomi.fireapp.R;
import com.devlomi.fireapp.adapters.FriendsRecyclerViewAdapter;
import com.devlomi.fireapp.model.UserInfo;
import com.devlomi.fireapp.utils.SharedPreferencesManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class AddedMeActivity extends AppCompatActivity {

    private List<UserInfo> invitedUsers;
    private FriendsRecyclerViewAdapter friendsRecyclerViewAdapter;

    private TextView placeholderText;
    private RecyclerView addedmeRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_added_me);

        getSupportActionBar().setTitle("Added Me");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        invitedUsers = new ArrayList<>();

        placeholderText = findViewById(R.id.added_placeholder);
        addedmeRecyclerView = findViewById(R.id.addedme_recycler);

        if( loadInvitedList() ) {
        }

        if( invitedUsers.size() > 0 )
            placeholderText.setVisibility(View.GONE);
        else
            placeholderText.setVisibility(View.VISIBLE);

        //
        friendsRecyclerViewAdapter = new FriendsRecyclerViewAdapter(this, invitedUsers);
        addedmeRecyclerView.setAdapter(friendsRecyclerViewAdapter);
        addedmeRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        friendsRecyclerViewAdapter.notifyDataSetChanged();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    private boolean loadInvitedList() {
        String jsonstring = SharedPreferencesManager.getAddedMeListJsonString();
        if( jsonstring == null || jsonstring.equals("") )
            return false;

        Gson gson = new Gson();
        TypeToken<List<UserInfo>> token = new TypeToken<List<UserInfo>>() {};
        invitedUsers = gson.fromJson(jsonstring, token.getType());
        return true;
    }

}
