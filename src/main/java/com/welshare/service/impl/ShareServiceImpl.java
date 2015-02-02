package com.welshare.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.welshare.dao.Dao;
import com.welshare.dao.MessageDao;
import com.welshare.dao.UserDao;
import com.welshare.model.LikeAction;
import com.welshare.model.LikeActionPK;
import com.welshare.model.Message;
import com.welshare.model.Picture;
import com.welshare.model.ScheduledMessage;
import com.welshare.model.ShortUrl;
import com.welshare.model.ShortenedLinkVisitData;
import com.welshare.model.Tag;
import com.welshare.model.User;
import com.welshare.model.enums.NotificationType;
import com.welshare.service.FollowingService;
import com.welshare.service.LikeResult;
import com.welshare.service.MessageService.EvictHomeStreamCacheStringParam;
import com.welshare.service.NotificationEventService;
import com.welshare.service.PictureService;
import com.welshare.service.ShareService;
import com.welshare.service.UrlShorteningService;
import com.welshare.service.ViralLinkService;
import com.welshare.service.annotations.SqlReadonlyTransactional;
import com.welshare.service.annotations.SqlTransactional;
import com.welshare.service.impl.shortening.DefaultUrlShorteningService.DefaultUrlShortener;
import com.welshare.service.social.SocialNetworkService;
import com.welshare.service.social.SocialUtils;
import com.welshare.util.Constants;
import com.welshare.util.GeneralUtils;
import com.welshare.util.WebUtils;

@Service
public class ShareServiceImpl extends BaseServiceImpl implements ShareService {

    private static final Logger logger = LoggerFactory.getLogger(ShareServiceImpl.class);
    private static final Joiner JOINER = Joiner.on(',');

    @Inject
    private MessageDao messageDao;

    @Inject
    private UserDao userDao;

    @Inject
    private FollowingService followingService;

    @Inject
    private List<SocialNetworkService> socialNetworkServices;

    @Inject
    private NotificationEventService notificationEventService;

    @Inject
    private PictureService pictureService;

    @Inject
    private List<UrlShorteningService> urlShorteners;

    @Inject @DefaultUrlShortener
    private UrlShorteningService welshareShortener;

    @Inject
    private ViralLinkService viralService;

    @Inject
    private PriorityQueue<ScheduledMessage> scheduledMessagesQueue;

    @Inject
    private CacheManager cacheManager;

    @Value("${base.url}")
    private String baseUrl;

    @PostConstruct
    public void init() {
        Collections.sort(urlShorteners, AnnotationAwareOrderComparator.INSTANCE);
        urlShorteners = Collections.unmodifiableList(urlShorteners);
        socialNetworkServices = Collections.unmodifiableList(socialNetworkServices);
    }

    @Override
    @EvictHomeStreamCacheStringParam
    @SqlTransactional
    public Message share(String messageText, String userId, List<String> pictureUrls,
            List<String> externalSites, List<String> hideFromUsernames, boolean hideFromCloseFriends) {

        Validate.notNull(userId);
        User user = getDao().getById(User.class, userId);

        // do not shorten URLs here - they were already shortened in the UI

        Message message = doShare(messageText, null, user, pictureUrls, hideFromUsernames, hideFromCloseFriends);
        createMentionNotifications(message);

        // social network services may add additional information to the message
        // but they will update it

        // asynchronous calls to social networks
        for (SocialNetworkService sns : socialNetworkServices) {
            if (externalSites.contains(sns.getIdPrefix())) {
                sns.share(message, user);
            }
        }

        return message;
    }

