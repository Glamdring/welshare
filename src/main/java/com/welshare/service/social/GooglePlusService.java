package com.welshare.service.social;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.googlecode.googleplus.Paging;
import com.googlecode.googleplus.Plus;
import com.googlecode.googleplus.model.activity.Activity;
import com.googlecode.googleplus.model.activity.ActivityCollection;
import com.googlecode.googleplus.model.activity.ActivityFeed;
import com.googlecode.googleplus.model.comment.Comment;
import com.googlecode.googleplus.model.comment.CommentFeed;
import com.googlecode.googleplus.model.history.HistoryCollection;
import com.googlecode.googleplus.model.history.Moment;
import com.googlecode.googleplus.model.history.MomentType;
import com.googlecode.googleplus.model.history.Target;
import com.welshare.dao.UserDao;
import com.welshare.model.Message;
import com.welshare.model.NotificationEvent;
import com.welshare.model.User;
import com.welshare.model.social.GooglePlusSettings;
import com.welshare.model.social.SocialNetworkSettings;
import com.welshare.service.MessageService.EvictHomeStreamCacheStringParam;
import com.welshare.service.ShareService.ResharingDetails;
import com.welshare.service.annotations.SqlTransactional;
import com.welshare.service.model.UserDetails;
import com.welshare.service.social.helper.GooglePlusHelper;
import com.welshare.service.social.model.GooglePlusStats;
import com.welshare.service.social.qualifiers.GooglePlus;
import com.welshare.util.Constants;
import com.welshare.util.collection.CollectionUtils;

@Service
@GooglePlus
@Order(2)
public class GooglePlusService implements SocialNetworkService {

    private static final Logger logger = LoggerFactory.getLogger(GooglePlusService.class);

    @Inject
    private UserDao dao;

    @Inject
    private GooglePlusHelper helper;

    @Value("${messages.per.fetch}")
    private int messagesPerFetch;

    @Value("${base.url}")
    private String baseUrl;

    @Override
    public void share(Message message, User user) {
        if (!isServiceEnabled(user)) {
            return;
        }
        Plus plus = helper.getClient(user);

        try {
            Moment moment = new Moment();
            moment.setType(MomentType.CREATE);
            moment.setTarget(new Target());
            moment.getTarget().setUrl(baseUrl + "/message/" + message.getId());
            plus.getHistoryOperations().insert(moment, HistoryCollection.VAULT);
        } catch (Exception ex) {
            handleException("Problem sharing message to Google+" + message, ex, user);
        }
    }

    @Override
    public void like(String originalMessageId, ResharingDetails likingDetails, User user) {
        // TODO Auto-generated method stub

    }

    @Override
    public void unlike(String originalMessageId, User user) {
        // TODO Auto-generated method stub

    }

    @Override
    public void reply(String originalMessageId, Message message) {
        // TODO Auto-generated method stub

    }

    @Override
    public Future<List<Message>> getMessages(Message lastMessage, User user) {
        // Plus.Activities.List list = plus.activities().list("me", "public");
        return SocialUtils.emptyFutureList();
    }

    @Override
    public Future<List<Message>> getMessages(User user) {
        return SocialUtils.emptyFutureList();
    }

    @Override
    public List<Message> getUserMessages(Message lastMessage, User user) {
        return Collections.emptyList();
    }

