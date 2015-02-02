package com.welshare.service.social.pictures;

public interface PictureProvider {
    /**
     * Gets a direct URL to a picture based on the a link to the page containing
     * the picture
     *
     * @param link
     * @return direct url, or null if this link can't be handle by this provider
     */
    String getImageURL(String link);
}