    @Override
    @SqlTransactional
    public Message reply(String message, String originalMessageId, String userId) {
        SocialNetworkService socialNetworkService = SocialUtils.getSocialNetworkService(socialNetworkServices, originalMessageId);

        Validate.notNull(userId);
        User user = getDao().getById(User.class, userId);

        Message result;

        // if this message is internal to the system
        // i.e. not belonging to an external social network
        if (socialNetworkService == null) {

            Message inReplyTo = get(Message.class, originalMessageId);
            Message originalMessage = getRootOriginalMessage(originalMessageId);

            // increase the score of the original author (but not the message)
            if (!originalMessage.getAuthor().equals(user)) {
                getDao().lock(originalMessage.getAuthor());
                originalMessage.getAuthor().setScore(originalMessage.getAuthor().getScore() + Constants.REPLY_SCORE);
            }

            result = doShare(message, originalMessageId, user, null, Collections.<String>emptyList(), false);

            getDao().lock(originalMessage);
            originalMessage.setReplies(originalMessage.getReplies() + 1);
            save(originalMessage);

            if (!user.getId().equals(originalMessage.getAuthor().getId())) {
                notificationEventService.createEvent(originalMessage, NotificationType.REPLY, user);
            }
            createMentionNotifications(result);

            // if this message was shared on other networks, try to reply there as well
            for (String externalId : inReplyTo.getAssociatedExternalIds()) {
                for (SocialNetworkService sns : socialNetworkServices) {
                    if (sns.shouldHandle(externalId)) {
                        sns.reply(externalId, result);
                    }
                }
            }
        } else {
            // if the original message is external, share as a new msg
            result = doShare(message, null, user, null, Collections.<String>emptyList(), false);
            result.setExternalOriginalMessageId(originalMessageId);

            socialNetworkService.reply(originalMessageId, result);
        }


        return result;
    }

    @Override
    @SqlTransactional
    @EvictHomeStreamCacheStringParam
    public LikeResult reshare(String messageId, ResharingDetails details, String userId) {

        Validate.notNull(userId);
        User user = getDao().getById(User.class, userId);

        SocialNetworkService socialNetworkService = SocialUtils.getSocialNetworkService(socialNetworkServices, messageId);

        Message reshareMessage = new Message();
        reshareMessage.setAuthor(user);
        reshareMessage.setDateTime(new DateTime());

        List<SocialNetworkService> processedServices = new ArrayList<SocialNetworkService>();
        List<String> originalUrls = Collections.emptyList();
        Message msg = null;

        // don't set for external message, i.e. non-existent in the system
        if (socialNetworkService == null) {
            msg = internalLike(messageId, details, user, reshareMessage, processedServices);
            if (msg == null) {
                return LikeResult.EMPTY;
            }
            originalUrls = WebUtils.extractUrls(msg.getText());
        } else {
            // the user might have chosen not to like it on the originating network
            if (details.getExternalSites().contains(socialNetworkService.getIdPrefix())) {
                socialNetworkService.like(messageId, details, user);
            }
            if (details.getExternalSites().contains(socialNetworkService.getIdPrefix()) && details.isShareAndLike()) {
                socialNetworkService.reshare(messageId, details.getComment(), user);
            }

            Message externalMessage = handleMessageObjectsForExternalLike(messageId,
                    details.getComment(), details.getEditedResharedMessage(), user,
                    socialNetworkService, reshareMessage);
            if (externalMessage == null) {
                return LikeResult.EMPTY;
            }
            originalUrls = WebUtils.extractUrls(externalMessage.getText());
        }

        if (msg != null) {
            notificationEventService.createEvent(msg, NotificationType.LIKE, user);
        }

        if (details.isReshareInternally()) {
            handleViralLinks(reshareMessage, originalUrls);

            reshareMessage = doShare(reshareMessage);
            if (msg != null) { // if it's an internal message
                createMentionNotifications(reshareMessage);
            }
        }
        // now share on all external networks, except those already handled above,
        // and except the originating one (if there's such)
        // (whenever a message was spread to other networks, and the current
        // user likes it on welshare, it is automatically liked on other
        // networks as well, if possible. On those that this didn't happen,
        // share as a new message)
        // Also take into account the user selection for externalSites -
        // if he doesn't want to share it to a given site it will not be in the list

        for (SocialNetworkService sns : socialNetworkServices) {
            if (sns != socialNetworkService
                    && details.getExternalSites().contains(sns.getIdPrefix())
                    && !processedServices.contains(sns)) {
                sns.share(reshareMessage, user);
            }
        }

        return new LikeResult(details.isReshareInternally() ? reshareMessage : null, msg == null ? 0 : msg.getScore());
    }

