package com.devlomi.fireapp.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.devlomi.fireapp.R;
import com.devlomi.fireapp.adapters.LanguageRecyclerViewAdapter;
import com.devlomi.fireapp.model.Language;
import com.devlomi.fireapp.utils.SharedPreferencesManager;

import java.util.ArrayList;
import java.util.List;

public class SettingLanguageActivity extends AppCompatActivity {

    private List<Language> languageList;
    private LanguageRecyclerViewAdapter adapter;
    private RecyclerView languageRecycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_language);

        getSupportActionBar().setTitle("Languages");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String language = getIntent().getStringExtra("Language");

        loadLanguage(language);

        languageRecycler = findViewById(R.id.language_recycler);

        adapter = new LanguageRecyclerViewAdapter(this, languageList);
        languageRecycler.setAdapter(adapter);
        languageRecycler.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        languageRecycler.setHasFixedSize(true);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadLanguage( String defaultLang ) {
        languageList = new ArrayList<>();

        Resources res = getResources();
        String[] languages = res.getStringArray(R.array.language_array);

        for( int i=0; i<languages.length; i++ ) {
            Language language = new Language();

            language.setLanguage(languages[i]);
            if( defaultLang.equals(languages[i]) )
                language.setCheck(true);
            else
                language.setCheck(false);

            languageList.add(language);
        }
    }

    private void saveLanguage() {
        int position = adapter.getCurrentPosition();
        String lang = languageList.get(position).getLanguage();
        SharedPreferencesManager.saveLanguage(lang);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //saveLanguage();
    }
}
