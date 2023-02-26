package com.devlomi.fireapp.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.cjt2325.cameralibrary.ResultCodes;
import com.devlomi.fireapp.R;
import com.devlomi.fireapp.activities.CameraActivity;
import com.devlomi.fireapp.activities.MyStatusActivity;
import com.devlomi.fireapp.activities.NewPostActivity;
import com.devlomi.fireapp.activities.TextStatusActivity;
import com.devlomi.fireapp.activities.ViewStatusActivity;
import com.devlomi.fireapp.adapters.MyPostRecyclerViewAdapter;
import com.devlomi.fireapp.adapters.PostsAdapter;
import com.devlomi.fireapp.adapters.UserStatusRecyclerViewAdapter;
import com.devlomi.fireapp.interfaces.StatusFragmentCallbacks;
import com.devlomi.fireapp.model.Post;
import com.devlomi.fireapp.model.Posts;
import com.devlomi.fireapp.model.TextStatus;
import com.devlomi.fireapp.model.UserInfo;
import com.devlomi.fireapp.model.constants.MessageType;
import com.devlomi.fireapp.model.constants.StatusType;
import com.devlomi.fireapp.model.realms.Status;
import com.devlomi.fireapp.model.realms.User;
import com.devlomi.fireapp.model.realms.UserStatuses;
import com.devlomi.fireapp.utils.BitmapUtils;
import com.devlomi.fireapp.utils.DirManager;
import com.devlomi.fireapp.utils.FileUtils;
import com.devlomi.fireapp.utils.FireConstants;
import com.devlomi.fireapp.utils.FireManager;
import com.devlomi.fireapp.utils.ImageEditorRequest;
import com.devlomi.fireapp.utils.IntentUtils;
import com.devlomi.fireapp.utils.KeyboardHelper;
import com.devlomi.fireapp.utils.MyApp;
import com.devlomi.fireapp.utils.NetworkHelper;
import com.devlomi.fireapp.utils.PostManager;
import com.devlomi.fireapp.utils.RealmHelper;
import com.devlomi.fireapp.utils.SharedPreferencesManager;
import com.devlomi.fireapp.utils.StatusManager;
import com.devlomi.fireapp.utils.TimeHelper;
import com.devlomi.fireapp.utils.Util;
import com.devlomi.fireapp.views.SharePostView;
import com.devlomi.fireapp.views.TextViewWithShapeBackground;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.fxn.pix.Options;
import com.fxn.pix.Pix;
import com.fxn.utility.ImageQuality;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zhihu.matisse.Matisse;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