    @Override
    @SqlTransactional
    public void simpleLike(String messageId, String userId) {
        User user = userDao.getById(User.class, userId);
        SocialNetworkService socialNetworkService = SocialUtils.getSocialNetworkService(socialNetworkServices, messageId);
        if (socialNetworkService == null) {
            Message reshareMessage = new Message();
            reshareMessage.setAuthor(user);
            reshareMessage.setDateTime(new DateTime());
            internalLike(messageId, ResharingDetails.EMPTY, user, reshareMessage, Lists.<SocialNetworkService>newArrayList());
        } else {
            socialNetworkService.like(messageId, ResharingDetails.EMPTY, user);
        }
    }

    /**
     * Modifies the passed "reshareMessage" and returns the liked message.
     * @param messageId
     * @param comment
     * @param user
     * @param socialNetworkService
     * @param reshareMessage
     * @return
     */
    private Message handleMessageObjectsForExternalLike(String messageId, String comment, String editedMessageText,
            User user, SocialNetworkService socialNetworkService, Message reshareMessage) {

        reshareMessage.setExternalOriginalMessageId(messageId);
        reshareMessage.setExternalLike(true);

        // adding the external message to the list of associated messages
        reshareMessage.getAssociatedExternalIds().add(socialNetworkService.getIdPrefix() + messageId);

        Message externalMessage = socialNetworkService.getMessageByExternalId(messageId, user);
        if (externalMessage == null) {
            return null; // the message can't be obtained
        }
        if (StringUtils.isNotBlank(editedMessageText)) {
            externalMessage.setShortText(editedMessageText);
        }

        // get rid of all @usernames for the reshareMessage
        String shortText = WebUtils.trimUsernames(externalMessage.getShortText());
        reshareMessage.setText(WebUtils.formatLike(shortText, comment,
                socialNetworkService.getUserDisplayName(externalMessage.getAuthor()),
                user.getProfile().getExternalLikeFormat()));

        if (reshareMessage.getText().length() > ShareService.MAX_MESSAGE_SIZE) {
            reshareMessage.setText(socialNetworkService.getShortText(externalMessage, user));
        }
        return externalMessage;
    }

    private Message internalLike(String messageId, ResharingDetails details, User user,
            Message reshareMessage, List<SocialNetworkService> processedServices) {
        reshareMessage.setText(details.getComment());
        reshareMessage.setLiking(true);

        // locking the entity so that the score update is consistent
        // TODO reconsider when there is more traffic and locks
        // are likely to cause major delays
        Message msg = getDao().getById(Message.class, messageId, true);
        if (msg == null) {
            return null;
        }

        if (msg.getOriginalMessage() != null) {
            msg = msg.getOriginalMessage();
            getDao().lock(msg);
        }
        reshareMessage.setOriginalMessage(msg);

        LikeActionPK actionPK = new LikeActionPK();
        actionPK.setMessage(msg);
        actionPK.setUser(user);

        // in case this user has already liked this message, simply return
        // null, which means no change
        LikeAction existing = messageDao.getById(LikeAction.class, actionPK);
        if (existing != null) {
            return null;
        }

        msg.setScore(msg.getScore() + 1); // just the likes count here

        save(msg);

        LikeAction action = new LikeAction();
        action.setPrimaryKey(actionPK);

        save(action);

        User originalAuthor = msg.getAuthor();
        getDao().lock(originalAuthor);
        originalAuthor.setScore(originalAuthor.getScore() + Constants.LIKE_SCORE);
        logger.debug("Increasing the score of " + originalAuthor + " with " + Constants.LIKE_SCORE);
        save(originalAuthor);


        // if this message was shared on other networks, try to like it there as well
        for (String externalId : msg.getAssociatedExternalIds()) {
            for (SocialNetworkService sns : socialNetworkServices) {
                if (sns.shouldHandle(externalId) && sns.isServiceEnabled(user)) {
                    sns.like(externalId, details, user);
                    if (details.isShareAndLike()) {
                        sns.reshare(externalId, details.getComment(), user);
                    }
                    processedServices.add(sns);
                }
            }
        }
        return msg;
    }

    private void handleViralLinks(Message message, List<String> originalUrls) {
        // loop all passed urls, checking if they are shortened by the welshare shortener
        // if they are, see if they are in the viral database, and if they are -
        // spawn new ones and replace the old ones with them in the message text
        for (String url : originalUrls) {
            if (welshareShortener.isShortened(url)) {
                ShortUrl shortUrl = viralService.followViralLink(url, message
                        .getAuthor().getId(), ShortenedLinkVisitData.EMPTY);
                if (shortUrl != null) {
                    message.setText(message.getText().replace(url, welshareShortener.getShortUrl(shortUrl.getKey())));
                }
            }
        }
    }

