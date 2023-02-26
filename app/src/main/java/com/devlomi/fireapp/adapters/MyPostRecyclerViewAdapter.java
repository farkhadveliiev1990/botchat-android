package com.devlomi.fireapp.adapters;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.devlomi.fireapp.R;
import com.devlomi.fireapp.activities.CommentsActivity;
import com.devlomi.fireapp.activities.EditPostActivity;
import com.devlomi.fireapp.activities.EnlargyActivity;
import com.devlomi.fireapp.activities.NewPostActivity;
import com.devlomi.fireapp.fragments.PostFragment;
import com.devlomi.fireapp.model.Post;
import com.devlomi.fireapp.model.realms.User;
import com.devlomi.fireapp.utils.BitmapUtils;
import com.devlomi.fireapp.utils.DirManager;
import com.devlomi.fireapp.utils.FireManager;
import com.devlomi.fireapp.utils.MediaDownloadManager;
import com.devlomi.fireapp.utils.PostManager;
import com.devlomi.fireapp.utils.PostMedia;
import com.devlomi.fireapp.utils.PostUtil;
import com.devlomi.fireapp.utils.RealmHelper;
import com.devlomi.fireapp.utils.SharedPreferencesManager;
import com.devlomi.fireapp.views.PostGridView;
import com.devlomi.fireapp.views.SharePostView;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class MyPostRecyclerViewAdapter extends RecyclerView.Adapter<MyPostRecyclerViewAdapter.BasicViewHolder> {

    private final List<Post> mValues;
    User user = RealmHelper.getInstance().getUser(FireManager.getUid());
    private LinearLayout coordinatorLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressDialog progressDialog;
    private String myName = SharedPreferencesManager.getUserName();
    private Context mContext;
    private MyPostRecyclerViewAdapter adapter;
    private PostFragment mFragment;
//    private WallPostGridViewAdapter wallAdapter;
    private boolean isDownloaded;


    private int current_type = -1;

    public MyPostRecyclerViewAdapter(Context context, List<Post> items, ProgressDialog progressDialog,
                                     LinearLayout coordinatorLayout, SwipeRefreshLayout swipeRefreshLayout,
                                     PostFragment fragment) {
        mValues = items;
        this.progressDialog = progressDialog;
        this.coordinatorLayout = coordinatorLayout;
        this.swipeRefreshLayout = swipeRefreshLayout;
        this.mContext = context;
        this.adapter = this;
        this.mFragment = fragment;
    }

    private String convertMilisecToDate( long milisec ) {
        Date currentDate = new Date(milisec);
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return df.format(currentDate);
    }

    private String findVideo( String uri ) {
        String filePath = uri;
        if( uri.indexOf("+") != -1 ) {
            filePath = uri.substring(uri.indexOf("+")+1);
        }
        return filePath;
    }

    @Override
    public int getItemCount() {
        if (mValues == null)
            return 0;
        return mValues.size();
    }

    @NonNull
    @Override
    public BasicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.wall_post, parent, false);
        return new BasicViewHolder(view, parent.getContext());
    }

    private void onBindLocation( Post object, BasicViewHolder holder, int position ) {
        String[] latlong =  object.getLatlng().split(",");
        double latitude = Double.parseDouble(latlong[0]);
        double longitude = Double.parseDouble(latlong[1]);

        holder.location.postLocationLayout.setVisibility(View.VISIBLE);
        holder.single.singleMediaLayout.setVisibility(View.GONE);
        holder.muliple.postGridView.setVisibility(View.GONE);

        holder.location.setMapLocation(new LatLng(latitude, longitude));
    }

    private void onBindSameObject( Post object, BasicViewHolder holder, int position ) {

        holder.setPost(object);

        holder.postIdView.setText(object.getId()); // id
        Glide.with(holder.postUserImage.getContext())
                .asBitmap()
                .load(object.getPhotoUri())
                .into(holder.postUserImage); // photo
        holder.postUserNameView.setText(object.getDisplayName()); // username

        String pt_time = convertMilisecToDate(object.getTime());
        holder.postTimeView.setText(pt_time); // time

        if( object.getText() != null && !object.getText().equals("") ) {
            holder.postContentView.setVisibility(View.VISIBLE);
            holder.postContentView.setText(object.getText());
        } else {
            holder.postContentView.setVisibility(View.GONE);
        }

        holder.postLikes.setText(Integer.toString(PostManager.getLikeCount(object.getLikes())));
        holder.postComments.setText(Integer.toString(PostManager.getCommentCount(object.getComments())));

        holder.postLikeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                PostManager.updateLike(object);
                adapter.notifyDataSetChanged();
            }
        });

        holder.postCommentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(mContext, CommentsActivity.class);
                intent.putExtra("POST", object);
                mContext.startActivity(intent);
            }
        });

        holder.postShareImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showShareView(v, object);
            }
        });

        holder.postOptionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(holder.postOptionsButton.getContext(),
                        holder.postOptionsButton);
                popupMenu.getMenuInflater().inflate(R.menu.menu_post_actions,popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        return handleMenuClick(item,holder, holder.postOptionsButton.getContext());
                    }
                });

                // compare my name and current name
                String current = (String) holder.postUserNameView.getText();
                if( myName.equals(current) )
                    popupMenu.show();
            }
        });
    }

    @Override
    public void onBindViewHolder(@NonNull BasicViewHolder holder, int position) {

//        Post object = mValues.get(position);
//
//        if( object != null ) {
//
//            // Location, Single and Multiple
//            if (object.getLatlng() != null && !object.getLatlng().equals("")) {
//                onBindLocation(object, holder, position);
//                onBindSameObject(object, holder, position);
//            } else {
//
//                if (object.getMedias() != null && object.getMedias().size() > 1) {
//
//                    WallPostGridViewAdapter wallAdapter = null;
//
//                    List<PostMedia> temps = new ArrayList<>();
//                    List<String> localPaths = new ArrayList<>();
//
//                    List<HashMap<String,Object>> tempList = object.getMedias();
//                    for( int i=0; i<tempList.size(); i++ ) {
//                        PostMedia postMedia = new PostMedia();
//                        HashMap<String,Object> entry = tempList.get(i);
//                        for(Map.Entry<String, Object> ventry :entry.entrySet()) {
//                            String vkey = ventry.getKey();
//                            Object vvalue = ventry.getValue();
//                            switch (vkey) {
//                                case "content":
//                                    postMedia.setContent(vvalue.toString());
//                                    break;
//                                case "duration":
//                                    postMedia.setDuration(Long.parseLong(vvalue.toString()));
//                                    break;
//                                case "mediaId":
//                                    postMedia.setMediaId(vvalue.toString());
//                                    break;
//                                case "timestamp":
//                                    postMedia.setTimestamp(Long.parseLong(vvalue.toString()));
//                                    break;
//                                case "type":
//                                    postMedia.setType(Integer.parseInt(vvalue.toString()));
//                                    break;
//                                case "thumbImg":
//                                    postMedia.setThumbImg(vvalue.toString());
//                                    localPaths.add(vvalue.toString());
//                                    break;
//                                case "userId":
//                                    postMedia.setUserId(vvalue.toString());
//                                    break;
//                                case "localPath":
//                                    postMedia.setLocalPath(vvalue.toString());
//                                    break;
//                            }
//                        }
//                        temps.add(postMedia);
//                    }
//
//                    if (wallAdapter == null)
//                        wallAdapter = new WallPostGridViewAdapter(
//                                holder.muliple.postGridView.getContext(),
//                                localPaths);
//
//                    holder.muliple.postGridView.setAdapter(wallAdapter);
//                    wallAdapter.notifyDataSetChanged();
//
//                    holder.muliple.postGridView.setVisibility(View.VISIBLE);
//                    holder.location.postLocationLayout.setVisibility(View.GONE);
//                    holder.single.singleMediaLayout.setVisibility(View.GONE);
//
//                    onBindSameObject(object, holder, position);
//
//                    holder.muliple.postGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                        @Override
//                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//
//                            PostMedia postMedia = temps.get(position);
//
//                            if (postMedia != null) {
//                                PostUtil.post = object;
//                                PostUtil.postMedia = postMedia;
//                                Intent intent = new Intent(mContext, EnlargyActivity.class);
////                                intent.putExtra("EDITPOST", object);
////                                intent.putExtra("POSTMEDIA", postMedia);
//                                mContext.startActivity(intent);
//                            }
//                        }
//                    });
//                } else{
//                    holder.muliple.postGridView.setVisibility(View.GONE);
//                    holder.single.playImage.setVisibility(View.GONE);
//                    holder.single.singleMediaLayout.setVisibility(View.GONE);
//
//                    if (object.getMedias() != null) {
//
//                        List<PostMedia> temps = new ArrayList<>();
//
//                        List<HashMap<String,Object>> tempList = object.getMedias();
//                        for( int i=0; i<tempList.size(); i++ ) {
//                            PostMedia postMedia = new PostMedia();
//                            HashMap<String,Object> entry = tempList.get(i);
//                            for(Map.Entry<String, Object> ventry :entry.entrySet()) {
//                                String vkey = ventry.getKey();
//                                Object vvalue = ventry.getValue();
//                                switch (vkey) {
//                                    case "content":
//                                        postMedia.setContent(vvalue.toString());
//                                        break;
//                                    case "duration":
//                                        postMedia.setDuration(Long.parseLong(vvalue.toString()));
//                                        break;
//                                    case "mediaId":
//                                        postMedia.setMediaId(vvalue.toString());
//                                        break;
//                                    case "timestamp":
//                                        postMedia.setTimestamp(Long.parseLong(vvalue.toString()));
//                                        break;
//                                    case "type":
//                                        postMedia.setType(Integer.parseInt(vvalue.toString()));
//                                        break;
//                                    case "thumbImg":
//                                        postMedia.setThumbImg(vvalue.toString());
//                                        break;
//                                    case "userId":
//                                        postMedia.setUserId(vvalue.toString());
//                                        break;
//                                    case "localPath":
//                                        postMedia.setLocalPath(vvalue.toString());
//                                        break;
//                                }
//                            }
//                            temps.add(postMedia);
//                        }
//
//                        holder.single.singleMediaLayout.setVisibility(View.VISIBLE);
//
//                        Glide.with(holder.single.postImage.getContext())
//                                .asBitmap()
//                                .load(BitmapUtils.encodeImageAsBytes(temps.get(0).getThumbImg()))
//                                .into(holder.single.postImage);
//
//                        holder.single.postImage.setVisibility(View.VISIBLE);
//                        holder.location.postLocationLayout.setVisibility(View.GONE);
//                        holder.muliple.postGridView.setVisibility(View.GONE);
//
//                        if (temps.get(0).getType() == 2)
//                            holder.single.playImage.setVisibility(View.VISIBLE);
//
//                        holder.single.postImage.setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                Intent intent = new Intent(mContext, EnlargyActivity.class);
//                                PostUtil.post = object;
//                                PostUtil.postMedia = temps.get(0);
////                                intent.putExtra("EDITPOST", object);
////                                intent.putExtra("POSTMEDIA", temps.get(0));
//                                mContext.startActivity(intent);
//                            }
//                        });
//
//                        onBindSameObject(object, holder, position);
//
//                    } else {
//                        holder.single.postImage.setVisibility(View.GONE);
//                        holder.single.playImage.setVisibility(View.GONE);
//
//                        onBindSameObject(object, holder, position);
//
//                    }
//                }
//            }
//        }
    }

    @Override
    public int getItemViewType(int position) {
        return -1;
    }

    public static class BasicViewHolder extends RecyclerView.ViewHolder {

        protected TextView postIdView;
        protected CircleImageView postUserImage;
        protected TextView postUserNameView;
        protected TextView postTimeView;
        protected ImageButton postOptionsButton;
        protected TextView postContentView;
        protected LinearLayout postLikeLayout;
        protected LinearLayout postCommentLayout;
        protected ImageView postShareImage;
        protected TextView postLikes;
        protected TextView postComments;

        public SingleMediaHolder single;
        public MultipleMediaHolder muliple;
        public GPSLocationHolder location;

        private Post post;

        public BasicViewHolder(@NonNull View itemView, Context context) {
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

            single = new SingleMediaHolder(itemView, context);
            muliple = new MultipleMediaHolder(itemView, context);
            location = new GPSLocationHolder(itemView, context);
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }
    }

    public static class SingleMediaHolder {

        protected RelativeLayout singleMediaLayout;
        protected ImageView postImage;
        protected ImageView playImage;

        public SingleMediaHolder(@NonNull View itemView, Context context) {


            singleMediaLayout = itemView.findViewById(R.id.single_media_layout);
            postImage = itemView.findViewById(R.id.post_image);
            playImage = itemView.findViewById(R.id.play_image);
        }
    }

    public static class MultipleMediaHolder {

        protected PostGridView postGridView;

        public MultipleMediaHolder(@NonNull View itemView, Context context) {


            postGridView = itemView.findViewById(R.id.postedit_gridview);
            postGridView.setFocusable(true);
        }
    }

    public static class GPSLocationHolder implements OnMapReadyCallback {

        protected RelativeLayout postLocationLayout;
        protected MapView mapView;

        protected GoogleMap mGoogleMap;
        protected LatLng mMapLocation;

        protected Context latContext;

        public GPSLocationHolder(@NonNull View itemView, Context context) {


            latContext = context;

            postLocationLayout = itemView.findViewById(R.id.post_location_layout);
            mapView = itemView.findViewById(R.id.map_view);

            mapView.onCreate(null);
            mapView.getMapAsync(this);
        }

        @Override
        public void onMapReady(GoogleMap googleMap) {
            mGoogleMap = googleMap;

            MapsInitializer.initialize(latContext);
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

        protected void updateMapContents() {
            // Since the mapView is re-used, need to remove pre-existing mapView features.
            mGoogleMap.clear();

            // Update the mapView feature data and camera position.
            mGoogleMap.addMarker(new MarkerOptions().position(mMapLocation));

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(mMapLocation, 17f);
            mGoogleMap.moveCamera(cameraUpdate);
        }
    }

        private boolean handleMenuClick(MenuItem item, BasicViewHolder holder, Context context) {
        switch(item.getItemId()) {
            case R.id.delete_post :
                new AlertDialog.Builder(context)
                        .setTitle("Delete Post")
                        .setMessage("Are you sure you want to delete this post?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                progressDialog.setCancelable(false);
                                progressDialog.setMessage("Deleting post!");
                                progressDialog.show();

                                Post post = holder.getPost();
                                PostManager.deletePost(post);
                                progressDialog.cancel();

                                PostManager.getPostsListItems(mValues,MyPostRecyclerViewAdapter.this,swipeRefreshLayout);
                                Snackbar.make(coordinatorLayout,"Succesfully deleted post",2500).setActionTextColor(Color.GREEN).show();
                            }
                        })
                        .setNegativeButton("No", null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                break;
            case R.id.edit_post :
                Post post = new Post();
                post = holder.getPost();

                Intent i = new Intent(coordinatorLayout.getContext(), NewPostActivity.class);
                i.putExtra("EDITPOST",post);
                coordinatorLayout.getContext().startActivity(i);
                break;
        }
        return false;
    }

    private void showShareView(View v, Post post) {
        User user = RealmHelper.getInstance().getUser(FireManager.getUid());

        mFragment.sharePostView.setData(post, mFragment);
        mFragment.sharePostView.show(v, user.getPhoto(), SharedPreferencesManager.getUserName());
    }
}
