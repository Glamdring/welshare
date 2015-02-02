package com.welshare.service;

import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;

import com.welshare.model.Message;
import com.welshare.model.Tag;

/**
 * The service via which the sharing is accomplished
 *
 * @author Bozhidar Bozhanov
 *
 */
public interface ShareService extends BaseService {

    String INCOMING = "in";
    int MAX_MESSAGE_SIZE = 300;

    /**
     * Share the passed message
     *
     * @param message
     * @param user
     * @param pictureUrls
     * @param list of external sites to share the message on
     *            can be null or empty. Contains full urls to the files
     * @param list of usernames the message is hidden from
     * @param whether to hide the message from close friends
     */
    Message share(String message,
            String userId,
            List<String> pictureUrls,
            List<String> externalSites,
            List<String> hideFromUsernames,
            boolean hideFromCloseFriends);

    /**
     * Reply with a message to the passed message id
     * @param message
     * @param originalMessageId
     * @param user
     */
    Message reply(String message, String originalMessageId, String userId);

    /**
     * reshares the given message:
     * - adds +1 to rep
     * - shows in timeline (similar to twitter retweet)
     * @param messageId reshared message id
     * @param more details about the liking
     * @param userId logged user id
     */
    LikeResult reshare(String messageId, ResharingDetails details, String userId);

    /**
     * Performs a simple like on the target message. It does not go to other
     * social networks - the action is performed only on the target message
     *
     * @param messageId
     * @param userId
     */
    void simpleLike(String messageId, String userId);

    /**
     * Parses a message text to return a set of tags
     *
     * @param messageText
     * @return set of tags
     */
    Set<Tag> parseTags(String messageText);

    /**
     * Remove the like from the given message
     *
     * @param messageId
     * @param user
     * @return the id of the like message that was deleted,
     *     or null if no message was deleted
     */
    String unlike(String messageId, String userId);

    /**
     * shorten the urls in the given text
     * @param messageText
     * @param user
     * @param trackViral
     * @param showTopBar
     * @return the message text with the shortened urls
     */
    String shortenUrls(String messageText, String userId, boolean showTopBar, boolean trackViral);

    /**
     * Schedule sharing in the future
     * @param text
     * @param userId
     * @param pictureFiles
     * @param externalSites
     * @param hideFromUsernames
     * @param hideFromCloseFriends
     * @param scheduledTime
     */
    void schedule(String text, String userId, List<String> pictureFiles, List<String> externalSites,
            List<String> hideFromUsernames, boolean hideFromCloseFriends, DateTime scheduledTime);


    public static class ResharingDetails {
        public static final ResharingDetails EMPTY = new ResharingDetails();

        private String comment;
        private List<String> externalSites; //externalSites on which to reshare the like
        private boolean reshareInternally; //whether to reshare the message internally
        private String editedResharedMessage; //the new message text
        private boolean shareAndLike; // share and also like (wherever
                                      // supported - checks originating message
                                      // and associated messages, if the liked
                                      // message was shared through welshare)

        public String getComment() {
            return comment;
        }
        public void setComment(String comment) {
            this.comment = comment;
        }
        public List<String> getExternalSites() {
            return externalSites;
        }
        public void setExternalSites(List<String> externalSites) {
            this.externalSites = externalSites;
        }
        public boolean isReshareInternally() {
            return reshareInternally;
        }
        public void setReshareInternally(boolean reshareInternally) {
            this.reshareInternally = reshareInternally;
        }
        public String getEditedResharedMessage() {
            return editedResharedMessage;
        }
        public void setEditedResharedMessage(String editedLikedMessage) {
            this.editedResharedMessage = editedLikedMessage;
        }
        public boolean isShareAndLike() {
            return shareAndLike;
        }
        public void setShareAndLike(boolean shareOnOriginatingNetwork) {
            this.shareAndLike = shareOnOriginatingNetwork;
        }
    }
}
