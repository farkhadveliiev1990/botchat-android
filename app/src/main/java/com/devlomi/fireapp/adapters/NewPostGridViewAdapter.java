package com.devlomi.fireapp.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.devlomi.fireapp.R;
import com.devlomi.fireapp.utils.PostManager;

import java.util.List;

public class NewPostGridViewAdapter extends BaseAdapter {

    private Context context;
    private List<String> listImageURLs;

    public NewPostGridViewAdapter(Context context, List<String> listImageURLs){
        this.context = context;
        this.listImageURLs = listImageURLs;
    }

    @Override
    public int getCount() {
        return (listImageURLs == null) ? 0 : listImageURLs.size();
    }

    @Override
    public Object getItem(int position) {
        return listImageURLs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.item_grid_image, null);
            viewHolder = new ViewHolder();
            viewHolder.imageView = convertView.findViewById(R.id.imageItem);
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder)convertView.getTag();
        }

        String filepath = PostManager.findVideo(listImageURLs.get(position));
        if( isImageFormat(filepath) ) {
            Glide.with(context)
                    .asBitmap()
                    .load(filepath)
                    .apply(new RequestOptions().override(100, 100))
                    .into(viewHolder.imageView);
        } else { // is video file
            Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(filepath, MediaStore.Images.Thumbnails.FULL_SCREEN_KIND);

            bitmap = ThumbnailUtils.extractThumbnail(bitmap, 100, 100,
                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
            viewHolder.imageView.setImageBitmap(bitmap);
        }

        return convertView;
    }

    private boolean isImageFormat( String url ) {
        boolean isImage = false;
        String extension = url.substring(url.lastIndexOf(".")+1);
        if( extension.equals("jpg") || extension.equals("png") )
            isImage = true;
        return isImage;
    }

    class ViewHolder{
        ImageView imageView;
    }
}
