package com.devlomi.fireapp.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.devlomi.fireapp.R;
import com.devlomi.fireapp.adapters.AllUsersRecyclerViewAdapter;
import com.devlomi.fireapp.model.UserInfo;
import com.devlomi.fireapp.utils.SharedPreferencesManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class QuickAddActivity extends AppCompatActivity {

    private List<UserInfo> userInfos;
    private AllUsersRecyclerViewAdapter allUsersRecyclerViewAdapter;
    private RecyclerView allUsersRecycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_add);

        getSupportActionBar().setTitle("Quick Add");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        userInfos = new ArrayList<>();

        allUsersRecycler = findViewById(R.id.allusers_recycler);

        loadRemailList();

        // set invite
        allUsersRecyclerViewAdapter = new AllUsersRecyclerViewAdapter(this, userInfos);
        allUsersRecycler.setAdapter(allUsersRecyclerViewAdapter);
        allUsersRecycler.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        allUsersRecyclerViewAdapter.notifyDataSetChanged();
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

    private boolean loadRemailList() {
        String jsonstring = SharedPreferencesManager.getRemainListJsonString();
        if( jsonstring == null || jsonstring.equals("") )
            return false;

        Gson gson = new Gson();
        TypeToken<List<UserInfo>> token = new TypeToken<List<UserInfo>>() {};
        userInfos = gson.fromJson(jsonstring, token.getType());
        return true;
    }

}
