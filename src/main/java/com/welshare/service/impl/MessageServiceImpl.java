package com.welshare.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.Minutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.primitives.Ints;
import com.welshare.dao.MessageDao;
import com.welshare.dao.Paging;
import com.welshare.dao.UserDao;
import com.welshare.dao.enums.MessageSearchType;
import com.welshare.model.ActiveReadersEntry;
import com.welshare.model.Favourite;
import com.welshare.model.FavouritePK;
import com.welshare.model.Following;
import com.welshare.model.LikeAction;
import com.welshare.model.LikeActionPK;
import com.welshare.model.Message;
import com.welshare.model.MessageFilter;
import com.welshare.model.Picture;
import com.welshare.model.ScheduledMessage;
import com.welshare.model.Tag;
import com.welshare.model.User;
import com.welshare.model.social.ExternalFavourite;
import com.welshare.model.social.ExternalFavourite.ExternalFavouriteId;
import com.welshare.model.social.ExternalUserThreshold;
import com.welshare.model.social.ExternalUserThreshold.ExternalUserThresholdId;
import com.welshare.service.FollowingService;
import com.welshare.service.MessageService;
import com.welshare.service.PictureService;
import com.welshare.service.UrlShorteningService;
import com.welshare.service.annotations.SqlReadonlyTransactional;
import com.welshare.service.annotations.SqlTransactional;
import com.welshare.service.model.WelshareStats;
import com.welshare.service.social.SocialNetworkService;
import com.welshare.service.social.SocialNetworkService.ExternalMessageAnalyticsData;
import com.welshare.service.social.SocialUtils;
import com.welshare.util.Constants;
import com.welshare.util.WebUtils;
import com.welshare.util.collection.CollectionUtils;

@Service
public class MessageServiceImpl extends BaseServiceImpl implements MessageService {

    private static final int MESSAGES_BY_SAME_AUTHOR_LIMIT = 12;

    private static final int RATIO_FOLLOWERS = 50000;

