package com.welshare.model.social;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class LinkedInSettings implements SocialNetworkSettings {

    private static final long serialVersionUID = 21878538661176273L;

    @Column(name="linkedInToken")
    private String token;

    @Column(name="linkedInTokenSecret")
    private String tokenSecret;

    @Column(name="linkedInUserId")
    private String userId;

    @Column(name="linkedInLastReadNotificationTimestamp", nullable=false)
    private long lastReadNotificationTimestamp;

    @Column(name="linkedInFetchMessages", nullable=false)
    private boolean fetchMessages;

    @Column(name="linkedInShareLikes", nullable=false)
    private boolean shareLikes;

    @Column(name="linkedInFetchImages", nullable=false)
    private boolean fetchImages;

    @Column(name="linkedInActive", nullable=false)
    private boolean active;

    @Column(name="linkedInShowInProfile", nullable=false)
    private boolean showInProfile;

    @Column(name="linkedInLastImportedMessageTime", nullable=false)
    private long lastImportedMessageTime;

    @Column(name="linkedInDisconnectReason")
    private String disconnectReason;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenSecret() {
        return tokenSecret;
    }

    public void setTokenSecret(String tokenSecret) {
        this.tokenSecret = tokenSecret;
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

    @Override
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean isShowInProfile() {
        return showInProfile;
    }

    public void setShowInProfile(boolean showInProfile) {
        this.showInProfile = showInProfile;
    }

    public long getLastImportedMessageTime() {
        return lastImportedMessageTime;
    }

    public void setLastImportedMessageTime(long lastImportedMessageTime) {
        this.lastImportedMessageTime = lastImportedMessageTime;
    }

    @Override
    public boolean isImportMessages() {
        return false;
    }

    public String getDisconnectReason() {
        return disconnectReason;
    }

    public void setDisconnectReason(String disconnectReason) {
        this.disconnectReason = disconnectReason;
    }
}
