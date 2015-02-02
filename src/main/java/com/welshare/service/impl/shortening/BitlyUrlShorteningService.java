package com.welshare.service.impl.shortening;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Inject;
import javax.inject.Qualifier;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import com.rosaloves.bitlyj.Bitly;
import com.rosaloves.bitlyj.ShortenedUrl;
import com.rosaloves.bitlyj.UrlClicks;
import com.welshare.dao.UserDao;
import com.welshare.model.ShortUrl;
import com.welshare.model.ShortenedLinkVisitData;
import com.welshare.model.User;
import com.welshare.model.social.BitlySettings;
import com.welshare.service.UrlShorteningService;
import com.welshare.service.annotations.SqlTransactional;
import com.welshare.service.impl.shortening.BitlyUrlShorteningService.BitlyUrlShortener;

@Service
@Order(1)
@BitlyUrlShortener
public class BitlyUrlShorteningService implements UrlShorteningService {

    @Inject
    private UserDao dao;

    @Override
    public String shortenUrl(String url, User user, boolean showTopBar,
            boolean trackViral) {
        // bit.ly not supporting top bar and viral tracking

        if (user.getBitlySettings() == null || StringUtils.isEmpty(user.getBitlySettings().getUser())) {
            return null;
        }
        ShortenedUrl shortened = Bitly.as(user.getBitlySettings().getUser(),
                user.getBitlySettings().getApiKey()).call(Bitly.shorten(url));

        return shortened.getShortUrl();
    }

    @Override
    public String shortenUrl(String url, User user) {
        return shortenUrl(url, user, false, false);
    }

    @Override
    public ShortUrl visit(String shortKey, ShortenedLinkVisitData data) {
        //do nothing for now. Later we can use ajax to count bit-ly link clicks,
        //even though they lead to bit.ly directly
        return null;
    }

    @Override
    public boolean isShortened(String url) {
        return url.startsWith("http://bit.ly");
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface BitlyUrlShortener { }

    @Override
    @SqlTransactional
    public void storeShorteningSettings(Object settings, String userId) {
        User user = dao.getById(User.class, userId, true);
        BitlySettings bitlySettings = (BitlySettings) settings;
        user.setBitlySettings(bitlySettings);
        dao.persist(user);

    }

    @Override
    public String getShortUrl(String key) {
        return "http://bit.ly/" + key;
    }

    @Override
    public String expand(String url) {
        //TODO not implemented. It requires API credentials
        return url;
    }

    @Override
    public int getClicks(String url, User user) {
       if (user.getBitlySettings() != null && StringUtils.isNotEmpty(user.getBitlySettings().getUser())) {
           Bitly.Provider bitly = Bitly.as(user.getBitlySettings().getUser(), user.getBitlySettings().getApiKey());
           if (user.getBitlySettings().getUser().equals(bitly.call(Bitly.info(url)).getCreatedBy())) {
               UrlClicks clicks = bitly.call(Bitly.clicks(url));
               return (int) clicks.getUserClicks();
           }
       }
       return -1;
    }
}
