package com.welshare.service.social.pictures;

import org.springframework.stereotype.Component;

@Component
public class BasicPictureProvider implements PictureProvider {
    private static final String[] EXTENSIONS = new String[] {".jpg", ".jpeg", ".png", ".gif"};
    @Override
    public String getImageURL(String link) {
        for (String extension : EXTENSIONS) {
            if (link.toLowerCase().endsWith(extension)) {
                return link;
            }
        }

        return null;
    }

}
