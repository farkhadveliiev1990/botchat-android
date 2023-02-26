package com.devlomi.fireapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.media.Image;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.devlomi.fireapp.R;
import com.devlomi.fireapp.model.Post;
import com.devlomi.fireapp.model.Posts;
import com.devlomi.fireapp.model.constants.StatusType;
import com.devlomi.fireapp.utils.BitmapUtils;
import com.devlomi.fireapp.utils.DirManager;
import com.devlomi.fireapp.utils.FileUtils;
import com.devlomi.fireapp.utils.FireManager;
import com.devlomi.fireapp.utils.MediaDownloadManager;
import com.devlomi.fireapp.utils.PostManager;
import com.devlomi.fireapp.utils.PostMedia;
import com.devlomi.fireapp.utils.PostUtil;
import com.devlomi.fireapp.utils.Util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class EnlargyActivity extends AppCompatActivity {

    private int currentApiVersion;
    private boolean isDisableUI;
    private boolean isLikeState;
    private MediaController mediaController;
    MediaPlayer.OnPreparedListener onPreparedListener;
    MediaPlayer.OnErrorListener onErrorListener;

    private Posts mPost;

    private ImageView enlarge_image;
    private VideoView enlarge_video;
    private ProgressBar progressBar;

    private PostManager.OnPostDownloadComplete onPostDownloadComplete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enlargy);

        // full screen
        currentApiVersion = Build.VERSION.SDK_INT;
        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        if (currentApiVersion >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(flags);
            final View decorView = getWindow().getDecorView();
            decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        decorView.setSystemUiVisibility(flags);
                    }
                }
            });
        }

        //onVideo prepared listener
        onPreparedListener = new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
//                enlarge_image.setVisibility(View.GONE);
//                progressBar.setVisibility(View.GONE);
//                enlarge_video.start();
            }
        };

        onErrorListener = new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                Toast.makeText(EnlargyActivity.this, R.string.error_playing_this, Toast.LENGTH_SHORT).show();
                return true;
            }
        };

        //
//        mPost = (Post) getIntent().getSerializableExtra("EDITPOST");
//        PostMedia media = (PostMedia) getIntent().getSerializableExtra("POSTMEDIA");
        mPost = PostUtil.post;
        PostMedia media = PostUtil.postMedia;

        if( mPost == null || media == null )
            return;

        // show image
        enlarge_image = findViewById(R.id.enlarge_image);
        enlarge_video = findViewById(R.id.enlarg_video);
        progressBar = findViewById(R.id.video_progress);

        //load thumb blurred image while loading original image or video
        Glide.with(this)
                .asBitmap()
                .load(BitmapUtils.simpleBlur(this, BitmapUtils.encodeImage(media.getThumbImg())))
                .into(enlarge_image);

        if( media.getType() == 2 ) {
            enlarge_video.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            enlarge_image.setVisibility(View.GONE);

            mediaController = new MediaController(this);
            mediaController.setAnchorView(enlarge_video);

            loadVideo(media);

        } else {
            enlarge_video.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            enlarge_image.setVisibility(View.VISIBLE);

            loadImage(media);
        }

        enlarge_video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
            }
        });
        enlarge_video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                progressBar.setVisibility(View.GONE);
            }
        });

        // image
        enlarge_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDisableUI = !isDisableUI;
                // if touch is true, ui disable
//                setDisableUI(isDisableUI);

            }
        });

        // UI Layout
        ImageView click_like_image = findViewById(R.id.click_like_image);
        TextView show_like_text = findViewById(R.id.show_like_text);
        TextView click_like_text = findViewById(R.id.click_like_text);
        TextView show_comment_text = findViewById(R.id.show_comment_text);

        // show like
        show_like_text.setText(Integer.toString(mPost.getPostLikes()));

        // show comment
        int comments = PostManager.getCommentCount(mPost.getPostComments());
        show_comment_text.setText(String.format("%d Comments", comments));

        // click like
        int originallikeColor = click_like_text.getCurrentTextColor();
        ColorFilter originalFilter = click_like_image.getColorFilter();

        LinearLayout click_like = findViewById(R.id.click_like);
        click_like.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                isLikeState = !isLikeState;
//
//                PostManager.updateLike(mPost);
//                int count = PostManager.getLikeCount(mPost.getPostLikes());
//                show_like_text.setText(String.format("%d", count));
            }
        });

        // click comment
        LinearLayout click_comment = findViewById(R.id.click_comment);
        click_comment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(EnlargyActivity.this, "Comment", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(EnlargyActivity.this, CommentsActivity.class);
                intent.putExtra("POST", mPost);
                startActivity(intent);
            }
        });

        // click share
        LinearLayout click_share = findViewById(R.id.click_share);
        click_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(EnlargyActivity.this, "Share", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void loadImage( PostMedia media ) {
        String url;
        if( media.getUserId().equals(FireManager.getUid()) )
            url = media.getLocalPath() == null ? media.getContent() : media.getLocalPath();
        else
            url = media.getContent();

        Glide.with(this)
                .asBitmap()
                .load(url)
                .into(enlarge_image);
    }

    private void loadVideo( PostMedia media ) {
        String localPath;

        if( media.getUserId().equals(FireManager.getUid()) )
            localPath = media.getLocalPath();
        else
            localPath = DirManager.getReceivedPostFile(media.getLocalPath()).getAbsolutePath();

        if( localPath == null ) {
            downloadVideo(media);
        } else {
            if (FileUtils.isFileExists(localPath)) {
                playVideo(localPath);
            } else {
                downloadVideo(media);
            }
        }
    }

    private void downloadVideo( PostMedia media ) {
        initStatusDownloadCompleteCallback();
        File postFile = DirManager.getReceivedPostFile(media.getLocalPath());
        PostManager.downloadVideoPost(media.getMediaId(), media.getContent(), postFile, onPostDownloadComplete);
    }

    private void playVideo( String path ) {
        progressBar.setVisibility(View.GONE);
        enlarge_video.setMediaController(mediaController);
        enlarge_video.requestFocus();
        enlarge_video.setVideoURI(Uri.parse(path));
        enlarge_video.setOnPreparedListener(onPreparedListener);
        enlarge_video.setOnErrorListener(onErrorListener);
        enlarge_video.start();
    }

    private void initStatusDownloadCompleteCallback() {
        if (onPostDownloadComplete == null) {
            onPostDownloadComplete = new PostManager.OnPostDownloadComplete() {
                @Override
                public void onComplete(String path) {
                    if (path != null) {
                        playVideo(path);
                    }
                }
            };
        }
    }

    @Override
    protected void onDestroy() {
        enlarge_video.stopPlayback();
        enlarge_video.setOnPreparedListener(null);
        enlarge_video.setOnErrorListener(null);

        super.onDestroy();
    }

    // fullscreen
    @SuppressLint("NewApi")
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (currentApiVersion >= Build.VERSION_CODES.KITKAT && hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        return super.onTouchEvent(event);
    }

}
