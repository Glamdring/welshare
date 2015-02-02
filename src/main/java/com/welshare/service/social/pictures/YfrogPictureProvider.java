package com.welshare.service.social.pictures;

import org.springframework.stereotype.Component;

@Component
public class YfrogPictureProvider implements PictureProvider {

    @Override
    public String getImageURL(String link) {
        if (link.contains("//yfrog.com")) {
            String key = link.substring(link.lastIndexOf('/') + 1);
            return "//yfrog.com/" + key + ":small";
        }
        return null;
    }
}
