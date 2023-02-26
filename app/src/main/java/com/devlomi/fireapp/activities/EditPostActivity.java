package com.devlomi.fireapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;

import com.devlomi.fireapp.R;
import com.devlomi.fireapp.model.Post;

public class EditPostActivity extends AppCompatActivity {

    Post post;
    LinearLayout editTextContainer;
    LinearLayout imagesContainer;
    EditText postText;
    GridView postImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        post =(Post) getIntent().getSerializableExtra("EDITPOST");
        Log.e("Got EditPost",post.toString());

        editTextContainer = findViewById(R.id.postedit_edittext_container);
        imagesContainer = findViewById(R.id.postedit_images_container);

        postText = findViewById(R.id.postedit_edittext);
        postImages = findViewById(R.id.postedit_gridview);

        if(post.getText() != null) {
            postText.setText(post.getText());
        }

    }

    class ImagesAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return 0;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }
    }
}
