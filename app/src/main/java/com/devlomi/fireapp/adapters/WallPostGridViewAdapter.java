package com.devlomi.fireapp.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.devlomi.fireapp.R;
import com.devlomi.fireapp.utils.BitmapUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class WallPostGridViewAdapter extends BaseAdapter {

    private Context context;
    private List<String> listImageURLs;

    public WallPostGridViewAdapter(Context context, List<String> listImageURLsHash){
        this.context = context;
        this.listImageURLs = listImageURLsHash;
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
        Log.e("GridViewAdapter",Integer.toString(position));
        Log.e("ImageItem",getItem(position).toString());
        Log.e("Images",listImageURLs.toString());
        WallPostGridViewAdapter.ViewHolder viewHolder;
        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.item_grid_image, null);
            viewHolder = new WallPostGridViewAdapter.ViewHolder();
            viewHolder.imageView = convertView.findViewById(R.id.imageItem);
            viewHolder.playView = convertView.findViewById(R.id.play_images);
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (WallPostGridViewAdapter.ViewHolder)convertView.getTag();
        }

        Glide.with(context)
//                .load(findVideo(listImageURLs.get(position)))
        .load(BitmapUtils.encodeImageAsBytes(listImageURLs.get(position)))
        .into(viewHolder.imageView);

        if( !isImageFormat(listImageURLs.get(position)) )
            viewHolder.playView.setVisibility(View.VISIBLE);

        return convertView;
    }

    private String findVideo( String uri ) {
        String filePath = uri;
        if( uri.indexOf("+") != -1 ) {
            filePath = uri.substring(uri.indexOf("+")+1);
        }
        return filePath;
    }

    private boolean isImageFormat( String url ) {
        if( url.indexOf("+") != -1 ) // if path is video
            return false;

        return true;
    }

    class ViewHolder{
        ImageView imageView;
        ImageView playView;
    }
}
