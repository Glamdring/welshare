package com.welshare.service.impl.shortening;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Qualifier;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import com.welshare.dao.ViralLinkDao;
import com.welshare.model.ShortUrl;
import com.welshare.model.ShortenedLinkVisitData;
import com.welshare.model.User;
import com.welshare.service.UrlShorteningService;
import com.welshare.service.annotations.SqlTransactional;
import com.welshare.service.impl.BaseServiceImpl;
import com.welshare.service.impl.shortening.DefaultUrlShorteningService.DefaultUrlShortener;
import com.welshare.util.GeneralUtils;

@Service
@Order(2)
@DefaultUrlShortener
public class DefaultUrlShorteningService extends BaseServiceImpl implements UrlShorteningService {

    public static final Logger logger = LoggerFactory.getLogger(DefaultUrlShorteningService.class);

    @Inject
    private ViralLinkDao viralDao;

    @Value("${url.shortener.domain}")
    private String urlShortenerDomain;

    private List<String> unframeableSites;

    @PostConstruct
    public void init() throws IOException {
        InputStream is = getClass().getResourceAsStream("/properties/unframeableSites.txt");
        unframeableSites = IOUtils.readLines(is);
    }

    @Override
    @SqlTransactional
    public String shortenUrl(String url, User user, boolean showTopBar,
            boolean trackViral) {

        // if this url cannot be shortened with a top bar, reset the param value
        if (trackViral || showTopBar) {
            for (String unframeable : unframeableSites) {
                if (url.startsWith(unframeable)) {
                    trackViral = false;
                    showTopBar = false;
                }
            }
        }

        String shortUrlKey = getShortKey();

        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setKey(shortUrlKey);
        shortUrl.setLongUrl(url);
        shortUrl.setUser(user);
        shortUrl.setTimeAdded(new DateTime());
        shortUrl.setTrackViral(trackViral);
        shortUrl.setShowTopBar(showTopBar);

        getDao().persist(shortUrl);

        if (trackViral) {
            viralDao.storeShortUrl(shortUrl);
        }

        return urlShortenerDomain + "/" + shortUrlKey;
    }

    private String getShortKey() {
        String shortUrlKey = GeneralUtils.generateShortKey();
        while (get(ShortUrl.class, shortUrlKey) != null) {
            shortUrlKey = GeneralUtils.generateShortKey();
        }
        return shortUrlKey;
    }

    @Override
    public String shortenUrl(String url, User user) {
        return shortenUrl(url, user, false, false);
    }

    @Override
    @SqlTransactional
    public ShortUrl visit(String shortKey, ShortenedLinkVisitData data) {
        ShortUrl url = get(ShortUrl.class, shortKey);
        if (url != null) {
            // not incrementing the visits here - a job will later go through
            // the counts of inserted data and set it here

            if (data != null) {
                data.setShortUrl(url);
                save(data);
            }

            if (url.isTrackViral()) {

            }
            return url;
        }
        return null;
    }

    @Override
    public boolean isShortened(String url) {
        return url.startsWith(urlShortenerDomain);
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface DefaultUrlShortener { }

    @Override
    public void storeShorteningSettings(Object settings, String userId) {
        // do nothing. No settings for the default shortening (yet)
    }

    @Override
    public String getShortUrl(String key) {
        return urlShortenerDomain + "/" + key;
    }

    @Override
    public String expand(String url) {
        String key = getKey(url);
        ShortUrl info = get(ShortUrl.class, key);
        if (info == null) {
            return url;
        } else {
            return info.getLongUrl();
        }
    }


    @Override
    public int getClicks(String url, User user) {
        ShortUrl entity = getDao().getById(ShortUrl.class, getKey(url));
        if (entity == null) {
            return -1;
        }
        return entity.getVisits();
    }

    private String getKey(String url) {
        String key = url.replace(urlShortenerDomain + "/", "");
        return key;
    }
}
