package com.welshare.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.Transient;

import com.google.common.collect.Maps;
import com.welshare.model.social.VideoData;

public class MessageData implements Serializable {

    private static final long serialVersionUID = -6809836236753388535L;

    @Transient
    private String externalId;
    @Transient
    private String externalSiteName;
    @Transient
    private String externalUrl;
    @Transient
    private String formattedText;
    /**
     * In most cases the same as text, but for example for facebook messages
     * with links contains only the message content without the title/desc/etc.
     */
    @Transient
    private String shortText;
    @Transient
    private VideoData videoData;
    @Transient
    private List<String> mentionedUsernames = new ArrayList<String>();
    @Transient
    private boolean favouritedByCurrentUser;
    @Transient
    private boolean likedByCurrentUser;
    @Transient
    private Message externalOriginalMessage;
    /**
     * Indicates whether links to this message should be to welshare.com/message/external
     * or should be to the target service.
     */
    @Transient
    private boolean openMessageInternally;
    /**
     * Clicks on all links (used for analytics)
     */
    @Transient
    private int clicks;

    /**
     * Used to display a map of likes/retweets/+1s. The key is a message key.
     */
    @Transient
    private Map<String, Integer> scores = Maps.newHashMap();

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getExternalSiteName() {
        return externalSiteName;
    }

    public void setExternalSiteName(String externalSiteName) {
        this.externalSiteName = externalSiteName;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public String getFormattedText() {
        return formattedText;
    }

    public void setFormattedText(String formattedText) {
        this.formattedText = formattedText;
    }

    public String getShortText() {
        return shortText;
    }

    public void setShortText(String shortText) {
        this.shortText = shortText;
    }

    public VideoData getVideoData() {
        return videoData;
    }

    public void setVideoData(VideoData videoData) {
        this.videoData = videoData;
    }

    public List<String> getMentionedUsernames() {
        return mentionedUsernames;
    }

    public void setMentionedUsernames(List<String> mentionedUsernames) {
        this.mentionedUsernames = mentionedUsernames;
    }

    public boolean isFavouritedByCurrentUser() {
        return favouritedByCurrentUser;
    }

    public void setFavouritedByCurrentUser(boolean favouritedByCurrentUser) {
        this.favouritedByCurrentUser = favouritedByCurrentUser;
    }

    public boolean isLikedByCurrentUser() {
        return likedByCurrentUser;
    }

    public void setLikedByCurrentUser(boolean likedByCurrentUser) {
        this.likedByCurrentUser = likedByCurrentUser;
    }

    public Message getExternalOriginalMessage() {
        return externalOriginalMessage;
    }

    public void setExternalOriginalMessage(Message externalOriginalMessage) {
        this.externalOriginalMessage = externalOriginalMessage;
    }

    public boolean isOpenMessageInternally() {
        return openMessageInternally;
    }

    public void setOpenMessageInternally(boolean openMessageInternally) {
        this.openMessageInternally = openMessageInternally;
    }

    public int getClicks() {
        return clicks;
    }

    public void setClicks(int clicks) {
        this.clicks = clicks;
    }

    public Map<String, Integer> getScores() {
        return scores;
    }

    public void setScores(Map<String, Integer> scores) {
        this.scores = scores;
    }
}