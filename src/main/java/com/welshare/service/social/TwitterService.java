package com.welshare.service.social;

import java.io.EOFException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import scala.actors.threadpool.Arrays;
import twitter4j.IDs;
import twitter4j.Paging;
import twitter4j.RelatedResults;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import com.google.common.base.Joiner;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.welshare.dao.UserDao;
import com.welshare.model.Message;
import com.welshare.model.NotificationEvent;
import com.welshare.model.User;
import com.welshare.model.enums.NotificationType;
import com.welshare.model.social.SocialNetworkSettings;
import com.welshare.model.social.TwitterSettings;
import com.welshare.service.MessageService.EvictHomeStreamCacheStringParam;
import com.welshare.service.ShareService.ResharingDetails;
import com.welshare.service.UrlShorteningService;
import com.welshare.service.annotations.SqlReadonlyTransactional;
import com.welshare.service.annotations.SqlTransactional;
import com.welshare.service.impl.shortening.DefaultUrlShorteningService.DefaultUrlShortener;
import com.welshare.service.model.ExternalNotificationEvent;
import com.welshare.service.model.UserDetails;
import com.welshare.service.social.helper.TwitterHelper;
import com.welshare.service.social.model.TwitterStats;
import com.welshare.util.Constants;
import com.welshare.util.WebUtils;
import com.welshare.util.collection.CollectionUtils;

@Service
@com.welshare.service.social.qualifiers.Twitter
@Order(0)
public class TwitterService implements SocialNetworkService {

    private static final int TWITTER_CHAR_LIMIT = 140;
    private static final int TWITTER_SHORTENER_URL_LENGTH = 20;

    private static final Logger logger = LoggerFactory
            .getLogger(TwitterService.class);

    private static final Comparator<Message> REPLY_TIME_COMPARATOR = new ReplyTimeComparator();

    @Inject
    private TwitterHelper helper;

    @Inject
    private UserDao dao;

    @Inject @DefaultUrlShortener
    private UrlShorteningService shortener;

    @Value("${messages.per.fetch}")
    private int messagesPerFetch;

    @Value("${base.url}")
    private String baseUrl;

    @Override
    @Async
    @SqlTransactional
    public void share(Message message, User user) {
        if (!isServiceEnabled(user)) {
            return;
        }

        Twitter t = helper.getTwitter(user);
        try {
            String messageText = getMessageText(message, user);
            messageText = welshareToTwitterUsernames(messageText);
            Status result = t.updateStatus(messageText);

            helper.addToAssociatedMessages(message, String.valueOf(result.getId()));
        } catch (TwitterException e) {
            handleException("Failed to share message on twitter", e, user);
        }

    }

    private String getMessageText(Message message, User user) {
        String messageText = message.getTextWithPictureUrls();
        if (message.isLiking()) {
            messageText = WebUtils.formatLike(message.getOriginalMessage().getTextWithPictureUrls(),
                    message.getTextWithPictureUrls(), message.getOriginalMessage().getAuthor().getNames(),
                    user.getProfile().getExternalLikeFormat());
        }

        // assume all urls are replaced with http://t.co/xxxxxxxx
        // (twitter does that automatically) and then compare to the char limit
        List<String> urls = WebUtils.extractUrls(messageText);
        int tweetLength = messageText.length();
        for (String url : urls) {
            tweetLength = tweetLength - (url.length() - TWITTER_SHORTENER_URL_LENGTH);
        }

        if (tweetLength > TWITTER_CHAR_LIMIT) {

            String url = baseUrl + "/message/" + message.getId();
            String shortUrl = shortener.shortenUrl(url, user);

            messageText = messageText.substring(0,
                    TWITTER_CHAR_LIMIT - shortUrl.length() - 3);

            messageText = WebUtils.trimTrailingUrl(messageText);
            messageText += ".. " + shortUrl;
        }

        return messageText;
    }

    @Override
    @Async
    public void like(String originalMessageId, ResharingDetails details, User user) {
        if (!isServiceEnabled(user)) {
            return;
        }

        long targetMsgId = helper.getTwitterId(originalMessageId);
        Twitter t = helper.getTwitter(user);

        try {
            if (StringUtils.isBlank(details.getComment()) && StringUtils.isBlank(details.getEditedResharedMessage())) {
                t.retweetStatus(targetMsgId);
            } else {
                Status s = t.showStatus(targetMsgId);
                if (s.isRetweet() && s.getRetweetedStatus() != null) {
                    s = s.getRetweetedStatus();
                }
                String text = s.getText();
                if (StringUtils.isNotBlank(details.getEditedResharedMessage())) {
                    text = details.getEditedResharedMessage();
                }
                String retweetText = details.getComment() + " RT @" + s.getUser().getScreenName() + ": " + text;
                // if longer than 140, simply retweet it, ignoring the comment
                if (retweetText.length() > TWITTER_CHAR_LIMIT) {
                    t.retweetStatus(targetMsgId);
                } else {
                    t.updateStatus(retweetText);
                }
            }
        } catch (TwitterException e) {
            handleException("Failed to like message on twitter", e, user);
        }
    }

