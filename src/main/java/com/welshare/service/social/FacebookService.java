package com.welshare.service.social;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeUtils;
import org.joda.time.Days;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.restfb.Connection;
import com.restfb.FacebookClient;
import com.restfb.LegacyFacebookClient;
import com.restfb.Parameter;
import com.restfb.exception.FacebookException;
import com.restfb.exception.FacebookGraphException;
import com.restfb.exception.FacebookNetworkException;
import com.restfb.types.FacebookType;
import com.restfb.types.Post;
import com.welshare.dao.UserDao;
import com.welshare.model.Message;
import com.welshare.model.NotificationEvent;
import com.welshare.model.User;
import com.welshare.model.social.FacebookSettings;
import com.welshare.model.social.SocialNetworkSettings;
import com.welshare.model.social.VideoData;
import com.welshare.service.MessageService.EvictHomeStreamCacheStringParam;
import com.welshare.service.ShareService.ResharingDetails;
import com.welshare.service.annotations.SqlReadonlyTransactional;
import com.welshare.service.annotations.SqlTransactional;
import com.welshare.service.enums.PictureSize;
import com.welshare.service.model.ExternalNotificationEvent;
import com.welshare.service.model.UserDetails;
import com.welshare.service.social.helper.FacebookHelper;
import com.welshare.service.social.helper.FacebookHelper.FqlUser;
import com.welshare.service.social.helper.FacebookHelper.StreamPost;
import com.welshare.service.social.model.FacebookStats;
import com.welshare.service.social.qualifiers.Facebook;
import com.welshare.util.Constants;
import com.welshare.util.RetryableOperation;
import com.welshare.util.WebUtils;
import com.welshare.util.collection.CollectionUtils;

/**
 * Important: stream is not fetched using the /me/home connnection, but using FQL:
 * http://developers.facebook.com/docs/reference/fql/stream/
 * @author bozho
 *
 */
@Service
@Facebook
@Order(1)
public class FacebookService implements SocialNetworkService {

    private static final String NEWS_FEED_QUERY = "SELECT post_id,app_id,source_id,updated_time,created_time,filter_key,attribution,actor_id,target_id,message,attachment,impressions,comments,likes,privacy,permalink,xid,tagged_ids,message_tags,description,description_tags FROM stream WHERE filter_key IN (SELECT filter_key FROM stream_filter WHERE uid=me() AND type='newsfeed') AND is_hidden = 0";

    private static final String LIMIT_PARAM = "limit";

    private static final String PICTURE_PARAM = "picture";

    private static final String LINK_PARAM = "link";

    private static final String MESSAGE_PARAM = "message";

    private static final String TYPE_PARAM = "type";

    private static final String OWN_FEED = "me/feed";

    private static final Logger logger = LoggerFactory.getLogger(FacebookService.class);

    private static final String LINKS = "me/links";

    @Inject
    private UserDao dao;

    @Inject
    private FacebookHelper helper;

    @Value("${messages.per.fetch}")
    private int messagesPerFetch;

    @Value("${base.url}")
    private String baseUrl;

    @Value("${facebook.retry.count}")
    private int retryCount;

    private ObjectMapper jsonMapper = new ObjectMapper();

    @Override
    @Async
    @SqlTransactional
    public void share(Message message, User user) {
        if (!isServiceEnabled(user)) {
            return;
        }
        final FacebookClient client = helper.getFacebookClient(user);
        try {
            String text = message.getTextWithPictureUrls();
            if (message.isLiking()) {
                text = WebUtils.formatLike(message.getOriginalMessage().getTextWithPictureUrls(),
                        message.getTextWithPictureUrls(),
                        message.getOriginalMessage().getAuthor().getNames(),
                        user.getProfile().getExternalLikeFormat());
            }

            final List<String> urls = WebUtils.extractUrls(text);
            FacebookType publishMessageResponse;

            NameConversionResult ncResult = welshareToFacebookNames(text, client);
            text = ncResult.getModifiedText();
            text = SocialUtils.trimSpecialSymbolElements(text);

            // Not yet used, waiting for facebook http://developers.facebook.com/bugs/296206447093532/?browse=search_4f17fd903ebf92d59040414
            Parameter tagsParam = null;
            try {
                String tags = jsonMapper.writeValueAsString(toMessageTags(text, ncResult.getNames()));
                if (tags != null) {
                    tagsParam = Parameter.with("tags", tags);
                }
            } catch (Exception ex) {
                logger.warn(ex.getMessage());
            }

            final String url;
            // remove the url from the message only if there is just 1 url,
            // and if it is at the end or at te beginning
            if (!urls.isEmpty()) {
                url = urls.get(0);
                if (urls.size() == 1 && (message.getText().endsWith(url) || message.getText().startsWith(url))) {
                    text = text.replace(url, "").trim();
                }
            } else {
                url = null;
            }

            // if there are urls, but no pictures
            if (!urls.isEmpty() && message.getPictureCount() == 0) {
                final VideoData videoData = helper.getVideoData(url);
                final String textParam = text;
                // if a regular link
                if (videoData == null ) {
                    publishMessageResponse = RetryableOperation.create(new Callable<FacebookType>() {
                        @Override
                        public FacebookType call() throws Exception {
                            return client.publish(LINKS,
                                    FacebookType.class,
                                    Parameter.with(MESSAGE_PARAM, textParam),
                                    Parameter.with(LINK_PARAM, url),
                                    Parameter.with(TYPE_PARAM, LINK_PARAM),
                                    Parameter.with(PICTURE_PARAM, getPictureUrl(url)));
                        }
                    }).retry(retryCount, FacebookException.class);
                } else {
                    // if a video, set the source and picture
                    publishMessageResponse = RetryableOperation.create(new Callable<FacebookType>() {
                        @Override
                        public FacebookType call() throws Exception {
                            return client.publish(LINKS,
                                FacebookType.class,
                                Parameter.with(MESSAGE_PARAM, textParam.isEmpty() ? url : textParam), // need to have some text due to a fb bug
                                Parameter.with(LINK_PARAM, url),
                                Parameter.with(PICTURE_PARAM, videoData.getPicture()),
                                Parameter.with(TYPE_PARAM, "video"),
                                Parameter.with("source", videoData.getPlayerUrl()));
                        }
                    }).retry(retryCount, FacebookException.class);
                }
            // if there are neither urls nor pictures
            } else if (message.getPictureCount() == 0) {
                final String textParam = text;
                publishMessageResponse = RetryableOperation.create(new Callable<FacebookType>() {
                    @Override
                    public FacebookType call() throws Exception {
                        return client.publish(OWN_FEED,
                                FacebookType.class,
                                Parameter.with(MESSAGE_PARAM, textParam));
                    }
                }).retry(retryCount, FacebookException.class);
            // if there are pictures
            } else {
                final String textParam = text;
                // picture URLs are scheme-agnostic, but facebook does not
                // recognize this, although it is a valid URL, hence http: is added
                final String picUrl = "http:" + WebUtils.addSuffix(message.getPictures().get(0)
                                .getPath(), PictureSize.LARGE.getSuffix());
                publishMessageResponse = RetryableOperation.create(new Callable<FacebookType>() {
                    @Override
                    public FacebookType call() throws Exception {
                        return client.publish(LINKS,
                            FacebookType.class,
                            Parameter.with(MESSAGE_PARAM, textParam.isEmpty() ? picUrl : textParam),
                            Parameter.with(TYPE_PARAM, LINK_PARAM),
                            Parameter.with(LINK_PARAM, picUrl),
                            Parameter.with(PICTURE_PARAM, picUrl));
                    }
                }).retry(retryCount, FacebookException.class);
            }

            helper.addToAssociatedMessages(message, publishMessageResponse.getId());
        } catch (FacebookException e) {
            handleException("Problem with sharing message on facebook", e, user);
        }
    }


