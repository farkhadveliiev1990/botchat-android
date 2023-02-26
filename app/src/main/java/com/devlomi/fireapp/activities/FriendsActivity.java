package com.devlomi.fireapp.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.devlomi.fireapp.R;
import com.devlomi.fireapp.adapters.MyFriendRecycleViewAdapter;
import com.devlomi.fireapp.model.UserInfo;
import com.devlomi.fireapp.model.realms.User;
import com.devlomi.fireapp.utils.RealmHelper;
import com.devlomi.fireapp.utils.SharedPreferencesManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

import io.realm.RealmResults;

public class FriendsActivity extends AppCompatActivity {

    private List<UserInfo> userList;
    private MyFriendRecycleViewAdapter adapter;
    private RecyclerView rvFriends;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        getSupportActionBar().setTitle("Friends");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loadFriendListJsonString();

        if( userList == null ) return;

        rvFriends = findViewById(R.id.rv_friends);

        adapter = new MyFriendRecycleViewAdapter(userList, this);
        rvFriends.setAdapter(adapter);
        rvFriends.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        adapter.notifyDataSetChanged();

    }

    private boolean loadFriendListJsonString() {
        String jsonstring = SharedPreferencesManager.getFriendsListJsonString();
        if( jsonstring == null || jsonstring.equals("") )
            return false;

        Gson gson = new Gson();
        TypeToken<List<UserInfo>> token = new TypeToken<List<UserInfo>>() {};
        userList = gson.fromJson(jsonstring, token.getType());
        return true;
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
}