    @Override
    @Async
    @SqlTransactional
    public void reply(String originalMessageId, Message message) {
        if (!isServiceEnabled(message.getAuthor())) {
            return;
        }

        long targetMsgId = helper.getTwitterId(originalMessageId);
        Twitter t = helper.getTwitter(message.getAuthor());

        String messageText = getMessageText(message, message.getAuthor());
        // if the message is reply to internal message, and this is an attempt
        // to reply to messages that were sent to twitter from welshare,
        // transform the usernames to twitter ones
        if (message.isReply()) {
            messageText = welshareToTwitterUsernames(messageText);
        }
        try {
            StatusUpdate update = new StatusUpdate(messageText);
            update.setInReplyToStatusId(targetMsgId);
            Status result = t.updateStatus(update);

            helper.addToAssociatedMessages(message, String.valueOf(result.getId()));
        } catch (TwitterException e) {
            handleException("Failed to reply to message on twitter", e, message.getAuthor());
        }
    }

    private String welshareToTwitterUsernames(String text) {
        List<String> usernames = WebUtils.extractMentionedUsernames(text);
        for (String username : usernames) {
            User wsUser = dao.getByUsername(username);
            if (wsUser != null) {
                String externalUsername = getExternalUsername(wsUser);
                if (externalUsername != null) {
                    text = text.replace("@" + username, "@" + externalUsername);
                } else {
                    // get rid of the @, otherwise the wrong user may be referenced in twitter
                    text = text.replace("@" + username, username);
                }
            }
        }

        return text;
    }

    @Override
    public void delete(String externalMessageId, User user) {
        if (!isServiceEnabled(user)) {
            return;
        }

        try {
            helper.getTwitter(user).destroyStatus(helper.getTwitterId(externalMessageId));
        } catch (TwitterException e) {
            handleException("Failed deleting message", e, user);
        }
    }

    @Override
    @SqlTransactional
    @EvictHomeStreamCacheStringParam
    public User storeSettings(SocialNetworkSettings settings, String userId) {
        if (settings instanceof TwitterSettings) {
            User user = dao.getById(User.class, userId, true);
            user.setTwitterSettings((TwitterSettings) settings);
            dao.persist(user);
            return user;
        }
        return null;
    }

    @Override
    @Async
    @SqlTransactional
    public Future<List<Message>> getMessages(Message lastMessage, User user) {

        if (!isServiceEnabled(user)) {
            return SocialUtils.emptyFutureList();
        }

        Twitter t = helper.getTwitter(user);
        try {
            Paging paging = new Paging();
            paging.setCount(messagesPerFetch + 2); // adding 2 because sometimes twitter returns less than the requested amount
            if (lastMessage != null) {
                paging.setMaxId(helper.getTwitterId(lastMessage.getData().getExternalId()));
                paging.setCount(messagesPerFetch + 3); // adding 3 (one more than 2 from above) because the reference message itself should not be displayed
            }
            List<Status> statuses = t.getHomeTimeline(paging);

            if (statuses.isEmpty()) {
                return SocialUtils.emptyFutureList();
            }

            List<Message> messages = helper.statusesToMessages(statuses,
                    user.getTwitterSettings().isFetchImages(), t,
                    user.getTwitterSettings().getUserId());

            // not duplicating the last item
            if (lastMessage != null) {
                messages = messages.subList(1, messages.size());
            }

            updateLastTweetId(messages, user);

            return SocialUtils.wrapMessageList(messages);
        } catch (TwitterException e) {
            handleException("Failed to obtain twitter timeline for user "
                            + user.getId(), e, user);

            return SocialUtils.emptyFutureList();
        }
    }


    @Override
    public boolean shouldHandle(String messageId) {
        if (messageId != null && messageId.startsWith(TwitterHelper.PUBLIC_ID_PREFIX)) {
            return true;
        }

        return false;
    }

    @Override
    public List<Message> getUserMessages(Message lastMessage, User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }
        Twitter t = helper.getTwitter(user);

