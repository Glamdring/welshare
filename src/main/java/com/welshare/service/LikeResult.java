package com.welshare.service;

import java.util.List;

import com.welshare.model.Message;

public class LikeResult {

    public static final LikeResult EMPTY = new LikeResult(0);

    private int newLikes;
    private Message message;
    private List<String> socialNetworkResults;

    public LikeResult(int score) {
        this.newLikes = score;
    }
    public LikeResult(Message message, int likes) {
        this.message = message;
        this.newLikes = likes;
    }

    public int getNewLikes() {
        return newLikes;
    }
    public void setNewLikes(int newScore) {
        this.newLikes = newScore;
    }
    public Message getMessage() {
        return message;
    }
    public void setMessage(Message message) {
        this.message = message;
    }
    public List<String> getSocialNetworkResults() {
        return socialNetworkResults;
    }
    public void setSocialNetworkResults(List<String> socialNetworkResults) {
        this.socialNetworkResults = socialNetworkResults;
    }
}
