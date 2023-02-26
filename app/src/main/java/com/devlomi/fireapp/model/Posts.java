package com.devlomi.fireapp.model;

import java.io.Serializable;
import java.util.HashMap;

public class Posts implements Serializable {
    private String postId;
    private String postText;
    private boolean postIsShared;
    private HashMap<String,Object> postMedias;
    private HashMap<String,Object> postComments;
    private int postShares;
    private long postTime;
    private int postType;
    private String postUid;
    private String postLocation;
    private String postName;
    private String postPhotoUrl;
    private boolean postIsLiked;
    private int postLikes;

    public Posts() {

    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getPostText() {
        return postText;
    }

    public void setPostText(String postText) {
        this.postText = postText;
    }

    public boolean getPostIsShared() {
        return postIsShared;
    }

    public void setPostIsShared(boolean postIsShared) {
        this.postIsShared = postIsShared;
    }

    public HashMap<String, Object> getPostMedias() {
        return postMedias;
    }

    public void setPostMedias(HashMap<String, Object> postMedias) {
        this.postMedias = postMedias;
    }

    public int getPostLikes() {
        return postLikes;
    }

    public boolean isPostIsLiked() {
        return postIsLiked;
    }

    public void setPostIsLiked(boolean postIsLiked) {
        this.postIsLiked = postIsLiked;
    }

    public void setPostLikes(int postLikes) {
        this.postLikes = postLikes;
    }

    public HashMap<String, Object> getPostComments() {
        return postComments;
    }

    public void setPostComments(HashMap<String, Object> postComments) {
        this.postComments = postComments;
    }

    public int getPostShares() {
        return postShares;
    }

    public void setPostShares(int postShares) {
        this.postShares = postShares;
    }

    public long getPostTime() {
        return postTime;
    }

    public void setPostTime(long postTime) {
        this.postTime = postTime;
    }

    public int getPostType() {
        return postType;
    }

    public void setPostType(int postType) {
        this.postType = postType;
    }

    public String getPostUid() {
        return postUid;
    }

    public void setPostUid(String postUid) {
        this.postUid = postUid;
    }

    public String getPostLocation() {
        return postLocation;
    }

    public void setPostLocation(String postLocation) {
        this.postLocation = postLocation;
    }

    public String getPostName() {
        return postName;
    }

    public void setPostName(String postName) {
        this.postName = postName;
    }

    public String getPostPhotoUrl() {
        return postPhotoUrl;
    }

    public void setPostPhotoUrl(String postPhotoUrl) {
        this.postPhotoUrl = postPhotoUrl;
    }
}