    private Object toMessageTags(String text, List<String> names) {
        // TODO Auto-generated method stub
        return null;
    }


    private NameConversionResult welshareToFacebookNames(String text, FacebookClient client) throws FacebookException {
        List<String> usernames = WebUtils.extractMentionedUsernames(text);
        List<String> names = Lists.newArrayList();
        for (String username : usernames) {
            User wsUser = dao.getByUsername(username);
            if (wsUser != null && StringUtils.isNotEmpty(wsUser.getFacebookSettings().getUserId())) {
                com.restfb.types.User fbUser = client.fetchObject(wsUser
                        .getFacebookSettings().getUserId(),
                        com.restfb.types.User.class);
                String name = StringUtils.trimToEmpty(fbUser.getFirstName()) + " "
                            + StringUtils.trimToEmpty(fbUser.getLastName());
                names.add(name);
                text = text.replace("@" + username, name);
            }
        }
        NameConversionResult result = new NameConversionResult();
        result.setNames(names);
        result.setModifiedText(text);
        return result;
    }

    private class NameConversionResult {
        private List<String> names;
        private String modifiedText;
        public List<String> getNames() {
            return names;
        }
        public void setNames(List<String> names) {
            this.names = names;
        }
        public String getModifiedText() {
            return modifiedText;
        }
        public void setModifiedText(String modifiedText) {
            this.modifiedText = modifiedText;
        }
    }

    private Object getPictureUrl(String url) {
        String result = helper.getPictureUrl(url);

        if (result != null) {
            return result;
        }

        return "";
    }

    @Override
    @Async
    public void like(String originalMessageId, ResharingDetails details, User user) {
        if (!isServiceEnabled(user)) {
            return;
        }
        FacebookClient client = helper.getFacebookClient(user);

        String fbId = getFacebookId(originalMessageId);

        try {
            client.publish("/" + fbId + "/likes", Boolean.class);

            // if it should not be reshared, but there's a comment, add it as reply
            if (!details.isShareAndLike()) {
                Message msg = new Message();
                msg.setText(details.getComment());
                msg.setAuthor(user);
                reply(originalMessageId, msg);
            }
        } catch (FacebookException e) {
            handleException("Problem with liking a message on facebook", e, user);
        }
    }


    @Override
    @Async
    @SqlTransactional
    public void reply(String originalMessageId, Message message) {
        if (!isServiceEnabled(message.getAuthor())) {
            return;
        }
        final FacebookClient client = helper.getFacebookClient(message.getAuthor());

        final String fbId = getFacebookId(originalMessageId);

        try {
            String text = message.getText();

            // trimming usernames. That's for the cases when the two users are
            // both on welshare and facebook, and one replies to the other on
            // welshare. This will include a username in the reply, which should
            // not be shown on facebook

            text = trimUsernames(text);
            text = SocialUtils.trimSpecialSymbolElements(text);

            final String textParam = text;
            FacebookType result = RetryableOperation.create(new Callable<FacebookType>() {
                @Override
                public FacebookType call() throws Exception {
                    return client.publish("/" + fbId + "/comments", FacebookType.class,
                            Parameter.with(MESSAGE_PARAM, textParam));
                }
            }).retry(retryCount, FacebookException.class);

            helper.addToAssociatedMessages(message, result.getId());
        } catch (FacebookException e) {
            handleException("Problem with replying to a message on facebook", e, message.getAuthor());
        }
    }

    private String trimUsernames(String text) {
        List<String> usernames = WebUtils.extractMentionedUsernames(text);
        for (String username : usernames) {
            text = text.replace("@" + username, "");
        }
        return text;
    }


    @Override
    @Async
    public Future<List<Message>> getMessages(Message lastMessage, User user) {
        DateTime dateTime = null;
        if (lastMessage != null) {
            dateTime = lastMessage.getDateTime();
        }
        return getNewsFeed(user, dateTime, false);
    }

    @Override
    public void delete(String externalMessageId, User user) {
        if (!isServiceEnabled(user)) {
            return;
        }
        FacebookClient client = helper.getFacebookClient(user);
        try {
            client.deleteObject(getFacebookId(externalMessageId));
        } catch (FacebookException ex) {
            handleException("Problem with delete message on facebook", ex, user);
        }
    }