    private void createMentionNotifications(Message message) {
        String originalAuthorUsername = null;
        if (message.getOriginalMessage() != null) {
            originalAuthorUsername = message.getOriginalMessage().getAuthor().getUsername();
        }

        List<String> usernames = WebUtils.extractMentionedUsernames(message.getText());
        for (String username : usernames) {
            if (username == null || username.equals(originalAuthorUsername)) {
                continue;
            }
            User targetUser = userDao.getByUsername(username);
            if (targetUser != null) {
                notificationEventService.createMentionEvent(message, targetUser);
            }
        }
    }

    public Message doShare(Message message) {

        getDao().lock(message.getAuthor());
        message.getAuthor().incrementMessageCount();
        save(message.getAuthor());

        // saving first, so that an ID is generated
        message = save(message);

        return message;
    }

    private Message doShare(final String messageText, final String originalMessageId, User user,
            List<String> pictureFiles, List<String> hideFromUsernames, boolean hideFromCloseFriends) {
        Message message = new Message();

        if (originalMessageId != null && !originalMessageId.isEmpty()) {
            Message originalMessage = getRootOriginalMessage(originalMessageId);
            message.setOriginalMessage(originalMessage);
        }

        message.setText(messageText);
        message.setDateTime(new DateTime());
        message.setTags(parseTags(messageText));
        message.setAuthor(user);

        fillHiddenFrom(user, hideFromUsernames, hideFromCloseFriends, message);
        fillAddressee(message);
        fillPictures(pictureFiles, message);

        message = doShare(message);

        return message;
    }

    private void fillHiddenFrom(User user, List<String> hideFromUsernames, boolean hideFromCloseFriends,
            Message message) {
        List<User> hideFrom = new ArrayList<User>();
        if (hideFromCloseFriends) {
            hideFrom.addAll(followingService.getCloseFriends(user.getId()));
        }
        if (!CollectionUtils.isEmpty(hideFromUsernames)) {
            for (String username : hideFromUsernames) {
                hideFrom.add(userDao.getByUsername(username));
            }
        }
        message.setHiddenFrom(hideFrom);
    }

    private Message getRootOriginalMessage(String originalMessageId) {
        Message originalMessage= get(Message.class, originalMessageId);
        while (originalMessage.getOriginalMessage() != null) {
            originalMessage = originalMessage.getOriginalMessage();
        }

        return originalMessage;
    }

    private void fillPictures(List<String> pictureFiles, Message message) {
        if (!CollectionUtils.isEmpty(pictureFiles)) {
            pictureFiles = pictureService.makePermanent(pictureFiles);
            List<Picture> pictures = new ArrayList<Picture>(pictureFiles.size());

            //list holding the generated short keys for images so that no duplication is possible
            List<String> generatedKeys = new ArrayList<String>(pictureFiles.size());
            for (String pictureFile : pictureFiles) {
                Picture pic = new Picture();
                pic.setPath(pictureFile);
                pic.setUploader(message.getAuthor());

                String shortKey = GeneralUtils.generateShortKey();
                // repeat until an unused key is generated (check current list and db)
                while (generatedKeys.contains(shortKey) || getDao().getByPropertyValue(Picture.class, "shortKey", shortKey) != null) {
                    shortKey = GeneralUtils.generateShortKey();
                }
                generatedKeys.add(shortKey);

                String publicUrl = baseUrl + "/picture/" + shortKey;
                pic.setPublicUrl(publicUrl);
                pic.setShortKey(shortKey);

                pictures.add(pic);
            }
            message.setPictures(pictures);
            message.setPictureCount(pictures.size());
        }
    }

    private void fillAddressee(final Message message) {
        if (message.getText().startsWith("@") && !message.isReply()) {
            List<String> mentionedUsernames = WebUtils.extractMentionedUsernames(message.getText());
            if (!mentionedUsernames.isEmpty()) {
                User addressee = userDao.getByUsername(mentionedUsernames.get(0));
                message.setAddressee(addressee);
            }
        }
    }

