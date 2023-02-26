package com.devlomi.fireapp.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cjt2325.cameralibrary.ResultCodes;
import com.devlomi.fireapp.R;
import com.devlomi.fireapp.activities.placespicker.Place;
import com.devlomi.fireapp.activities.placespicker.PlacesPickerActivity;
import com.devlomi.fireapp.adapters.NewPostGridViewAdapter;
import com.devlomi.fireapp.model.Post;
import com.devlomi.fireapp.model.Posts;
import com.devlomi.fireapp.model.constants.MessageType;
import com.devlomi.fireapp.utils.BitmapUtils;
import com.devlomi.fireapp.utils.DirManager;
import com.devlomi.fireapp.utils.FileUtils;
import com.devlomi.fireapp.utils.FireConstants;
import com.devlomi.fireapp.utils.FireManager;
import com.devlomi.fireapp.utils.Glide4Engine;
import com.devlomi.fireapp.utils.IntentUtils;
import com.devlomi.fireapp.utils.KeyboardHelper;
import com.devlomi.fireapp.utils.PostManager;
import com.devlomi.fireapp.utils.PostMedia;
import com.devlomi.fireapp.utils.PostMediaCreator;
import com.devlomi.fireapp.utils.SharedPreferencesManager;
import com.devlomi.fireapp.utils.Util;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.storage.UploadTask;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.devlomi.fireapp.activities.ChatActivity.MAX_SELECTABLE;

