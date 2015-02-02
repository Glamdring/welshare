package com.welshare.model.social;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class TwitterSettings implements SocialNetworkSettings {

    private static final long serialVersionUID = -4570232515080347886L;

    @Column(name="twitterToken")
    private String token;

    @Column(name="twitterTokenSecret")
    private String tokenSecret;

    @Column(name="twitterUserId", nullable=false)
    private long userId;

    @Column(name="twitterLastReadNotificationId", nullable=false)
    private long lastReadNotificationId;

    @Column(name="twitterLastReadNotificationTimestamp", nullable=false)
    private long lastReadNotificationTimestamp;

    @Column(name="twitterLastReceivedTweetId", nullable=false)
    private long lastReceivedTweetId;

    @Column(name="twitterFetchMessages", nullable=false)
    private boolean fetchMessages;

    @Column(name="twitterShareLikes", nullable=false)
    private boolean shareLikes;

    @Column(name="twitterFetchImages", nullable=false)
    private boolean fetchImages;

    @Column(name="twitterRealFollowers", nullable=false)
    private int realFollowers;

    @Column(name="twitterShowInProfile", nullable=false)
    private boolean showInProfile;

    @Column(name="twitterLastImportedMessageTime", nullable=false)
    private long lastImportedMessageTime;

    @Column(name="twitterImportMessages", nullable=false)
    private boolean importMessages;

    @Column(name="twitterDisconnectReason")
    private String disconnectReason;

    @Column(name="twitterTweetWeeklySummary", nullable=false)
    private boolean tweetWeeklySummary;

    @Column(name="twitterLastSummaryNotificationTimestamp", nullable=false)
    private long lastSummaryNotificationTimestamp;

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

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getLastReadNotificationId() {
        return lastReadNotificationId;
    }

    public void setLastReadNotificationId(long lastReadNotificationId) {
        this.lastReadNotificationId = lastReadNotificationId;
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

    public long getLastReceivedTweetId() {
        return lastReceivedTweetId;
    }

    public void setLastReceivedTweetId(long lastReceivedTweetId) {
        this.lastReceivedTweetId = lastReceivedTweetId;
    }

    public int getRealFollowers() {
        return realFollowers;
    }

    public void setRealFollowers(int realFollowers) {
        this.realFollowers = realFollowers;
    }

    @Override
    public boolean isShowInProfile() {
        return showInProfile;
    }

    public void setShowInProfile(boolean showInProfile) {
        this.showInProfile = showInProfile;
    }

    public long getLastReadNotificationTimestamp() {
        return lastReadNotificationTimestamp;
    }

    public void setLastReadNotificationTimestamp(long lastReadNotificationTimestamp) {
        this.lastReadNotificationTimestamp = lastReadNotificationTimestamp;
    }

    @Override
    public boolean isActive() {
        return true; //twitter always on (for now)
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

    public boolean isTweetWeeklySummary() {
        return tweetWeeklySummary;
    }

    public void setTweetWeeklySummary(boolean tweetWeeklySummary) {
        this.tweetWeeklySummary = tweetWeeklySummary;
    }

    public long getLastSummaryNotificationTimestamp() {
        return lastSummaryNotificationTimestamp;
    }

    public void setLastSummaryNotificationTimestamp(long lastWeekNotificationId) {
        this.lastSummaryNotificationTimestamp = lastWeekNotificationId;
    }
}
