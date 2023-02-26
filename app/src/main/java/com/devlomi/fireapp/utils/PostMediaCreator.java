package com.devlomi.fireapp.utils;

import com.devlomi.fireapp.model.Post;
import com.devlomi.fireapp.model.constants.StatusType;

import java.util.Date;

public class PostMediaCreator {

    public static PostMedia createImagePost(String imagePath) {

        String thumbImg = BitmapUtils.decodeImage(imagePath, false);
        PostMedia postMedia = new PostMedia(FireManager.getUid(), null, null, 0,
                thumbImg, imagePath, new Date().getTime(), 1);

        return postMedia;
    }

    public static PostMedia createVideoPost( String videoPath ) {

        String thumbImg = BitmapUtils.generateVideoThumbAsBase64(videoPath);
        long mediaLengthInMillis = Util.getMediaLengthInMillis(MyApp.context(), videoPath);

        PostMedia postMedia = new PostMedia(FireManager.getUid(), null, null, mediaLengthInMillis,
                thumbImg, videoPath, new Date().getTime(), 2);

        return postMedia;
    }

    public static Post createLocationPost( String latlng ) {
        return null;
    }
}
