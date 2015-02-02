package com.welshare.service.social.video;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.lang3.math.NumberUtils;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.welshare.model.social.VideoData;

@Component
public class VimeoVideoExtractor implements VideoExtractor {


    public static final String DEFAULT_PICTURE = "http://a.vimeocdn.com/images/logo_vimeo_land.png";
    private static final String VIMEO_BASE_URL = "vimeo.com/";

    private static final Logger logger = LoggerFactory.getLogger(VimeoVideoExtractor.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public VideoData getVideoData(String url) {
        if (url.contains(VIMEO_BASE_URL)) {
            VideoData data = new VideoData();
            String key = url.substring(url.indexOf(VIMEO_BASE_URL) + VIMEO_BASE_URL.length());

            if (!NumberUtils.isNumber(key)) {
                return null;
            }
            //data.setPlayerUrl("http://player.vimeo.com/video/" + key + "?title=0&amp;byline=0&amp;portrait=0&amp;autoplay=true");
            data.setPlayerUrl("http://vimeo.com/moogaloop.swf?clip_id=" + key + "&autoplay=1");
            data.setEmbedCode("<iframe src=\"" + data.getPlayerUrl() + "\" width=\"480\" height=\"390\" frameborder=\"0\"></iframe>");
            data.setKey(key);

            try {
                URL apiUrl = new URL("http://vimeo.com/api/v2/video/" + key + ".json");
                VimeoImageData[] apiData = mapper.readValue(apiUrl, VimeoImageData[].class);
                data.setPicture(apiData[0].getThumbnailSmall());
            } catch (IOException ex) {
                logger.warn("Can't extract picture for vimeo video; using default", ex);
                data.setPicture(DEFAULT_PICTURE);
            }

            return data;
        }
        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class VimeoImageData {
        private String id;
        private String title;
        @JsonProperty("thumbnail_small")
        private String thumbnailSmall;
        @JsonProperty("thumbnail_medium")
        private String thumbnailMedium;

        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }
        public String getTitle() {
            return title;
        }
        public void setTitle(String title) {
            this.title = title;
        }
        public String getThumbnailSmall() {
            return thumbnailSmall;
        }
        public void setThumbnailSmall(String thumbnailSmall) {
            this.thumbnailSmall = thumbnailSmall;
        }
        public String getThumbnailMedium() {
            return thumbnailMedium;
        }
        public void setThumbnailMedium(String thumbnailMedium) {
            this.thumbnailMedium = thumbnailMedium;
        }
    }
}