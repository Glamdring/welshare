package com.welshare.service.social.helper;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.URLEntity;
import twitter4j.auth.AccessToken;

import com.welshare.model.Message;
import com.welshare.model.Picture;
import com.welshare.model.User;
import com.welshare.model.social.TwitterSettings;
import com.welshare.service.annotations.SqlTransactional;
import com.welshare.service.social.pictures.PictureProvider;
import com.welshare.util.SecurityUtils;
import com.welshare.util.WebUtils;

@Component
public class TwitterHelper extends SocialHelper {
    public static final String PUBLIC_ID_PREFIX = "tw";

    private static final Logger logger = LoggerFactory.getLogger(TwitterHelper.class);

    @Value("${twitter.app.name}")
    private String welshareAppName;

    @Inject
    private TwitterFactory factory;

    @Inject
    private List<PictureProvider> pictureProviders;

    public List<Message> statusesToMessages(List<Status> statuses, boolean fetchImages, Twitter twitter, long userId, boolean filterInternalMessages) {
        List<Message> messages = new ArrayList<Message>(statuses.size());

        for (Status status : statuses) {
            // skip messages that are coming from welshare
            if (filterInternalMessages
                    && status.getSource().contains(welshareAppName)) {
                continue;
            }
            Message message = statusToMessage(status, fetchImages, twitter, userId);

            messages.add(message);
        }
        return messages;
    }

    public List<Message> statusesToMessages(List<Status> statuses, boolean fetchImages, Twitter twitter, long userId) {
        return statusesToMessages(statuses, fetchImages, twitter, userId, true);
    }


    public Message statusToMessage(Status status, boolean fetchImages,
            Twitter twitter, final long userId) {

        if (status == null) {
            return null;
        }
        Message message = new Message();
        message.getData().setExternalSiteName("twitter");
        message.getData().setExternalId(PUBLIC_ID_PREFIX + status.getId());
        message.setDateTime(new DateTime(status.getCreatedAt()));
        message.setScore((int) status.getRetweetCount());
        if (message.getScore() == 0 && status.getRetweetedStatus() != null) {
            message.setScore((int) status.getRetweetedStatus().getRetweetCount());
        }
        message.setReplies(-1); // -1 means undetermined. May show an option to
                                // retrieve replies in the UI
        message.setLiking(status.isRetweet());

        User externalAuthor = null;

        if (status.isRetweet()) {
            message.setText(status.getRetweetedStatus().getText() + " (retweeted by @"
                    + status.getUser().getScreenName() + ")");
            message.setShortText(status.getRetweetedStatus().getText());
            externalAuthor = getExternalAuthor(status.getRetweetedStatus());
        } else {
            message.setText(status.getText());
            externalAuthor = getExternalAuthor(status);
        }

        boolean retweetedByMe = status.isRetweetedByMe();
        message.getData().setLikedByCurrentUser(retweetedByMe);
        //not using getHashtags, because non-ascii tags might not be present? TODO
        //message.setTags(shareService.parseTags(message.getText()));

        message.setAuthor(externalAuthor);

        message.getData().setExternalUrl("http://twitter.com/" + externalAuthor.getUsername() + "/status/" + status.getId());

        // show the actual urls behind t.co urls
        URLResult result = getActualUrls(status, message.getText());
        List<String> urls = result.getUrls();
        message.setText(result.getModifiedText());

        if (fetchImages) {
            List<Picture> pictures = new ArrayList<Picture>();
            for (String url : urls) {
                for (PictureProvider pp : pictureProviders) {
                    String thumbUrl = pp.getImageURL(url);
                    if (thumbUrl != null) {
                        Picture picture = new Picture();
                        picture.setExternal(true);
                        picture.setPath(thumbUrl);
                        picture.setExternalUrl(url);
                        pictures.add(picture);
                        break;
                    }
                }
            }

            // Get twitter internal pictures as well
            if (status.getMediaEntities() != null) {
                for (MediaEntity media : status.getMediaEntities()) {
                    Picture picture = new Picture();
                    picture.setExternal(true);
                    picture.setPath(media.getMediaURL().toString());
                    picture.setExternalUrl("http://" + media.getDisplayURL().toString());
                    pictures.add(picture);
                }
            }

            message.setPictures(pictures);
            message.setPictureCount(pictures.size());
        }

        return message;
    }

