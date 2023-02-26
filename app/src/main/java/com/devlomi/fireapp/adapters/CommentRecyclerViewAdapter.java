package com.devlomi.fireapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.devlomi.fireapp.R;
import com.devlomi.fireapp.model.Comment;
import com.devlomi.fireapp.utils.BitmapUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentRecyclerViewAdapter extends RecyclerView.Adapter<CommentRecyclerViewAdapter.MyViewHolder> {

    private Context mContext;
    private List<Comment> mComments;

    public CommentRecyclerViewAdapter(Context mContext, List<Comment> mComments) {
        this.mContext = mContext;
        this.mComments = mComments;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.row_comment, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Comment comment = mComments.get(position);

        if( comment == null ) return;

        if( !comment.getPhotoUrl().equals("") ) {

            Glide.with(mContext)
                    .asBitmap()
                    .load(BitmapUtils.encodeImageAsBytes(comment.getPhotoUrl()))
                    .into(holder.user_image);

        }

        holder.username_text.setText(comment.getUserName());
        holder.content_text.setText(comment.getContent());

        long currentTime = Long.parseLong(comment.getTime());
        Date currentDate = new Date(currentTime);
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        holder.time_text.setText(df.format(currentDate));
    }

    @Override
    public int getItemCount() {
        return mComments.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        CircleImageView user_image;
        TextView username_text;
        TextView content_text;
        TextView time_text;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            user_image = itemView.findViewById(R.id.user_image);
            username_text = itemView.findViewById(R.id.comment_username);
            content_text = itemView.findViewById(R.id.comment_content);
            time_text = itemView.findViewById(R.id.comment_time);
        }
    }
}
