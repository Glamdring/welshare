package com.welshare.service.social.pictures;

import org.springframework.stereotype.Component;

@Component
public class TwitpicPictureProvider implements PictureProvider {

    @Override
    public String getImageURL(String link) {
        if (link.contains("//twitpic.com")) {
            String key = link.substring(link.lastIndexOf('/'));
            return "//twitpic.com/show/mini/" + key;
        }
        return null;
    }

}
