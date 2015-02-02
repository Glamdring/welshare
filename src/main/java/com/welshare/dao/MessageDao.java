package com.welshare.dao;

import java.util.List;

import org.joda.time.DateTime;

import com.welshare.dao.enums.MessageSearchType;
import com.welshare.model.ActiveReadersEntry;
import com.welshare.model.Message;
import com.welshare.model.Picture;
import com.welshare.model.ScheduledMessage;
import com.welshare.model.Tag;
import com.welshare.model.User;

public interface MessageDao extends Dao {

    List<Message> getMessages(User user, List<User> followedUsers, Message startMessage, int maxResults);

    List<Message> getUserMessages(User owner, int maxResults, int startIdx);

    List<Tag> getTagsByStart(String tagPart);

    List<Message> getReplies(String originalMessageId);

    void deleteReplies(String messageId);

    void deleteNotifications(String messageId);

    List<Message> getMessages(User loggedUser, List<User> followedUsers, int messagesPerFetch, int page);

    List<Message> getIncomingMessages(User user, List<User> followedUsers, DateTime lastMessageDateTime);

    List<Message> search(String keywords, List<User> followedUsers, MessageSearchType type,
            User currentUser, Paging paging);

    List<Message> getTaggedMessages(String tag, Paging paging);

    void delete(Message message);

    void initialize(Message message);

    /**
     * Gets a list of message ids that is a subset of the passed list of messages, containing
     * those that are favourited by the user
     * @param messages
     * @param user
     * @return
     */
    List<String> getFavouritedMessageIds(List<Message> messages, User user);

    Message getMessages(Picture picture);

    /**
     * @param fromTime
     * @param toTime
     * @return a list of scheduled messages that are before the given time
     */
    List<ScheduledMessage> getScheduledMessages(DateTime toTime);

    List<Message> getOldMessages(User author, int count);

    /**
     * Gets a list of messages eligible for analytics
     * @param author
     * @param maxResults
     * @param startIdx
     * @return
     */
    List<Message> getAnalytics(User author, int maxResults, int startIdx);

    List<ActiveReadersEntry> getActiveReaderEntries(User user, String idPrefix);
}