import static android.app.Activity.RESULT_OK;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class PostFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener {

    //max duration for status video time (30sec)
    public static final int MAX_STATUS_VIDEO_TIME = 30;

    List<Posts> postsList = new ArrayList<>();
//    MyPostRecyclerViewAdapter adapter;
    PostsAdapter adapter;
    View fragment;
    LinearLayout coordinatorLayout;
    SwipeRefreshLayout swipeRefreshLayout;
    RecyclerView postsRecyclerView;
    ProgressDialog progressDialog;
    public SharePostView sharePostView;
    private ImageView backgroundImage;
    private CircleImageView plusCircle;
    private LinearLayout postLayout;

    private OnListFragmentInteractionListener mListener;
    ArrayList<String> returnValue = new ArrayList<>();

    private ImageView myBackground;
    private TextViewWithShapeBackground myTextStatusBackground;
    private CircleImageView myPhoto;
    private TextView myStoryTitle;
    private RelativeLayout myStatusRelayout;

    // Stories
    private UserStatuses myStatuses;
    private UserStatusRecyclerViewAdapter statusAdapter;
    private RecyclerView statusRecyclerView;
    private RealmResults<UserStatuses> statusesList;

    StatusFragmentCallbacks callbacks;

    public PostFragment() {
    }

    @Override
    public boolean showAds() {
        return false;
    }

    @Override
    public void onRefresh() {
        //PostManager.getPostsListItems(postsList,adapter,swipeRefreshLayout);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragment = inflater.inflate(R.layout.fragment_post_list, container, false);

        postLayout = fragment.findViewById(R.id.post_linear);
        coordinatorLayout = fragment.findViewById(R.id.posts_coordinator);
        swipeRefreshLayout = fragment.findViewById(R.id.posts_swipe_container);
        backgroundImage = fragment.findViewById(R.id.status_back_image);
        plusCircle = fragment.findViewById(R.id.plus_circle);

        initMyStatusView();

        String uri = SharedPreferencesManager.getMyPhoto();
        Glide.with(getActivity())
                .asBitmap()
                .load(uri)
                .into(backgroundImage);

        plusCircle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showCreateStoryDialog(getActivity());
            }
        });

        statusesList = RealmHelper.getInstance().getAllStatuses();

        // My Statuses
        initMyStatuses();

        // statuses
        statusRecyclerView = fragment.findViewById(R.id.story_recyclerview);
        initStatusAdapter();

        progressDialog = new ProgressDialog(this.getContext());

        // post
        postsRecyclerView = fragment.findViewById(R.id.posts_recyclerview);
        postsRecyclerView.setHasFixedSize(true);
        postsRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));

        adapter = new PostsAdapter(postsList, this);
        postsRecyclerView.setAdapter(adapter);
        fetchPost();

        sharePostView = fragment.findViewById(R.id.sharePostView);

        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                //PostManager.getPostsListItems(postsList,adapter,swipeRefreshLayout);
            }
        });
        return fragment;
    }

    private void showCreateStoryDialog(Activity activity) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.dialog_layout);

        ImageView textStory = dialog.findViewById(R.id.textstatus_image);
        textStory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callbacks != null)
                    callbacks.openTextStatus();
                dialog.dismiss();
            }
        });

        ImageView cameraStory = dialog.findViewById(R.id.medistatus_image);
        cameraStory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callbacks != null)
                    callbacks.openCamera();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void initStatusAdapter() {
        statusAdapter = new UserStatusRecyclerViewAdapter(getContext(), statusesList);
        statusRecyclerView.setAdapter(statusAdapter);
        statusRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(),
                RecyclerView.HORIZONTAL, false));
        statusAdapter.notifyDataSetChanged();
    }

    private void initMyStatusView() {

        myBackground = fragment.findViewById(R.id.back_image);
        myTextStatusBackground = fragment.findViewById(R.id.text_status);
        myPhoto = fragment.findViewById(R.id.user_circle);
        myStoryTitle = fragment.findViewById(R.id.name_text);
        myStatusRelayout = fragment.findViewById(R.id.status_relayout);

        myStatusRelayout.setVisibility(View.GONE);
        myStatusRelayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myStatuses != null && !myStatuses.getFilteredStatuses().isEmpty()) {
                    Intent intent = new Intent(getActivity(), ViewStatusActivity.class);
                    intent.putExtra(IntentUtils.UID, myStatuses.getUserId());
                    startActivity(intent);
                }
            }
        });
    }

    private void initMyStatuses() {

        myStatuses = RealmHelper.getInstance().getUserStatuses(FireManager.getUid());
    }

    private void setMyStatus() {
        if (myStatuses == null)
            initMyStatuses();

        if( myStatuses != null
                && !myStatuses.getFilteredStatuses().isEmpty() ) {

            myStatusRelayout.setVisibility(View.VISIBLE);

            Status lastStatus = myStatuses.getStatuses().last();
            String statusTime = TimeHelper.getStatusTime(lastStatus.getTimestamp());

            if (lastStatus.getType() == StatusType.IMAGE || lastStatus.getType() == StatusType.VIDEO) {
                myTextStatusBackground.setVisibility(View.GONE);
                myBackground.setVisibility(View.VISIBLE);

                Glide.with(getActivity()).asBitmap().load(BitmapUtils.encodeImageAsBytes(lastStatus.getThumbImg())).into(myBackground);
            } else if( lastStatus.getType() == StatusType.TEXT ) {
                myTextStatusBackground.setVisibility(View.VISIBLE);
                myBackground.setVisibility(View.GONE);
                TextStatus textStatus = lastStatus.getTextStatus();
                myTextStatusBackground.setText(textStatus.getText());
                myTextStatusBackground.setShapeColor(Color.parseColor(textStatus.getBackgroundColor()));
            }
            Glide.with(getActivity()).asBitmap().load(BitmapUtils.encodeImageAsBytes(SharedPreferencesManager.getThumbImg())).into(myPhoto);

            myStoryTitle.setText("Your story");
        } else {
            myStatusRelayout.setVisibility(View.GONE);
        }

    }

    @Override
    public void onQueryTextChange(String newText) {
        super.onQueryTextChange(newText);

//        if( statusAdapter != null )
//            statusAdapter.filter(newText);
    }

    @Override
    public void onSearchClose() {
        super.onSearchClose();

//        statusAdapter = new UserStatusRecyclerViewAdapter(getContext(), statusesList);
//        if( statusRecyclerView != null )
//            statusRecyclerView.setAdapter(statusAdapter);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        isSelf = true;
        callbacks = (StatusFragmentCallbacks) context;

        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
//        if( adapter != null )
//            adapter.startListening();

    }

    private boolean isSelf = false;
    @Override
    public void onResume() {
        super.onResume();

        setMyStatus();

        if( isSelf ) {
            isSelf = false;
        } else {
            if( adapter != null ) {
                //refresh();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();

//        if( adapter != null )
//            adapter.stopListening();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void refresh() {
        //PostManager.getPostsListItems(postsList,adapter,swipeRefreshLayout);
    }

    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(Post item);
    }

    /* Manage Stories */
    public void onCameraActivityResult(int resultCode, Intent data) {
        if (resultCode != ResultCodes.CAMERA_ERROR_STATE) {
            if (resultCode == ResultCodes.IMAGE_CAPTURE_SUCCESS) {
                String path = data.getStringExtra(IntentUtils.EXTRA_PATH_RESULT);
                ImageEditorRequest.open(getActivity(), path);

            } else if (resultCode == ResultCodes.VIDEO_RECORD_SUCCESS) {
                String path = data.getStringExtra(IntentUtils.EXTRA_PATH_RESULT);
                uploadVideoStatus(path);
            } else if (resultCode == ResultCodes.PICK_IMAGE_FROM_CAMERA) {
                List<String> mPaths = Matisse.obtainPathResult(data);
                for (String mPath : mPaths) {
                    if (!FileUtils.isFileExists(mPath)) {
                        Toast.makeText(getActivity(), MyApp.context().getResources().getString(R.string.image_video_not_found), Toast.LENGTH_SHORT).show();
                        return;
                    }

                }


                //Check if it's a video
                if (FileUtils.isPickedVideo(mPaths.get(0))) {

                    //check if video is longer than 30sec
                    long mediaLengthInMillis = Util.getMediaLengthInMillis(getContext(), mPaths.get(0));
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(mediaLengthInMillis);
                    if (seconds <= MAX_STATUS_VIDEO_TIME) {
                        for (String mPath : mPaths) {
                            uploadVideoStatus(mPath);
                        }
                    } else {
                        Toast.makeText(getActivity(), MyApp.context().getResources().getString(R.string.video_length_is_too_long), Toast.LENGTH_SHORT).show();
                    }


                } else {
                    //if it's only one image open image editor
                    if (mPaths.size() == 1)
                        ImageEditorRequest.open(getActivity(), mPaths.get(0));
                    else
                        for (String path : mPaths) {
                            uploadImageStatus(path);
                        }
                }
            }
        }
    }

    private void uploadVideoStatus(String path) {
        if (!NetworkHelper.isConnected(MyApp.context())) {
            Toast.makeText(getActivity(), MyApp.context().getResources().getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getActivity(), R.string.uploading_status, Toast.LENGTH_SHORT).show();

        StatusManager.uploadStatus(path, StatusType.VIDEO, true, new StatusManager.UploadStatusCallback() {
            @Override
            public void onComplete(boolean isSuccessful) {
                if (isSuccessful) {
                    setMyStatus();
                    Toast.makeText(getActivity(), MyApp.context().getResources().getString(R.string.status_uploaded), Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(getActivity(), MyApp.context().getResources().getString(R.string.error_uploading_status), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadImageStatus(String path) {
        if (!NetworkHelper.isConnected(MyApp.context())) {
            Toast.makeText(MyApp.context(), MyApp.context().getResources().getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show();
            return;
        }


        Toast.makeText(MyApp.context(), MyApp.context().getResources().getString(R.string.uploading_status), Toast.LENGTH_SHORT).show();
        String mPath = compressImage(path);


        StatusManager.uploadStatus(mPath, StatusType.IMAGE, false, new StatusManager.UploadStatusCallback() {
            @Override
            public void onComplete(boolean isSuccessful) {
                if (isSuccessful) {
                    setMyStatus();
                    Toast.makeText(getActivity(), MyApp.context().getResources().getString(R.string.status_uploaded), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), MyApp.context().getResources().getString(R.string.error_uploading_status), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    //compress image when user chooses an image from gallery
    private String compressImage(String imagePath) {
        //generate file in sent images folder
        File file = DirManager.generateFile(MessageType.SENT_IMAGE);
        //compress image and copy it to the given file
        BitmapUtils.compressImage(imagePath, file);

        return file.getPath();
    }

    public void onImageEditSuccess(@NotNull String imagePath) {
        uploadImageStatus(imagePath);
    }

    public void onTextStatusResult(TextStatus textStatus) {
        if (!NetworkHelper.isConnected(MyApp.context())) {
            Toast.makeText(MyApp.context(), R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MyApp.context(), R.string.uploading_status, Toast.LENGTH_SHORT).show();
            StatusManager.uploadTextStatus(textStatus, new StatusManager.UploadStatusCallback() {
                @Override
                public void onComplete(boolean isSuccessful) {
                    if (isSuccessful)
                        setMyStatus();
                }
            });
        }

    }

    //fetch status when user swipes to this page
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser && callbacks != null)
            callbacks.fetchStatuses();
    }



    private void fetchPost() {
        FireConstants.postsRef.orderByKey().limitToLast(50).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if( dataSnapshot.getValue() != null ) {
                    Posts posts = dataSnapshot.getValue(Posts.class);

                    if( postsList != null && postsList.size() > 0 ) {

                        boolean isContain = false;
                        for( int i=0; i<postsList.size(); i++ ) {
                            if(postsList.get(i).getPostId().equals(posts.getPostId())) {
                                isContain = true;
                                break;
                            }
                        }

                        if( !isContain ) {
                            postsList.add(0, posts);
                            adapter.notifyDataSetChanged();

                            // save
//                            PostManager.savePostListJsonString(postsList);
                        }
                    } else {
                        postsList.add(0, posts);
                        adapter.notifyDataSetChanged();

                        // save
//                        PostManager.savePostListJsonString(postsList);
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if( requestCode == 2222 ) { // comment activity
            if( resultCode == RESULT_OK ) {
                Posts post = (Posts)data.getSerializableExtra("postt");
                for( int i=0; i<postsList.size(); i++ ) {
                    if( postsList.get(i).getPostId().equals(post.getPostId()) ) {
                        postsList.get(i).setPostComments(post.getPostComments());
                        adapter.notifyItemChanged(i);
                        break;
                    }
                }
            }
        }
    }

    public void setNewPost( @Nullable Intent data ) { // new post
        if( data == null ) return;

        Posts post = (Posts)data.getSerializableExtra("newpost");
        postsList.remove(0);
        postsList.add(0,post);
        //adapter.notifyDataSetChanged();
    }
}
