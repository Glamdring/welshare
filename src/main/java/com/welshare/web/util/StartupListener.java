package com.welshare.web.util;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.logging.LogManager;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.SessionCookieConfig;
import javax.servlet.annotation.WebListener;

import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.google.code.linkedinapi.client.constant.ApplicationConstants;
import com.google.common.collect.Maps;
import com.welshare.service.UrlShorteningService;
import com.welshare.service.social.helper.ExtendedLinkedInApiClient;
import com.welshare.service.social.helper.FacebookHelper;
import com.welshare.service.social.helper.GooglePlusHelper;
import com.welshare.service.social.helper.LinkedInHelper;
import com.welshare.service.social.helper.TwitterHelper;
import com.welshare.service.social.pictures.PictureProvider;
import com.welshare.service.social.video.VideoExtractor;
import com.welshare.util.WebUtils;

@WebListener
public class StartupListener implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(StartupListener.class);

    @Override
    public void contextDestroyed(ServletContextEvent e) {
        // do nothing on destroy
    }

    @Override
    public void contextInitialized(ServletContextEvent e) {
        System.setProperty(ApplicationConstants.CLIENT_DEFAULT_IMPL, ExtendedLinkedInApiClient.class.getName());

        DateTimeZone.setDefault(DateTimeZone.UTC);
        Locale.setDefault(Locale.ENGLISH);
        SessionCookieConfig sessionCookieConfig = e.getServletContext().getSessionCookieConfig();
        sessionCookieConfig.setHttpOnly(true);
        try {
            LogManager.getLogManager().readConfiguration(e.getServletContext().getResourceAsStream("/WEB-INF/classes/logging.properties"));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to initialize java.util.logging", ex);
        }
        ApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(e.getServletContext());
        Map<String, VideoExtractor> videoExtractors = ctx.getBeansOfType(VideoExtractor.class);
        WebUtils.videoExtractors.addAll(videoExtractors.values());

        Map<String, PictureProvider> pictureProviders = ctx.getBeansOfType(PictureProvider.class);
        WebUtils.pictureProviders.addAll(pictureProviders.values());

        Map<String, UrlShorteningService> urlShorteners = ctx.getBeansOfType(UrlShorteningService.class);
        WebUtils.urlShorteners.addAll(urlShorteners.values());

        initializeSocialNetworks(e.getServletContext());

        logger.info("Welshare startup successful");
    }

    private void initializeSocialNetworks(ServletContext ctx) {
        Map<String, SocialNetwork> networks = Maps.newLinkedHashMap();
        Map<String, SocialNetwork> networksByPrefix = Maps.newLinkedHashMap();

        SocialNetwork twitter = new SocialNetwork();
        twitter.setPrefix(TwitterHelper.PUBLIC_ID_PREFIX);
        twitter.setName("Twitter");
        twitter.setSiteName("twitter");
        twitter.setIcon("twitter_connect.png");
        twitter.setIndicatorIcon("twitter_indicator.png");
        twitter.setSharingEnabled(true);
        twitter.setLikeAndReshare(false);
        twitter.setFollowersUrl("http://twitter.com/followers");
        twitter.setFollowingUrl("http://twitter.com/following");
        networks.put(twitter.getSiteName(), twitter);
        networksByPrefix.put(twitter.getPrefix(), twitter);

        SocialNetwork facebook = new SocialNetwork();
        facebook.setPrefix(FacebookHelper.PUBLIC_ID_PREFIX);
        facebook.setName("Facebook");
        facebook.setSiteName("facebook");
        facebook.setIcon("facebook_connect.png");
        facebook.setIndicatorIcon("facebook_connect.png");
        facebook.setSharingEnabled(true);
        facebook.setLikeAndReshare(true);
        facebook.setFollowersUrl("http://facebook.com/friends");
        facebook.setFollowingUrl("http://facebook.com/friends");
        networks.put(facebook.getSiteName(), facebook);
        networksByPrefix.put(facebook.getPrefix(), facebook);

        SocialNetwork googlePlus = new SocialNetwork();
        googlePlus.setPrefix(GooglePlusHelper.PUBLIC_ID_PREFIX);
        googlePlus.setName("Google+");
        googlePlus.setSiteName("googlePlus");
        googlePlus.setIcon("googleplus_connect.png");
        googlePlus.setIndicatorIcon("googleplus_connect.png");
        googlePlus.setSharingEnabled(true);
        googlePlus.setLikeAndReshare(false);
        googlePlus.setFollowersUrl("https://plus.google.com/circles/addedyou");
        googlePlus.setFollowingUrl("https://plus.google.com/circles");
        networks.put(googlePlus.getSiteName(), googlePlus);
        networksByPrefix.put(googlePlus.getPrefix(), googlePlus);

        SocialNetwork linkedIn = new SocialNetwork();
        linkedIn.setPrefix(LinkedInHelper.PUBLIC_ID_PREFIX);
        linkedIn.setName("LinkedIn");
        linkedIn.setSiteName("linkedIn");
        linkedIn.setIcon("linkedin_connect.png");
        linkedIn.setIndicatorIcon("linkedin_connect.png");
        linkedIn.setSharingEnabled(true);
        linkedIn.setLikeAndReshare(true);
        linkedIn.setFollowersUrl("http://www.linkedin.com/connections");
        linkedIn.setFollowingUrl("http://www.linkedin.com/connections");
        networks.put(linkedIn.getSiteName(), linkedIn);
        networksByPrefix.put(linkedIn.getPrefix(), linkedIn);
        ctx.setAttribute("socialNetworks", networks);
        ctx.setAttribute("socialNetworksByPrefix", networksByPrefix);
    }

    public static class SocialNetwork {
        private String prefix;
        private String icon; //relative to /static/images/social
        private String indicatorIcon; //relative to /static/images/social
        private String name; //used for displaying to users
        private String siteName; //used internally
        private boolean sharingEnabled;
        private String followersUrl;
        private String followingUrl;
        private boolean likeAndReshare; // whether the network supports both actions, or just a single one

        public String getPrefix() {
            return prefix;
        }
        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }
        public String getIcon() {
            return icon;
        }
        public void setIcon(String icon) {
            this.icon = icon;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getSiteName() {
            return siteName;
        }
        public void setSiteName(String siteName) {
            this.siteName = siteName;
        }
        public boolean isSharingEnabled() {
            return sharingEnabled;
        }
        public void setSharingEnabled(boolean sharingEnabled) {
            this.sharingEnabled = sharingEnabled;
        }
        public String getFollowersUrl() {
            return followersUrl;
        }
        public void setFollowersUrl(String followersUrl) {
            this.followersUrl = followersUrl;
        }
        public String getFollowingUrl() {
            return followingUrl;
        }
        public void setFollowingUrl(String followingUrl) {
            this.followingUrl = followingUrl;
        }
        public String getIndicatorIcon() {
            return indicatorIcon;
        }
        public void setIndicatorIcon(String indicatorIcon) {
            this.indicatorIcon = indicatorIcon;
        }
        public boolean isLikeAndReshare() {
            return likeAndReshare;
        }
        public void setLikeAndReshare(boolean likeAndReshare) {
            this.likeAndReshare = likeAndReshare;
        }
    }
}
