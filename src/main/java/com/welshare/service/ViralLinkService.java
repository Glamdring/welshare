package com.welshare.service;

import java.io.OutputStream;

import com.welshare.model.ShortUrl;
import com.welshare.model.ShortenedLinkVisitData;
import com.welshare.model.ViralShortUrl;

public interface ViralLinkService {

    /**
     *
     * @param key
     * @param currentUser
     * @param data
     * @return url containing the long url and the new key
     */
    ShortUrl followViralLink(String key, String currentUserId, ShortenedLinkVisitData data);

    /**
     * Transfers an image (a sketch) of the graph for a given key
     * to the passed output stream
     * @param key
     * @param out
     */
    void getViralGraphImage(String key, OutputStream out);

    ViralShortUrl getViralUrl(String key);
}