    @Override
    @SqlTransactional
    public String shortenUrls(String messageText, String userId,
            boolean showTopBar, boolean trackViral) {

        Validate.notNull(userId);
        User user = getDao().getById(User.class, userId);

        // Attempt all shorteners, until one succeeds
        // This means if 3rd party shorteners fail, the default one will be used
        // and if it fails, the url will not be shortened
        List<String> urls = WebUtils.extractUrls(messageText);
        urlLoop:
        for (String url : urls) {
            for (UrlShorteningService urlShortener : urlShorteners) {
                if (urlShortener.isShortened(url)) {
                    continue urlLoop;
                }
            }
            for (UrlShorteningService urlShortener : urlShorteners) {
                String shortened = urlShortener.shortenUrl(url, user, showTopBar, trackViral);
                if (shortened != null) {
                    messageText = messageText.replace(url, shortened);
                    break;
                }
            }
        }

        return messageText;
    }

    @Override
    @SqlReadonlyTransactional
    public Set<Tag> parseTags(String messageText) {
        Set<Tag> tags = new HashSet<Tag>();

        List<String> tagNames = WebUtils.extractTags(messageText);

        for (String tagName : tagNames) {
            Tag tag = getDao().getByPropertyValue(Tag.class, "name", tagName);
            if (tag == null) {
                tag = new Tag();
                tag.setName(tagName);
            } else {
                getDao().lock(tag);
            }
            tag.setOccurrences(tag.getOccurrences() + 1);
            tags.add(tag);
        }

        return tags;
    }

    @Override
    protected Dao getDao() {
        return messageDao;
    }

    @Override
    @EvictHomeStreamCacheStringParam
    @SqlTransactional
    public String unlike(String messageId, String userId) {
        Validate.notNull(userId);
        User user = getDao().getById(User.class, userId);

        SocialNetworkService socialNetworkService = SocialUtils.getSocialNetworkService(socialNetworkServices, messageId);
        boolean isExternal = socialNetworkService != null;

        if (isExternal) {
            socialNetworkService.unlike(messageId, user);
            // delete the message generated on welshare when liking
            Message msg = getDao().getByPropertyValue(Message.class, "externalOriginalMessageId", messageId);
            if (msg != null) {
                delete(msg);
                return msg.getId();
            }
            return null;
        } else {
            Message message = getDao().getById(Message.class, messageId, true);
            Message ownMessage = getDao().getByPropertyValue(Message.class, "originalMessage", message);
            if (ownMessage != null) {
                messageDao.deleteNotifications(ownMessage.getId());
                getDao().delete(ownMessage);
            }
            LikeAction la = getDao().getById(LikeAction.class, new LikeActionPK(message, user));
            if (la != null) {
                delete(la);
            }

            message.setScore(message.getScore() - 1);
            save(message);

            message.getAuthor().setScore(message.getAuthor().getScore() - 1);
            save(message.getAuthor());

            return null;
        }
    }

    @Override
    @SqlTransactional
    public void schedule(String text, String userId, List<String> pictureFiles, List<String> externalSites,
            List<String> hideFromUsernames, boolean hideFromCloseFriends, DateTime scheduledTime) {

        // The DateTime is already in UTC, so no conversions

        if (scheduledTime.isBeforeNow()) {
            throw new IllegalArgumentException("scheduledTime must not be in the past. " + scheduledTime);
        }

        ScheduledMessage msg = new ScheduledMessage();

        // storing comma-separated (rather than as lists, which would make storing more complex (@ElementCollection for ex))
        msg.setHideFromUsernames(JOINER.join(hideFromUsernames));
        msg.setPictureUrls(JOINER.join(pictureFiles));
        msg.setExternalSites(JOINER.join(externalSites));
        msg.setUserId(userId);
        msg.setText(text);
        msg.setHideFromCloseFriends(hideFromCloseFriends);
        msg.setScheduledTime(scheduledTime);
        msg.setTimeOfScheduling(new DateTime());
        msg = save(msg);

        // also push the message to the queue if it is scheduled within one hour (+5 min)
        if (scheduledTime.isBefore(new DateTime().plusMinutes(65))) {
            scheduledMessagesQueue.add(msg);
        }
    }
}
