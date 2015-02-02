package com.welshare.service.social.model;

import java.util.LinkedHashMap;
import java.util.Map;

import org.joda.time.DateMidnight;

public class GooglePlusStats {

    private Map<DateMidnight, Integer> posts = new LinkedHashMap<DateMidnight, Integer>();
    private Map<DateMidnight, Integer> replies = new LinkedHashMap<DateMidnight, Integer>();
    private Map<DateMidnight, Integer> plusOnes = new LinkedHashMap<DateMidnight, Integer>();

    private int maxPosts;
    private int maxReplies;
    private int maxPlusOnes;

    private int maxCount;

    private double averagePosts;
    private double averageReplies;
    private double averagePlusOnes;

    public Map<DateMidnight, Integer> getPosts() {
        return posts;
    }
    public void setPosts(Map<DateMidnight, Integer> messages) {
        this.posts = messages;
    }
    public Map<DateMidnight, Integer> getPlusOnes() {
        return plusOnes;
    }
    public void setPlusOnes(Map<DateMidnight, Integer> likes) {
        this.plusOnes = likes;
    }
    public Map<DateMidnight, Integer> getReplies() {
        return replies;
    }
    public void setReplies(Map<DateMidnight, Integer> replies) {
        this.replies = replies;
    }
    public int getMaxPosts() {
        return maxPosts;
    }
    public void setMaxPosts(int maxPosts) {
        this.maxPosts = maxPosts;
    }
    public int getMaxReplies() {
        return maxReplies;
    }
    public void setMaxReplies(int maxLikes) {
        this.maxReplies = maxLikes;
    }
    public int getMaxPlusOnes() {
        return maxPlusOnes;
    }
    public void setMaxPlusOnes(int maxReplies) {
        this.maxPlusOnes = maxReplies;
    }
    public int getMaxCount() {
        return maxCount;
    }
    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }
    public double getAveragePosts() {
        return averagePosts;
    }
    public void setAveragePosts(double averagePosts) {
        this.averagePosts = averagePosts;
    }
    public double getAveragePlusOnes() {
        return averagePlusOnes;
    }
    public void setAveragePlusOnes(double averageLikes) {
        this.averagePlusOnes = averageLikes;
    }
    public double getAverageReplies() {
        return averageReplies;
    }
    public void setAverageReplies(double averageReplies) {
        this.averageReplies = averageReplies;
    }
}