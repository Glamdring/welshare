package com.welshare.service.social.helper;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.welshare.dao.MessageDao;
import com.welshare.dao.UserDao;
import com.welshare.model.Message;
import com.welshare.model.Picture;
import com.welshare.model.User;
import com.welshare.model.social.VideoData;
import com.welshare.service.ShareService;
import com.welshare.service.annotations.SqlTransactional;
import com.welshare.service.social.SocialNetworkService;
import com.welshare.service.social.pictures.PictureProvider;
import com.welshare.service.social.video.VideoExtractor;
import com.welshare.util.Constants;
import com.welshare.util.WebUtils;

public abstract class SocialHelper {

    private static final String[] PICTURE_EXTENSIONS = {".png", ".jpg", ".gif"};

    @Inject
    private List<PictureProvider> pictureProviders;

    @Inject
    private List<VideoExtractor> videoExtractors;

    @Inject
    private MessageDao messageDao;

    @Inject
    private UserDao userDao;

    protected SocialHelper() {
        //reducing visibility of default constructor
    }

    public String getPictureUrl(String url) {
        for (String ext : PICTURE_EXTENSIONS) {
            if (url.endsWith(ext)) {
                return url;
            }
        }
        for (PictureProvider provider : pictureProviders) {
            String providerResult = provider.getImageURL(url);
            if (providerResult != null) {
                return providerResult;
            }
        }
        return null;
    }

    public VideoData getVideoData(String url) {
        for (VideoExtractor extractor : videoExtractors) {
            VideoData vd = extractor.getVideoData(url);
            if (vd != null) {
                return vd;
            }
        }
        return null;
    }

    @SqlTransactional
    public void addToAssociatedMessages(Message message, String responseId) {
        if (message.getId() != null) {
            message = messageDao.getById(Message.class, message.getId());
            message.getAssociatedExternalIds().add(getPrefix() + responseId);
            messageDao.persist(message);
        }
    }

    @SqlTransactional
    public void forciblyDisconnect(SocialNetworkService service, User user) {
        user.setClosedHomepageConnectLinks(false);
        userDao.persist(user);
        service.disconnect(user.getId());
    }

    @SqlTransactional
    public void incrementMessageCount(String userId, int count) {
        User user = userDao.getById(User.class, userId, true);
        user.setMessages(user.getMessages() + count);
        userDao.persist(user);
    }

    public String getShortText(Message externalMessage) {
        String text = externalMessage.getShortText();
        if (text.length() > ShareService.MAX_MESSAGE_SIZE) {
            String url = externalMessage.getData().getExternalUrl();
            text = StringUtils.left(text, ShareService.MAX_MESSAGE_SIZE - url.length() - 4);
            text = WebUtils.trimTrailingUrl(text);
            text += "... " + url;
        }
        return text;
    }

    protected UserDao getUserDao() {
        return userDao;
    }

    @SqlTransactional
    public void importExternalMessages(User user, List<Message> messages) {
        for (Message message : messages) {
            cleanupInvalidCharacters(message);
            // (re)setting values that should not go in the database - they are used for only displaying external messages
            message.setAuthor(user);
            message.setScore(0);
            message.setReplies(0);
            message.setLiking(false);
            if (message.getOriginalMessage() != null) {
                message.setExternalOriginalMessageId(message.getOriginalMessage().getData().getExternalId());
                message.setOriginalMessage(null);
            }
            message.setPictureCount(0);
            message.setPictures(Collections.<Picture>emptyList());
            message.setAssociatedExternalIds(Collections.singletonList(message.getData().getExternalId()));
            message.setImported(true);
            message.setImportSource(getPrefix());
            if (message.getText().length() > Constants.MAX_MESSAGE_LENGTH) {
                message.setText(StringUtils.left(message.getText(), 230) + "... " + message.getData().getExternalUrl());
            }
            messageDao.persist(message);
        }
        incrementMessageCount(user.getId(), messages.size());
        if (!messages.isEmpty()) {
            setLastImportedTime(user.getId(), messages.iterator().next().getDateTime().getMillis() + 1000);
        }
    }

    private void cleanupInvalidCharacters(Message message) {
        // remove unicode surrogate characters which are not supported by myqsl
        String text = message.getText();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (!Character.isHighSurrogate(ch) && !Character.isLowSurrogate(ch)) {
                sb.append(ch);
            }
        }
        message.setText(sb.toString());
    }

    protected abstract void setLastImportedTime(String id, long millis);

    protected abstract String getPrefix();

}
