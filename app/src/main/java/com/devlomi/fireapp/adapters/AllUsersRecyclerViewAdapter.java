package com.devlomi.fireapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.devlomi.fireapp.R;
import com.devlomi.fireapp.activities.FriendProfileActivity;
import com.devlomi.fireapp.model.FriendSystem;
import com.devlomi.fireapp.model.UserInfo;
import com.devlomi.fireapp.utils.FireConstants;
import com.devlomi.fireapp.utils.FireManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class AllUsersRecyclerViewAdapter extends RecyclerView.Adapter<AllUsersRecyclerViewAdapter.MyViewHolder> {

    private Context mContext;
    private AllUsersRecyclerViewAdapter adapter;
    private List<UserInfo> userInfos;

    public AllUsersRecyclerViewAdapter(Context mContext, List<UserInfo> users) {
        this.mContext = mContext;
        this.userInfos = users;
        adapter = this;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.user_item, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        UserInfo userInfo = userInfos.get(position);
        if( userInfo != null ) {
            if( userInfo.getPhoto() != null ) {
                Glide.with(mContext)
                        .asBitmap()
                        .load(Uri.parse(userInfo.getPhoto()))
                        .into(holder.photoView);
            }

            FireConstants.presenceRef.child(userInfo.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if( dataSnapshot.getValue() != null ) {
                        Object online = dataSnapshot.getValue();
                        if( online.equals("Online") )
                            holder.badgeView.setBackgroundResource(R.drawable.online_badge);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            holder.nameText.setText(userInfo.getName());
            holder.contentText.setText(userInfo.getStatus());

            holder.userLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, FriendProfileActivity.class);
                    intent.putExtra("FRIEND", userInfo);
                    mContext.startActivity(intent);
                }
            });

            //
            holder.invite.setVisibility(View.GONE);
            FireConstants.friendRequestRef.child(FireManager.getUid()).child("sent").child(userInfo.getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            holder.invite.setVisibility(View.VISIBLE);
                            if( dataSnapshot.getValue() != null ) {
                                holder.invite.setText("Added");
                            } else {
                                holder.invite.setText("Add");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

//            if( isPendingUser(userInfo) )
//                holder.invite.setText("Added");
//            else
//                holder.invite.setText("Add");

            holder.invite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if( holder.invite.getText().toString().equals("Add") ) {

//                        requestInvite(userInfo);
                        //
                        holder.invite.setEnabled(false);
                        FireConstants.friendRequestRef.child(FireManager.getUid()).child("sent")
                                .child(userInfo.getUid()).setValue(UUID.randomUUID().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                FireConstants.friendRequestRef.child(userInfo.getUid()).child("received")
                                        .child(FireManager.getUid()).setValue(UUID.randomUUID().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        holder.invite.setText("Added");

                                        holder.invite.setEnabled(true);
                                    }
                                });
                            }
                        });
                    } else {
                        holder.invite.setEnabled(false);
                        FireConstants.friendRequestRef.child(FireManager.getUid()).child("sent")
                                .child(userInfo.getUid()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                FireConstants.friendRequestRef.child(userInfo.getUid()).child("received")
                                        .child(FireManager.getUid()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        holder.invite.setText("Add");

                                        holder.invite.setEnabled(true);
                                    }
                                });
                            }
                        });
                    }
                    //mContext.startActivity(IntentUtils.getShareAppIntent(mContext));
                }
            });
        }

    }

    private boolean isPendingUser( UserInfo info ) {
        boolean isPending = false;

        if( info.getFriendSystem() != null ) {
            HashMap<String,Object> invited = info.getFriendSystem().getInvited();
            if( invited != null ) {
                for(Map.Entry<String, Object> invitedEntry : invited.entrySet()) {
                    String uid = (String) invitedEntry.getValue();
                    if( FireManager.getUid().equals(uid) ) {
                        isPending = true;
                        break;
                    }
                }
            }

        }
        return isPending;
    }

    @Override
    public int getItemCount() {
        return userInfos.size();
    }

    private void requestInvite( UserInfo userInfo ) {

        setOtherInvited(userInfo);

        FireManager.getUserInfoByUid(FireManager.getUid(), new FireManager.userInfoListener() {
            @Override
            public void onFound(UserInfo mineInfo) {
                setMineInvite(mineInfo, userInfo);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onNotFound() {

            }
        });

    }

    private void setMineInvite(UserInfo mine, UserInfo other) {

        FriendSystem friendSystem;

        if( mine == null ) return;

        String puuid = UUID.randomUUID().toString();

        if( mine.getFriendSystem() == null ) {
            HashMap<String,String> child = new HashMap<>();
            child.put(puuid, other.getUid());

            HashMap<String,Object> parent = new HashMap<>();
            parent.put("invite", child);

            FireConstants.usersRef.child(mine.getUid()).child("friendSystem").setValue(parent);
        } else {
            friendSystem = mine.getFriendSystem();
            if( friendSystem.getInvite() == null ) {
                HashMap<String,String> child = new HashMap<>();
                puuid = UUID.randomUUID().toString();
                child.put(puuid, other.getUid());

                FireConstants.usersRef.child(mine.getUid()).child("friendSystem").child("invite").setValue(child);
            } else {
                HashMap<String,Object> invite = friendSystem.getInvite();
                puuid = UUID.randomUUID().toString();
                invite.put(puuid, other.getUid());
                FireConstants.usersRef.child(mine.getUid()).child("friendSystem").child("invite").setValue(invite);
            }
        }
    }

    private void setOtherInvited( UserInfo userInfo ) {
        FriendSystem friendSystem;

        if( userInfo == null ) return;

        String puuid = UUID.randomUUID().toString();

        if( userInfo.getFriendSystem() == null ) {
            HashMap<String,String> child = new HashMap<>();
            child.put(puuid, FireManager.getUid());

            HashMap<String,Object> parent = new HashMap<>();
            parent.put("invited", child);

            FireConstants.usersRef.child(userInfo.getUid()).child("friendSystem").setValue(parent);
        } else {
            friendSystem = userInfo.getFriendSystem();

            if( friendSystem.getInvited() == null ) {
                HashMap<String,String> child = new HashMap<>();
                puuid = UUID.randomUUID().toString();
                child.put(puuid, FireManager.getUid());

                HashMap<String,Object> parent = new HashMap<>();
                parent.put("invited", child);

                FireConstants.usersRef.child(userInfo.getUid()).child("friendSystem").setValue(parent);
            } else {
                HashMap<String,Object> invited = friendSystem.getInvited();
                puuid = UUID.randomUUID().toString();
                invited.put(puuid, FireManager.getUid());
                FireConstants.usersRef.child(userInfo.getUid()).child("friendSystem").child("invited").setValue(invited);
            }
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        CircleImageView photoView;
        TextView nameText;
        TextView contentText;
        Button invite;
        RelativeLayout userLayout;
        ImageView badgeView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            photoView = itemView.findViewById(R.id.user_photo_image);
            nameText = itemView.findViewById(R.id.user_fullname);
            contentText = itemView.findViewById(R.id.user_content_text);
            invite = itemView.findViewById(R.id.user_invite_button);
            userLayout = itemView.findViewById(R.id.user_layout);
            badgeView = itemView.findViewById(R.id.user_badge);
        }
    }
}
