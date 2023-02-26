package com.devlomi.fireapp.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.devlomi.fireapp.R;
import com.devlomi.fireapp.activities.CommentsActivity;
import com.devlomi.fireapp.activities.EnlargyActivity;
import com.devlomi.fireapp.activities.NewPostActivity;
import com.devlomi.fireapp.activities.main.MainActivity;
import com.devlomi.fireapp.fragments.PostFragment;
import com.devlomi.fireapp.model.Post;
import com.devlomi.fireapp.model.Posts;
import com.devlomi.fireapp.utils.BitmapUtils;
import com.devlomi.fireapp.utils.DirManager;
import com.devlomi.fireapp.utils.FileUtils;
import com.devlomi.fireapp.utils.FireConstants;
import com.devlomi.fireapp.utils.FireManager;
import com.devlomi.fireapp.utils.PostManager;
import com.devlomi.fireapp.utils.PostMedia;
import com.devlomi.fireapp.utils.PostUtil;
import com.devlomi.fireapp.utils.SharedPreferencesManager;
import com.devlomi.fireapp.views.PostGridView;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostViewHolder> {

    private final int TYPE_IMGVIDEO = 0;
    private final int TYPE_TEXT = 1;
    private final int TYPE_LOCATION = 2;

    private List<Posts> posts;
    private Context context;
    private PostsAdapter adapter;
    private PostFragment fragment;

    public PostsAdapter(List<Posts> posts, PostFragment postFragment) {
        this.posts = posts;
        this.adapter = this;
        this.fragment = postFragment;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.wall_post, parent, false);

        return new PostViewHolder(view, parent.getContext());
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Posts post = posts.get(position);

        if( post == null ) return;

        switch (post.getPostType()) {
            case TYPE_IMGVIDEO:
                onBindImgVideoPost(holder, post);
                break;
            case TYPE_TEXT:
                onBindTextPost(holder, post);
                break;
            case TYPE_LOCATION:
                onBindLocationPost(holder, post);
                break;
        }

    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull PostViewHolder holder) {
        super.onViewAttachedToWindow(holder);
    }

    private void onBindImgVideoPost(PostViewHolder holder, Posts post ) {
        onBindSameObject(holder, post);

        // enable mediaview and disable mapview
        holder.locationRayout.setVisibility(View.GONE);

        HashMap<String,Object> hashMap = post.getPostMedias();
        if( hashMap == null ) return;

        List<PostMedia> postMedias = new ArrayList<>();
        if( hashMap.entrySet().size() > 1 ) {

            holder.singleRayout.setVisibility(View.GONE);
            holder.multipleMediaView.setVisibility(View.VISIBLE);

            List<String> localPaths = new ArrayList<>();

            postMedias.addAll(getPostMedia(hashMap));
            for( int i=0; i<postMedias.size(); i++ )
                localPaths.add(postMedias.get(i).getThumbImg());

            WallPostGridViewAdapter wallAdapter = new WallPostGridViewAdapter(
                    holder.multipleMediaView.getContext(), localPaths);

            holder.multipleMediaView.setAdapter(wallAdapter);
            wallAdapter.notifyDataSetChanged();

            holder.multipleMediaView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    PostMedia postMedia = postMedias.get(position);

                    if( postMedia != null ) {
                        Intent intent = new Intent(context, EnlargyActivity.class);
                        PostUtil.post = post;
                        PostUtil.postMedia = postMedia;
                        context.startActivity(intent);
                    }
                }
            });

        } else {

            holder.singleRayout.setVisibility(View.VISIBLE);
            holder.multipleMediaView.setVisibility(View.GONE);

            postMedias.addAll(getPostMedia(hashMap));

            Glide.with(context)
                    .asBitmap()
                    .load(BitmapUtils.encodeImageAsBytes(postMedias.get(0).getThumbImg()))
                    .into(holder.singleThumbImage);

            if( postMedias.get(0).getType() == 2 )
                holder.videoIconImage.setVisibility(View.VISIBLE);
            else
                holder.videoIconImage.setVisibility(View.GONE);

            holder.singleThumbImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, EnlargyActivity.class);
                    PostUtil.post = post;
                    PostUtil.postMedia = postMedias.get(0);
                    context.startActivity(intent);
                }
            });
        }
    }

    private List<PostMedia> getPostMedia( HashMap<String,Object> hashMap) {
        List<PostMedia> postMediaList = new ArrayList<>();

        for(Map.Entry<String, Object> entry :hashMap.entrySet()) {
            PostMedia postMedia = new PostMedia();

            String key = entry.getKey();
            HashMap<String,Object> value = (HashMap<String,Object>)entry.getValue();
            for(Map.Entry<String, Object> ventry :value.entrySet()) {
                String sub_key = ventry.getKey();
                Object sub_value = ventry.getValue();

                switch (sub_key) {
                    case "content":
                        postMedia.setContent(sub_value.toString());
                        break;
                    case "duration":
                        postMedia.setDuration(Long.parseLong(sub_value.toString()));
                        break;
                    case "timestamp":
                        postMedia.setTimestamp(Long.parseLong(sub_value.toString()));
                        break;
                    case "type":
                        postMedia.setType(Integer.parseInt(sub_value.toString()));
                        break;
                    case "thumbImg":
                        postMedia.setThumbImg(sub_value.toString());
                        break;
                    case "userId":
                        postMedia.setUserId(sub_value.toString());
                        break;
                    case "localPath":
                        postMedia.setLocalPath(sub_value.toString());
                        break;
                }
            }

            postMediaList.add(postMedia);
        }

        return postMediaList;
    }

    private void onBindTextPost(PostViewHolder holder, Posts post ) {
        onBindSameObject(holder, post);

        holder.locationRayout.setVisibility(View.GONE);
        holder.singleRayout.setVisibility(View.GONE);
        holder.multipleMediaView.setVisibility(View.GONE);
    }

    private void onBindLocationPost(PostViewHolder holder, Posts post ) {
        onBindSameObject(holder, post);

        holder.locationRayout.setVisibility(View.VISIBLE);
        holder.singleRayout.setVisibility(View.GONE);
        holder.multipleMediaView.setVisibility(View.GONE);

        String[] latlong =  post.getPostLocation().split(",");
        double latitude = Double.parseDouble(latlong[0]);
        double longitude = Double.parseDouble(latlong[1]);

        holder.setMapLocation(new LatLng(latitude, longitude));
    }

    private void onBindSameObject(PostViewHolder holder, Posts post ) {

        // user photo
        Glide.with(holder.postUserImage.getContext())
                .asBitmap()
                .load(post.getPostPhotoUrl())
                .into(holder.postUserImage);

        // user name
        holder.postUserNameView.setText(post.getPostName());

        // post time
        String pt_time = convertMilisecToDate(post.getPostTime());
        holder.postTimeView.setText(pt_time);

        // post content
        if( post.getPostText() != null && !post.getPostText().equals("") ) {
            holder.postContentView.setVisibility(View.VISIBLE);
            holder.postContentView.setText(post.getPostText());
        } else {
            holder.postContentView.setVisibility(View.GONE);
        }

        // post likes and comments
        holder.postLikeCheckbox.setText(Integer.toString(post.getPostLikes()));
        holder.postComments.setText(Integer.toString(PostManager.getCommentCount(post.getPostComments())));

        holder.postLikeCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isLiked = post.isPostIsLiked();
                int count = post.getPostLikes();
                if( isLiked ) {
                    isLiked = false;
                    count --;
                } else {
                    isLiked = true;
                    count ++;
                }

                PostManager.updateLikes(post, count, isLiked, new PostManager.OnCompleteLikes() {
                    @Override
                    public void OnComplete( int likes ) {
                        holder.postLikeCheckbox.setText(Integer.toString(likes));
                    }
                });
            }
        });

        // click post comment
        holder.postCommentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(fragment.getContext(), CommentsActivity.class);
                intent.putExtra("POST", post);
                fragment.startActivityForResult(intent, 2222);

            }
        });

        // click post share
        holder.postShareImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharePost(post);
            }
        });

        // post option
        holder.postOptionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(holder.postOptionsButton.getContext(),
                        holder.postOptionsButton);
                popupMenu.getMenuInflater().inflate(R.menu.menu_post_actions,popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        return handleMenuClick(item,holder.postOptionsButton.getContext(), post);
                    }
                });

                // compare my name and current name
                String current = (String) holder.postUserNameView.getText();
                if( SharedPreferencesManager.getUserName().equals(current) )
                    popupMenu.show();
            }
        });
    }

    private List<Uri> uriList;
    private void sharePost(Posts post) {
        uriList = new ArrayList<>();
        int type = post.getPostType();
        switch (type) {
            case TYPE_IMGVIDEO:
                shareMedia("", post);
                break;
            case TYPE_TEXT:
                shareText("", post.getPostText());
                break;
            case TYPE_LOCATION:
                shareLocation("", post.getPostText(), post.getPostLocation());
                break;
        }
    }

    private void shareLocation(String title, String description, String latlong) {
//        String uri = "geo:" + latlong + "?q=" + latlong;
//
//        Intent intent = new Intent(Intent.ACTION_SEND);
//        intent.setType("text/plain");
//        intent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");
//        intent.putExtra(Intent.EXTRA_TEXT, uri);
//        context.startActivity(Intent.createChooser(intent, "Share Via"));

        String uri = "https://www.google.com/maps/?q=" + latlong ;
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT,  uri);
        context.startActivity(Intent.createChooser(sharingIntent, "Share in..."));
    }

    private void shareText(String title, String description) {
        String sharebody = title + "\n" + description;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");
        intent.putExtra(Intent.EXTRA_TEXT, sharebody);
        context.startActivity(Intent.createChooser(intent, "Share Via"));
    }

    private void shareMedia(String title, Posts post) {
        String sharebody = title + "\n" + post.getPostText();

        Intent intent = new Intent(Intent.ACTION_SEND);

        List<PostMedia> mediaList = getPostMedia(post.getPostMedias());
//        for( int i=0; i<mediaList.size(); i++ ) {
            PostMedia media = mediaList.get(0);

            if( media.getType() == 1 ) { // image

                File imageFolder = new File(context.getCacheDir(), "images");

                if(!imageFolder.exists()) {
                    imageFolder.mkdir();
                }

                String localPath = media.getLocalPath();
                String filename = localPath.substring(localPath.lastIndexOf("/")+1);

                File file = new File(imageFolder, filename);
                if( !file.exists() ) {
                    try {
                        Bitmap bitmap = BitmapUtils.encodeImage(media.getThumbImg());
                        FileOutputStream stream = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 80, stream);
                        stream.flush();
                        stream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                Uri uri = FileProvider.getUriForFile(context,
                        "com.devlomi.fireapp.provider", file);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.setType("image/*");

            } else { // video
                String localPath = media.getLocalPath();
                if( FileUtils.isFileExists(localPath) ) { // mine
                    File file = saveVideoFile(localPath);

                    Uri uri = FileProvider.getUriForFile(context,
                            "com.devlomi.fireapp.provider", file);
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    intent.setType("video/mp4");

                } else {
                    localPath = DirManager.getReceivedPostFile(media.getLocalPath()).getAbsolutePath();
                    if( FileUtils.isFileExists(localPath) ) { // other
                        File file = saveVideoFile(localPath);

                        Uri uri = FileProvider.getUriForFile(context,
                                "com.devlomi.fireapp.provider", file);
                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                        intent.setType("video/*");
                    }
                }
            }

//        }
        intent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");
        intent.putExtra(Intent.EXTRA_TEXT, sharebody);
        context.startActivity(Intent.createChooser(intent, "Share Via"));

    }

    private File saveVideoFile(String localPath) {
        File imageFolder = new File(context.getCacheDir(), "videos");

        if(!imageFolder.exists()) {
            imageFolder.mkdir();
        }

        String filename = localPath.substring(
                localPath.lastIndexOf("/")+1);

        File file = new File(imageFolder, filename);
        if( !file.exists() ) {
            try {
                FileUtils.copyFile(localPath, file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return file;
    }

    private boolean handleMenuClick(MenuItem item, Context context, Posts post) {
        switch(item.getItemId()) {
            case R.id.delete_post :
                new AlertDialog.Builder(context)
                        .setTitle("Delete Post")
                        .setMessage("Are you sure you want to delete this post?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deletePost(post);
                            }
                        })
                        .setNegativeButton("No", null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                break;
            case R.id.edit_post :
                Intent i = new Intent(fragment.getContext(), NewPostActivity.class);
                i.putExtra("EDITPOST",post);
                fragment.startActivityForResult(i, 5555);
                break;
        }
        return false;
    }

    private void deletePost( Posts post ) {
        FireConstants.postsRef.child(post.getPostId()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if( task.isSuccessful() )
                    posts.remove(post);
            }
        });
    }

    private String convertMilisecToDate( long milisec ) {
        Date currentDate = new Date(milisec);
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return df.format(currentDate);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public class PostViewHolder extends RecyclerView.ViewHolder implements OnMapReadyCallback {

        TextView postIdView;
        CircleImageView postUserImage;
        TextView postUserNameView;
        TextView postTimeView;
        ImageButton postOptionsButton;
        TextView postContentView;
        LinearLayout postLikeLayout;
        LinearLayout postCommentLayout;
        ImageView postShareImage;
        TextView postLikes;
        TextView postComments;
        CheckBox postLikeCheckbox;

        // single media
        RelativeLayout singleRayout;
        ImageView singleThumbImage;
        ImageView videoIconImage;

        // multiple medias
        PostGridView multipleMediaView;

        // location
        RelativeLayout locationRayout;
        MapView postMapView;
        GoogleMap mGoogleMap;
        LatLng mMapLocation;
        Context mContext;

        public PostViewHolder(@NonNull View itemView, Context context) {
            super(itemView);

            postIdView = itemView.findViewById(R.id.post_id);
            postUserImage = itemView.findViewById(R.id.post_userimg);
            postUserNameView = itemView.findViewById(R.id.post_userfullname);
            postTimeView = itemView.findViewById(R.id.post_time);
            postOptionsButton = itemView.findViewById(R.id.post_options);
            postContentView = itemView.findViewById(R.id.post_text);
            postLikeLayout = itemView.findViewById(R.id.like_layout);
            postCommentLayout = itemView.findViewById(R.id.comment_layout);
            postShareImage = itemView.findViewById(R.id.post_share);
            postLikes = itemView.findViewById(R.id.post_likes);
            postComments = itemView.findViewById(R.id.post_comments);
            postLikeCheckbox = itemView.findViewById(R.id.like_checkbox);

            // single media
            singleRayout = itemView.findViewById(R.id.single_media_layout);
            singleThumbImage = itemView.findViewById(R.id.post_image);
            videoIconImage = itemView.findViewById(R.id.play_image);

            // multiple medias
            multipleMediaView = itemView.findViewById(R.id.postedit_gridview);
            multipleMediaView.setFocusable(true);

            // location
            locationRayout = itemView.findViewById(R.id.post_location_layout);
            postMapView = itemView.findViewById(R.id.post_map_view);
            postMapView.onCreate(null);
            postMapView.getMapAsync(this);

            mContext = context;
        }

        @Override
        public void onMapReady(GoogleMap googleMap) {
            mGoogleMap = googleMap;

            MapsInitializer.initialize(mContext);
            googleMap.getUiSettings().setMapToolbarEnabled(false);

            // If we have mapView data, update the mapView content.
            if (mMapLocation != null) {
                updateMapContents();
            }
        }

        public void setMapLocation(LatLng location) {
            mMapLocation = location;

            // If the mapView is ready, update its content.
            if (mGoogleMap != null) {
                updateMapContents();
            }
        }

        private void updateMapContents() {
            // Since the mapView is re-used, need to remove pre-existing mapView features.
            mGoogleMap.clear();

            // Update the mapView feature data and camera position.
            mGoogleMap.addMarker(new MarkerOptions().position(mMapLocation));

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(mMapLocation, 17f);
            mGoogleMap.moveCamera(cameraUpdate);
        }
    }
}
