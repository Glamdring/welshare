package com.welshare.model.social;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class FacebookSettings implements SocialNetworkSettings {

    private static final long serialVersionUID = 1575082357199885617L;

    @Column(name="facebookToken")
    private String token;

    @Column(name="facebookUserId")
    private String userId;

    @Column(name="facebookLastReadNotificationTimestamp", nullable=false)
    private long lastReadNotificationTimestamp;

    @Column(name="facebookFetchMessages", nullable=false)
    private boolean fetchMessages;

    @Column(name="facebookShareLikes", nullable=false)
    private boolean shareLikes;

    @Column(name="facebookFetchImages", nullable=false)
    private boolean fetchImages;

    /**
     * Whether to include a link to the welshare profile after "like" and "comment" in the fb actions
     */
    @Column(name="facebookShowProfileLinkAction", nullable=false)
    private boolean showProfileLinkAction;

    @Column(name="facebookShowInProfile", nullable=false)
    private boolean showInProfile;

    @Column(name="facebookLastImportedMessageTime", nullable=false)
    private long lastImportedMessageTime;

    @Column(name="facebookImportMessages", nullable=false)
    private boolean importMessages;

    @Column(name="facebookDisconnectReason")
    private String disconnectReason;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getLastReadNotificationTimestamp() {
        return lastReadNotificationTimestamp;
    }

    public void setLastReadNotificationTimestamp(long lastReadNotificationTimestamp) {
        this.lastReadNotificationTimestamp = lastReadNotificationTimestamp;
    }

    @Override
    public boolean isFetchMessages() {
        return fetchMessages;
    }

    public void setFetchMessages(boolean fetchMessages) {
        this.fetchMessages = fetchMessages;
    }

    public boolean isShareLikes() {
        return shareLikes;
    }

    public void setShareLikes(boolean shareLikes) {
        this.shareLikes = shareLikes;
    }

    public boolean isFetchImages() {
        return fetchImages;
    }

    public void setFetchImages(boolean fetchImages) {
        this.fetchImages = fetchImages;
    }

    public boolean isShowProfileLinkAction() {
        return showProfileLinkAction;
    }

    public void setShowProfileLinkAction(boolean showProfileLinkAction) {
        this.showProfileLinkAction = showProfileLinkAction;
    }

    @Override
    public boolean isShowInProfile() {
        return showInProfile;
    }

    public void setShowInProfile(boolean showInProfile) {
        this.showInProfile = showInProfile;
    }

    @Override
    public boolean isActive() {
        return true; //facebook always on, for now
    }

    public long getLastImportedMessageTime() {
        return lastImportedMessageTime;
    }

    public void setLastImportedMessageTime(long lastImportedMessageTime) {
        this.lastImportedMessageTime = lastImportedMessageTime;
    }

    @Override
    public boolean isImportMessages() {
        return importMessages;
    }

    public void setImportMessages(boolean importMessages) {
        this.importMessages = importMessages;
    }

    public String getDisconnectReason() {
        return disconnectReason;
    }

    public void setDisconnectReason(String disconnectReason) {
        this.disconnectReason = disconnectReason;
    }
}
