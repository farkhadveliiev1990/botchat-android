package com.devlomi.fireapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.devlomi.fireapp.R;
import com.devlomi.fireapp.activities.ViewStatusActivity;
import com.devlomi.fireapp.model.TextStatus;
import com.devlomi.fireapp.model.realms.User;
import com.devlomi.fireapp.model.realms.UserStatuses;
import com.devlomi.fireapp.utils.BitmapUtils;
import com.devlomi.fireapp.utils.FireManager;
import com.devlomi.fireapp.utils.IntentUtils;
import com.devlomi.fireapp.utils.RealmHelper;
import com.devlomi.fireapp.utils.SharedPreferencesManager;
import com.devlomi.fireapp.views.TextViewWithShapeBackground;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import io.realm.RealmList;
import io.realm.RealmResults;

import static com.devlomi.fireapp.utils.FontUtil.isFontExists;

public class UserStatusRecyclerViewAdapter extends RecyclerView.Adapter<UserStatusRecyclerViewAdapter.MyViewHolder> {

    private Context mContext;
    private RealmResults<UserStatuses> userStatusesList;
    private RealmResults<UserStatuses> originalList;

    public UserStatusRecyclerViewAdapter(Context mContext, RealmResults<UserStatuses> userStatuses) {
        this.mContext = mContext;
        this.userStatusesList = userStatuses;
        this.originalList = this.userStatusesList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.status_item, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        UserStatuses userStatuses = userStatusesList.get(position);
        if( userStatuses != null ) {

            if( userStatuses.getUser() == null ) return ;

            User user = userStatuses.getUser();
            holder.nameText.setText(userStatuses.getUser().getUserName());

            Glide.with(mContext)
                    .asBitmap()
                    .load(BitmapUtils.encodeImageAsBytes(user.getThumbImg()))
                    .into(holder.userImage);

            TextStatus textStatus = userStatuses.getStatuses().last().getTextStatus();
            if( textStatus == null ) {
                holder.backgroundImage.setVisibility(View.VISIBLE);
                holder.textStatus.setVisibility(View.GONE);
                Glide.with(mContext)
                        .asBitmap()
                        .load(BitmapUtils.encodeImageAsBytes(userStatuses.getStatuses().last().getThumbImg()))
                        .into(holder.backgroundImage);
                holder.setInitialTextStatusValues();
            } else {
                //holder.storyCardView.setCardBackgroundColor(Color.parseColor(textStatus.getBackgroundColor()));
                holder.backgroundImage.setVisibility(View.GONE);
                holder.textStatus.setVisibility(View.VISIBLE);

                holder.textStatus.setText(textStatus.getText());
                holder.textStatus.setShapeColor(Color.parseColor(textStatus.getBackgroundColor()));
                holder.setTypeFace(holder.textStatus, textStatus.getFontName());
            }

            holder.storyCardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, ViewStatusActivity.class);
                    intent.putExtra(IntentUtils.UID, userStatuses.getUserId());
                    mContext.startActivity(intent);
                }
            });
        }
    }


    public void filter(String query) {

        if (query.trim().isEmpty()) {
            userStatusesList = originalList;
        } else {
            RealmResults<UserStatuses> userStatuses = RealmHelper.getInstance().searchForStatus(query);
            userStatusesList = userStatuses;
        }

        notifyDataSetChanged();

    }

    @Override
    public int getItemCount() {
        return userStatusesList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        ImageView backgroundImage;
        CircleImageView userImage;
        TextView nameText;
        CardView storyCardView;
        TextViewWithShapeBackground textStatus;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            backgroundImage = itemView.findViewById(R.id.back_image);
            userImage = itemView.findViewById(R.id.user_circle);
            nameText = itemView.findViewById(R.id.name_text);
            storyCardView = itemView.findViewById(R.id.story_cardview);
            textStatus = itemView.findViewById(R.id.text_status);
        }

        private void setInitialTextStatusValues() {
            textStatus.setText("");
            textStatus.setShapeColor(Color.BLACK);
        }

        private void setTypeFace(TextView textView, String fontName) {
            if (isFontExists(fontName)) {
                textView.setTypeface(Typeface.createFromAsset(mContext.getAssets(), "fonts/" + fontName));
            }
        }
    }
}
