package com.welshare.service.social.model;

import java.util.LinkedHashMap;
import java.util.Map;

import org.joda.time.DateMidnight;

public class FacebookStats {

    private Map<DateMidnight, Integer> posts = new LinkedHashMap<DateMidnight, Integer>();
    private Map<DateMidnight, Integer> comments = new LinkedHashMap<DateMidnight, Integer>();
    private Map<DateMidnight, Integer> likes = new LinkedHashMap<DateMidnight, Integer>();

    private int maxPosts;
    private int maxComments;
    private int maxLikes;

    private int maxCount;

    private double averagePosts;
    private double averageComments;
    private double averageLikes;

    public Map<DateMidnight, Integer> getPosts() {
        return posts;
    }
    public void setPosts(Map<DateMidnight, Integer> messages) {
        this.posts = messages;
    }
    public Map<DateMidnight, Integer> getLikes() {
        return likes;
    }
    public void setLikes(Map<DateMidnight, Integer> likes) {
        this.likes = likes;
    }
    public Map<DateMidnight, Integer> getComments() {
        return comments;
    }
    public void setComments(Map<DateMidnight, Integer> replies) {
        this.comments = replies;
    }
    public int getMaxPosts() {
        return maxPosts;
    }
    public void setMaxPosts(int maxPosts) {
        this.maxPosts = maxPosts;
    }
    public int getMaxComments() {
        return maxComments;
    }
    public void setMaxComments(int maxLikes) {
        this.maxComments = maxLikes;
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
    public double getAveragePosts() {
        return averagePosts;
    }
    public void setAveragePosts(double averagePosts) {
        this.averagePosts = averagePosts;
    }
    public double getAverageLikes() {
        return averageLikes;
    }
    public void setAverageLikes(double averageLikes) {
        this.averageLikes = averageLikes;
    }
    public double getAverageComments() {
        return averageComments;
    }
    public void setAverageComments(double averageReplies) {
        this.averageComments = averageReplies;
    }
}