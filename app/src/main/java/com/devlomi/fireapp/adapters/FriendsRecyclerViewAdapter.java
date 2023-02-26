package com.devlomi.fireapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.devlomi.fireapp.model.realms.User;
import com.devlomi.fireapp.utils.FireConstants;
import com.devlomi.fireapp.utils.FireManager;
import com.devlomi.fireapp.utils.RealmHelper;
import com.devlomi.fireapp.utils.SharedPreferencesManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.devlomi.fireapp.utils.ContactUtils.contactExists;
import static com.devlomi.fireapp.utils.ContactUtils.queryForNameByNumber;

public class FriendsRecyclerViewAdapter extends RecyclerView.Adapter<FriendsRecyclerViewAdapter.FriendsViewHolder> {

    private Context mContext;
    private FriendsRecyclerViewAdapter adapter;
    private List<UserInfo> invitedUsers;

    public FriendsRecyclerViewAdapter(Context mContext, List<UserInfo> users) {
        this.mContext = mContext;
        this.invitedUsers = users;
        adapter = this;
    }

    @NonNull
    @Override
    public FriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.friend_item, parent, false);

        return new FriendsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendsViewHolder holder, int position) {

        UserInfo userInfo = invitedUsers.get(position);

        if( userInfo != null ) {
            if(userInfo.getPhoto() != null) {
                Glide.with(mContext)
                        .asBitmap()
                        .load(Uri.parse(userInfo.getPhoto()))
                        .into(holder.friendPhotoView);
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

            holder.friendNameView.setText(userInfo.getName());
            holder.friendContenview.setText(userInfo.getStatus());

            holder.itemlLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, FriendProfileActivity.class);
                    intent.putExtra("FRIEND", userInfo);
                    mContext.startActivity(intent);
                }
            });

            holder.acceptButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doAccept(userInfo);
                    //doAcceptButton(userInfo, position);
                }
            });
        }

    }

    private void doAccept( UserInfo userInfo ) {

        // remove values.
        FireConstants.friendRequestRef.child(FireManager.getUid()).child("received")
                .child(userInfo.getUid()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if( task.isComplete() ) {
                    FireConstants.friendRequestRef.child(userInfo.getUid()).child("sent")
                            .child(FireManager.getUid()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            // register to friends
                            FireConstants.usersRef.child(userInfo.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if( dataSnapshot.getValue() != null ) {
                                        User user = dataSnapshot.getValue(User.class);

                                        RealmHelper.getInstance().saveObjectToRealm(user);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                            // remove user in array
                            invitedUsers.remove(userInfo);
                            adapter.notifyDataSetChanged();

                            //
                            saveInvitedListJsonString();

                            // save to firebase.
                            saveFriendsToFirebase(userInfo);
                        }
                    });
                }
            }
        });
    }

    private void saveInvitedListJsonString() {
        Gson gson = new Gson();
        String jsonstring = gson.toJson(invitedUsers);

        SharedPreferencesManager.saveAddedMeListJsonString(jsonstring);
    }

    private void saveFriendsToFirebase(UserInfo userInfo) {
        FireConstants.friendsRef.child(FireManager.getUid()).child(userInfo.getUid()).setValue(userInfo.getPhone())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if( task.isComplete() ) {
                            FireConstants.friendsRef.child(userInfo.getUid()).child(FireManager.getUid()).setValue(FireManager.getPhoneNumber())
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {

                                        }
                                    });
                        }
                    }
                });
    }

    private void doAcceptButton(UserInfo userInfo, int position) {

        setOtherFriend(userInfo);

        FireManager.getUserInfoByUid(FireManager.getUid(), new FireManager.userInfoListener() {
            @Override
            public void onFound(UserInfo mine) {
                setMineFriends(mine, userInfo);
                invitedUsers.remove(invitedUsers.get(position));
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onNotFound() {

            }
        });
    }

    private void setMineFriends(UserInfo mine, UserInfo info) {
        FriendSystem friendSystem;

        UserInfo userInfo = mine;
        UserInfo other = info;
        if( userInfo == null || other == null ) return;

        String puuid = UUID.randomUUID().toString();

        if( userInfo.getFriendSystem() == null ) {
            HashMap<String,String> child = new HashMap<>();
            child.put(puuid, other.getUid());

            HashMap<String,Object> parent = new HashMap<>();
            parent.put("friends", child);

            FireConstants.usersRef.child(userInfo.getUid()).child("friendSystem").setValue(parent);
        } else {
            friendSystem = userInfo.getFriendSystem();

            deleteInvited(friendSystem, userInfo, other.getUid());

            if( friendSystem.getFriends() == null ) {
                HashMap<String,String> child = new HashMap<>();
                puuid = UUID.randomUUID().toString();
                child.put(puuid, other.getUid());

                HashMap<String,Object> parent = new HashMap<>();
                parent.put("friends", child);

                FireConstants.usersRef.child(userInfo.getUid()).child("friendSystem").setValue(parent);
            } else {
                HashMap<String,Object> friend = friendSystem.getFriends();
                puuid = UUID.randomUUID().toString();
                friend.put(puuid, other.getUid());
                FireConstants.usersRef.child(userInfo.getUid()).child("friendSystem").child("friends").setValue(friend);
            }
        }

        String phoneNumber = info.getPhone();
        if (phoneNumber != null && !phoneNumber.equals(FireManager.getPhoneNumber())) {
            //check if contact has installed this app
            FireManager.isHasFireApp(phoneNumber, new FireManager.IsHasAppListener() {
                @Override
                public void onFound(User user) {

                    //if user installed this app
                    //get user info
                    //get current  user from realm if exists
                    User storedUser = RealmHelper.getInstance().getUser(user.getUid());
                    //save name by get the contact from phone book
                    String name = queryForNameByNumber(mContext, phoneNumber);
                    boolean isStored = contactExists(mContext, phoneNumber);
                    //if user is not exists in realm save it
                    if (storedUser == null) {
                        //save user name
                        user.setUserName(name);
                        user.setStoredInContacts(isStored);
                        //save user with his info(photo,number,uid etc..)
                        RealmHelper.getInstance().saveObjectToRealm(user);
                    } else {
                        //if user is exists in database update his info if they are not same
                        RealmHelper.getInstance().updateUserInfo(user, storedUser, name, isStored);
                    }

                }

                //if user does not installed this app
                @Override
                public void onNotFound() {

                }
            });

        }

    }

    private void setOtherFriend( UserInfo userInfo ) {

        if( userInfo == null ) return;

        FriendSystem friendSystem;
        String puuid = UUID.randomUUID().toString();

        if( userInfo.getFriendSystem() == null ) {
            HashMap<String,String> child = new HashMap<>();
            child.put(puuid, FireManager.getUid());

            FireConstants.usersRef.child(userInfo.getUid()).child("friendSystem").child("friends").setValue(child);
        } else {
            friendSystem = userInfo.getFriendSystem();

            deleteInvite(friendSystem, userInfo, FireManager.getUid());

            if( friendSystem.getFriends() == null ) {
                HashMap<String,String> child = new HashMap<>();
                puuid = UUID.randomUUID().toString();
                child.put(puuid, FireManager.getUid());

                FireConstants.usersRef.child(userInfo.getUid()).child("friendSystem").child("friends").setValue(child);
            } else {
                HashMap<String,Object> friends = friendSystem.getFriends();
                puuid = UUID.randomUUID().toString();
                friends.put(puuid, FireManager.getUid());
                FireConstants.usersRef.child(userInfo.getUid()).child("friendSystem").child("friends").setValue(friends);
            }

        }
    }

    private void deleteInvite( FriendSystem friendSystem, UserInfo user, String uid ) {
        if( friendSystem.getInvite() != null ) {
            HashMap<String,Object> invites = new HashMap<>();
            for( Map.Entry<String, Object> entry : friendSystem.getInvite().entrySet() ) {
                String key = entry.getKey();
                String value = (String) entry.getValue();
                if( !value.equals(uid) )
                    invites.put(key, value);
            }

            FireConstants.usersRef.child(user.getUid()).child("friendSystem").child("invite").setValue(invites);
        }
    }

    private void deleteInvited( FriendSystem friendSystem, UserInfo user, String uid ) {
        if( friendSystem.getInvited() != null ) {
            HashMap<String,Object> invites = new HashMap<>();
            for( Map.Entry<String, Object> entry : friendSystem.getInvited().entrySet() ) {
                String key = entry.getKey();
                String value = (String) entry.getValue();
                if( !value.equals(uid) )
                    invites.put(key, value);
            }

            FireConstants.usersRef.child(user.getUid()).child("friendSystem").child("invited").setValue(invites);
        }
    }

    @Override
    public int getItemCount() {
        return invitedUsers.size();
    }

    public class FriendsViewHolder extends RecyclerView.ViewHolder {

        RelativeLayout itemlLayout;
        CircleImageView friendPhotoView;
        TextView friendNameView;
        TextView friendContenview;
        Button acceptButton;
        ImageView badgeView;

        public FriendsViewHolder(@NonNull View itemView) {
            super(itemView);

            itemlLayout = itemView.findViewById(R.id.friend_item_layout);
            friendPhotoView = itemView.findViewById(R.id.friend_photo_image);
            friendNameView = itemView.findViewById(R.id.friend_fullname);
            friendContenview = itemView.findViewById(R.id.friend_content_text);
            acceptButton = itemView.findViewById(R.id.user_accept_button);
            badgeView = itemView.findViewById(R.id.user_badge);
        }
    }
}
