package com.welshare.service.social.model;

import java.util.LinkedHashMap;
import java.util.Map;

import org.joda.time.DateMidnight;

public class TwitterStats {

    private Map<DateMidnight, Integer> tweets = new LinkedHashMap<DateMidnight, Integer>();
    private Map<DateMidnight, Integer> retweets = new LinkedHashMap<DateMidnight, Integer>();
    private Map<DateMidnight, Integer> mentions = new LinkedHashMap<DateMidnight, Integer>();

    private int maxTweets;
    private int maxRetweets;
    private int maxMentions;

    private int maxCount;

    private double averageTweets;
    private double averageRetweets;
    private double averageMentions;


    public Map<DateMidnight, Integer> getTweets() {
        return tweets;
    }
    public void setTweets(Map<DateMidnight, Integer> tweets) {
        this.tweets = tweets;
    }
    public Map<DateMidnight, Integer> getRetweets() {
        return retweets;
    }
    public void setRetweets(Map<DateMidnight, Integer> likes) {
        this.retweets = likes;
    }
    public Map<DateMidnight, Integer> getMentions() {
        return mentions;
    }
    public void setMentions(Map<DateMidnight, Integer> mentions) {
        this.mentions = mentions;
    }
    public int getMaxTweets() {
        return maxTweets;
    }
    public void setMaxTweets(int maxTweets) {
        this.maxTweets = maxTweets;
    }
    public int getMaxRetweets() {
        return maxRetweets;
    }
    public void setMaxRetweets(int maxRetweets) {
        this.maxRetweets = maxRetweets;
    }
    public int getMaxMentions() {
        return maxMentions;
    }
    public void setMaxMentions(int maxMentions) {
        this.maxMentions = maxMentions;
    }
    public double getAverageTweets() {
        return averageTweets;
    }
    public void setAverageTweets(double averageTweets) {
        this.averageTweets = averageTweets;
    }
    public double getAverageRetweets() {
        return averageRetweets;
    }
    public void setAverageRetweets(double averageRetweets) {
        this.averageRetweets = averageRetweets;
    }
    public double getAverageMentions() {
        return averageMentions;
    }
    public void setAverageMentions(double averageMentions) {
        this.averageMentions = averageMentions;
    }
    public int getMaxCount() {
        return maxCount;
    }
    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }
}
