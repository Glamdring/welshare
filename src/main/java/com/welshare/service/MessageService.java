package com.welshare.service;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.CacheEvict;

import com.google.common.collect.Lists;
import com.welshare.dao.enums.MessageSearchType;
import com.welshare.model.ActiveReadersEntry;
import com.welshare.model.Message;
import com.welshare.model.ScheduledMessage;
import com.welshare.model.Tag;
import com.welshare.model.User;
import com.welshare.service.model.WelshareStats;

public interface MessageService extends BaseService {

    String USER_STREAM_CACHE = "userStreamCache";

    /**
     * The number of messages is fixed, and the messages fetched start from
     * the last message currently shown.
     *
     * The list of currently shown messages is required, because the new messages
     * are obtained based on the last message that is actually displayed
     *
     * @param userId
     * @param list of currently shown messages. Cannot be null
     * @param important whether a list of only important messages should be returned
     * @param filterNetwork a network (prefix) from which to get results, or null to get everything
     *
     * @return collection of messages and a collection of the last message for each service (internal and external)
     */
    MessagesResult getMessages(String userId, Collection<Message> currentMessages, boolean important, String filterNetwork);

    /**
     * Overloaded method of the one above, for showing messages from all networks
     * @param userId
     * @param currentMessages
     * @param important
     * @return
     */
    MessagesResult getMessages(String userId, Collection<Message> currentMessages, boolean important);

    /**
     * Fetches all incoming messages that are not yet retrieved
     *
     * @param userId
     * @param list of currently shown messages. Cannot be null
     * @param whether the returned message should be above the important threshold only
     * @return list of messages
     */
    Collection<Message> getIncomingMessages(String userId, Collection<Message> currentMessages, boolean important);

    /**
     * Returns a list of the messages of the given user
     * The number of messages is fixed, and the messages fetched start from
     * the "start" number
     *
     * @param user
     * @param start
     * @return list
     */
    Collection<Message> getUserMessages(User user, User currentUser, int start);

    /**
     * Returns a list of the messages of the given user
     * The number of messages is fixed, and the messages fetched start from
     * the "start" number
     *
     * @param user
     * @param start
     * @return list
     */
    Collection<Message> getUserMessages(String username, User currentUser, int start);

    /**
     * Lists suggestions from a start tag
     * @param tagPart
     */
    List<Tag> getTagSuggestions(String tagPart);

    /**
     * Retrieves all comments/replies to a given message
     * @param originalMessageId
     * @return the full list of replies to a given message
     */
    Collection<Message> getReplies(String originalMessageId, User user);

    /**
     * Delete the given message
     * @param messageId
     * @return the message that was just deleted
     */
    Message delete(String messageId, boolean deleteExternal, User user);

    /**
     * Gets a list of users who like the specified message
     *
     * @param messageId
     * @param user current user
     * @return list
     */
    List<User> getLikers(String messageId, User user);

    /**
     * Gets a list of message for a given page number (for the traditional-
     * style paging)
     *
     * @param loggedUser
     * @param page
     * @return list of messages for the given page
     */
    Collection<Message> getPagedMessages(User loggedUser, int page);

    /**
     * Performs a search on all internal messages
     *
     * @param keywords
     * @param type the type of search
     * @param user the currently logged user
     * @Param page
     * @return list of result messages
     */
    Collection<Message> search(String keywords, MessageSearchType type,
            User currentUser, int page);

    /**
     * Add the specified message to the user's favourites
     *
     * @param messageId
     * @param user
     */
    void favourite(String messageId, User user);

    /**
     * Removes a message from the favourites of the given user
     *
     * @param messageId
     * @param user
     */
    void unfavourite(String messageId, User user);

    /**
     * Get tagged messages
     * @param tag
     * @param page
     * @param current user (or null, if no logged in user)
     * @return list of messages
     *
     */
    List<Message> getTaggedMessages(String tag, int page, User user);

