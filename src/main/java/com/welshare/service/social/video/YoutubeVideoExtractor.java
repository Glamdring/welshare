package com.welshare.service.social.video;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.welshare.model.social.VideoData;
import com.welshare.util.WebUtils;

@Component
public class YoutubeVideoExtractor implements VideoExtractor {

    private static final String LONG_DOMAIN = "youtube.com/";
    private static final String SHORT_DOMAIN = "youtu.be/";

    @Override
    public VideoData getVideoData(String url) {

        String key = null;
        String timeHashParam = "";

        if ((url.contains(LONG_DOMAIN) || url.contains(SHORT_DOMAIN)) && url.contains("#t=")) {
            timeHashParam = url.substring(url.lastIndexOf("#t="));
            url = url.replace(timeHashParam, "");
        }

        if (url.contains(LONG_DOMAIN)) {
            Map<String, List<String>> params = WebUtils.getQueryParams(url);
            List<String> values = params.get("v");
            if (values == null || values.isEmpty()) {
                // a non-video link in youtube
                return null;
            }
            key = values.get(0);
        }

        if (url.contains(SHORT_DOMAIN)) {
            key =  url.substring(url.indexOf(SHORT_DOMAIN) + SHORT_DOMAIN.length());
            if (key.contains("?")) {
                key = key.substring(0, key.indexOf("?"));
            }
        }

        if (key == null) {
            return null;
        }

        VideoData vd = new VideoData();
        vd.setKey(key);
        vd.setPlayerUrl("http://www.youtube.com/e/" + key + "?autoplay=1" + timeHashParam);
        vd.setPicture("http://img.youtube.com/vi/" + key + "/0.jpg");
        vd.setEmbedCode("<iframe title=\"YouTube video player\" width=\"480\" height=\"390\" src=\"" + vd.getPlayerUrl() + "\" frameborder=\"0\" allowfullscreen></iframe>");
        return vd;
    }
}
