package com.welshare.web;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.WebUtils;

import com.welshare.model.ShortUrl;
import com.welshare.model.ShortenedLinkVisitData;
import com.welshare.model.ViralShortUrl;
import com.welshare.service.UrlShorteningService;
import com.welshare.service.ViralLinkService;
import com.welshare.service.impl.shortening.DefaultUrlShorteningService.DefaultUrlShortener;

@Controller
public class ShortenedUrlController {

    private static final String WELSHARE_VIRAL_SESSION_ID = "welshareViralSessionId";

    private static final String SHORT_URL = "shortUrl";

    private static final String URL = "url";

    private static final Logger logger = LoggerFactory.getLogger(ShortenedUrlController.class);

    @Inject @DefaultUrlShortener
    private UrlShorteningService service;

    @Inject
    private ViralLinkService viralService;

    @Value("${base.url}")
    private String baseUrl;

    @Value("${assets.version}")
    private String assetsVersion;

    @RequestMapping(value="/short/{key}")
    public String resolveShortenedUrl(@PathVariable String key, Locale locale,
            @RequestHeader(value="Referer", required=false) String referer,
            HttpServletResponse response, HttpServletRequest request,
            Model model) throws IOException {

        ShortenedLinkVisitData data = new ShortenedLinkVisitData();
        data.setReferer(StringUtils.left(referer, 250));
        data.setLanguage(locale.getLanguage());
        data.setIp(request.getRemoteAddr());
        data.setSessionId(request.getSession().getId());
        data.setDateTime(new DateTime());

        ShortUrl url = service.visit(key, data);
        if (url != null && !url.isTrackViral()) {
            if (url.isShowTopBar()) {
                model.addAttribute(URL, url);
                model.addAttribute(SHORT_URL, service.getShortUrl(key));
                return "viral/topBarLink";
            } else {
                response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                response.setHeader("Location", url.getLongUrl());
            }
        } else {
            // look for a special cookie storing the original session id of the current user
            Cookie existingCookie = WebUtils.getCookie(request, WELSHARE_VIRAL_SESSION_ID);
            String userId = null;
            if (existingCookie != null) {
                data.setSessionId(existingCookie.getValue());
                userId = existingCookie.getValue();
            }

            url = viralService.followViralLink(key, userId, data);
            if (url == null || url.getKey() == null) {
                logger.warn("A null url or null key was received from grapbDB for URL: " + request.getRequestURL() + ". Data: " + data);
                // if the key is not found in either the url db or the viral graph db, redirect to the main site
                response.sendRedirect(baseUrl + request.getRequestURI().replace("/short", ""));
                return null;
            }
            if (url.getKey().equals(key)) {
                model.addAttribute(URL, url);
                model.addAttribute(SHORT_URL, service.getShortUrl(key));

                // add a cookie to recognize returning visitors
                Cookie cookie = createViralCookie(data.getSessionId());
                response.addCookie(cookie);

                return "viral/topBarLink";
            } else  {
                response.sendRedirect(service.getShortUrl(url.getKey()));
            }
        }

        return null;
    }

    private Cookie createViralCookie(String id) {
        Cookie cookie = new Cookie(WELSHARE_VIRAL_SESSION_ID, id);
        cookie.setMaxAge(DateTimeConstants.SECONDS_PER_WEEK);
        cookie.setPath("/");
        return cookie;
    }

    @RequestMapping("/viralGraphImage/{key}")
    public void getViralGraphImage(@PathVariable String key, HttpServletResponse response) throws IOException {
        OutputStream out = response.getOutputStream();
        response.setContentType("image/png");
        viralService.getViralGraphImage(key, out);
    }

    @RequestMapping("/viral/info/{key}")
    public String getViralInfo(@PathVariable String key, Model model) {
        ViralShortUrl url = viralService.getViralUrl(key);
        model.addAttribute(URL, url);
        model.addAttribute(SHORT_URL, service.getShortUrl(key));
        return "viral/info";
    }

    @RequestMapping("/shortener/userId/{userId}")
    public void setShortenerCookie(@PathVariable String userId,
            HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        Cookie existingCookie = WebUtils.getCookie(request, WELSHARE_VIRAL_SESSION_ID);
        if (existingCookie == null || !existingCookie.getValue().equals(userId)) {
            response.addCookie(createViralCookie(userId));
        }

        request.getRequestDispatcher("/static/" + assetsVersion + "/images/pixel.jpg").forward(request, response);

    }
}