    WelshareStats getStats(User user);

    List<Message> getFavourites(User user);

    /**
     * Gets the external URL of the original message that a message with the
     * given id is related to
     *
     * @param id of the message whose original is looked up
     * @return the url, or null if there is no original
     */
    String getOriginalExternalUrl(String id);

    Message getMessage(String id, User user);

    /**
     * Gets a list of important messages that this user has missed
     * due to being offline
     * @param user
     * @return collection of important messages (both internal and external)
     */
    Collection<Message> getMissedImportantMessages(User user);

    /**
     * Edit the given message. Allowed only in case there are no likes or replies.
     * @param messageId
     * @param newText
     * @param loggedUser
     * @return the edited message on success, null if editing is not allowed
     */
    Message edit(String messageId, String newText, User loggedUser);

    /**
     * Gets a list of top messages for the previous day, limited by threshold and a threshold ratio (=likes per X followers)
     *
     * @param threshold
     * @param thresholdRatio
     * @return
     */
    List<Message> getTopDailyMessages(int threshold, int thresholdRatio, User loggedUser);

    /**
     * Gets the top recent messages of the given user
     * @param loggedUser
     * @return list of top messages, sorted by score
     */
    List<Message> getTopRecentMessages(User loggedUser);

    List<ScheduledMessage> getScheduledMessages(String userId);

    void deleteScheduledMessage(long id);

    /**
     * Gets a list of old messages that will be suggested for sharing again
     * @return
     */
    List<Message> getOldMessages(String userId);

    List<Message> getMoreOldMessages(String userId);

    /**
     * Simply invalidates the cached stream for a given user
     */
    void invalidateStreamCache(String userId);

    /**
     * Gets a lists of messages that have shortened links for which there is available analytics
     * @param userId
     * @return
     */
    List<Message> getAnalytics(String userId);

    Map<String, BestTimesToShare> getBestTimesToShare(String userId);

    class MessagesResult implements Serializable {
        private static final long serialVersionUID = -8619789425202479825L;

        private Collection<Message> messages;
        private Collection<Message> newestMessages;
        public Collection<Message> getMessages() {
            return messages;
        }
        public void setMessages(Collection<Message> messages) {
            this.messages = messages;
        }
        public Collection<Message> getNewestMessages() {
            return newestMessages;
        }
        public void setNewestMessages(Collection<Message> lastMessages) {
            this.newestMessages = lastMessages;
        }
    }

    class BestTimesToShare {
        private List<ActiveReadersEntry> weekdays = Lists.newArrayList();
        private List<ActiveReadersEntry> weekends = Lists.newArrayList();
        private int maxValue;
        private String socialNetworkPrefix;
        public List<ActiveReadersEntry> getWeekdays() {
            return weekdays;
        }
        public void setWeekdays(List<ActiveReadersEntry> weekdays) {
            this.weekdays = weekdays;
        }
        public List<ActiveReadersEntry> getWeekends() {
            return weekends;
        }
        public void setWeekends(List<ActiveReadersEntry> weekends) {
            this.weekends = weekends;
        }
        public String getSocialNetworkPrefix() {
            return socialNetworkPrefix;
        }
        public void setSocialNetworkPrefix(String socialNetworkPrefix) {
            this.socialNetworkPrefix = socialNetworkPrefix;
        }
        public int getMaxValue() {
            return maxValue;
        }
        public void setMaxValue(int maxValue) {
            this.maxValue = maxValue;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @CacheEvict(value = USER_STREAM_CACHE, key="'messages-' + #user.id + '-home'")
    public @interface EvictHomeStreamCache { }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @CacheEvict(value = USER_STREAM_CACHE, key="'messages-' + #userId + '-home'")
    public @interface EvictHomeStreamCacheStringParam { }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @CacheEvict(value = USER_STREAM_CACHE, key="'userMessages-' + #user.id + '-home'")
    public @interface EvictUserMessages { }

}
