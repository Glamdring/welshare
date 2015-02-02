package com.welshare.service.social;

import java.util.List;
import java.util.concurrent.Future;

import org.joda.time.DateTime;

import com.welshare.model.Message;
import com.welshare.model.NotificationEvent;
import com.welshare.model.User;
import com.welshare.model.social.SocialNetworkSettings;
import com.welshare.service.ShareService.ResharingDetails;
import com.welshare.service.model.UserDetails;

public interface SocialNetworkService {

    void share(Message message, User user);

    void like(String originalMessageId, ResharingDetails likingDetails, User user);

    void unlike(String originalMessageId, User user);

    void reply(String originalMessageId, Message message);

    Future<List<Message>> getMessages(Message lastMessage, User user);

    Future<List<Message>> getMessages(User user);

    List<Message> getUserMessages(Message lastMessage, User user);

    /**
     * Returns a sorted by date list of the replies to the current messages
     * @param originalMessageId
     * @param user
     * @return list of replies
     */
    List<Message> getReplies(String originalMessageId, User user);

    /**
     * Stores the given settings object for the specified userId
     * @param settings
     * @param userId
     * @return the user with the filled settings
     */
    User storeSettings(SocialNetworkSettings settings, String userId);

    boolean shouldHandle(String messageId);

    User getInternalUserByExternalId(String externalUserId);

    UserDetails getUserDetails(String externalUserId, User user);

    Message getMessageByExternalId(String externalMessageId, User currentUser);

    void delete(String externalMessageId, User user);

    List<Message> getIncomingMessages(Message lastMessage, User user);

    List<UserDetails> getFriends(User user);

    List<UserDetails> getUserDetails(List<String> ids, User user);

    List<String> getFollowerIds(User user);

    void publishInitialMessage(User user);

    List<NotificationEvent> getUnreadNotifications(User user);

    /**
     * Gets X notifications, until the given event. The passed event can be null,
     * meaning that the latest notifications should be fetched.
     * @param maxEvent
     * @param count
     * @param user
     * @return list of notifications
     */
    List<NotificationEvent> getNotifications(NotificationEvent maxEvent, int count, User user);

    /**
     * Marks all notifications from the external system as read in the local
     * system (welshare) and optionally in the remote system
     *
     * @param user
     */
    void markNotificationsAsRead(User user);

    List<User> getLikers(String externalMessageId, User user);

    /**
     * Disconnects the given user from the social network
     * @param user
     * @return the user with updated details
     */
    User disconnect(String userId);

    /**
     * Find a User unique URL by his username
     *
     * @param username
     * @param user the current authenticated user
     *
     * @return the user details of the target user, or null if not found
     */
    String getUserId(String username, User user);

    String getUserDisplayName(User author);

    Object getStats(User user);

    /**
     *
     * @return the prefix that is appended to message ids
     */
    String getIdPrefix();

    /**
     *
     * @param user
     * @return the username that the passed user is registered with on the external network
     */
    String getExternalUsername(User user);

    String getShortText(Message externalMessage, User user);

    /**
     * @param user
     * @return list of messages that appeared after the user was last online
     */
    List<Message> getMissedIncomingMessages(User user);

    /**
     * Get all messages from the previous day
     * @param user
     * @return
     */
    List<Message> getYesterdayMessages(User user);

    boolean shouldShareLikes(User user);

    /**
     * Checks if the service is enabled for the given user
     * @param user
     * @return
     */
    boolean isServiceEnabled(User user);

    boolean isFriendWithCurrentUser(String externalUserId, User currentUser);

    /**
     * Gets the latest messages of the user identified by the given external id
     * @param externalId
     * @param user
     */
    List<Message> getMessagesOfUser(String externalId, User user);

    void favourite(String messageId, User user);

    void follow(String externalUserId, User user);

    void edit(Message editedMessage, User user);

    /**
     * Returns a list of the top scoring messages for the authenticated user
     * @param user
     * @return
     */
    List<Message> getTopRecentMessages(User user);

    /**
     * Calculates the social reputation for this social network
     * @param user
     * @param since dateTime since which the reputation should be calculated. Can be null (means from the beginning)
     * @return reputation as integer
     */
    int calculateReputation(User user, DateTime since);

    /**
     * Imports all external message of the user into welshare (apart from those coming from welshare)
     * @param user
     */
    void importMessages(User user);

    /**
     * Get the estimated number of potential readers currently online
     * @param user
     * @return estimated count
     */
    int getCurrentlyActiveReaders(User user);

    /**
     * Reshare the message. This is an additional action to "like". Some
     * services might lack both liking and resharing, for example twitter only
     * has retweet (which maps to 'like', being the default), while facebook has
     * like+share
     *
     * @param messageId
     * @param comment
     * @param user
     */
    void reshare(String messageId, String comment, User user);

    UserDetails getCurrentUserDetails(User user);

    void fillMessageAnalyticsData(List<ExternalMessageAnalyticsData> list, User user);

    /**
     * Disconnects users that no longer exist on the target network
     * @param users
     */
    void disconnectDeletedUsers(List<User> users);

    public static final class ExternalMessageAnalyticsData {
        private String messageId;
        private String externalMessageId;
        private int score;
        public String getMessageId() {
            return messageId;
        }
        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }
        public String getExternalMessageId() {
            return externalMessageId;
        }
        public void setExternalMessageId(String externalMessageId) {
            this.externalMessageId = externalMessageId;
        }
        public int getScore() {
            return score;
        }
        public void setScore(int score) {
            this.score = score;
        }
    }
}
