package com.devlomi.fireapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager.widget.PagerAdapter;

import com.devlomi.fireapp.R;

/**
 * Created by ravi on 2/7/18.
 */

public class SliderAdapter extends PagerAdapter {

    Context context;
    LayoutInflater layoutInflater;

    public SliderAdapter(Context context){
        this.context=context;
    }

    public int[] slideImages = {
            R.drawable.social,
            R.drawable.translate,
            R.drawable.marketplace
    };

    public String[] slideHeadings ={
            "Welcome to SocialBot",
            "Translation",
            "Marketplace, On-Demand Services,\nJob Search"

    };

    public String[] slideDescriptions ={
            "Socialbot is your all in one Social Platform, with loads of features like Meet new friends, chat one-on-one or in groups, voice and video calling , Share pics, videos, gifs and more.",
            "Chat to your friends in their native language, Select your preferred language from more than 100 languages in your profile page, it automatically translates your text messages to your friends selected language.",
            "- Sell all you unwanted goods, to users close to your location.\n\n- Connect to Professionals near you, Search, Book & Manage any home or office related services.\n\n- Our convenient app gives you access to 1000s of job vacancies anywhere, anytime."

    };


    @Override
    public int getCount() {
        return slideHeadings.length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == (ConstraintLayout) object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {

        layoutInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.slide_layout, container, false);

        ImageView slideImageView = (ImageView) view.findViewById(R.id.iv_image_icon);
        TextView slideHeading = (TextView) view.findViewById(R.id.tv_heading);
        TextView slideDescription = (TextView) view.findViewById(R.id.tv_description);

        slideImageView.setImageResource(slideImages[position]);
        slideHeading.setText(slideHeadings[position]);
        slideDescription.setText(slideDescriptions[position]);

        container.addView(view);

        return view;

    }


    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((ConstraintLayout) object);  //todo: RelativeLayout??
    }
}