    @Override
    public List<Message> getUserMessages(Message lastMessage, User user) {
        try {
            return getMessages(user, lastMessage, OWN_FEED).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Future<List<Message>> getMessages(User user, Message lastMessage, String connection) {
        if (!isServiceEnabled(user)) {
            return SocialUtils.emptyFutureList();
        }
        FacebookClient client = helper.getFacebookClient(user);

        try {
            Parameter[] params;
            if (lastMessage != null) {
                params = new Parameter[2];
                params[1] = Parameter.with("until", lastMessage.getDateTime()
                        .getMillis() / DateTimeConstants.MILLIS_PER_SECOND - 1);
            } else {
                params = new Parameter[1];
            }
            params[0] = Parameter.with(LIMIT_PARAM, messagesPerFetch);
            Connection<Post> con = client.fetchConnection(connection, Post.class, params);
            List<Post> result = con.getData();

            if (result.isEmpty()) {
                return SocialUtils.emptyFutureList();
            }

            List<Message> messages = helper.postsToMessages(result,
                    user.getFacebookSettings().isFetchImages(),
                    user.getFacebookSettings().getUserId(), client);

            return SocialUtils.wrapMessageList(messages);
        } catch (FacebookException ex) {
            handleException("Problem with getting messages from facebook", ex, user);
            return SocialUtils.emptyFutureList();
        }
    }

    private Future<List<Message>> getNewsFeed(User user, DateTime lastMessageDateTime, boolean background) {
        if (!isServiceEnabled(user)) {
            return SocialUtils.emptyFutureList();
        }

        FacebookClient client = null;
        if (background) {
            client = helper.getBackgroundFacebookClient(user);
        } else {
            client = helper.getFacebookClient(user);
        }
        try {
            String untilClause = "";
            if (lastMessageDateTime != null) {
                untilClause = " AND created_time < "
                        + (lastMessageDateTime.getMillis() / DateTimeConstants.MILLIS_PER_SECOND - 1);
            }

            List<StreamPost> feed = client.executeFqlQuery(NEWS_FEED_QUERY + untilClause + " LIMIT " + messagesPerFetch, StreamPost.class);

            List<Message> messages = helper.streamPostsToMessages(feed,
                    user.getFacebookSettings().isFetchImages(),
                    user.getFacebookSettings().getUserId(), client);

            return SocialUtils.wrapMessageList(messages);
        } catch (FacebookException ex) {
            handleException("Problem with getting news feed from facebook", ex, user);
            return SocialUtils.emptyFutureList();
        }
    }

    @Override
    public List<Message> getReplies(String originalMessageId, User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }
        final FacebookClient client = helper.getFacebookClient(user);

        try {
            final String fbId = getFacebookId(originalMessageId);

            RetryableOperation<Connection<Post>> retryable = RetryableOperation.create(new Callable<Connection<Post>>() {
                @Override
                public Connection<Post> call() throws Exception {
                    Connection<Post> con = client.fetchConnection(fbId + "/comments", Post.class, Parameter.with(LIMIT_PARAM, 200));
                    return con;
                }
            });
            Connection<Post> con = retryable.retry(retryCount, FacebookException.class);

            List<Post> result = con.getData();
            List<Message> messages = helper.postsToMessages(result,
                    user.getFacebookSettings().isFetchImages(),
                    user.getFacebookSettings().getUserId(), client, false);

            for (Message message : messages) {
                message.setExternalOriginalMessageId(originalMessageId);
            }
            return messages;
        } catch (FacebookException ex) {
            handleException("Problem with getting replies from facebook", ex, user);
            return Collections.emptyList();
        }
    }

    @Override
    @SqlTransactional
    @EvictHomeStreamCacheStringParam
    public User storeSettings(SocialNetworkSettings settings, String userId) {
        if (settings instanceof FacebookSettings) {
            User user = dao.getById(User.class, userId, true);
            FacebookSettings fbSettings = (FacebookSettings) settings;
            // fetch the facebook user id
            if (fbSettings.getUserId() == null) {
                FacebookClient client = helper.getFacebookClient(fbSettings);
                try {
                    com.restfb.types.User current = client.fetchObject("me", com.restfb.types.User.class);
                    fbSettings.setUserId(current.getId());
                } catch (FacebookException e) {
                    handleException("Problem getting the id of the current user", e, user);
                }
            }
            user.setFacebookSettings(fbSettings);
            dao.persist(user);

            return user;
        }

        return null;
    }

    @Override
    public boolean shouldHandle(String messageId) {
        if (messageId != null && messageId.startsWith(FacebookHelper.PUBLIC_ID_PREFIX)) {
            return true;
        }
        return false;
    }

    @Override
    @SqlReadonlyTransactional
    public User getInternalUserByExternalId(String externalUserId) {
        return dao.getByPropertyValue(User.class, "facebookSettings.userId", externalUserId);
    }

    @Override
    @Cacheable(value="singleExternalMessageCache", key="#externalMessageId + '-' + #user?.id")
    public Message getMessageByExternalId(String externalMessageId, User user) {
        if (!isServiceEnabled(user)) {
            return null;
        }

        FacebookClient client = helper.getFacebookClient(user);
        try {
            Post post = client.fetchObject(getFacebookId(externalMessageId), Post.class);
            return helper.postToMessage(post, user.getFacebookSettings()
                    .isFetchImages(), user.getFacebookSettings().getUserId(), client);

        } catch (FacebookException ex) {
            handleException("Problem with getting a message from facebook", ex, user);
            return null;
        }
    }

    @Override
    public List<Message> getIncomingMessages(Message lastMessage, User user) {

        if (lastMessage == null) {
            return Collections.emptyList();
        }

        return getIncomingMessages(lastMessage.getDateTime().getMillis(), user);

    }


    private List<Message> getIncomingMessages(long lastMessageMillis, User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }
        try {
            FacebookClient client = helper.getBackgroundFacebookClient(user);

            List<StreamPost> feed = client.executeFqlQuery(NEWS_FEED_QUERY + " AND created_time > "
                    + (lastMessageMillis / DateTimeConstants.MILLIS_PER_SECOND + 1) + " LIMIT 20",
                    StreamPost.class);

            List<Message> messages = helper.streamPostsToMessages(feed,
                    user.getFacebookSettings().isFetchImages(),
                    user.getFacebookSettings().getUserId(), client);

            return messages;
        } catch (FacebookException e) {
            //do nothing, will re-attempt on next run
            handleException("Problem retrieving incoming facebook messages", e, user);
            return Collections.emptyList();
        }
    }

