package com.welshare.model.social;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class GooglePlusSettings implements SocialNetworkSettings {

    private static final long serialVersionUID = -1680641204090182169L;

    @Column(name="googlePlusToken")
    private String token;

    @Column(name="googlePlusRefreshToken")
    private String refreshToken;

    @Column(name="googlePlusUserId")
    private String userId;

    @Column(name="googlePlusLastReadNotificationTimestamp", nullable=false)
    private long lastReadNotificationTimestamp;

    @Column(name="googlePlusFetchMessages", nullable=false)
    private boolean fetchMessages;

    @Column(name="googlePlusShareLikes", nullable=false)
    private boolean shareLikes;

    @Column(name="googlePlusFetchImages", nullable=false)
    private boolean fetchImages;

    @Column(name="googlePlusActive", nullable=false)
    private boolean active;

    @Column(name="googlePlusShowInProfile", nullable=false)
    private boolean showInProfile;

    @Column(name="googlePluslastImportedMessageTime", nullable=false)
    private long lastImportedMessageTime;

    @Column(name="googlePlusImportMessages", nullable=false)
    private boolean importMessages;

    @Column(name="googlePlusDisconnectReason")
    private String disconnectReason;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
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
