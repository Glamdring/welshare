package com.welshare.service;

import com.welshare.model.ShortUrl;
import com.welshare.model.ShortenedLinkVisitData;
import com.welshare.model.User;

public interface UrlShorteningService {

    String shortenUrl(String url, User user);

    String shortenUrl(String url, User user, boolean showTopBar, boolean trackViral);

    /**
     * Indicates that the user has visited the link with the given key.
     *
     * @param shortKey
     * @return the url corresponding to the key
     *
     */
    ShortUrl visit(String shortKey, ShortenedLinkVisitData data);

    /**
     * checks if the given url is already shortened
     * @param url
     * @return true if already shortened
     */
    boolean isShortened(String url);

    /**
     * Expands a short url
     * @param short url
     * @return long url
     */
    String expand(String url);

    void storeShorteningSettings(Object settings, String userId);

    String getShortUrl(String key);

    /**
     *
     * @param url
     * @param user
     * @return number of times the url has been clicked, or -1 if it is unknown
     */
    int getClicks(String url, User user);
}