    public URLResult getActualUrls(Status status, String text) {
        String shortModifiedText = text;
        List<String> urls = new ArrayList<String>();
        URLEntity[] urlEntities = status.getURLEntities();
        // if no entities are found, see if it's a retweet and get the ones of the retweeted message
        if (urlEntities == null && status.getRetweetedStatus() != null) {
            urlEntities = status.getRetweetedStatus().getURLEntities();
        }
        if (urlEntities != null) {
            for (URLEntity entity : urlEntities) {
                if (entity != null && entity.getURL() != null) {
                    String url = entity.getURL().toString();

                    if (entity.getDisplayURL() != null) {
                        shortModifiedText = shortModifiedText.replace(url, entity.getDisplayURL());
                    }
                    if (url.contains("://t.co/") && entity.getExpandedURL() != null) {
                        String newUrl = entity.getExpandedURL().toString();
                        text = text.replace(url, newUrl);
                        url = newUrl;
                        // sometimes twitter doesn't gives us the display URLs, so we have to do it manually
                        if (entity.getDisplayURL() == null) {
                            shortModifiedText = shortModifiedText.replace(url, StringUtils.left(entity.getExpandedURL().toString(), 20));
                        }
                    }

                    urls.add(url);
                }
            }
        } else {
            urls = WebUtils.extractUrls(text);
        }

        URLResult result = new URLResult();
        result.setUrls(urls);
        result.setModifiedText(text);
        result.setShortModifiedText(shortModifiedText);
        return result;
    }

    public static class URLResult {
        private List<String> urls;
        private String modifiedText;
        private String shortModifiedText;

        public List<String> getUrls() {
            return urls;
        }
        public void setUrls(List<String> urls) {
            this.urls = urls;
        }
        public String getModifiedText() {
            return modifiedText;
        }
        public void setModifiedText(String modifiedText) {
            this.modifiedText = modifiedText;
        }
        public String getShortModifiedText() {
            return shortModifiedText;
        }
        public void setShortModifiedText(String shortModifiedText) {
            this.shortModifiedText = shortModifiedText;
        }
    }

    public User getExternalAuthor(Status status) {
        User externalAuthor = new User();
        twitter4j.User twUser = status.getUser();
        fillUserData(externalAuthor, twUser);
        return externalAuthor;
    }

    public long getTwitterId(String externalId) {
        return Long.parseLong(externalId.replace(TwitterHelper.PUBLIC_ID_PREFIX, ""));
    }

    @Cacheable(value="socialClientsCache", key="#settings.token")
    public Twitter getTwitter(TwitterSettings settings) {
        if (settings == null) {
            return null;
        }
        AccessToken token = new AccessToken(
                SecurityUtils.decrypt(settings.getToken()),
                settings.getTokenSecret());
        Twitter t = factory.getInstance(token);

        return t;
    }

    @Cacheable(value="socialClientsCache", key="#user.twitterSettings.token")
    public Twitter getTwitter(User user) {
        TwitterSettings settings = user.getTwitterSettings();
        return getTwitter(settings);
    }

    public void fillUserData(User user, twitter4j.User twUser) {
        user.getProfile().setBio(twUser.getDescription());
        if (twUser.getProfileImageURL() != null) {
            user.setProfilePictureURI(twUser.getProfileImageURL().toString());
        }
        user.setUsername(twUser.getScreenName());
        user.setNames(twUser.getName());
        user.getProfile().setCity(twUser.getLocation());
        user.setFollowers(twUser.getFollowersCount());
        user.setFollowing(twUser.getFriendsCount());
        user.setMessages(twUser.getStatusesCount());
        user.setExternalUrl("http://twitter.com/" + twUser.getScreenName());
        user.setExternalId(PUBLIC_ID_PREFIX + twUser.getId());
    }

    @Override
    protected String getPrefix() {
        return PUBLIC_ID_PREFIX;
    }

    @SqlTransactional
    @Override
    public void setLastImportedTime(String userId, long time) {
        User user = getUserDao().getById(User.class, userId); // needed, because the entity is detached
        user.getTwitterSettings().setLastImportedMessageTime(time);
        getUserDao().persist(user);
    }

    public String getWelshareAppName() {
        return welshareAppName;
    }
}
