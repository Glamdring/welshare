package com.welshare.service.social.video;

import com.welshare.model.social.VideoData;

public interface VideoExtractor {

    VideoData getVideoData(String url);
}