        try {
            List<Status> statuses;
            if (lastMessage != null) {
                Paging paging = new Paging(helper.getTwitterId(lastMessage.getData().getExternalId()));
                statuses = t.getUserTimeline(paging);
            } else {
                statuses = t.getUserTimeline();
            }
            List<Message> messages = helper.statusesToMessages(statuses, user
                    .getTwitterSettings().isFetchImages(), null, 0);

            return messages;
        } catch (TwitterException e) {
            handleException("Failed to obtain user timeline for user "
                            + user.getId(), e, user);

            return Collections.emptyList();
        }
    }

    @Override
    @SqlReadonlyTransactional
    public User getInternalUserByExternalId(String externalUserId) {
        if (StringUtils.isNumeric(externalUserId)) {
            try {
                long longId = Long.parseLong(externalUserId);
                return dao.getByPropertyValue(User.class, "twitterSettings.userId", longId);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    @Override
    @Cacheable(value="singleExternalMessageCache", key="#externalMessageId")
    public Message getMessageByExternalId(String externalMessageId, User user) {
        if (!isServiceEnabled(user)) {
            return null;
        }
        Twitter t = helper.getTwitter(user);

        try {
           Status status = t.showStatus(helper.getTwitterId(externalMessageId));
            return helper.statusToMessage(status, user.getTwitterSettings()
                    .isFetchImages(), t, user.getTwitterSettings().getUserId());
        } catch (TwitterException e) {
            handleException("Failed to obtain single external message for id "
                    + externalMessageId, e, user);

            return null;
        }
    }

    @Override
    public List<Message> getReplies(String originalMessageId, User user) {

        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }

        Twitter t = helper.getTwitter(user);

        try {
            RelatedResults results = t.getRelatedResults(helper.getTwitterId(originalMessageId));
            List<Status> conversations = results.getTweetsWithConversation();
            Status originalStatus = t.showStatus(helper.getTwitterId(originalMessageId));
            if (conversations.isEmpty()) {
                conversations = results.getTweetsWithReply();
            }

            if (conversations.isEmpty()) {
                conversations = new ArrayList<Status>();
                Status status = originalStatus;
                while (status.getInReplyToStatusId() > 0) {
                    status = t.showStatus(status.getInReplyToStatusId());
                    conversations.add(status);
                }
            }
            // show the current message in the conversation, if there's such
            if (!conversations.isEmpty()) {
                conversations.add(originalStatus);
            }

            List<Message> messages = helper.statusesToMessages(conversations,
                    false, t, user.getTwitterSettings().getUserId(), false);

            Collections.sort(messages, REPLY_TIME_COMPARATOR);

            for (Message message : messages) {
                message.setExternalOriginalMessageId(originalMessageId);
            }
            return messages;
        } catch (TwitterException ex) {
            handleException("Failed getting replies", ex, user);
            return Collections.emptyList();
        }
    }

    @Override
    @SqlTransactional
    public List<Message> getIncomingMessages(Message lastMessage, User user) {

        if (lastMessage == null) {
            return Collections.emptyList();
        }

        List<Message> messages =
            getIncomingMessages(helper.getTwitterId(lastMessage.getData().getExternalId()), user);

        updateLastTweetId(messages, user);

        return messages;
    }

    private void updateLastTweetId(List<Message> messages, User user) {
        if (!messages.isEmpty()) {
            user = dao.getById(User.class, user.getId(), true);
            user.getTwitterSettings().setLastReceivedTweetId(
                    helper.getTwitterId(messages.iterator().next().getData().getExternalId()));
            dao.persist(user);
        }
    }

    @Override
    public List<Message> getMissedIncomingMessages(User user) {
        return getIncomingMessages(user.getTwitterSettings().getLastReceivedTweetId(), user);
    }

    private List<Message> getIncomingMessages(long lastMessageId, User user) {
        if (!isServiceEnabled(user) || lastMessageId <= 0) {
            return Collections.emptyList();
        }
        Twitter t = helper.getTwitter(user);

        Paging paging = new Paging();
        paging.setPage(1);
        paging.setCount(200); //configurable?
        paging.setSinceId(lastMessageId);

        try {
            List<Status> statuses = t.getHomeTimeline(paging);
            List<Message> messages = helper.statusesToMessages(statuses,
                    user.getTwitterSettings().isFetchImages(), t,
                    user.getTwitterSettings().getUserId());

            return messages;
        } catch (TwitterException ex) {
            handleException("Problem fetching incoming tweets", ex, user);
            // do nothing. Will re-attempt on next run
            return Collections.emptyList();
        }
    }

    @Override
    @SqlReadonlyTransactional
    public List<UserDetails> getFriends(User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }

        Twitter client = helper.getTwitter(user);
        try {
            IDs friendsIds = client.getFriendsIDs(-1);
            long[] friendsList = friendsIds.getIDs();
            List<UserDetails> userDetails = new ArrayList<UserDetails>(friendsList.length);
            for (long friendId : friendsList) {
                User wsUser = dao.getByPropertyValue(User.class, "twitterSettings.userId", friendId);
                if (wsUser != null) {
                    UserDetails details = new UserDetails(wsUser);
                    userDetails.add(details);
                }
            }
            logger.debug("Fetched " + userDetails.size() + " friends from twitter that are registered on welshare");
            return userDetails;

        } catch (TwitterException e) {
            handleException("Problem getting twitter friends", e, user);
            return Collections.emptyList();
        }
    }

    @Override
    public void publishInitialMessage(User user) {
        if (!isServiceEnabled(user)) {
            return;
        }

        Twitter client = helper.getTwitter(user);
        try {
            //TODO i18nize
            client.updateStatus("I am using Welshare! http://welshare.com");
        } catch (TwitterException e) {
            handleException("Problem sharing initial message to twitter", e, user);
        }
    }

    @Override
    public List<NotificationEvent> getUnreadNotifications(User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }

        return getNotifications(user, 0, new DateTime(user.getTwitterSettings().getLastReadNotificationTimestamp()), null, 200);
    }

    @Override
    public List<NotificationEvent> getNotifications(NotificationEvent maxEvent, int count, User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }

        long maxId = 0;
        if (maxEvent != null && maxEvent.getExternalMessageId() != null) {
            maxId = helper.getTwitterId(maxEvent.getExternalMessageId());
        }
        DateTime timestamp = null;
        if (maxEvent != null && maxEvent.getDateTime() != null) {
            timestamp = maxEvent.getDateTime();
        }
        return getNotifications(user, maxId, null, timestamp, count);
    }

    private List<NotificationEvent> getNotifications(User user, long maxId, DateTime lastRead, DateTime timestamp, int count) {

        //TODO make use of return http.get("https://api.twitter.com/i/activity/about_me.json?count=20&include_entities=1", auth);
        //which is still a private twitter API

        int pageSize = count;
        if (pageSize > 200) {
            pageSize = 200;
        }
        Paging paging = new Paging(1, pageSize);
        Twitter t = helper.getTwitter(user);

        if (maxId > 0) {
            paging.setMaxId(maxId);
        }

        try {
            List<Status> targetStatuses = new ArrayList<Status>();
            while (true) {
                List<Status> currentPage = t.getMentionsTimeline(paging);
                targetStatuses.addAll(currentPage);
                if (targetStatuses.size() >= count || currentPage.isEmpty()) {
                    break;
                }
                // time-limited
                if (timestamp != null && timestamp.isBefore(currentPage.get(currentPage.size() - 1).getCreatedAt().getTime())) {
                    break;
                }
                paging.setPage(paging.getPage() + 1);
            }

            List<Status> retweets = new ArrayList<Status>();
            paging = new Paging(1, pageSize);
            while (true) {
                List<Status> currentPage = t.getRetweetsOfMe(paging);
                retweets.addAll(currentPage);
                if (retweets.size() >= count || currentPage.isEmpty()) {
                    break;
                }
                // time-limited
                if (timestamp != null && timestamp.isBefore(currentPage.get(currentPage.size() - 1).getCreatedAt().getTime())) {
                    break;
                }
                paging.setPage(paging.getPage() + 1);
            }

            targetStatuses.addAll(retweets);

            List<NotificationEvent> events = Lists.newArrayListWithCapacity(targetStatuses.size());

            for (Status status : targetStatuses) {
                // if the current update is unread and we are looking for read updates, skip
                if (lastRead != null && new DateTime(status.getCreatedAt()).isBefore(lastRead)) {
                    continue;
                }

                ExternalNotificationEvent event = new ExternalNotificationEvent();
                event.setDateTime(new DateTime(status.getCreatedAt()));
                event.setExternalMessageId(TwitterHelper.PUBLIC_ID_PREFIX
                        + status.getId());

                event.setRead(false);
                event.setRecipient(user);

                boolean isReplyToCurrent = status.getInReplyToUserId() == user.getTwitterSettings().getUserId();
                boolean isRetweet = false;

                // twitter now gives retweets as separate status-updates, so we extract that
                if (status.getRetweetedStatus() != null) {
                    status = status.getRetweetedStatus();
                }

                if (!isReplyToCurrent) {
                    // if it is not a reply, and not a retweet - it's a mention.
                    // Also, if the author is not == the connected user,
                    // otherwise sometimes twitter gives 0 retweets, but it
                    // appears that the user has mentioned himself. Also, this eliminates self-mentions
                    if (status.getRetweetCount() == 0 && !status.isRetweet() && status.getUser().getId() != user.getTwitterSettings().getUserId()) {
                        event.setNotificationType(NotificationType.MENTION);
                    } else {
                        List<Status> rts = t.getRetweets(status.getId());
                        isRetweet = true;
                        List<String> retweeterNames = new ArrayList<String>(rts.size());
                        for (Status rt : rts) {
                            retweeterNames.add(rt.getUser().getName() + " (" + rt.getUser().getScreenName() + ")");
                        }
                        String message = "retweeted your message: "; //TODO i18n?
                        if (retweeterNames.size() == 1) {
                            message = " has " + message;
                        } else if (retweeterNames.size() > 1) {
                            message = " (" + retweeterNames.size() + ") have " + message;
                            event.setCount(retweeterNames.size());
                        } else if (retweeterNames.isEmpty() && status.isRetweet()) {
                            message = status.getUser().getName() + " has " + message;
                        } else if (retweeterNames.isEmpty()) {
                            message = "Someone has " + message;
                        }

                        String statusText = status.getText();
                        statusText = helper.getActualUrls(status, statusText).getShortModifiedText();

                        // don't set NotificationType. Use only the text message
                        event.setTextMessage(Joiner.on(", ").join(retweeterNames) + message + statusText);

                        //set the latest (first) retweeter as the sender (for visualization purposes)
                        User externalAuthor = new User();
                        if (!rts.isEmpty()) {
                            helper.fillUserData(externalAuthor, rts.iterator().next().getUser());
                        } else {
                            helper.fillUserData(externalAuthor, status.getUser());
                        }
                        event.setSender(externalAuthor);
                    }
                } else {
                    event.setNotificationType(NotificationType.REPLY);
                }

                if (!isRetweet) {
                    event.setSender(helper.getExternalAuthor(status));
                    event.setTextMessage(status.getText());
                }

                event.setHref("http://twitter.com/"
                        + status.getUser().getScreenName() + "/status/"
                        + status.getId());
                event.setExternalSiteName("twitter");
                event.setShowInternally(true);

                events.add(event);
            }

            return events;
        } catch (TwitterException e) {
            handleException("Problem getting notifications from twitter", e, user);
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
        List<NotificationEvent> notifications = getUnreadNotifications(user);
        Iterator<NotificationEvent> iterator = notifications.iterator();
        if (iterator.hasNext()) {
            NotificationEvent event = iterator.next();
            // TODO don't store retweets as last ids, so that newer retweets of the same msg can be tracked
            String lastId = event.getExternalMessageId();
            long lastTwitterId = helper.getTwitterId(lastId);
            user.getTwitterSettings().setLastReadNotificationId(lastTwitterId);
            user.getTwitterSettings().setLastReadNotificationTimestamp(event.getDateTime().getMillis());
            dao.persist(user);
        }
    }

    @Override
    public List<User> getLikers(String externalMessageId, User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }

        Twitter t = helper.getTwitter(user);
        long twitterId = helper.getTwitterId(externalMessageId);
        try {
            Status s = t.showStatus(twitterId);
            if (s.getRetweetedStatus() != null) {
                twitterId = s.getRetweetedStatus().getId();
            }
            Paging paging = new Paging();
            paging.setCount(20);
            List<Status> response = t.getRetweets(twitterId);
            List<User> likers = new ArrayList<User>(response.size());
            for (Status retweet : response) {
                User liker = new User();
                helper.fillUserData(liker, retweet.getUser());
                likers.add(liker);
            }

            return likers;
        } catch (TwitterException e) {
            handleException("Problem getting retweeters of message with id "
                    + twitterId, e, user);
            return Collections.emptyList();
        }

    }

    @Override
    @SqlTransactional
    @EvictHomeStreamCacheStringParam
    public User disconnect(String userId) {
        User user = dao.getById(User.class, userId, true);

        // clear the settings
        user.getTwitterSettings().setFetchMessages(false);
        user.getTwitterSettings().setToken(null);
        user.getTwitterSettings().setUserId(0);
        return dao.persist(user);
    }

    @Override
    public void unlike(String originalMessageId, User user) {
        if (!isServiceEnabled(user)) {
            return;
        }
        Twitter t = helper.getTwitter(user);
        try {
            ResponseList<Status> list = t.getRetweets(helper.getTwitterId(originalMessageId));
            for (Status s : list) {
                if (s.getUser().getId()  == user.getTwitterSettings().getUserId()) {
                    t.destroyStatus(s.getId());
                    break;
                }
            }
        } catch (TwitterException ex) {
            handleException("Problem undoing retweet of message with id " + originalMessageId, ex, user);
        }
    }

    @Override
    public String getUserId(String username, User user) {
        if (!isServiceEnabled(user)) {
            return null;
        }

        Twitter t = helper.getTwitter(user);

        try {
            twitter4j.User twUser = t.showUser(username);
            if (twUser != null) {
                return TwitterHelper.PUBLIC_ID_PREFIX + twUser.getId();
            } else {
                return null;
            }
        } catch (TwitterException ex) {
            // user not found, or other problem - return null
            if (ex.getRateLimitStatus() != null
                    && ex.getRateLimitStatus().getRemainingHits() > 0) {
                logger.info(ex.getMessage());
            }
            return null;
        }
    }

    @Override
    public String getUserDisplayName(User author) {
        return "@" + author.getUsername();
    }

    private void handleException(String string, TwitterException e, User user) {
        Throwable rootCause = ExceptionUtils.getRootCause(e);
        if ((e.getRateLimitStatus() == null || e.getRateLimitStatus().getRemainingHits() > 0) && e.getStatusCode() != 403) {
            if (e.isCausedByNetworkIssue()
                    || rootCause instanceof SocketTimeoutException
                    || rootCause instanceof EOFException
                    || (rootCause != null && StringUtils.equals(rootCause.getMessage(), "Premature EOF"))
                    || StringUtils.trimToEmpty(e.getMessage()).contains("Twitter servers are up, but overloaded with requests")) {
                logger.warn("Network issue when connecting to twitter: " + ExceptionUtils.getMessage(e.getCause()));
            } else if (e.getMessage() != null && e.getMessage().contains("No status found with that ID")) {
                //ignore - deleted tweet
            } else if (e.getMessage() != null && e.getMessage().contains("key/secret, access token/secret")) {
                user.getTwitterSettings().setDisconnectReason(StringUtils.left(e.getMessage(), 250));
                helper.forciblyDisconnect(this, user); // problem with token, disconnect user
            } else if (e.getStatusCode() == 404) {
                logger.warn("No resource found: " + e.getMessage() + " | " + string + " | User is " + user);
            } else if (e.getStatusCode() == 500) {
                logger.warn("Internal twitter error: " + e.getMessage() + " | " + string + " | User is " + user);
            } else {
                logger.warn(string + " | User is " + user, e);
            }
        } else if (e.getStatusCode() == 403) {
            logger.warn("Update limit exceeded: " + e.getMessage());
        } else {
            logger.warn("Rate limit exceeded");
        }

        SocialNetworkStatusHolder.addStatus("twitterProblem");
    }

    @Override
    public TwitterStats getStats(User user) {
        if (!isServiceEnabled(user)) {
            return null;
        }
        Twitter t = helper.getTwitter(user);

        try {

            Paging paging = new Paging();
            paging.setCount(200); // max allowed
            TwitterStats stats = new TwitterStats();
            List<Status> statuses = t.getUserTimeline(paging);
            if (statuses.isEmpty()) {
                return stats;
            }

            // start and end of displaying. Chronologically reverse order
            DateMidnight start = new DateMidnight(statuses.get(statuses.size() - 1).getCreatedAt());
            DateMidnight end = new DateMidnight(statuses.get(0).getCreatedAt());

            if (Days.daysBetween(start, end).getDays() > 30) {
                start = end.minusDays(30);
            }

            fillStats(statuses, stats.getTweets(), start, end);

            // Get mentions and retweets only as old as the last tweet
            paging.setSinceId(statuses.get(statuses.size() - 1).getId());
            fillStats(t.getMentions(paging), stats.getMentions(), start, end);
            fillStats(t.getRetweetsOfMe(paging), stats.getRetweets(), start, end);


            int days = Days.daysBetween(start, end).getDays();
            if (days == 0) {
                return stats; // no further calculation
            }

            int[] tweetsMaxAndSum = CollectionUtils.getMaxAndSum(stats.getTweets());
            stats.setMaxTweets(tweetsMaxAndSum[0]);
            stats.setAverageTweets(tweetsMaxAndSum[1] / days);

            int[] retweetsMaxAndSum = CollectionUtils.getMaxAndSum(stats.getRetweets());
            stats.setMaxRetweets(retweetsMaxAndSum[0]);
            stats.setAverageRetweets(retweetsMaxAndSum[1] / days);

            int[] mentionsMaxAndSum = CollectionUtils.getMaxAndSum(stats.getMentions());
            stats.setMaxMentions(mentionsMaxAndSum[0]);
            stats.setAverageMentions(mentionsMaxAndSum[1] / days);

            stats.setMaxCount(NumberUtils.max(
                    stats.getMaxTweets(),
                    stats.getMaxMentions(),
                    stats.getMaxRetweets()));

            return stats;
        } catch (TwitterException e) {
            handleException("Problem fetching statistics", e, user);
            return null;
        }
    }

    private void fillStats(List<Status> list, Map<DateMidnight, Integer> map, DateMidnight start, DateMidnight end) {
        LinkedList<Status> linkedList = new LinkedList<Status>(list);
        Iterator<Status> iterator = linkedList.descendingIterator();
        if (!iterator.hasNext()) {
            return;
        }

        Multiset<DateMidnight> data = LinkedHashMultiset.create();

        Status currentStatus = iterator.next();
        DateMidnight current = new DateMidnight(currentStatus.getCreatedAt());
        while (iterator.hasNext() || currentStatus != null) {
            DateMidnight msgTime = new DateMidnight(currentStatus.getCreatedAt());
            if (current.equals(msgTime)) {
                data.add(current);
                if (iterator.hasNext()) {
                    currentStatus = iterator.next();
                } else {
                    currentStatus = null;
                }
            } else {
                current = current.plusDays(1);
            }
        }

        for (DateMidnight dm = start; !dm.isAfter(end); dm = dm.plusDays(1)) {
            map.put(dm, data.count(dm));
        }
    }

    @Override
    public String getIdPrefix() {
        return TwitterHelper.PUBLIC_ID_PREFIX;
    }

    @Override
    public String getExternalUsername(User user) {
        if (!isServiceEnabled(user)) {
            return null;
        }

        try {
            return helper.getTwitter(user).showUser(user.getTwitterSettings().getUserId()).getScreenName();
        } catch (TwitterException ex) {
            handleException("Problem getting external username", ex, user);
            return null;
        } catch (Exception ex) {
            logger.warn("Problem getting external username" + ex.getMessage());
            return null;
        }
    }

    @Override
    public String getShortText(Message externalMessage, User user) {
        return externalMessage.getText(); //can't be longer than the limit of welshare
    }

    private static class ReplyTimeComparator implements Comparator<Message> {

        @Override
        public int compare(Message m1, Message m2) {
            // ascending
            return m1.getDateTime().compareTo(m2.getDateTime());
        }
    }

    @Override
    public boolean shouldShareLikes(User user) {
        return user.getTwitterSettings().isShareLikes();
    }

    @Override
    public boolean isServiceEnabled(User user) {
        return user != null && user.getTwitterSettings() != null
                && user.getTwitterSettings().getToken() != null
                && user.getTwitterSettings().isFetchMessages();
    }

    @Override
    public UserDetails getUserDetails(String externalUserId, User currentUser) {
        if (!isServiceEnabled(currentUser)) {
            return null;
        }

        Twitter t = helper.getTwitter(currentUser);
        long twitterId = helper.getTwitterId(externalUserId);
        try {
            User user = new User();
            twitter4j.User twUser = t.showUser(twitterId);
            helper.fillUserData(user, twUser);

            return new UserDetails(user);
        } catch (TwitterException e) {
            handleException("Problem getting user", e, currentUser);
            return null;
        }
    }

    @Override
    public boolean isFriendWithCurrentUser(String externalUserId,
            User currentUser) {
        if (!isServiceEnabled(currentUser)) {
            return false;
        }

        Twitter t = helper.getTwitter(currentUser);

        try {
            return !t.lookupFriendships(new long[]{helper.getTwitterId(externalUserId)}).isEmpty();
        } catch (TwitterException e) {
            handleException("Problem checking friendship", e, currentUser);
            return false;
        }
    }

    @Override
    public List<Message> getMessagesOfUser(String externalId, User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }

        Twitter t = helper.getTwitter(user);

        try {
            List<Status> tweets = t.getUserTimeline(helper.getTwitterId(externalId));
            return helper.statusesToMessages(tweets, user.getTwitterSettings().isFetchMessages(), null, user.getTwitterSettings().getUserId(), false);
        } catch (TwitterException e) {
            handleException("Problem getting messages of user " + externalId, e, user);
            return Collections.emptyList();
        }
    }

    @Override
    public void favourite(String messageId, User user) {
        if (!isServiceEnabled(user)) {
            return;
        }

        Twitter t = helper.getTwitter(user);

        try {
            t.createFavorite(helper.getTwitterId(messageId));
        } catch (TwitterException e) {
            handleException("Problem favouriting a message", e, user);
        }

    }

    @Override
    public void follow(String externalUserId, User user) {
        if (!isServiceEnabled(user)) {
            return;
        }

        Twitter t = helper.getTwitter(user);
        try {
            t.createFriendship(helper.getTwitterId(externalUserId));
        } catch (TwitterException e) {
            handleException("Problem following user " + externalUserId, e, user);
        }

    }

    @Override
    public List<Message> getYesterdayMessages(User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }
        Twitter t = helper.getTwitter(user);

        Paging paging = new Paging();
        paging.setCount(200);

        List<Message> messages = new ArrayList<Message>();
        for (int page = 1; page < 50; page++) {
            try {
                paging.setPage(page);

                List<Status> statuses = t.getHomeTimeline(paging);
                messages.addAll(helper.statusesToMessages(statuses,
                    user.getTwitterSettings().isFetchImages(), null,
                    user.getTwitterSettings().getUserId()));

                if (statuses.isEmpty() || messages.get(messages.size() - 1).getDateTime().isBefore(new DateTime().minusDays(1))) {
                    break;
                }

            } catch (TwitterException ex) {
                handleException("Problem fetching daily messages", ex, user);
                // simply skipping this iteration
            }
        }

        return messages;
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
        if (externalMessage.getScore() > 0) {
            return;
        }
        if (externalMessage.getDateTime().isBefore(new DateTime().minusMinutes(10))) {
            return;
        }
        if (getReplies(externalMessageId, user).size() > 0) {
            return;
        }
        delete(externalMessageId, user);
        share(editedMessage, user);
    }

    @Override
    @Async
    @SqlTransactional
    public Future<List<Message>> getMessages(User user) {
        return getMessages(null, user);
    }

    @Override
    public List<Message> getTopRecentMessages(User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }
        Twitter t = helper.getTwitter(user);
        try {
             Paging paging = new Paging();
             paging.setCount(100);
             List<Status> statuses = t.getRetweetsOfMe(paging);

             return helper.statusesToMessages(statuses, true, null, user.getTwitterSettings().getUserId(), false);
        } catch (TwitterException ex) {
            handleException("Problem getting top recent messages", ex, user);
            return Collections.emptyList();
        }
    }

    @Override
    public int calculateReputation(User user, DateTime since) {
        if (!isServiceEnabled(user)) {
            return -1;
        }
        Twitter t = helper.getTwitter(user);

        Paging paging = new Paging(200);
        int reputation = 0;
        for (int page = 1; page < 30; page++) {
            try {
                paging.setPage(page);
                ResponseList<Status> retweets = t.getRetweetsOfMe(paging);
                boolean breakAfter = false;
                for (Status status : retweets) {
                    reputation += status.getRetweetCount() * Constants.LIKE_SCORE;
                    if (since != null && since.isAfter(new DateTime(status.getCreatedAt()))) {
                        breakAfter = true;
                        break;
                    }
                }
                ResponseList<Status> mentions = t.getMentions(paging);
                for (Status status : mentions) {
                    if (status.getText().contains("RT")) {
                        reputation += Constants.LIKE_SCORE;
                    } else {
                        reputation += Constants.REPLY_SCORE;
                    }
                    if (since != null && since.isAfter(new DateTime(status.getCreatedAt()))) {
                        breakAfter = true;
                        break;
                    }
                }

                if (breakAfter || (mentions.isEmpty() && retweets.isEmpty())) {
                    break;
                }
            } catch (TwitterException ex) {
                handleException("Problem calculating twitter reputation on page " + page + " for user " + user, ex, user);
                if (ex.getRateLimitStatus() != null && ex.getRateLimitStatus().getRemainingHits() == 0) {
                    break;
                }
            }
        }
        return reputation;
    }

    @Override
    public void importMessages(User user) {
        if (!isServiceEnabled(user) || !user.getTwitterSettings().isImportMessages()) {
            return;
        }

        Twitter t = helper.getTwitter(user);

        Paging paging = new Paging();
        paging.setCount(200);

        DateTime since = new DateTime(user.getTwitterSettings().getLastImportedMessageTime());

        List<Message> messages = new ArrayList<Message>();
        mainLoop:
        for (int page = 1; page < 50; page++) {
            try {
                paging.setPage(page);

                List<Status> statuses = t.getUserTimeline(paging);
                for (Status status : statuses) {
                    if (new DateTime(status.getCreatedAt()).isBefore(since)) {
                        break mainLoop;
                    }
                    if (!status.getSource().contains(helper.getWelshareAppName())) {
                        Message msg = helper.statusToMessage(
                                status, false, null, user.getTwitterSettings().getUserId());
                        if (status.isRetweet()) {
                            msg.setText(msg.getShortText() + " (via @" + msg.getAuthor().getUsername() + ")");
                        }
                        // don't import retweets that were made through welshare
                        if (status.isRetweet() && dao.getByPropertyValue(Message.class, "externalOriginalMessageId",
                                        TwitterHelper.PUBLIC_ID_PREFIX + status.getRetweetedStatus().getId()) != null) {
                            continue;
                        }
                        messages.add(msg);
                    }
                }

                if (statuses.isEmpty()) {
                    break;
                }

            } catch (TwitterException ex) {
                handleException("Problem importing twitter messages for user " + user, ex, user);
                if (ex.getRateLimitStatus() != null && ex.getRateLimitStatus().getRemainingHits() == 0) {
                    return;
                }
                // simply skipping this iteration, unless this is a rate-limit issue
            }
        }

        helper.importExternalMessages(user, messages);
    }

    @Override
    @Cacheable(value="readerCountCache", key="#user.id") //caching in the generic stream cache
    public int getCurrentlyActiveReaders(User user) {
        if (!isServiceEnabled(user)) {
            return 0;
        }

        Twitter t = helper.getTwitter(user);

        try {
            // currently not supporting more than 5000 followers
            IDs ids = t.getFollowersIDs(-1);
            int online = 0;
            DateTime tenMinutesAgo = new DateTime().minusMinutes(10);

            for (int i = 0; i < ids.getIDs().length; i += 100) {
                List<twitter4j.User> followers = t.lookupUsers(Arrays.copyOfRange(ids.getIDs(), i, i + 99));
                for (twitter4j.User follower : followers) {
                    if (follower.getStatus() != null
                            && new DateTime(follower.getStatus().getCreatedAt()).isAfter(tenMinutesAgo)) {
                        online++;
                    }
                }
            }

            return online;
        } catch (TwitterException ex) {
            handleException("Problem counting online readers of user " + user, ex, user);
            return 0;
        }
    }

    @Override
    public void reshare(String messageId, String comment, User user) {
        // not supported
    }

    @Override
    public void fillMessageAnalyticsData(List<ExternalMessageAnalyticsData> list, User user) {
        if (!isServiceEnabled(user)) {
            return;
        }
        // twitter actually does not support batch lookup for statuses, so we get the latest retweets and fill the data based on that
        Twitter t = helper.getTwitter(user);

        Paging paging = new Paging();
        paging.count(list.size() * 2);
        try {
            List<Status> retweets = t.getRetweetsOfMe(paging);
            for (Status rt : retweets) {
                for (ExternalMessageAnalyticsData data : list) {
                    if (rt.getId() == helper.getTwitterId(data.getExternalMessageId())) {
                        data.setScore((int) rt.getRetweetCount());
                    }
                }
            }
        } catch (TwitterException e) {
            handleException("Problem filling twitter analytics data", e, user);
        }
    }

    @Override
    public UserDetails getCurrentUserDetails(User user) {
        if (!isServiceEnabled(user)) {
            return null;
        }
        return getUserDetails(getIdPrefix() + user.getTwitterSettings().getUserId(), user);
    }

    @Override
    public void disconnectDeletedUsers(List<User> users) {
        for (User user : users) {
            if (!isServiceEnabled(user)) {
                continue;
            }
            Twitter t = helper.getTwitter(user);
            try {
                t.showUser(user.getTwitterSettings().getUserId());
            } catch (TwitterException e) {
                if (e.getStatusCode() == 404) {
                    logger.info("Disconnecting user " + user + " because their " + getIdPrefix() + " account was deleted");
                    disconnect(user.getId());
                }
            }
        }
    }

    @Override
    public List<String> getFollowerIds(User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }

        Twitter t = helper.getTwitter(user);

        try {
            List<String> result = Lists.newArrayList();
            long cursor = -1;
            while (true) {
                IDs ids = t.getFollowersIDs(cursor);
                for (long id : ids.getIDs()) {
                    result.add(String.valueOf(id));
                }
                cursor = ids.getNextCursor();
                if (cursor == 0 || ids.getIDs().length == 0) {
                    break;
                }
            }
            return result;
        } catch (TwitterException ex) {
            handleException("Problem getting followers", ex, user);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserDetails> getUserDetails(List<String> ids, User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }
        Twitter t = helper.getTwitter(user);
        List<UserDetails> result = Lists.newArrayListWithCapacity(ids.size());
        try {
            long[] longIds = new long[ids.size()];
            for (int i = 0; i < ids.size(); i++) {
                longIds[i] = helper.getTwitterId(ids.get(i));
            }
            ResponseList<twitter4j.User> response = t.lookupUsers(longIds);
            for (twitter4j.User twUser : response) {
                User wsUser = new User();
                helper.fillUserData(wsUser, twUser);
                result.add(new UserDetails(wsUser));
            }
            return result;
        } catch (TwitterException ex) {
            handleException("Problem getting user details", ex, user);
            return Collections.emptyList();
        }
    }
}