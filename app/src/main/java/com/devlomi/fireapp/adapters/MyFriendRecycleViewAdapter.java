package com.devlomi.fireapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.devlomi.fireapp.R;
import com.devlomi.fireapp.activities.CallingActivity;
import com.devlomi.fireapp.activities.ChatActivity;
import com.devlomi.fireapp.activities.FriendProfileActivity;
import com.devlomi.fireapp.activities.NewChatActivity;
import com.devlomi.fireapp.activities.main.MainActivity;
import com.devlomi.fireapp.fragments.FriendsFragment;
import com.devlomi.fireapp.model.UserInfo;
import com.devlomi.fireapp.model.constants.FireCallType;
import com.devlomi.fireapp.model.realms.User;
import com.devlomi.fireapp.utils.FireConstants;
import com.devlomi.fireapp.utils.FireManager;
import com.devlomi.fireapp.utils.IntentUtils;
import com.devlomi.fireapp.utils.RealmHelper;
import com.devlomi.fireapp.utils.SharedPreferencesManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import io.realm.RealmResults;

public class MyFriendRecycleViewAdapter extends RecyclerView.Adapter<MyFriendRecycleViewAdapter.MyViewHolder> {

    private List<UserInfo> mFriendList;
    private Context mContext;
    private MyFriendRecycleViewAdapter adapter;

    public MyFriendRecycleViewAdapter(List<UserInfo> mFriendList, Context mContext) {
        this.mFriendList = mFriendList;
        this.mContext = mContext;
        adapter = this;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.myfriend_item, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        UserInfo user = mFriendList.get(position);
        if(user != null) {
            if( user.getPhoto() != null ) {
                Glide.with(mContext)
                        .asBitmap()
                        .load(Uri.parse(user.getPhoto()))
                        .into(holder.friendPhoto);
            }

            holder.friendName.setText(user.getName());
            holder.friendStatus.setText(user.getStatus());

            holder.friendDeleteImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    RealmHelper.getInstance().deleteUser(mFriendList, user);
//                    FireManager.saveFriendUsers(RealmHelper.getInstance().getListOfUsers());
                    mFriendList.remove(user);
                    saveFriendsListJsonString();
                    adapter.notifyDataSetChanged();

                    deleteFriendInFirebase(user);
                }
            });

            holder.friendChatImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, ChatActivity.class);
                    intent.putExtra(IntentUtils.UID, user.getUid());
                    mContext.startActivity(intent);
                }
            });

            holder.friendCallImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent callScreen = new Intent(mContext, CallingActivity.class);
                    callScreen.putExtra(IntentUtils.PHONE_CALL_TYPE, FireCallType.OUTGOING);
                    callScreen.putExtra(IntentUtils.ISVIDEO, false);
                    callScreen.putExtra(IntentUtils.UID, user.getUid());
                    mContext.startActivity(callScreen);
                }
            });

            holder.itemLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FireManager.getUserInfoByUid(user.getUid(), new FireManager.userInfoListener() {
                        @Override
                        public void onFound(UserInfo userInfo) {
                            Intent intent = new Intent(mContext, FriendProfileActivity.class);
                            intent.putExtra("FRIEND", userInfo);
                            mContext.startActivity(intent);
                        }

                        @Override
                        public void onNotFound() {

                        }
                    });
                }
            });
        }
    }

    private void saveFriendsListJsonString() {
        Gson gson = new Gson();
        String jsonstring = gson.toJson(mFriendList);

        SharedPreferencesManager.saveFriendsListJsonString(jsonstring);
    }

    private void deleteFriendInFirebase( UserInfo user ) {
        FireConstants.friendsRef.child(FireManager.getUid()).child(user.getUid()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if( task.isComplete() ) {
                    FireConstants.friendsRef.child(user.getUid()).child(FireManager.getUid()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                        }
                    });
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mFriendList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        CircleImageView friendPhoto;
        TextView friendName;
        TextView friendStatus;
        ImageView friendDeleteImage;
        ImageView friendChatImage;
        ImageView friendCallImage;
        RelativeLayout itemLayout;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            friendPhoto = itemView.findViewById(R.id.friend_photo_image);
            friendName = itemView.findViewById(R.id.friend_fullname);
            friendStatus = itemView.findViewById(R.id.friend_status);
            friendDeleteImage = itemView.findViewById(R.id.friend_delete_image);
            friendChatImage = itemView.findViewById(R.id.friend_chat_image);
            friendCallImage = itemView.findViewById(R.id.friend_call_image);
            itemLayout = itemView.findViewById(R.id.friend_item_layout);
        }
    }
}