    private String getFacebookId(String externalId) {
        String id = externalId.replace(FacebookHelper.PUBLIC_ID_PREFIX, "");
        //only the last number is the id, the rest is auxiliary and stopped working
        //id = id.substring(id.lastIndexOf('_') + 1);
        return id;
    }

    @Override
    @SqlReadonlyTransactional
    public List<UserDetails> getFriends(User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }

        FacebookClient client = helper.getFacebookClient(user);
        try {
            Connection<com.restfb.types.User> friends = client.fetchConnection("me/friends", com.restfb.types.User.class);
            List<com.restfb.types.User> friendsList = friends.getData();
            List<UserDetails> userDetails = new ArrayList<UserDetails>(friendsList.size());
            for (com.restfb.types.User friend : friendsList) {
                User wsUser = dao.getByPropertyValue(User.class, "facebookSettings.userId", friend.getId());
                if (wsUser != null) {
                    UserDetails details = new UserDetails(wsUser);
                    userDetails.add(details);
                }
            }

            logger.debug("Fetched " + userDetails.size() + " friends from facebook that are registered on welshare");
            return userDetails;

        } catch (FacebookException e) {
            handleException("Problem getting facebook friends", e, user);
            return Collections.emptyList();
        }
    }

    @Override
    public void publishInitialMessage(User user) {
        if (!isServiceEnabled(user)) {
            return;
        }

        FacebookClient client = helper.getFacebookClient(user);
        try {
            //TODO i18nize
            client.publish(OWN_FEED,
                    FacebookType.class,
                    Parameter.with(MESSAGE_PARAM, "I am using Welshare!"),
                    Parameter.with(LINK_PARAM, "http://welshare.com"));
        } catch (FacebookException e) {
            handleException("Problem sharing initial message to facebook", e, user);
        }
    }

    @Override
    public List<NotificationEvent> getUnreadNotifications(User user) {
       if (!isServiceEnabled(user)) {
           return Collections.emptyList();
       }

        return getNotifications(user, 0, user.getFacebookSettings()
                .getLastReadNotificationTimestamp(), 200, false);
    }

    @Override
    public List<NotificationEvent> getNotifications(NotificationEvent maxEvent, int count, User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }

        return getNotifications(user,
                maxEvent == null ? 0 : maxEvent.getDateTime().getMillis(), 0,
                count, true);

    }

    private List<NotificationEvent> getNotifications(User user,
            long maxTimeMillis, long lastTimeMillis, int count, boolean includeRead) {

        FacebookClient client = helper.getBackgroundFacebookClient(user);

        try {
            StringBuilder query = new StringBuilder("SELECT notification_id, sender_id, title_html, title_text, body_html, href, object_id, updated_time FROM notification WHERE recipient_id=").append(user.getFacebookSettings().getUserId());
            query.append(" AND is_unread=").append(includeRead ? "0" : "1").append(" AND is_hidden=0");
            if (maxTimeMillis > 0) {
                query.append(" AND created_time<=").append(maxTimeMillis / DateTimeConstants.MILLIS_PER_SECOND + 1);
            } else {
                // start_time can be 0
                query.append(" AND created_time>=").append(lastTimeMillis / DateTimeConstants.MILLIS_PER_SECOND + 1);
            }
            query.append(" LIMIT ").append(count);
            List<Notification> notifications = client.executeFqlQuery(query.toString(), Notification.class);

            List<NotificationEvent> events = new ArrayList<NotificationEvent>(notifications.size());
            Set<String> userIds = new HashSet<String>(notifications.size());
            for (Notification n : notifications) {
                if (n.getTitleHtml() != null && n.getTitleHtml().contains("http://apps.facebook.com/")) {
                    continue; // app notifications are skipped
                }
                ExternalNotificationEvent ne = new ExternalNotificationEvent();
                ne.setDateTime(new DateTime(n.getUpdatedTime() * DateTimeConstants.MILLIS_PER_SECOND));
                ne.setExternalMessageId(FacebookHelper.PUBLIC_ID_PREFIX + n.getObjectId());

                if (n.getTitleText() != null
                        && (n.getTitleText().contains("commented")
                        || n.getTitleText().contains("tagged")
                        || n.getTitleText().contains("like"))
                        && !n.getTitleHtml().contains("photo")) {
                    ne.setShowInternally(true);
                } else {
                    ne.setShowInternally(false);
                }
                ne.setHref(n.getHref());
                ne.setRecipient(user);
                ne.setTextMessage(n.getTitleText());
                ne.setExternalNotificationId(n.getNotificationId());
                ne.setExternalSiteName("facebook");

                User tempUser = new User();
                tempUser.setExternalId(n.getSenderId());
                ne.setSender(tempUser);

                events.add(ne);

                userIds.add("uid=" + n.getSenderId());
            }

            Map<String, User> users = helper.getUsersData(helper.getFacebookClient(user), userIds);

            for (NotificationEvent ne : events) {
                ne.setSender(users.get(ne.getSender().getExternalId()));
            }
            return events;
        } catch (FacebookException ex) {
            handleException("Problem getting facebook notifications", ex, user);
            return Collections.emptyList();
        }
    }

    @Override
    @SqlTransactional
    public void markNotificationsAsRead(User user) {
        if (!isServiceEnabled(user)) {
            return;
        }
        user = dao.getById(User.class, user.getId(), true);
        user.getFacebookSettings().setLastReadNotificationTimestamp(
                DateTimeUtils.currentTimeMillis());
        dao.persist(user);

        LegacyFacebookClient client = helper.getLegacyClient(user.getFacebookSettings().getToken());
        try {
            List<NotificationEvent> events = getUnreadNotifications(user);
            if (events.isEmpty()) {
                return;
            }
            List<String> ids = Lists.newArrayListWithCapacity(events.size());
            for (NotificationEvent event : events) {
                ids.add(((ExternalNotificationEvent) event).getExternalNotificationId());
            }
            client.execute("notifications.markRead", Parameter.with("notification_ids", ids));
        } catch (FacebookException ex) {
            handleException("Error marking messages as read", ex, user);
        }
    }

    @Override
    public List<User> getLikers(String externalMessageId, User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }

        String facebookId = getFacebookId(externalMessageId);

        FacebookClient client = helper.getFacebookClient(user);

        try {
            Connection<com.restfb.types.User> conn = client.fetchConnection(facebookId + "/likes", com.restfb.types.User.class);
            List<com.restfb.types.User> data = conn.getData();

            List<User> users = new ArrayList<User>(data.size());
            for (com.restfb.types.User fbLiker : data) {
                User liker = new User();
                helper.fillUserData(liker, fbLiker);
                users.add(liker);
            }
            return users;
        } catch (FacebookException e) {
            handleException("Problem getting likers of a message with id " + facebookId, e, user);
            return Collections.emptyList();
        }
    }

    @Override
    @SqlTransactional
    @EvictHomeStreamCacheStringParam
    public User disconnect(String userId) {
        User user = dao.getById(User.class, userId, true);
        if (user == null || user.getFacebookSettings() == null) {
            return null;
        }

        // clear the settings
        user.getFacebookSettings().setFetchMessages(false);
        user.getFacebookSettings().setToken(null);
        user.getFacebookSettings().setUserId(null);

        return dao.persist(user);
    }

    @Override
    public void unlike(String originalMessageId, User user) {
        if (!isServiceEnabled(user)) {
            return;
        }
        String fbId = getFacebookId(originalMessageId);

        FacebookClient client = helper.getFacebookClient(user);

        try {
            client.deleteObject("/" + fbId + "/likes");
        } catch (FacebookException e) {
            handleException("Error removing like on message " + originalMessageId, e, user);
        }
    }

    @Override
    public String getUserId(String username, User user) {
        if (!isServiceEnabled(user)) {
            return null;
        }

        FacebookClient client = helper.getFacebookClient(user);

        try {
            List<FqlUser> result = client.executeFqlQuery("SELECT uid FROM user WHERE uid='" + username + "' OR username='" + username + "'", FqlUser.class);
            if (result.isEmpty()) {
                return null;
            } else {
                return result.get(0).getId();
            }
        } catch (FacebookException ex) {
            handleException("Error finding user by username: " + username, ex, user);
            return null;
        }
    }

    public static class Notification {
        @com.restfb.Facebook(value="notification_id")
        private String notificationId;

        @com.restfb.Facebook(value="sender_id")
        private String senderId;

        @com.restfb.Facebook(value="recipient_id")
        private String recipientId;

        @com.restfb.Facebook(value="created_time")
        private Long createdTime;

        @com.restfb.Facebook(value="updated_time")
        private Long updatedTime;

        @com.restfb.Facebook(value="title_html")
        private String titleHtml;

        @com.restfb.Facebook(value="title_text")
        private String titleText;

        @com.restfb.Facebook(value="body_html")
        private String bodyHtml;

        @com.restfb.Facebook(value="body_text")
        private String bodyText;

        @com.restfb.Facebook(value="href")
        private String href;

        @com.restfb.Facebook(value="app_id")
        private String appId;

        @com.restfb.Facebook(value="is_unread")
        private Integer unread;

        @com.restfb.Facebook(value="is_hidden")
        private Integer hidden;

        @com.restfb.Facebook(value="object_id")
        private String objectId;

        @com.restfb.Facebook(value="object_Type")
        private String objectType;

        public String getNotificationId() {
            return notificationId;
        }

        public void setNotificationId(String notificationId) {
            this.notificationId = notificationId;
        }

        public String getSenderId() {
            return senderId;
        }

        public void setSenderId(String senderId) {
            this.senderId = senderId;
        }

        public String getRecipientId() {
            return recipientId;
        }

        public void setRecipientId(String recipientId) {
            this.recipientId = recipientId;
        }

        public Long getCreatedTime() {
            return createdTime;
        }

        public void setCreatedTime(Long createdTime) {
            this.createdTime = createdTime;
        }

        public Long getUpdatedTime() {
            return updatedTime;
        }

        public void setUpdatedTime(Long updatedTime) {
            this.updatedTime = updatedTime;
        }

        public String getTitleHtml() {
            return titleHtml;
        }

        public void setTitleHtml(String titleHtml) {
            this.titleHtml = titleHtml;
        }

        public String getTitleText() {
            return titleText;
        }

        public void setTitleText(String titleText) {
            this.titleText = titleText;
        }

        public String getBodyHtml() {
            return bodyHtml;
        }

        public void setBodyHtml(String bodyHtml) {
            this.bodyHtml = bodyHtml;
        }

        public String getBodyText() {
            return bodyText;
        }

        public void setBodyText(String bodyText) {
            this.bodyText = bodyText;
        }

        public String getHref() {
            return href;
        }

        public void setHref(String href) {
            this.href = href;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public Integer isUnread() {
            return unread;
        }

        public void setUnread(Integer unread) {
            this.unread = unread;
        }

        public Integer isHidden() {
            return hidden;
        }

        public void setHidden(Integer hidden) {
            this.hidden = hidden;
        }

        public String getObjectId() {
            return objectId;
        }

        public void setObjectId(String objectId) {
            this.objectId = objectId;
        }

        public String getObjectType() {
            return objectType;
        }

        public void setObjectType(String objectType) {
            this.objectType = objectType;
        }
    }


    @Override
    public String getUserDisplayName(User author) {
        String name = author.getNames();
        if (StringUtils.isEmpty(name)) {
            name = author.getUsername();
            if (StringUtils.isEmpty(name)) {
                name = "unknown";
            }
        }
        return name;
    }

    @Override
    @Cacheable(value="statisticsCache", key="'fb' + #user.id")
    public FacebookStats getStats(User user) {
        if (!isServiceEnabled(user)) {
            return null;
        }
        FacebookClient client = helper.getBackgroundFacebookClient(user);

        try {

            FacebookStats stats = new FacebookStats();

            Connection<Post> con = client.fetchConnection(OWN_FEED, Post.class, Parameter.with(LIMIT_PARAM, 200));
            List<Post> posts = con.getData();

            if (posts.isEmpty()) {
                return stats;
            }

            LinkedList<Post> linkedList = new LinkedList<Post>(posts);
            Iterator<Post> iterator = linkedList.descendingIterator();

            Multiset<DateMidnight> postsData = LinkedHashMultiset.create();
            Multiset<DateMidnight> likesData = LinkedHashMultiset.create();
            Multiset<DateMidnight> commentsData = LinkedHashMultiset.create();

            Post currentPost = iterator.next();
            DateMidnight current = new DateMidnight(currentPost.getCreatedTime());
            DateMidnight start = current;

            while (iterator.hasNext() || currentPost != null) {
                DateMidnight msgTime = new DateMidnight(currentPost.getCreatedTime());
                if (current.equals(msgTime)) {
                    postsData.add(current);
                    likesData.add(current, currentPost.getLikesCount() == null ? 0
                                    : currentPost.getLikesCount().intValue());

                    if (currentPost.getComments() != null && currentPost.getComments().getCount() != null) {
                        commentsData.add(current, currentPost.getComments().getCount().intValue());
                    }

                    if (iterator.hasNext()) {
                        currentPost = iterator.next();
                    } else {
                        currentPost = null;
                    }
                } else {
                    current = current.plusDays(1);
                }
            }
            DateMidnight end = current;

            // represent only the last 30 days
            if (Days.daysBetween(start, end).getDays() > 30) {
                start = end.minusDays(30);
            }

            for (DateMidnight dm = start; !dm.isAfter(end); dm = dm.plusDays(1)) {
                stats.getPosts().put(dm, postsData.count(dm));
                stats.getComments().put(dm, commentsData.count(dm));
                stats.getLikes().put(dm, likesData.count(dm));
            }


            int days = Days.daysBetween(start, end).getDays();
            if (days == 0) {
                return stats; //no further calculation
            }

            int[] postsMaxAndSum = CollectionUtils.getMaxAndSum(stats.getPosts());
            stats.setMaxPosts(postsMaxAndSum[0]);
            stats.setAveragePosts(postsMaxAndSum[1] / days);

            int[] likesMaxAndSum = CollectionUtils.getMaxAndSum(stats.getLikes());
            stats.setMaxLikes(likesMaxAndSum[0]);
            stats.setAverageLikes(likesMaxAndSum[1] / days);

            int[] commentsMaxAndSum = CollectionUtils.getMaxAndSum(stats.getComments());
            stats.setMaxComments(commentsMaxAndSum[0]);
            stats.setAverageComments(commentsMaxAndSum[1] / days);

            stats.setMaxCount(NumberUtils.max(
                    stats.getMaxPosts(),
                    stats.getMaxComments(),
                    stats.getMaxLikes()));

            return stats;
        } catch (FacebookException e) {
            handleException("Problem fetching statistics", e, user);
            return null;
        }
    }

    @Override
    public String getIdPrefix() {
        return FacebookHelper.PUBLIC_ID_PREFIX;
    }

    private void handleException(String message, FacebookException ex, User user) {
        SocialNetworkStatusHolder.addStatus("facebookProblem");

        // well, technically, this is not a best practice,
        // but thus a lot of code duplication is avoided.
        // Also, checking for a string in the exception message
        // is not good, but that seems the only way to determine the exact exception
        if (ex instanceof FacebookNetworkException) {
            logger.warn(message + ": " + ex.getMessage() + " : " + ExceptionUtils.getRootCauseMessage(ex));
        } else if (ex instanceof FacebookGraphException) {
            if (ex.getMessage() != null) {
                String errorMessage = ex.getMessage().toLowerCase();
                // if the token is not valid, or the user has revoked a
                // permission (arbitrarily chosen manage_notifications),
                // disconnect
                if (errorMessage.contains("validating access token") || errorMessage.contains("manage_notifications")) {
                    // resetting facebook token
                    SocialNetworkStatusHolder.addStatus("facebookTokenProblem");
                    logger.info("Access token problem with user " + user + ". Resetting token. The problem is due to exception: " + ex.getMessage());
                    user.getFacebookSettings().setDisconnectReason(errorMessage);
                    helper.forciblyDisconnect(this, user);
                    return;
                }

                if (errorMessage.contains("#803") // object missing
                        || errorMessage.contains("#200") // object not accessible
                        || errorMessage.contains("#210") // user not accessible
                        || errorMessage.contains("#100") // invalid parameter - given when delete is attempted on non-existent status
                        || errorMessage.contains("#506") // duplicate status
                        || errorMessage.contains("Unsupported delete request")) { // missing target object for deletion
                    // ignore, do nothing, log in debug only
                    logger.debug(message + ": " + ex.getMessage() + " : " + ExceptionUtils.getRootCauseMessage(ex), " | User is " + user);
                    return;
                }

                if (errorMessage.contains("#341")) {
                    logger.info("Facebook post limit reached by user: " + user);
                    return;
                }
            }
            logger.warn(message + " | User is " + user, ex);
        } else {
            logger.warn(message + " | User is " + user, ex);
        }
    }


    @Override
    public String getExternalUsername(User user) {
        return null; //no username in facebook
    }


    @Override
    public String getShortText(Message externalMessage, User user) {
        if (!isServiceEnabled(user)) {
            return externalMessage.getText();
        }

        return helper.getShortText(externalMessage);
    }

    @Override
    public List<Message> getMissedIncomingMessages(User user) {
        return getIncomingMessages(user.getLastLogout(), user);
    }

    @Override
    public boolean shouldShareLikes(User user) {
        return user.getFacebookSettings().isShareLikes();
    }

    @Override
    public boolean isServiceEnabled(User user) {
        return user != null && user.getFacebookSettings() != null
                && user.getFacebookSettings().getToken() != null
                && user.getFacebookSettings().isFetchMessages();
    }

    @Override
    public UserDetails getUserDetails(String externalUserId, User currentUser) {
        if (!isServiceEnabled(currentUser)) {
            return null;
        }

        FacebookClient client = helper.getFacebookClient(currentUser);

        try {
            com.restfb.types.User fbUser = client.fetchObject(getFacebookId(externalUserId), com.restfb.types.User.class);
            if (fbUser == null) {
                throw new FacebookGraphException("0", "null facebook user returned", 500);
            }
            User user = new User();
            helper.fillUserData(user, fbUser);
            UserDetails details = new UserDetails(user);
            FqlUser result = client.executeFqlQuery("SELECT friend_count FROM user WHERE uid=me()", FqlUser.class).get(0);
            details.setFollowers(result.getFriendCount());
            return details;
        } catch (FacebookException ex) {
            handleException("Problem getting facebook user", ex, currentUser);
            return null;
        }
    }

    @Override
    public boolean isFriendWithCurrentUser(String externalUserId,
            User currentUser) {
        if (!isServiceEnabled(currentUser)) {
            return false;
        }

        FacebookClient client = helper.getFacebookClient(currentUser);
        try {
            com.restfb.types.User friend = client.fetchObject(
                    currentUser.getFacebookSettings().getUserId() + "/friends/"
                        + getFacebookId(externalUserId),
                    com.restfb.types.User.class);

            return friend != null;
        } catch (FacebookException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("#110")) {
                //TODO this is a page, handle pages as well
                return true;
            } else {
                handleException("Problem getting friends of user " + externalUserId, ex, currentUser);
            }
        }
        return false;
    }

    @Override
    public List<Message> getMessagesOfUser(String externalId, User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }

        FacebookClient client = helper.getFacebookClient(user);

        try {
            Connection<Post> posts = client.fetchConnection(getFacebookId(externalId) + "/feed", Post.class);
            return helper.postsToMessages(posts.getData(), true, user.getFacebookSettings().getUserId(), client, false);
        } catch (FacebookException ex) {
            handleException("Problem getting friends of user " + externalId, ex, user);
            return Collections.emptyList();
        }
    }


    @Override
    public void favourite(String messageId, User user) {
        // no notion of 'favourites' on facebook. TODO: consider sending message to self
    }


    @Override
    public void follow(String externalUserId, User user) {
        // facebook does not allow friending through the API
    }


    @Override
    public List<Message> getYesterdayMessages(User user) {
        return getIncomingMessages(new DateTime().minusDays(1).getMillis(), user);
    }


    @Override
    @Async
    @SqlTransactional
    public void edit(Message editedMessage, User user) {
        if (!isServiceEnabled(user)) {
            return;
        }

        String externalMessageId = null;
        for (String externalId : editedMessage.getAssociatedExternalIds()) {
            if (shouldHandle(externalId)) {
                externalMessageId = externalId;
                break;
            }
        }
        editedMessage.getAssociatedExternalIds().remove(externalMessageId);

        // don't delete & repost if there are replies or retweets already or the
        // message is more than a couple of minutes old
        Message externalMessage = getMessageByExternalId(externalMessageId, user);
        if (externalMessage.getScore() > 0 || externalMessage.getReplies() > 0) {
            return;
        }
        if (externalMessage.getDateTime().isBefore(new DateTime().minusMinutes(10))) {
            return;
        }
        delete(externalMessageId, user);
        share(editedMessage, user);
    }


    @Override
    @Async
    public Future<List<Message>> getMessages(User user) {
        return getMessages(null, user);
    }


    @Override
    public List<Message> getTopRecentMessages(User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }

        FacebookClient client = helper.getFacebookClient(user);

        try {
            Connection<Post> con = client.fetchConnection(OWN_FEED, Post.class, Parameter.with(LIMIT_PARAM, 100));
            List<Post> posts = new ArrayList<Post>(con.getData());
            for (Iterator<Post> it = posts.iterator(); it.hasNext();) {
                Post post = it.next();
                if (!((post.getLikes() != null && post.getLikes().getCount() != null && post.getLikes().getCount().longValue() > 0)
                        || (post.getLikesCount() != null && post.getLikesCount().longValue() > 0))) {
                    it.remove();
                }
            }

            List<Message> messages = helper.postsToMessages(posts, true, user.getFacebookSettings().getUserId(), client, false);
            return messages;
        } catch (FacebookException ex) {
            handleException("Problem fetching recent top own messages", ex, user);
            return Collections.emptyList();
        }
    }


    @Override
    public int calculateReputation(User user, DateTime since) {
        if (!isServiceEnabled(user)) {
            return -1;
        }

        final FacebookClient client = helper.getBackgroundFacebookClient(user);

        int reputation = 0;

        if (since != null) {
            try {
                Connection<Post> con = client.fetchConnection(OWN_FEED, Post.class,
                        Parameter.with(LIMIT_PARAM, 100),
                        Parameter.with("since", since.getMillis()));
                reputation += calculateReputation(con);
            } catch (FacebookException ex) {
                handleException("Problem calculating facebook reputation for user " + user, ex, user);
            }
        } else {
            for (int page = 0; page < 30; page++) {
                // if the user was disconnected when handling an exception, break
                if (!user.getFacebookSettings().isFetchMessages()) {
                    break;
                }
                try {
                    final int currentPage = page;
                    RetryableOperation<Connection<Post>> retryable = RetryableOperation.create(new Callable<Connection<Post>>() {
                        @Override
                        public Connection<Post> call() throws Exception {
                            Connection<Post> con = client.fetchConnection(OWN_FEED, Post.class,
                                    Parameter.with(LIMIT_PARAM, 100),
                                    Parameter.with("offset", currentPage));
                            return con;
                        }
                    });
                    Connection<Post> con = retryable.retry(retryCount, FacebookException.class);

                    if (con.getData().isEmpty()) {
                        break;
                    }
                    reputation += calculateReputation(con);

                } catch (FacebookException ex) {
                    handleException("Problem calculating facebook reputation for user " + user, ex, user);
                }
            }
        }

        return reputation;
    }


    private int calculateReputation(Connection<Post> con) {
        int reputation = 0;
        for (Post post : con.getData()) {
            // likes weigh less than retweets and welshare likes, because they don't mean "share"
            if (post.getLikesCount() != null) {
                reputation += post.getLikesCount().intValue() * (Constants.LIKE_SCORE - 2);
            } else if (post.getLikes() != null && post.getLikes().getCount() != null) {
                reputation += post.getLikes().getCount().intValue() * (Constants.LIKE_SCORE - 2);
            }

            if (post.getComments() != null && post.getComments().getCount() != null) {
                int commentCount = post.getComments().getCount().intValue();
                commentCount = Math.min(commentCount, 7); //limit to 7 comments; not taking long discussion into account
                reputation +=  commentCount * Constants.REPLY_SCORE;
            }
        }
        return reputation;
    }


    @Override
    public void importMessages(User user) {
        if (!isServiceEnabled(user) || !user.getFacebookSettings().isImportMessages()) {
            return;
        }
        try {
            FacebookClient client = helper.getBackgroundFacebookClient(user);

            Parameter[] params = new Parameter[2];
            params[0] = Parameter.with(LIMIT_PARAM, 100);
            params[1] = Parameter.with("since", user.getFacebookSettings().getLastImportedMessageTime() / DateTimeConstants.MILLIS_PER_SECOND + 1);

            Connection<Post> con = client.fetchConnection(OWN_FEED, Post.class, params);
            List<Post> posts = con.getData();

            if (posts.size() == 0) {
                return;
            }

            List<Message> messages = helper.postsToMessages(posts,
                    user.getFacebookSettings().isFetchImages(),
                    user.getFacebookSettings().getUserId(), null);

            helper.importExternalMessages(user, messages);

        } catch (FacebookException e) {
            handleException("Problem importing facebook messages for user " + user , e, user);
        }

    }


    @Override
    public int getCurrentlyActiveReaders(User user) {
        if (!isServiceEnabled(user)) {
            return 0;
        }
        try {
            DateTime tenMinutesAgo = new DateTime().minusMinutes(10);
            List<Message> messages = getIncomingMessages(tenMinutesAgo.getMillis(), user);
            Set<String> uniqueUserIds = Sets.newHashSet();
            for (Message message : messages) {
                // distinguish real people from pages by the birth date or gender field. (probably there's a better way though)
                if (message.getAuthor().getProfile().getBirthDate() != null || message.getAuthor().getProfile().getGender() != null) {
                    uniqueUserIds.add(message.getAuthor().getExternalId());
                }
            }
            return uniqueUserIds.size();
        } catch (Exception e) {
            logger.warn("Problem getting currently active facebook friends", e);
            return 0;
        }
    }


    @Override
    public void reshare(String originalMessageId, String comment, User user) {

        if (!isServiceEnabled(user)) {
            return;
        }
        FacebookClient client = helper.getFacebookClient(user);

        String fbId = getFacebookId(originalMessageId);

        try {
            Post post = client.fetchObject(fbId, Post.class);
            Message message = helper.postToMessage(post, false, null, client);
            message.setAuthor(user);
            String commentPrefix = "";
            if (StringUtils.isNotBlank(comment)) {
                commentPrefix = comment + ": ";
            }
            message.setText(commentPrefix + message.getShortText() + " (via " + post.getFrom().getName() + ")");
            share(message, user);
        } catch (FacebookException ex) {
            handleException("Failed to reshare a message on FB", ex, user);
        }
    }


    @Override
    public void fillMessageAnalyticsData(List<ExternalMessageAnalyticsData> list, User user) {
        if (!isServiceEnabled(user)) {
            return;
        }
        FacebookClient client = helper.getFacebookClient(user);
        List<String> ids = Lists.newArrayListWithCapacity(list.size());

        try {
            for (ExternalMessageAnalyticsData data : list) {
                ids.add(getFacebookId(data.getExternalMessageId()));
            }
            MultiplePosts posts = client.fetchObjects(ids, MultiplePosts.class);
            for (Post post : posts.getPosts()) {
                for (ExternalMessageAnalyticsData data : list) {
                    data.setScore(post.getLikesCount().intValue());
                }
            }
        } catch (FacebookException ex) {
            handleException("Problem getting facebook analytics data", ex, user);
        }
    }

    static class MultiplePosts {
        @Facebook
        private List<Post> posts;

        public List<Post> getPosts() {
            return posts;
        }

        public void setPosts(List<Post> posts) {
            this.posts = posts;
        }
    }

    @Override
    public UserDetails getCurrentUserDetails(User user) {
        if (!isServiceEnabled(user)) {
            return null;
        }
        return getUserDetails(getIdPrefix() + user.getFacebookSettings().getUserId(), user);

    }


    @Override
    public void disconnectDeletedUsers(List<User> users) {
        for (User user : users) {
            if (!isServiceEnabled(user)) {
                continue;
            }
            FacebookClient client = helper.getFacebookClient(user);
            try {
                com.restfb.types.User fbUser = client.fetchObject(user.getFacebookSettings().getUserId(), com.restfb.types.User.class);
                if (fbUser == null) {
                    //TODO verify this and uncomment
                    //disconnect(user.getId());
                }
            } catch (FacebookGraphException e) {
                //disconnect(user.getId());
                //TODO verify this and uncomment
            } catch (FacebookException e) {
                // ignore the rest
            }
        }
    }


    @Override
    public List<String> getFollowerIds(User user) {
        return Collections.emptyList(); //TODO get friends and subscribers
    }


    @Override
    public List<UserDetails> getUserDetails(List<String> ids, User user) {
        return Collections.emptyList();
    }
}