    private static final Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);

    private static final Comparator<Message> LIKES_COMPARATOR = new LikesComparator();
    private static final MessageTimeComparator MESSAGE_TIME_COMPARATOR = new MessageTimeComparator();

    @Value("${messages.per.fetch}")
    private int messagesPerFetch;

    @Inject
    private MessageDao dao;

    @Inject
    private UserDao userDao;

    @Inject
    private List<SocialNetworkService> socialNetworkServices;

    @Inject
    private PictureService pictureService;

    @Inject
    private FollowingService followingService;

    @Inject
    private PriorityQueue<ScheduledMessage> scheduledMessagesQueue;

    @Inject
    private List<UrlShorteningService> urlShorteners;

    @Override
    @SqlTransactional
    @Cacheable(value = USER_STREAM_CACHE, key="(#important ? 'important' : '') + 'messages-' + #userId + '-' + (#currentMessages.empty ? 'home' : #currentMessages[#currentMessages.size()-1].publicId) + '-' + #filterNetwork")
    public MessagesResult getMessages(String userId, Collection<Message> currentMessages, boolean important, String filterNetwork) {

        List<Message> newestMessages = new ArrayList<Message>();

        User user = getDao().getById(User.class, userId);
        if (currentMessages == null) {
            currentMessages = Collections.emptyList();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Getting home stream of " + user
                    + " where last message=" + (currentMessages.isEmpty() ? "none"
                    : ((List<Message>) currentMessages).get(currentMessages
                            .size() - 1)));
        }

        List<Message> listCopy = new ArrayList<Message>(currentMessages);

        Message lastInternal = getLastMessage(listCopy, true, false);

        List<Message> internalMessages = Lists.newArrayList();
        if (filterNetwork == null || filterNetwork.equals("ws")) {
            List<User> followedUsers = followingService.getFollowing(user.getId());
            internalMessages = dao.getMessages(user, followedUsers, lastInternal, messagesPerFetch);
            if (!internalMessages.isEmpty()) {
                newestMessages.add(internalMessages.get(0));
            }
        }

        List<Future<List<Message>>> futures = Lists.newArrayListWithExpectedSize(socialNetworkServices.size());

        for (SocialNetworkService snService : socialNetworkServices) {
            if (filterNetwork == null || snService.getIdPrefix().equals(filterNetwork)) {
                Message lastExternalMessage = getLastMessage(listCopy, snService,
                        true, true);
                Future<List<Message>> externalResult = snService.getMessages(lastExternalMessage, user);
                futures.add(externalResult);
            }
        }

        List<Message> resultList = new ArrayList<Message>(internalMessages);
        for (Future<List<Message>> future : futures) {
            try {
                List<Message> externalResult = future.get();
                resultList.addAll(externalResult);
                if (!externalResult.isEmpty()) {
                    newestMessages.add(externalResult.get(0));
                }
            } catch (Exception e) {
                logger.warn("Uncaught social network problem", e);
            }
        }

        // filter all messages that are newer than the newest form the current
        // they should be fetched by getIncomingMessages, and not by getMessages
        // (this matters for "get more messages", not for the home stream,
        // where "current" is empty)
        filterNewerMessages(resultList, currentMessages);

        if (important) {
            filterByImportantMessageThreshold(user.getProfile().getImportantMessageScoreThreshold(),
                user.getProfile().getImportantMessageScoreThresholdRatio(), user, resultList);
        }

        resultList = filterAndFillMetadata(user, resultList, currentMessages, true, true);

        MessagesResult result = new MessagesResult();
        result.setMessages(resultList);
        result.setNewestMessages(newestMessages);
        return result;
    }

    @Override
    @Cacheable(value = USER_STREAM_CACHE, key="(#important ? 'important' : '') + 'messages-' + #userId + '-' + (#currentMessages.empty ? 'home' : #currentMessages[#currentMessages.size()-1].publicId)")
    @SqlTransactional
    public MessagesResult getMessages(String userId, Collection<Message> currentMessages, boolean important) {
        return getMessages(userId, currentMessages, important, null);
    }

    private void filterNewerMessages(List<Message> result, Collection<Message> currentMessages) {
        if (currentMessages.isEmpty()) {
            return;
        }
        Message newest = currentMessages.iterator().next();
        for (Iterator<Message> it = result.iterator(); it.hasNext(); ) {
            Message msg = it.next();
            if (msg.getDateTime().isAfter(newest.getDateTime())) {
                it.remove();
            }
        }
    }

    private List<Message> filterAndFillMetadata(User user,
            List<Message> result, Collection<Message> currentMessages,
            boolean limitCount, boolean filter) {

        filterHiddenMessages(user, result);

        Collections.sort(result, MESSAGE_TIME_COMPARATOR);

        filterDuplicates(result);
        // i.e. leave only one message for message+replies
        filterPseudoDuplicates(result, currentMessages);

        if (limitCount) {
            // creating a new array list, because sublists are not serializable,
            // and that's needed for caching, storing in session, etc.
            result = new ArrayList<Message>(result.subList(0, getCount(result)));
        }

        // filtering after the results were ordered and cut to the nth message
        // otherwise older messages from a source could wrongly appear in results.
        // This way less than 20 messages may be fetched
        if (filter) {
            filterByUserThreshold(result, user);
            filterByMessageFilters(result, user);
        }

        fillCurrentUserRelatedData(result, user);

        fillExternalOriginalMessages(result, user);

        // initialize anything that may be lazy for internal messages
        // doing this at the latest possible moment so that no unnecessary queries
        // are executed. (not utilizing OSIV)
        for (Message message : result) {
            if (message.getId() != null) {
                dao.initialize(message);
            }
        }

        return result;
    }

    private void filterHiddenMessages(User user, List<Message> result) {
        for (Iterator<Message> it = result.iterator(); it.hasNext();) {
            Message msg = it.next();
            if (user == null && !msg.getHiddenFrom().isEmpty()) {
                it.remove();
                continue;
            }
            if (msg.getHiddenFrom().contains(user)) {
                it.remove();
                continue;
            }
        }
    }

    private void filterByMessageFilters(List<Message> result, User user) {
        List<MessageFilter> filters = getDao().getListByPropertyValue(
                MessageFilter.class, "user", user);

        for (MessageFilter filter : filters) {
            for (Iterator<Message> it = result.iterator(); it.hasNext();) {
                if (!filter.shouldDisplayMessage(it.next())) {
                    it.remove();
                }
            }
        }
    }

    private void fillExternalOriginalMessages(Collection<Message> result, User user) {
        for (Message msg : result) {
            if (msg.getId() != null && msg.getExternalOriginalMessageId() != null && !msg.isExternalLike()) {
                for (SocialNetworkService sns : socialNetworkServices) {
                    if (sns.shouldHandle(msg.getExternalOriginalMessageId())) {
                        // getting the message using the token of the message author
                        // that's because the viewing user might not be authenticated with that service
                        //TODO cache this!
                        Message externalOriginalMessage = sns.getMessageByExternalId(msg.getExternalOriginalMessageId(), msg.getAuthor());
                        msg.getData().setExternalOriginalMessage(externalOriginalMessage);
                        // if the current user is not authenticated then links to external messages
                        // should not be opened internally in welshare
                        if (externalOriginalMessage != null) {
                            externalOriginalMessage.getData()
                            .setOpenMessageInternally(sns.isServiceEnabled(user));
                        }
                    }
                }
            }
        }
    }

    private void filterByUserThreshold(List<Message> result, User currentUser) {
        for (Iterator<Message> it = result.iterator(); it.hasNext();) {
            Message message = it.next();
            User followed = message.getAuthor();

            if (message.getData().getExternalId() == null) {
                Following following = followingService.findFollowing(currentUser, followed);
                if (following != null && following.getLikesThreshold() > message.getScore()) {
                    it.remove();
                }
                if (following != null && following.isHideReplies() && message.getText().startsWith("@")) {
                    it.remove();
                }
            } else {
                ExternalUserThresholdId id = new ExternalUserThresholdId(currentUser, followed.getExternalId());
                ExternalUserThreshold threshold = dao.getById(ExternalUserThreshold.class, id);
                if (threshold != null && message.getScore() < threshold.getThreshold()) {
                    it.remove();
                }
                if (threshold != null && threshold.isHideReplies() && message.getText().startsWith("@")) {
                    it.remove();
                }
            }
        }
    }

    private void fillCurrentUserRelatedData(List<Message> messages, User user) {
        List<String> favouritedMessagesIds = dao.getFavouritedMessageIds(messages, user);
        if (user != null) {
            for (Message msg : messages) {
                // only internal messages are checked here
                // external messages are populated with this information
                // when they are fetched. It should not be (re)set here for them
                if (isLikedByCurrentUser(msg, user)) {
                    msg.getData().setLikedByCurrentUser(true);
                }

                if (favouritedMessagesIds.contains(msg.getPublicId())) {
                    msg.getData().setFavouritedByCurrentUser(true);
                }
            }
        }
    }

    private void filterByImportantMessageThreshold(int threshold, int thresholdRatio, User user, List<Message> messages) {
        // count the links that appear more than "threshold" times
        Multiset<String> links = HashMultiset.create();

        for (Iterator<Message> it = messages.iterator(); it.hasNext();) {
            Message msg = it.next();


            List<String> urls = WebUtils.extractUrls(msg.getText());
            boolean hasImportantLink = false;
            for (String url : urls) {
                links.add(url);
                if (links.count(url) >= threshold) {
                    hasImportantLink = true;
                }
            }

            // fixed threshold
            if (msg.getScore() < threshold && !hasImportantLink) {
                it.remove();
                continue;
            }
            // ratio per 50000 followers. For less than 50000 followers this is omitted (/50000=0)
            if (msg.getAuthor() != null && msg.getScore() < (msg.getAuthor().getFollowers() / RATIO_FOLLOWERS) * thresholdRatio
                    && !hasImportantLink && msg.getScore() != 101) { //twitter max = 101, which sucks..
                it.remove();
            }

            // skip own posts
            if (msg.getAuthor().equals(user)) {
                it.remove();
            }
        }
    }

    private boolean isLikedByCurrentUser(Message message, User user) {
        if (message.getData().getExternalId() == null && message.getScore() > 0) {
            LikeAction la = get(LikeAction.class, new LikeActionPK(message, user));
            if (la != null) {
                return true;
            }
        }
        return false;
    }

    private Message getLastMessage(List<Message> currentMessages,
            boolean oldest, boolean external) {
        return getLastMessage(currentMessages, null, oldest, external);
    }

    private Message getLastMessage(List<Message> currentMessages,
            SocialNetworkService snService, boolean oldest, boolean external) {
        Message last = null;

        if (oldest) {
            currentMessages = Lists.reverse(currentMessages);
        }

        for (Message msg : currentMessages) {
            if (external && snService != null) {
                if (snService.shouldHandle(msg.getData().getExternalId())) {
                    last = msg;
                    break;
                }
            } else {
                if (msg.getData().getExternalId() == null) {
                    last = msg;
                    break;
                }
            }
        }

        return last;
    }

    /**
     * Removes messages whose replies or likes exist and are ordered higher
     * because of having a more recent datetime. The method expects the messages
     * to be sorted by creation time, descending
     *
     * @param messages
     * @param currentMessages
     */
    private void filterPseudoDuplicates(List<Message> messages,
            Collection<Message> currentMessages) {

        List<Message> all = new ArrayList<Message>(currentMessages);
        all.addAll(messages);

        for (Message existingMessage : all) {

            // don't do anything with regular or external messages
            if (existingMessage.getOriginalMessage() == null
                    || existingMessage.getData().getExternalId() != null) {
                continue;
            }

            // remove the original message (if exists)
            messages.remove(existingMessage.getOriginalMessage());

            // if two messages are replies of the same original message,
            // retain only the newest of them
            for (Message message : all) {
                if (message.getDateTime().isBefore(existingMessage.getDateTime())
                    && (message.isReply() || message.isLiking())
                    && message.getOriginalMessage() != null
                    && message.getOriginalMessage().equals(existingMessage.getOriginalMessage())) {
                    messages.remove(existingMessage);
                }
            }
        }
    }

    @Override
    @Cacheable(value = USER_STREAM_CACHE, key="'userMessages-' + #user.id + '-' + #start")
    @SqlReadonlyTransactional
    public List<Message> getUserMessages(User user, User currentUser, int start) {

        List<Message> result = dao.getUserMessages(user, messagesPerFetch, start);

        result = filterAndFillMetadata(currentUser, result,
                Collections.<Message> emptyList(), true, false);

        return result;
    }

    @Override
    @Cacheable(value = USER_STREAM_CACHE, key="'userMessages-username-' + #username + '-' + #start")
    @SqlReadonlyTransactional
    public List<Message> getUserMessages(String username, User currentUser, int start) {
        User user = userDao.getByUsername(username);
        if (user == null) {
            return Collections.emptyList();
        }

        return getUserMessages(user, currentUser, start);
    }

    @Override
    protected MessageDao getDao() {
        return dao;
    }

    @Override
    @SqlReadonlyTransactional
    public List<Tag> getTagSuggestions(String tagPart) {
        Validate.notNull(tagPart);
        if (tagPart.length() == 0) {
            return Collections.emptyList();
        }
        List<Tag> tags = getDao().getTagsByStart(tagPart);

        return tags;
    }

    @Override
    @SqlReadonlyTransactional
    public List<Message> getReplies(String originalMessageId, User user) {
        SocialNetworkService sns = SocialUtils.getSocialNetworkService(
                socialNetworkServices, originalMessageId);

        List<Message> result;
        if (sns != null) {
            result = sns.getReplies(originalMessageId, user);
        } else {
            result = getDao().getReplies(originalMessageId);
            fillCurrentUserRelatedData(result, user);
        }

        return result;
    }

    @Override
    @SqlTransactional
    @EvictHomeStreamCache
    @EvictUserMessages
    public Message delete(String messageId, boolean deleteExternal, User user) {
        Message message = get(Message.class, messageId);

        if (message == null) {
            return null;
        }

        boolean author = message.getAuthor().equals(user);
        boolean addressee = user.equals(message.getAddressee());
        if (author || addressee) {

            if (author && deleteExternal) {
                for (String externalId : message.getAssociatedExternalIds()) {
                    for (SocialNetworkService sns : socialNetworkServices) {
                        if (sns.shouldHandle(externalId)) {
                            sns.delete(externalId, user);
                        }
                    }
                }
            }

            if (author && message.isReply()) {
                getDao().lock(message.getOriginalMessage().getAuthor());
                getDao().lock(message.getOriginalMessage());

                // if it is not a reply to own message, decrease score
                if (!message.getOriginalMessage().getAuthor().equals(user)) {
                    message.getOriginalMessage()
                    .getAuthor().setScore(message.getOriginalMessage().getAuthor()
                                .getScore() - Constants.REPLY_SCORE);
                }

                message.getOriginalMessage().setReplies(
                        message.getOriginalMessage().getReplies() - 1);
                save(message.getOriginalMessage());

            } else if (author && message.isLiking() && message.getOriginalMessage() != null) {
                getDao().lock(message.getOriginalMessage());
                getDao().lock(message.getOriginalMessage().getAuthor());
                message.getOriginalMessage().setScore(
                        message.getOriginalMessage().getScore() - 1);
                message.getOriginalMessage()
                        .getAuthor()
                        .setScore(
                                message.getOriginalMessage().getAuthor()
                                        .getScore() - 1);
                save(message.getOriginalMessage());

                LikeAction la = get(LikeAction.class,
                        new LikeActionPK(message.getOriginalMessage(), user));
                delete(la);
            }

            getDao().deleteReplies(message.getId());
            getDao().deleteNotifications(message.getId());
            for (Picture pic : message.getPictures()) {
                pictureService.deleteFiles(pic);
            }
            getDao().delete(message);
        }

        getDao().lock(message.getAuthor());
        message.getAuthor().decrementMessageCount();
        save(message.getAuthor());

        return message;
    }

    private void filterDuplicates(final List<Message> result) {
        Iterator<Message> it = result.iterator();

        while (it.hasNext()) {
            Message msg = it.next();
            if (msg.getData().getExternalId() != null || msg.isImported()) {
                for (Message cMsg : result) {
                    // same text, but not the same message
                    if (StringUtils.equals(msg.getText(), cMsg.getText())
                            && !msg.getPublicId().equals(cMsg.getPublicId())) {
                        it.remove();
                        break;
                    }
                }
            }
        }
    }

    private int getCount(List<Message> result) {
        return Math.min(result.size(), messagesPerFetch);
    }

    @Override
    @SqlReadonlyTransactional
    public List<User> getLikers(String messageId, User user) {

        if (messageId == null || messageId.isEmpty()) {
            return Collections.emptyList();
        }

        SocialNetworkService sns = SocialUtils.getSocialNetworkService(
                socialNetworkServices, messageId);
        if (sns != null) {
            return sns.getLikers(messageId, user);
        }

        List<LikeAction> likes = dao.getListByPropertyValue(LikeAction.class,
                "primaryKey.message.id", messageId);

        List<User> likers = new ArrayList<User>(likes.size());
        for (LikeAction like : likes) {
            likers.add(like.getUser());
        }

        return likers;
    }

    @Override
    @SqlTransactional
    public List<Message> getPagedMessages(User loggedUser, int page) {
        return dao.getMessages(loggedUser, followingService.getFollowing(loggedUser.getId()), messagesPerFetch, page);
    }

    @Override
    @Cacheable(value = USER_STREAM_CACHE, key="'incomingMessages-' + #userId")
    @SqlTransactional
    public Collection<Message> getIncomingMessages(String userId,
            Collection<Message> currentMessages, boolean important) {

        User user = userDao.getById(User.class, userId);

        Message lastMessage = null;
        List<Message> listCopy = Collections.emptyList();

        if (currentMessages != null) {
            listCopy = new ArrayList<Message>(currentMessages);
            lastMessage = getLastMessage(listCopy, false, false);
        }

        List<Message> messages = new ArrayList<Message>();
        if (lastMessage != null) {
            messages = dao.getIncomingMessages(user, followingService.getFollowing(userId), lastMessage.getDateTime());
        }

        for (SocialNetworkService sns : socialNetworkServices) {
            Message lastExternal = getLastMessage(listCopy, sns, false, true);
            if (lastExternal != null) {
                messages.addAll(sns.getIncomingMessages(lastExternal, user));
            }
        }

        if (important) {
            filterByImportantMessageThreshold(user.getProfile().getImportantMessageScoreThreshold(),
                    user.getProfile().getImportantMessageScoreThresholdRatio(), user, messages);
        }

        messages = filterAndFillMetadata(user, messages, currentMessages, false, true);

        return messages;
    }

    @Override
    @SqlReadonlyTransactional
    public Collection<Message> search(String keywords, MessageSearchType type,
            User currentUser, int page) {
        List<User> followedUsers = Collections.emptyList();
        if (type == MessageSearchType.STREAM) {
            followedUsers = followingService.getFollowing(currentUser.getId());
        }
        List<Message> result = dao.search(keywords, followedUsers, type, currentUser, new Paging(page, messagesPerFetch));
        filterAndFillMetadata(currentUser, result, Collections.<Message>emptyList(), false, false);

        return result;
    }

    @Override
    @SqlTransactional
    public void favourite(String messageId, User user) {
        SocialNetworkService sns =  SocialUtils.getSocialNetworkService(socialNetworkServices, messageId);

        if (sns == null) {
            Favourite fv = new Favourite();
            Message msg = get(Message.class, messageId);
            FavouritePK pk = new FavouritePK(msg, user);
            fv.setPrimaryKey(pk);
            save(fv);
        } else {
            ExternalFavourite fv = new ExternalFavourite();
            fv.setExternalMessageId(messageId);
            fv.setUser(user);
            save(fv);
            sns.favourite(messageId, user);
        }
    }

    @Override
    @SqlTransactional
    public void unfavourite(String messageId, User user) {
        if (!SocialUtils.isExternal(messageId, socialNetworkServices)) {
            Message msg = get(Message.class, messageId);
            FavouritePK pk = new FavouritePK(msg, user);
            Favourite fv = getDao().getById(Favourite.class, pk);
            if (fv != null) {
                delete(fv);
            }
        } else {
            ExternalFavouriteId pk = new ExternalFavouriteId(messageId, user);
            ExternalFavourite fv = getDao().getById(ExternalFavourite.class, pk);
            if (fv != null) {
                delete(fv);
            }
        }
    }

    @Override
    @Cacheable(value = USER_STREAM_CACHE, key="'taggedMessages-' + #user?.id")
    @SqlReadonlyTransactional
    public List<Message> getTaggedMessages(String tag, int page, User user) {
        List<Message> messages = getDao().getTaggedMessages(tag, new Paging(page, messagesPerFetch));
        filterAndFillMetadata(user, messages, Collections.<Message>emptyList(), false, false);
        return messages;
    }

    @Override
    @SqlReadonlyTransactional
    public WelshareStats getStats(User user) {
        WelshareStats stats = new WelshareStats();
        List<Message> messages = getDao().getUserMessages(user, 200, 0);

        if (messages.isEmpty()) {
            return stats;
        }

        LinkedList<Message> linkedList = new LinkedList<Message>(messages);
        Iterator<Message> iterator = linkedList.descendingIterator();

        Multiset<DateMidnight> messagesData = LinkedHashMultiset.create();
        Multiset<DateMidnight> likesData = LinkedHashMultiset.create();
        Multiset<DateMidnight> repliesData = LinkedHashMultiset.create();

        Message currentMessage = iterator.next();
        DateMidnight current = new DateMidnight(currentMessage.getDateTime());
        DateMidnight start = current;
        while (iterator.hasNext() || currentMessage != null) {
            // skip imported messages
            DateMidnight msgTime = new DateMidnight(currentMessage.getDateTime());
            if (current.equals(msgTime)) {
                if (!currentMessage.isImported()) {
                    messagesData.add(current);
                    likesData.add(current, currentMessage.getScore());
                    repliesData.add(current, currentMessage.getReplies());
                }
                if (iterator.hasNext()) {
                    currentMessage = iterator.next();
                } else {
                    currentMessage = null;
                }
            } else {
                current = current.plusDays(1);
            }
        }
        DateMidnight end = current;
        if (Days.daysBetween(start, end).getDays() > 30) {
            start = end.minusDays(30);
        }

        for (DateMidnight dm = start; !dm.isAfter(end); dm = dm.plusDays(1)) {
            stats.getMessages().put(dm, messagesData.count(dm));
            stats.getReplies().put(dm, repliesData.count(dm));
            stats.getLikes().put(dm, likesData.count(dm));
        }


        int days = Days.daysBetween(start, end).getDays();
        if (days == 0) {
            return stats; // no further calculation
        }

        int[] messagesMaxAndSum = CollectionUtils.getMaxAndSum(stats.getMessages());
        stats.setMaxMessages(messagesMaxAndSum[0]);
        stats.setAverageMessages(messagesMaxAndSum[1] / days);

        int[] likesMaxAndSum = CollectionUtils.getMaxAndSum(stats.getLikes());
        stats.setMaxLikes(likesMaxAndSum[0]);
        stats.setAverageLikes(likesMaxAndSum[1] / days);

        int[] repliesMaxAndSum = CollectionUtils.getMaxAndSum(stats.getReplies());
        stats.setMaxReplies(repliesMaxAndSum[0]);
        stats.setAverageReplies(repliesMaxAndSum[1] / days);

        stats.setMaxCount(NumberUtils.max(
                stats.getMaxMessages(),
                stats.getMaxReplies(),
                stats.getMaxLikes()));

        return stats;
    }

    @Override
    @SqlReadonlyTransactional
    public List<Message> getFavourites(User user) {
        List<Favourite> favourites = getDao().getListByPropertyValue(Favourite.class, "primaryKey.user", user);
        List<Message> messages = new ArrayList<Message>(favourites.size());
        for (Favourite fav : favourites) {
            messages.add(fav.getPrimaryKey().getMessage());
        }

        List<ExternalFavourite> externalFavourites = getDao().getListByPropertyValue(ExternalFavourite.class, "user", user);
        for (ExternalFavourite efav : externalFavourites) {
            SocialNetworkService sns = SocialUtils.getSocialNetworkService(socialNetworkServices, efav.getExternalMessageId());
            messages.add(sns.getMessageByExternalId(efav.getExternalMessageId(), user));
        }

        fillCurrentUserRelatedData(messages, user);
        return messages;
    }

    @Override
    public String getOriginalExternalUrl(String id) {
        Validate.notNull(id);

        Message message = get(Message.class, id);
        if (message == null || message.getExternalOriginalMessageId() == null) {
            return null;
        }

        for (SocialNetworkService sns : socialNetworkServices) {
            if (sns.shouldHandle(message.getExternalOriginalMessageId())) {
                Message external = sns.getMessageByExternalId(message.getExternalOriginalMessageId(), message.getAuthor());
                if (external != null) {
                    return external.getData().getExternalUrl();
                }
            }
        }

        return null;
    }

    @Override
    @SqlReadonlyTransactional
    public Message getMessage(String id, User user) {
        Message msg = get(Message.class, id);
        if (msg != null) {
            fillExternalOriginalMessages(Collections.singletonList(msg), user);
            dao.initialize(msg);
        }
        return msg;
    }

    @Override
    @SqlReadonlyTransactional
    public Collection<Message> getMissedImportantMessages(User user) {
        List<Message> messages = new ArrayList<Message>();
        for (SocialNetworkService sns : socialNetworkServices) {
            messages.addAll(sns.getMissedIncomingMessages(user));
        }

        DateTime lastLogout = new DateTime(user.getLastLogout());
        // if the last logout was less than an hour ago, assume it was 12 hours ago,
        // so that more messages are displayed (although they might not be actually 'missed')
        if (Minutes.minutesBetween(lastLogout, new DateTime()).getMinutes() < 60) {
            lastLogout = new DateTime().minusHours(12);
        }
        messages.addAll(dao.getIncomingMessages(user,
                followingService.getFollowing(user.getId()), lastLogout));

        filterByImportantMessageThreshold(user.getProfile().getImportantMessageScoreThreshold(),
                user.getProfile().getImportantMessageScoreThresholdRatio(), user, messages);

        messages = filterAndFillMetadata(user, messages,
                Collections.<Message> emptyList(), false, true);

        return messages;
    }

    public static void main(String[] args) {
        System.out.println((1777990 / RATIO_FOLLOWERS) * 2);
    }

    @Override
    @SqlTransactional
    public Message edit(String messageId, String newText, User loggedUser) {
        Message msg = getDao().getById(Message.class, messageId, true);
        if (msg == null || msg.getReplies() > 0 || msg.getScore() > 0 || !msg.getAuthor().equals(loggedUser)) {
            return null;
        }
        msg.setText(newText);
        msg = save(msg);

        fillExternalOriginalMessages(Collections.singletonList(msg), loggedUser);
        dao.initialize(msg);

        for (SocialNetworkService sns : socialNetworkServices) {
            sns.edit(msg, loggedUser);
        }

        return msg;
    }

    @Override
    public List<Message> getTopDailyMessages(int threshold, int thresholdRatio, User user) {
        List<Message> messages = new ArrayList<Message>();
        for (SocialNetworkService sns : socialNetworkServices) {
            messages.addAll(sns.getYesterdayMessages(user));
        }
        messages.addAll(dao.getIncomingMessages(user,
                followingService.getFollowing(user.getId()),
                new DateTime().minusDays(1)));

        filterByImportantMessageThreshold(threshold, thresholdRatio, user, messages);
        filterAndFillMetadata(user, messages, Collections.<Message>emptyList(), false, true);

        // filter out messages from the same author if he has too many on the list
        Multiset<String> authors = HashMultiset.create(messages.size());
        for (Iterator<Message> it = messages.iterator(); it.hasNext();) {
            Message msg = it.next();
            if (authors.count(msg.getAuthor().getPublicId()) > MESSAGES_BY_SAME_AUTHOR_LIMIT) {
                it.remove();
                continue;
            }
            authors.add(msg.getAuthor().getPublicId());
        }
        return messages;
    }

    @Override
    @Cacheable(value="topRecentMessagesCache", key="#user.id")
    public List<Message> getTopRecentMessages(User user) {
        List<Message> messages = new ArrayList<Message>();
        for (SocialNetworkService sns : socialNetworkServices) {
            messages.addAll(sns.getTopRecentMessages(user));
        }
        List<Message> latest = dao.getMessages(user, followingService.getFollowing(user.getId()), 100, 1);
        for (Iterator<Message> it = latest.iterator(); it.hasNext();) {
            if (it.next().getScore() == 0) {
                it.remove();
            }
        }
        filterAndFillMetadata(user, messages, Collections.<Message>emptyList(), false, false);
        Collections.sort(messages, LIKES_COMPARATOR);
        // limit to 50
        return new ArrayList<Message>(messages.subList(0, Math.min(messages.size(), 50)));
    }

    @Override
    @SqlReadonlyTransactional
    public List<ScheduledMessage> getScheduledMessages(String userId) {
        return getDao().getOrderedListByPropertyValue(ScheduledMessage.class, "userId", userId, "scheduledTime");
    }

    @Override
    @SqlTransactional
    public void deleteScheduledMessage(long id) {
        ScheduledMessage msg = get(ScheduledMessage.class, id);
        if (msg != null) {
            delete(msg);
            scheduledMessagesQueue.remove(msg);
        }
    }

    @Override
    @SqlReadonlyTransactional
    @Cacheable(value="oldMessagesCache", key="#userId")
    public List<Message> getOldMessages(String userId) {
        User user = get(User.class, userId);
        if (user.getMessages() > 50) {
            List<Message> result = getDao().getOldMessages(user, 4 + RandomUtils.nextInt(3));
            for (Message message : result) {
                if (message.getId() != null) {
                    dao.initialize(message);
                }
            }
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    @SqlReadonlyTransactional
    public List<Message> getMoreOldMessages(String userId) {
        // same as the other method, but without caching
        return getOldMessages(userId);
    }

    private static class MessageTimeComparator implements Comparator<Message> {
        @Override
        public int compare(Message m1, Message m2) {
            // descending
            return -1 * m1.getDateTime().compareTo(m2.getDateTime());
        }
    }

    private static class LikesComparator implements Comparator<Message> {
        @Override
        public int compare(Message msg1, Message msg2) {
            // descending
            return -1 * Ints.compare(msg1.getScore(), msg2.getScore());
        }
    }

    @Override
    @EvictHomeStreamCacheStringParam
    public void invalidateStreamCache(String userId) {
        //do nothing, invalidation is handled by the annotation @Evict.. annotation
    }

    @Override
    @SqlReadonlyTransactional
    @Cacheable("analyticsCache")
    public List<Message> getAnalytics(String userId) {
        // some O(n^2) below, but the upper bound is 100 elements, so doesn't matter that much
        User user = dao.getById(User.class, userId);
        List<Message> messages = dao.getAnalytics(user, 100, 0); // TODO get more?
        Map<SocialNetworkService, List<ExternalMessageAnalyticsData>> externalIdsBatch = Maps.newHashMap();
        for (SocialNetworkService sns : socialNetworkServices) {
            externalIdsBatch.put(sns, Lists.<ExternalMessageAnalyticsData>newArrayList());
        }
        for (Iterator<Message> it = messages.iterator(); it.hasNext();) {
            Message msg = it.next();
            boolean proceed = fillShortenedUrlsAnalytics(user, it, msg);
            if (proceed) {
                fillExternalMessageBatch(user, it, msg, externalIdsBatch);
            }
        }

        for (SocialNetworkService sns : externalIdsBatch.keySet()) {
            List<ExternalMessageAnalyticsData> list = externalIdsBatch.get(sns);
            sns.fillMessageAnalyticsData(list, user);
            for (Message message : messages) {
                for (ExternalMessageAnalyticsData data : list) {
                    if (message.getId().equals(data.getMessageId())) {
                        message.getData().getScores().put(sns.getIdPrefix(), data.getScore());
                    }
                }
            }
        }

        return messages;
    }

    private boolean fillShortenedUrlsAnalytics(User user, Iterator<Message> it, Message msg) {
        List<String> urls = WebUtils.extractUrls(msg.getText());
        if (urls.isEmpty() && msg.getAssociatedExternalIds().isEmpty()) {
            it.remove();
            return false;
        }
        int clicks = 0;
        boolean hasShortenedUrls = false;
        for (String url : urls) {
            for (UrlShorteningService shortener : urlShorteners) {
                if (shortener.isShortened(url)) {
                    int currentClicks = shortener.getClicks(url, user);
                    if (currentClicks != -1) {
                        clicks += currentClicks;
                        hasShortenedUrls = true;
                    }
                    break; // no need to loop the rest of the shorteners
                }
            }
        }
        if (!hasShortenedUrls && msg.getAssociatedExternalIds().isEmpty()) {
            it.remove();
            return false;
        }
        msg.getData().setClicks(clicks);
        return true;
    }

    private void fillExternalMessageBatch(User user, Iterator<Message> it, Message msg, Map<SocialNetworkService, List<ExternalMessageAnalyticsData>> externalIds) {
        if (msg.getAssociatedExternalIds().isEmpty()) {
            it.remove();
            return;
        }
        for (String externalId : msg.getAssociatedExternalIds()) {
            SocialNetworkService sns = SocialUtils.getSocialNetworkService(socialNetworkServices, externalId);
            ExternalMessageAnalyticsData data = new ExternalMessageAnalyticsData();
            data.setMessageId(msg.getId());
            data.setExternalMessageId(externalId);
            externalIds.get(sns).add(data);
        }
    }

    @Override
    @SqlReadonlyTransactional
    @Cacheable("suggestedTimeToShareCache")
    public Map<String, BestTimesToShare> getBestTimesToShare(String userId) {
        User user = userDao.getById(User.class, userId);
        Map<String, BestTimesToShare> result = Maps.newLinkedHashMap();
        for (SocialNetworkService sns : socialNetworkServices) {
            List<ActiveReadersEntry> allEntries = dao.getActiveReaderEntries(user, sns.getIdPrefix());
            BestTimesToShare btts = new BestTimesToShare();
            btts.setSocialNetworkPrefix(sns.getIdPrefix());
            int max = 0;
            for (ActiveReadersEntry entry : allEntries) {
                if (entry.isWeekend()) {
                    btts.getWeekends().add(entry);
                } else {
                    btts.getWeekdays().add(entry);
                }
                if (entry.getActivePercentage() > max) {
                    max = (int) entry.getActivePercentage();
                }
            }
            if (!btts.getWeekdays().isEmpty()) {
                List<ActiveReadersEntry> clone = new ArrayList<ActiveReadersEntry>(btts.getWeekdays());
                Collections.sort(btts.getWeekdays());
                logger.debug("Was list sorted? " + clone.equals(btts.getWeekdays()));
            }
            btts.setMaxValue(max + 1);
            result.put(sns.getIdPrefix(), btts);
        }

        return result;
    }
}