    @Override
    public List<Message> getReplies(String originalMessageId, User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }
        Plus plus = helper.getClient(user);
        try {
            CommentFeed feed = plus.getCommentOperations().list(getGooglePlusId(originalMessageId));
            List<Message> result = Lists.newArrayList();
            for (Comment comment : feed.getItems()) {
                Message message = new Message();
                message.setText(StringEscapeUtils.unescapeHtml4(comment.getObject().getContent()));
                message.setDateTime(comment.getPublished());
                message.getData().setExternalId(comment.getId());
                message.setAuthor(helper.actorToUser(comment.getActor()));
                result.add(message);
            }
            return result;
        } catch (Exception ex) {
            handleException("Problem getting comments to message with id: " + originalMessageId, ex, user);
            return Collections.emptyList();
        }
    }

    @Override
    @SqlTransactional
    @EvictHomeStreamCacheStringParam
    public User storeSettings(SocialNetworkSettings settings, String userId) {
        if (settings instanceof GooglePlusSettings) {
            User user = dao.getById(User.class, userId, true);
            user.setGooglePlusSettings((GooglePlusSettings) settings);
            dao.persist(user);
            return user;
        }
        return null;
    }

    @Override
    public boolean shouldHandle(String messageId) {
        if (messageId != null && messageId.startsWith(GooglePlusHelper.PUBLIC_ID_PREFIX)) {
            return true;
        }
        return false;
    }

    @Override
    public User getInternalUserByExternalId(String externalUserId) {
        return dao.getByPropertyValue(User.class, "googlePlusSettings.userId", externalUserId);
    }

    @Override
    public UserDetails getUserDetails(String externalUserId, User user) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @Cacheable(value="singleExternalMessageCache", key="#externalMessageId")
    public Message getMessageByExternalId(String externalMessageId, User currentUser) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void delete(String externalMessageId, User user) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<Message> getIncomingMessages(Message lastMessage, User user) {
        return Collections.emptyList();
    }

    @Override
    public List<UserDetails> getFriends(User user) {
        return Collections.emptyList();
    }

    @Override
    public void publishInitialMessage(User user) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<NotificationEvent> getUnreadNotifications(User user) {
        return Collections.emptyList();
    }

    @Override
    public List<NotificationEvent> getNotifications(NotificationEvent maxEvent, int count, User user) {
        return Collections.emptyList();
    }

    @Override
    public void markNotificationsAsRead(User user) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<User> getLikers(String externalMessageId, User user) {
        return Collections.emptyList();
    }

    @Override
    @SqlTransactional
    @EvictHomeStreamCacheStringParam
    public User disconnect(String userId) {
        User user = dao.getById(User.class, userId, true);
        if (user == null || user.getGooglePlusSettings() == null) {
            return null;
        }

        // clear the settings
        user.getGooglePlusSettings().setFetchMessages(false);
        user.getGooglePlusSettings().setToken(null);
        user.getGooglePlusSettings().setRefreshToken(null);
        user.getGooglePlusSettings().setUserId(null);

        return dao.persist(user);
    }

    @Override
    public String getUserId(String username, User user) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getUserDisplayName(User author) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @Cacheable(value="statisticsCache", key="'gp' + #user.id")
    public GooglePlusStats getStats(User user) {
        if (!isServiceEnabled(user)) {
            return null;
        }
        Plus plus = helper.getClient(user);
        try {
            GooglePlusStats stats = new GooglePlusStats();

            ActivityFeed feed = plus.getActivityOperations().list(user.getGooglePlusSettings().getUserId(),
                    ActivityCollection.PUBLIC, new Paging(91));
            List<Activity> posts = feed.getItems();
            if (posts.isEmpty()) {
                return stats;
            }

            LinkedList<Activity> linkedList = new LinkedList<Activity>(posts);
            Iterator<Activity> iterator = linkedList.descendingIterator();

            Multiset<DateMidnight> postsData = LinkedHashMultiset.create();
            Multiset<DateMidnight> plusOnesData = LinkedHashMultiset.create();
            Multiset<DateMidnight> commentsData = LinkedHashMultiset.create();

            Activity currentPost = iterator.next();
            DateMidnight current = new DateMidnight(currentPost.getPublished());
            DateMidnight start = current;

            while (iterator.hasNext() || currentPost != null) {
                DateMidnight msgTime = new DateMidnight(currentPost.getPublished());
                if (current.equals(msgTime)) {
                    postsData.add(current);
                    plusOnesData.add(current, (int) currentPost.getObject().getPlusoners().getTotalItems());
                    commentsData.add(current, (int) currentPost.getObject().getReplies().getTotalItems());

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
                stats.getReplies().put(dm, commentsData.count(dm));
                stats.getPlusOnes().put(dm, plusOnesData.count(dm));
            }

            int days = Days.daysBetween(start, end).getDays();
            if (days == 0) {
                return stats; // no further calculation
            }

            int[] postsMaxAndSum = CollectionUtils.getMaxAndSum(stats.getPosts());
            stats.setMaxPosts(postsMaxAndSum[0]);
            stats.setAveragePosts(postsMaxAndSum[1] / days);

            int[] plusOnesMaxAndSum = CollectionUtils.getMaxAndSum(stats.getPlusOnes());
            stats.setMaxPlusOnes(plusOnesMaxAndSum[0]);
            stats.setAveragePlusOnes(plusOnesMaxAndSum[1] / days);

            int[] repliesMaxAndSum = CollectionUtils.getMaxAndSum(stats.getReplies());
            stats.setMaxReplies(repliesMaxAndSum[0]);
            stats.setAverageReplies(repliesMaxAndSum[1] / days);

            stats.setMaxCount(NumberUtils.max(stats.getMaxPosts(), stats.getMaxReplies(),
                    stats.getMaxPlusOnes()));

            return stats;
        } catch (Exception e) {
            handleException("Problem fetching statistics", e, user);
            return null;
        }
    }

    @Override
    public String getIdPrefix() {
        return GooglePlusHelper.PUBLIC_ID_PREFIX;
    }

    @Override
    public String getExternalUsername(User user) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getShortText(Message externalMessage, User user) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Message> getMissedIncomingMessages(User user) {
        return Collections.emptyList();
    }

    @Override
    public List<Message> getYesterdayMessages(User user) {
        return Collections.emptyList();
    }

    @Override
    public boolean shouldShareLikes(User user) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isServiceEnabled(User user) {
        return user != null && user.getGooglePlusSettings() != null
                && user.getGooglePlusSettings().getToken() != null
                && user.getGooglePlusSettings().isFetchMessages();
    }

    @Override
    public boolean isFriendWithCurrentUser(String externalUserId, User currentUser) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<Message> getMessagesOfUser(String externalId, User user) {
        return Collections.emptyList();
    }

    @Override
    public void favourite(String messageId, User user) {
        // TODO Auto-generated method stub

    }

    @Override
    public void follow(String externalUserId, User user) {
        // TODO Auto-generated method stub

    }

    @Override
    public void edit(Message editedMessage, User user) {
        // TODO Auto-generated method stub
    }

    private void handleException(String message, Exception e, User user) {
        if (e instanceof HttpClientErrorException) {
            String responseBody = ((HttpClientErrorException) e).getResponseBodyAsString();
            // if unauthorized
            if (((HttpClientErrorException) e).getStatusCode() == HttpStatus.UNAUTHORIZED) {
                logger.info("Auth problem with user " + user + ". Resetting token. The problem is due to exception: " + e.getMessage() + "; Response body: " + responseBody);
                user.getGooglePlusSettings().setDisconnectReason(e.getMessage());
                helper.forciblyDisconnect(this, user);
            } else {
                logger.warn(message + ": " + e.getMessage() + " : " + ExceptionUtils.getRootCauseMessage(e) + "; Response body: " + responseBody);
            }

        } else if (e.getCause() instanceof ConnectionPoolTimeoutException || e.getCause() instanceof SocketTimeoutException) {
            logger.warn("Google+ timeout (for user: " + user + ") " + ExceptionUtils.getRootCauseMessage(e));
        } else {
            logger.warn(message, e);
        }
    }

    @Override
    public List<Message> getTopRecentMessages(User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }
        Plus plus = helper.getClient(user);
        try {
            ActivityFeed feed = plus.getActivityOperations().list("me", ActivityCollection.PUBLIC,
                    new Paging(91));
            List<Activity> activities = new ArrayList<Activity>(feed.getItems());
            for (Iterator<Activity> it = activities.iterator(); it.hasNext();) {
                if (it.next().getObject().getPlusoners().getTotalItems() == 0) {
                    it.remove();
                }
            }

            return helper.activitiesToMessages(activities, true, false);
        } catch (Exception ex) {
            handleException("Problem getting top recent messages", ex, user);
            return Collections.emptyList();
        }
    }

    public String getGooglePlusId(String externalId) {
        return externalId.replace(GooglePlusHelper.PUBLIC_ID_PREFIX, "");
    }

    @Override
    public int calculateReputation(User user, DateTime since) {
        if (!isServiceEnabled(user)) {
            return -1;
        }
        Plus plus = helper.getClient(user);
        int reputation = 0;
        Paging paging = new Paging(91);
        String pageToken = "";
        mainLoop:
        for (int page = 0; page < 30; page++) {
            // if the user was disconnected when handling an exception, break
            if (!user.getGooglePlusSettings().isFetchMessages()) {
                break;
            }
            try {
                paging.setPageToken(pageToken);
                ActivityFeed feed = plus.getActivityOperations().list("me", ActivityCollection.PUBLIC, paging);
                for (Activity activity : feed.getItems()) {
                    if (since != null && since.isAfter(activity.getPublished())) {
                        break mainLoop;
                    }
                    reputation += activity.getObject().getPlusoners().getTotalItems() * (Constants.LIKE_SCORE - 2);
                    reputation += activity.getObject().getResharers().getTotalItems() * Constants.LIKE_SCORE;

                    long replyCount = activity.getObject().getReplies().getTotalItems();
                    replyCount = Math.min(replyCount, 7); //limit to 7 replies; not taking long discussion into account
                    reputation +=  replyCount * Constants.REPLY_SCORE;
                }
                pageToken = feed.getNextPageToken();

                if (StringUtils.isEmpty(pageToken) || feed.getItems().isEmpty()) {
                    break;
                }
            } catch (Exception ex) {
                handleException("Problem calculating Google+ reputation for user " + user, ex, user);
            }
        }

        return reputation;
    }

    @Override
    public void importMessages(User user) {
        if (!isServiceEnabled(user) || !user.getGooglePlusSettings().isImportMessages()) {
            return;
        }
        Plus plus = helper.getClient(user);

        DateTime since = new DateTime(user.getGooglePlusSettings().getLastImportedMessageTime());

        List<Message> messages = Lists.newArrayList();
        Paging paging = new Paging(91);
        String pageToken = "";
        mainLoop:
        for (int page = 0; page < 30; page++) {
            // if the user was disconnected when handling an exception, break
            if (!user.getGooglePlusSettings().isFetchMessages()) {
                break;
            }
            try {
                paging.setPageToken(pageToken);
                ActivityFeed feed = plus.getActivityOperations().list("me", ActivityCollection.PUBLIC, paging);
                for (Activity activity : feed.getItems()) {
                    if (since != null && since.isAfter(activity.getPublished())) {
                        break mainLoop;
                    }
                    //TODO filter messages coming from welshare
                    messages.add(helper.activityToMessage(activity, false));
                }
                pageToken = feed.getNextPageToken();

                if (StringUtils.isEmpty(pageToken) || feed.getItems().isEmpty()) {
                    break;
                }
            } catch (Exception ex) {
                handleException("Problem importing Google+ messages for user " + user, ex, user);
            }
        }

        helper.importExternalMessages(user, messages);
    }

    @Override
    public int getCurrentlyActiveReaders(User user) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void reshare(String message, String comment, User user) {
        // TODO Auto-generated method stub
    }

    @Override
    public void fillMessageAnalyticsData(List<ExternalMessageAnalyticsData> list, User user) {
        // TODO Auto-generated method stub

    }

    @Override
    public UserDetails getCurrentUserDetails(User user) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void disconnectDeletedUsers(List<User> users) {
        // TODO Auto-generated method stub
    }

    @Override
    public List<String> getFollowerIds(User user) {
        return Collections.emptyList();
    }

    @Override
    public List<UserDetails> getUserDetails(List<String> ids, User user) {
        return Collections.emptyList();
    }
}
