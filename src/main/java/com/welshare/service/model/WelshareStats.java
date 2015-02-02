package com.welshare.service.model;

import java.util.LinkedHashMap;
import java.util.Map;

import org.joda.time.DateMidnight;

public class WelshareStats {

    private Map<DateMidnight, Integer> messages = new LinkedHashMap<DateMidnight, Integer>();
    private Map<DateMidnight, Integer> replies = new LinkedHashMap<DateMidnight, Integer>();
    private Map<DateMidnight, Integer> likes = new LinkedHashMap<DateMidnight, Integer>();

    private int maxMessages;
    private int maxReplies;
    private int maxLikes;

    private int maxCount;

    private double averageMessages;
    private double averageReplies;
    private double averageLikes;

    public Map<DateMidnight, Integer> getMessages() {
        return messages;
    }
    public void setMessages(Map<DateMidnight, Integer> messages) {
        this.messages = messages;
    }
    public Map<DateMidnight, Integer> getLikes() {
        return likes;
    }
    public void setLikes(Map<DateMidnight, Integer> likes) {
        this.likes = likes;
    }
    public Map<DateMidnight, Integer> getReplies() {
        return replies;
    }
    public void setReplies(Map<DateMidnight, Integer> replies) {
        this.replies = replies;
    }
    public int getMaxMessages() {
        return maxMessages;
    }
    public void setMaxMessages(int maxPosts) {
        this.maxMessages = maxPosts;
    }
    public int getMaxReplies() {
        return maxReplies;
    }
    public void setMaxReplies(int maxLikes) {
        this.maxReplies = maxLikes;
    }
    public int getMaxLikes() {
        return maxLikes;
    }
    public void setMaxLikes(int maxReplies) {
        this.maxLikes = maxReplies;
    }
    public int getMaxCount() {
        return maxCount;
    }
    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }
    public double getAverageMessages() {
        return averageMessages;
    }
    public void setAverageMessages(double averagePosts) {
        this.averageMessages = averagePosts;
    }
    public double getAverageLikes() {
        return averageLikes;
    }
    public void setAverageLikes(double averageLikes) {
        this.averageLikes = averageLikes;
    }
    public double getAverageReplies() {
        return averageReplies;
    }
    public void setAverageReplies(double averageReplies) {
        this.averageReplies = averageReplies;
    }
}