public class NewPostActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final int LOCATION_TYPE = 2;
    public static final int IMGANDVID_TYPE = 0;
    public static final int ONLYTEXT_TYPE = 1;

    private static final int POSTTYPE_IMAGE = 1;
    private static final int POSTTYPE_VIDEO = 2;

    private static final int CAMERA_REQUEST = 4659;
    private static final int PICK_GALLERY_REQUEST = 4815;

    private int post_type = -1;

    CoordinatorLayout coordinatorLayout;
    ProgressDialog progressDialog;
    Bundle savedInstanceState;
    private String savefilename = "";

    private final int IMAGE_DIMENSION = 100;
    EditText postText;

    // Mou
    private ImageButton galleryImages;
    private ImageButton cameraImage;
    private ImageButton locationImage;
    private ImageButton docImage;
    private ImageButton attachImage;

    private static final int PICK_LOCATION_REQUEST = 7125;

    private MapView mapView;

    private GoogleMap mGoogleMap;
    private LatLng mMapLocation;

    //
    private String latlng = "";

    GridView imagesPreview;
    NewPostGridViewAdapter adapter;

    ArrayList<String> imagesPaths;
    private Posts post;

    public CoordinatorLayout getCoordinatorLayout() {
        return coordinatorLayout;
    }

    public ProgressDialog getProgressDialog() {
        return progressDialog;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.savedInstanceState = savedInstanceState;
        setContentView(R.layout.activity_new_post);
        coordinatorLayout = findViewById(R.id.newPostCoordinator);
        progressDialog = new ProgressDialog(NewPostActivity.this);

        postText = findViewById(R.id.post_text);
        postText.requestFocus();
        KeyboardHelper.openSoftKeyboard(this, postText);

        mapView = findViewById(R.id.mapView);

        mapView.onCreate(null);
        mapView.getMapAsync(this);
        // Mou
        galleryImages = findViewById(R.id.gallery_images);
        cameraImage = findViewById(R.id.camera_image);
        locationImage = findViewById(R.id.location_image);
        docImage = findViewById(R.id.document_image);
        attachImage = findViewById(R.id.attachment_image);
        //

        imagesPreview = findViewById(R.id.gridview_images_preview);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float width = displayMetrics.widthPixels / displayMetrics.density;
        int columns = (int) width/IMAGE_DIMENSION;
        imagesPreview.setNumColumns(columns);
        imagesPreview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                imagesPaths.remove(position);
                adapter.notifyDataSetChanged();
                return false;
            }
        });

        imagesPaths = new ArrayList<>();

        post =(Posts) getIntent().getSerializableExtra("EDITPOST");
        if( post != null ) {
            importDataFromPost(post);
            getSupportActionBar().setTitle("Edit Post");
        } else {
            post = new Posts();
            getSupportActionBar().setTitle(R.string.new_post);
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // gallery button
        galleryImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImages();
            }
        });

        // camera button
        cameraImage.setOnClickListener(new View.OnClickListener() {// Camera
            @Override
            public void onClick(View v) {
                startCamera();
            }
        });

        locationImage.setOnClickListener(new View.OnClickListener() {// location
            @Override
            public void onClick(View v) {
                pickLocation();
            }
        });

        docImage.setOnClickListener(new View.OnClickListener() {// document
            @Override
            public void onClick(View v) {

            }
        });

        attachImage.setOnClickListener(new View.OnClickListener() {// attachment
            @Override
            public void onClick(View v) {

            }
        });
        //
    }

    private void startCamera() {
        startActivityForResult(new Intent(NewPostActivity.this, CameraActivity.class), CAMERA_REQUEST);
    }

    private void pickLocation() {
        startActivityForResult(new Intent(this, PlacesPickerActivity.class), PICK_LOCATION_REQUEST);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_post, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackPressed();
        else if (item.getItemId() == R.id.post_items) {
            // remove previous post
//            if( post != null )
//                PostManager.deletePost(post);

            postBlog();
        }
        return super.onOptionsItemSelected(item);
    }

    private void postBlog() {
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Creating post!");
        progressDialog.show();

        if( latlng.equals("") ) {
            if(imagesPaths != null && imagesPaths.size() > 0)
                post_type = IMGANDVID_TYPE;
            else
                post_type = ONLYTEXT_TYPE;
        } else { // location
            post_type = LOCATION_TYPE;
        }

        createPost(
                postText.getText().toString(),
                post_type,
                imagesPaths,
                latlng,
                this
        );
    }

    private void pickImages() {
        Matisse.from(NewPostActivity.this)
                .choose(MimeType.of(MimeType.MP4, MimeType.THREEGPP, MimeType.THREEGPP2
                        , MimeType.JPEG, MimeType.BMP, MimeType.PNG, MimeType.GIF))
                .countable(true)
                .maxSelectable(MAX_SELECTABLE)
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                .thumbnailScale(0.85f)
                .imageEngine(new Glide4Engine())
                .forResult(PICK_GALLERY_REQUEST);
    }

    private String createImagePath(String imagePath, boolean fromCamera) {
        int type = MessageType.SENT_IMAGE;

        File file = DirManager.generateFile(type);
        String fileExtension = Util.getFileExtensionFromPath(imagePath);

        if (fileExtension.equals("gif")) {
            try {
                FileUtils.copyFile(imagePath, file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            //compress image and copy it to the given file
            BitmapUtils.compressImage(imagePath, file);
        }
        //if this image is captured by the camera in our app
        // then we need to delete the captured image after copying it to another directory
        if (fromCamera) {
            //delete captured image from camera after compress it
            FileUtils.deleteFile(imagePath);
        }

        String filePath = file.getPath();

        //set the file size
        String fileSize = Util.getFileSizeFromLong(file.length(), true);

        return filePath;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // result for multiple pick from gallery
        if( requestCode == CAMERA_REQUEST && resultCode != ResultCodes.CAMERA_ERROR_STATE ) {

            if (resultCode == ResultCodes.IMAGE_CAPTURE_SUCCESS) {

                String path = data.getStringExtra(IntentUtils.EXTRA_PATH_RESULT);

                path = createImagePath(path, true);
                setMultimedia(path);

            } else if (resultCode == ResultCodes.VIDEO_RECORD_SUCCESS) {

                String path = data.getStringExtra(IntentUtils.EXTRA_PATH_RESULT);

                setMultimedia(path);
            }

        } else if( requestCode == PICK_GALLERY_REQUEST && resultCode == RESULT_OK ) {
            List<String> mPaths = Matisse.obtainPathResult(data);
            for (String mPath : mPaths) {

                if (!FileUtils.isFileExists(mPath)) {
                    Toast.makeText(NewPostActivity.this, R.string.image_video_not_found, Toast.LENGTH_SHORT).show();
                    continue;
                }

                //Check if it's a video
                if (FileUtils.isPickedVideo(mPath)) {

                    setMultimedia(mPath);

                } else {
                    String path = createImagePath(mPath, false);
                    setMultimedia(path);
                }

            }
        }

        if (requestCode == PICK_LOCATION_REQUEST && resultCode == RESULT_OK) {
            Place place = data.getParcelableExtra(Place.EXTRA_PLACE);
            LatLng ll = place.getLatLng();
            latlng = String.format("%s,%s", String.valueOf(ll.latitude),
                    String.valueOf(ll.longitude));

            mapView.setVisibility(View.VISIBLE);
            setMapLocation(ll);
        }
    }

    private void setMultimedia( String path ) {
        imagesPaths.add(path);

        if( adapter == null ) {
            adapter = new NewPostGridViewAdapter(NewPostActivity.this,imagesPaths);
            imagesPreview.setAdapter(adapter);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        if( isSavePost ) {
            setResult();
        }
        super.onBackPressed();
    }

    private void setResult() {
        Intent intent = new Intent();
        intent.putExtra("newpost", post);
        setResult(1123, intent);
    }

    private void importDataFromPost( Posts post ) {

        postText.setText(post.getPostText());

        switch (post.getPostType()) {
            case IMGANDVID_TYPE:
                imagesPreview.setVisibility(View.VISIBLE);
                mapView.setVisibility(View.GONE);

                previewPost(post, IMGANDVID_TYPE);
                break;
            case ONLYTEXT_TYPE:
                break;
            case LOCATION_TYPE:
                imagesPreview.setVisibility(View.GONE);
                mapView.setVisibility(View.VISIBLE);

                previewPost(post, LOCATION_TYPE);
                break;
        }
    }

    private void previewPost(Posts post, int type) {
        HashMap<String,Object> hashMap = post.getPostMedias();

        for( Map.Entry<String,Object> entry: hashMap.entrySet() ) {
            String key = entry.getKey();
            HashMap<String,Object> value = (HashMap<String,Object>)entry.getValue();

            for( Map.Entry<String,Object> ventry : value.entrySet() ) {
                String subKey = ventry.getKey();
                Object subValue = ventry.getValue();

                if( type == IMGANDVID_TYPE ) {
                    if( subKey.equals("localPath") ) {
                        setMultimedia(subValue.toString());
                        break;
                    }
                }
            }

            if( type == LOCATION_TYPE ) {
                if( key.equals("postLocation") ) {
                    String[] latlong =  post.getPostLocation().split(",");
                    double latitude = Double.parseDouble(latlong[0]);
                    double longitude = Double.parseDouble(latlong[1]);

                    setMapLocation(new LatLng(latitude, longitude));
                }
            }
        }

    }

    private void previewMap(Posts post) {

    }

    private String saveImage(Drawable drawable) {
        String savedImagePath = null;

        String imageFileName = String.format("%d.jpg", System.currentTimeMillis());
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/post";
        File storageDir = new File(path);
        boolean success = true;
        if (!storageDir.exists()) {
            success = storageDir.mkdirs();
        }
        if (success) {
            File imageFile = new File(storageDir, imageFileName);
            savedImagePath = imageFile.getAbsolutePath();
            try {
                OutputStream fOut = new FileOutputStream(imageFile);
                Bitmap bitmap = drawableToBitmap(drawable);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                fOut.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Add the image to the system gallery
            //galleryAddPic(savedImagePath);
            Toast.makeText(NewPostActivity.this, "IMAGE SAVED", Toast.LENGTH_LONG).show();
        }
        return savedImagePath;
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        MapsInitializer.initialize(this);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        // If we have mapView data, update the mapView content.
        if (mMapLocation != null) {
            updateMapContents();
        }

    }

    private void setMapLocation(LatLng location) {
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

    /////////////////////////////////////////////////////////////////
    // Create Post
    /////////////////////////////////////////////////////////////////

    public void createPost(String postTxt, int postType, List<String> imagesPaths, String latlng, Object caller) {
        switch (postType) {
            case IMGANDVID_TYPE:
                createMediaPost(postTxt, postType, imagesPaths, caller);
                break;
            case ONLYTEXT_TYPE:
                createTextPost(postTxt, postType, caller);
                break;
            case LOCATION_TYPE:
                createLocationPosts(postTxt, postType, latlng, caller);
                break;
        }
    }

    private void saveMedia( String key, List<String> mediapaths, Object caller ) {
        List<Integer> types = new ArrayList<>();
        List<PostMedia> postMedias = new ArrayList<>();

        for( String mediapath: mediapaths ) {

            PostMedia postMedia;
            String extension = Util.getFileExtensionFromPath(mediapath);
            if( extension.equals("jpg") ) {
                types.add(POSTTYPE_IMAGE);
                postMedia = PostMediaCreator.createImagePost(mediapath);
            }
            else {
                types.add(POSTTYPE_VIDEO);
                postMedia = PostMediaCreator.createVideoPost(mediapath);
            }
            postMedias.add(postMedia);

            final String fileName = Util.getFileNameFromPath(mediapath);
            FireManager.getRef(FireManager.POST_TYPE, fileName).putFile(Uri.fromFile(new File(mediapath))).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> uploadTask) {
                    if( uploadTask.isSuccessful() ) {

                        if( types.get(mediapaths.indexOf(mediapath)) == POSTTYPE_IMAGE ) { // is image
                            uploadTask.getResult().getStorage().getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    if( task.isSuccessful() ) {

                                        final Uri uri = task.getResult().normalizeScheme();
                                        PostMedia temp = postMedias.get(mediapaths.indexOf(mediapath));
                                        temp.setContent(String.valueOf(uri));

                                        String sub_key = FireConstants.postsRef.child(key).child("posMedias").push().getKey();

                                        setMedias(temp, sub_key);

                                        HashMap<String,Object> hashMap = new HashMap<>();
                                        hashMap.put("content", temp.getContent());
                                        hashMap.put("duration", temp.getDuration());
                                        hashMap.put("localPath", temp.getLocalPath());
                                        hashMap.put("thumbImg", temp.getThumbImg());
                                        hashMap.put("timestamp", temp.getTimestamp());
                                        hashMap.put("type", temp.getType());
                                        hashMap.put("userId", temp.getUserId());

                                        FireConstants.postsRef
                                                .child(key).child("postMedias").child(sub_key).updateChildren(hashMap)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        PostMedia last = postMedias.get(mediapaths.size()-1);
                                                        if( temp.equals(last) ) {
                                                            setNewPostMedias();
                                                            gotoNewPostActivity(caller);
                                                        }
                                                    }
                                                });
                                    }
                                }
                            });
                        } else { // is video
                            final String filePathBucket = uploadTask.getResult().getStorage().getPath();
                            PostMedia temp = postMedias.get(mediapaths.indexOf(mediapath));
                            temp.setContent(filePathBucket);

                            String sub_key = FireConstants.postsRef.child(key).child("posMedias").push().getKey();

                            setMedias(temp, sub_key);

                            HashMap<String,Object> hashMap = new HashMap<>();
                            hashMap.put("content", temp.getContent());
                            hashMap.put("duration", temp.getDuration());
                            hashMap.put("localPath", temp.getLocalPath());
                            hashMap.put("thumbImg", temp.getThumbImg());
                            hashMap.put("timestamp", temp.getTimestamp());
                            hashMap.put("type", temp.getType());
                            hashMap.put("userId", temp.getUserId());

                            FireConstants.postsRef
                                    .child(key).child("postMedias").child(sub_key).updateChildren(hashMap)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            PostMedia last = postMedias.get(mediapaths.size()-1);
                                            if( temp.equals(last) ) {
                                                setNewPostMedias();
                                                gotoNewPostActivity(caller);
                                            }
                                        }
                                    });
                        }

                    } else {

                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Snackbar.make(((NewPostActivity) caller).getCoordinatorLayout(),e.getMessage(),2500).setActionTextColor(Color.RED).show();
                }
            });

        }
    }

    private static void gotoNewPostActivity( Object caller ) {
        Snackbar.make(((NewPostActivity) caller).getCoordinatorLayout(),"Succesfully added post",2500).setActionTextColor(Color.GREEN).show();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                ((NewPostActivity) caller).onBackPressed();
            }
        }, 3500);
        ((NewPostActivity) caller).getProgressDialog().dismiss();
    }

    private void createMediaPost(String text, int postType, List<String> mediapaths, Object caller) {

        String key = FireConstants.postsRef.push().getKey();
        HashMap<String,Object> data = createPostHashMap(key, text, postType, null);
        FireConstants.postsRef
                .child(key)
                .updateChildren(data).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if( task.isSuccessful() ) {
                    saveMedia(key, mediapaths, caller);
                }
            }
        });


    }

    public HashMap<String,Object> createPostHashMap( String key, String postText, int type, String latlong) {
        // new post
        setNewPost(key, postText, type, latlong);
        //

        HashMap<String,Object> data = new HashMap<>();

        if(!postText.equals("") && !postText.equals(null))
            data.put("postText",post.getPostText());
        data.put("postId", post.getPostId());
        data.put("postUid", post.getPostUid());
        data.put("postName", post.getPostName());
        data.put("postPhotoUrl", post.getPostPhotoUrl());
        data.put("postShares",post.getPostShares());
        data.put("postIsShared",post.getPostIsShared());
        data.put("postTime", post.getPostTime());
        data.put("postType", post.getPostType());
        if( latlong != null && !latlong.equals("") )
            data.put("postLocation", post.getPostLocation());

        return data;
    }

    private void createTextPost( String text, int postType, Object caller ) {
        String key = FireConstants.postsRef.push().getKey();
        HashMap<String,Object> data = createPostHashMap(key, text, postType, null);

        FireConstants.postsRef
                .child(key)
                .updateChildren(data).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if( task.isSuccessful() ) {
                    // save to preference
                    isSavePost = true;

                    // go back
                    Handler handler = new Handler();
                    Snackbar.make(((NewPostActivity) caller).getCoordinatorLayout(),"Succesfully added post",2500).setActionTextColor(Color.GREEN).show();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            ((NewPostActivity) caller).onBackPressed();
                        }
                    }, 3500);
                    ((NewPostActivity) caller).getProgressDialog().dismiss();
                } else {
                    ((NewPostActivity) caller).getProgressDialog().dismiss();
                    Snackbar.make(((NewPostActivity) caller).getCoordinatorLayout(),"Text must be provided!",2500).setActionTextColor(Color.RED).show();
                }
            }
        });
    }

    private void createLocationPosts( String text, int postType, String latlng, Object caller ) {
        String key = FireConstants.postsRef.push().getKey();
        HashMap<String,Object> data = createPostHashMap(key, text, postType, latlng);

        FireConstants.postsRef
                .child(key)
                .updateChildren(data).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if( task.isSuccessful() ) {
                    // save to preference
                    isSavePost = true;

                    // go back
                    Handler handler = new Handler();
                    Snackbar.make(((NewPostActivity) caller).getCoordinatorLayout(),"Succesfully added post",2500).setActionTextColor(Color.GREEN).show();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            ((NewPostActivity) caller).onBackPressed();
                        }
                    }, 3500);
                    ((NewPostActivity) caller).getProgressDialog().dismiss();
                } else {
                    ((NewPostActivity) caller).getProgressDialog().dismiss();
                    Snackbar.make(((NewPostActivity) caller).getCoordinatorLayout(),"Location must be provided!",2500).setActionTextColor(Color.RED).show();
                }
            }
        });
    }

    private void setNewPost( String key, String postText, int type, String latlong) {

        post.setPostId(key);
        post.setPostUid(FireManager.getUid());
        post.setPostIsShared(false);
        post.setPostShares(0);
        post.setPostText(postText);
        post.setPostName(SharedPreferencesManager.getUserName());
        post.setPostPhotoUrl(SharedPreferencesManager.getMyPhoto());
        post.setPostType(type);
        post.setPostTime(new Date().getTime());
        post.setPostIsLiked(false);
        post.setPostLikes(0);
        if( latlong != null && !latlong.equals("") )
            post.setPostLocation(latlong);

    }

    Set<Map<String,Object>> mapSet = new HashSet<>();
    private void setMedias( PostMedia postMedia, String sKey ) {
        Map<String,Object> ssHashMap = new HashMap<>();

        ssHashMap.put("content", postMedia.getContent());
        ssHashMap.put("duration", postMedia.getDuration());
        ssHashMap.put("localPath", postMedia.getLocalPath());
        ssHashMap.put("thumbImg", postMedia.getThumbImg());
        ssHashMap.put("timestamp", postMedia.getTimestamp());
        ssHashMap.put("type", postMedia.getType());
        ssHashMap.put("userId", postMedia.getUserId());

        Map<String,Object> sHashMap = new HashMap<>();
        sHashMap.put(sKey, ssHashMap);

        mapSet.add(sHashMap);

    }

    private boolean isSavePost = false;
    private void setNewPostMedias() {

        HashMap<String,Object> hashMap = new HashMap<>();
        for( Map<String,Object> map : mapSet ) {
            String[] keyArray = new String[map.keySet().size()];
            map.keySet().toArray(keyArray);

            String key = keyArray[0];

            hashMap.put(key, map.get(key));
        }
//        hashMap.put("postMedias", value);
        post.setPostMedias(hashMap);

        isSavePost = true;
    }

    /////////////////////////////////////////////////////////////////

    // Download video
    private class DownloadFile extends AsyncTask<String, String, String> {

        private ProgressDialog progressDialog;
        private String fileName;
        private String folder;
        private boolean isDownloaded;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            this.progressDialog = new ProgressDialog(NewPostActivity.this);
            this.progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            this.progressDialog.setCancelable(false);
            this.progressDialog.setMessage("Loading media");
            this.progressDialog.show();
        }

        @Override
        protected String doInBackground(String... strings) {
            int count;

            URL url = null;
            try {
                url = new URL(strings[0]);
                URLConnection connection = url.openConnection();
                connection.connect();

                // getting file length
                int lengthOfFile = connection.getContentLength();
                // input stream to read file - with 8k buffer
                InputStream input = new BufferedInputStream(url.openStream(), 8192);

                String imageFileName = String.format("%d.mp4", System.currentTimeMillis());
                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/post";
                File storageDir = new File(path);
                boolean success = true;
                if (!storageDir.exists()) {
                    success = storageDir.mkdirs();
                }
                if( success ) {
                    File imageFile = new File(storageDir, imageFileName);
                    savefilename = imageFile.getAbsolutePath();
                    try {
                        OutputStream output = new FileOutputStream(imageFile);

                        byte data[] = new byte[1024];

                        long total = 0;

                        while ((count = input.read(data)) != -1) {
                            total += count;
                            // publishing the progress....
                            // After this onProgressUpdate will be called
                            publishProgress("" + (int) ((total * 100) / lengthOfFile));

                            // writing data to file
                            output.write(data, 0, count);
                        }
                        // flushing output
                        output.flush();

                        // closing streams
                        output.close();
                        input.close();
                        return "Downloaded at: " + imageFile;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception  e) {
                e.printStackTrace();
            }
            return "Something went wrong";
        }

        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            progressDialog.setProgress(Integer.parseInt(progress[0]));
        }


        @Override
        protected void onPostExecute(String message) {
            // dismiss the dialog after the file was downloaded
            this.progressDialog.dismiss();

            // Display File path after downloading
            Toast.makeText(getApplicationContext(),
                    message, Toast.LENGTH_LONG).show();

            imagesPaths.add(String.format("+%s",savefilename));

            if( adapter == null ) {
                adapter = new NewPostGridViewAdapter(NewPostActivity.this,imagesPaths);
                imagesPreview.setAdapter(adapter);
            }
            adapter.notifyDataSetChanged();

        }
    }
}
