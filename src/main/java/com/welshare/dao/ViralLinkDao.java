package com.welshare.dao;

import com.welshare.model.ShortUrl;
import com.welshare.model.ShortenedLinkVisitData;
import com.welshare.model.ViralShortUrl;

public interface ViralLinkDao {

    ViralShortUrl spawnLink(String originalKey, String userId,
            ShortenedLinkVisitData data);

    ViralShortUrl getLink(String key);

    void storeShortUrl(ShortUrl shortUrl);

}