package com.devlomi.fireapp.utils;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.devlomi.fireapp.activities.NewPostActivity;
import com.devlomi.fireapp.adapters.MyPostRecyclerViewAdapter;
import com.devlomi.fireapp.fragments.PostFragment;
import com.devlomi.fireapp.model.Comment;
import com.devlomi.fireapp.model.Post;
import com.devlomi.fireapp.model.Posts;
import com.devlomi.fireapp.model.realms.User;
import com.droidninja.imageeditengine.Constants;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.realm.RealmResults;

public class PostManager {

    public static String findVideo( String uri ) {
        String filePath = uri;
        if( uri.indexOf("+") != -1 ) {
            filePath = uri.substring(uri.indexOf("+")+1);
        }
        return filePath;
    }


    private static void savePost( List<PostMedia> postMedias, String postText, int type, Object caller) {
        HashMap<String,Object> data = new HashMap<>();
        HashMap<String,String> likesList = new HashMap<>();
        HashMap<String,Object> commentsList = new HashMap<>();

        if(!postText.equals("") && !postText.equals(null))
            data.put("postText",postText);
        data.put("postUid", FireManager.getUid());
        data.put("postName", SharedPreferencesManager.getUserName());
        data.put("postPhotoUrl", SharedPreferencesManager.getMyPhoto());
        //data.put("postImages",iuuids);
        data.put("postLikes",likesList);
        data.put("postComments",commentsList);
        data.put("postShares",0);
        data.put("postIsShared",false);
        data.put("postTime", new Date().getTime());
        data.put("postType", type);
        data.put("postMedias", postMedias);


        String puuid = UUID.randomUUID().toString();
        FireConstants.postsRef.child(FireManager.getUid())
                .child(puuid)
                .updateChildren(data);

        Snackbar.make(((NewPostActivity) caller).getCoordinatorLayout(),"Succesfully added post",2500).setActionTextColor(Color.GREEN).show();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                ((NewPostActivity) caller).onBackPressed();
            }
        }, 3500);
        ((NewPostActivity) caller).getProgressDialog().dismiss();

    }

    public static void deletePost(Post post) {
//        if( post.getImages() != null ) {
//            List<String> imagesKeys = new ArrayList<>(post.getImages().keySet());
//            for(String url: imagesKeys) {
//                StorageReference ref = FireConstants.postsStorageRef.child(url);
//                ref.delete().addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        //If the image fails to delete do something (maybe add it to a future job to delete it)
//                        int failure = 0;
//                    }
//                }).addOnSuccessListener(new OnSuccessListener<Void>() {
//                    @Override
//                    public void onSuccess(Void aVoid) {
//                        int success = 0;
//                    }
//                }).addOnCompleteListener(new OnCompleteListener<Void>() {
//                    @Override
//                    public void onComplete(@NonNull Task<Void> task) {
//                        int complete = 0;
//                    }
//                });
//            }
//        }
//        FireConstants.postsRef.child(FireManager.getUid()).child(post.getId()).removeValue();
    }

    //
    public static void updateLikes( Posts post, int count, boolean isLiked, OnCompleteLikes onCompleteLikes ) {
        FireConstants.postsRef.child(post.getPostId()).child("postIsLiked")
                .setValue(isLiked).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if( task.isSuccessful() ) {
                    post.setPostIsLiked(isLiked);

                    FireConstants.postsRef.child(post.getPostId()).child("postLikes")
                            .setValue(count).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if( task.isSuccessful() ) {
                                post.setPostLikes(count);

                                onCompleteLikes.OnComplete(count);
                            }
                        }
                    });
                }
            }
        });
    }

    public static int getLikeCount( HashMap<String,String> likes ) {

        if( likes == null ) return 0;
        return likes.size();
    }

    public static int getCommentCount( HashMap<String,Object> comments ) {

        if( comments == null ) return 0;
        return comments.size();
    }

    /**
     * This method consists of fetching posts of the current user and his contacts.
     * @return List
     */
    public static void getPostsListItems(List<Post> postsList,Object adapter,Object swipeRefreshLayout) {
        ((SwipeRefreshLayout) swipeRefreshLayout).setRefreshing(true);

        RealmResults<User> users = RealmHelper.getInstance().getListOfUsers();
        FireManager.getAllPosts(users, new FireManager.postsListener() {
            @Override
            public void onFound(List<Post> ps) {
                postsList.clear();

                sortComments(ps);

                Log.e("Posts List",postsList.toString());
                ((SwipeRefreshLayout) swipeRefreshLayout).setRefreshing(false);
                ((MyPostRecyclerViewAdapter) adapter).notifyDataSetChanged();
            }
            @Override
            public void onNotFound() {
                ((SwipeRefreshLayout) swipeRefreshLayout).setRefreshing(false);
            }

            private void sortComments(List<Post> ps) {
                Collections.sort(ps, new Comparator<Post>() {
                    @Override
                    public int compare(Post o1, Post o2) {
                        String s2 = String.format("%d", o2.getTime());
                        String s1 = String.format("%d", o1.getTime());
                        return s2.compareTo(s1);
                    }
                });
                postsList.addAll(ps);
            }
        });
    }

    private static Task<Uri> uploadImageTask(String path, String uuid) throws IOException {
        InputStream stream = new FileInputStream(new File(path));
        final StorageReference ref = FireConstants.postsStorageRef.child(uuid);
        UploadTask uploadTask = ref.putStream(stream);

//        Uri uri = uploadTask.getResult().getStorage().getDownloadUrl();

        return uploadTask.continueWithTask(task -> ref.getDownloadUrl());
//        return uploadTask.continueWithTask(task -> ref.getDownloadUrl());
    }

    public static void getUsersListItem(List<User> usersList, Object adapter, Object swipeRefreshLayout) {

        ((SwipeRefreshLayout) swipeRefreshLayout).setRefreshing(true);

    }

    private static List<String> currentDownloadPostOperations = new ArrayList<>();

    public static void downloadVideoPost(final String id, String url, final File file, final OnPostDownloadComplete onComplete) {

        //prevent duplicates download
        if (currentDownloadPostOperations.contains(id))
            return;

        currentDownloadPostOperations.add(id);

        FireConstants.storageRef.child(url)
                .getFile(file)
                .addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                        if (currentDownloadPostOperations.contains(id))
                            currentDownloadPostOperations.remove(currentDownloadPostOperations);

                        if (task.isSuccessful()) {
                            if (onComplete != null)
                                onComplete.onComplete(file.getPath());
                        } else {
                            if (onComplete != null)
                                onComplete.onComplete(null);
                        }
                    }
                });

    }

    public interface OnPostDownloadComplete {
        void onComplete(String path);
    }

    public interface OnCompleteLikes {
        void OnComplete(int likes);
    }

    public static void savePostListJsonString( List<Posts> postsList ) {
        Gson gson = new Gson();
        String jsonstring = gson.toJson(postsList);

        SharedPreferencesManager.savePostListJsonString(jsonstring);
    }

    public static List<Posts> loadPostListJsonString() {
        String jsonstring = SharedPreferencesManager.getPostListJsonString();
        if( jsonstring == null || jsonstring.equals("") )
            return null;

        Gson gson = new Gson();
        TypeToken<List<Posts>> token = new TypeToken<List<Posts>>() {};

        List<Posts> postsList = gson.fromJson(jsonstring, token.getType());
        return postsList;
    }

}
