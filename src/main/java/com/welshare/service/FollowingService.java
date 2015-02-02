package com.welshare.service;

import java.util.List;
import java.util.Set;

import com.welshare.model.Following;
import com.welshare.model.User;
import com.welshare.service.model.UserDetails;

public interface FollowingService extends BaseService {

    /**
     * Start following a given user
     * @param follower
     * @param followed
     *
     */
    void follow(String followerId, String followedId);

    void unfollow(String followerId, String followedId);

    boolean toggleCloseFriend(String followerId, String followedId);

    List<UserDetails> getFollowersDetails(String followedId);

    List<UserDetails> getFollowingDetails(String followerId);

    /**
     * Gets a list of close friends to the passed user
     * @param username
     * @return list of close friends' details
     */
    List<UserDetails> getCloseFriendsDetails(String userId);

    Set<UserDetails> getFriendSuggestions(String userId);

    /**
     * Updates the list of user details with data related to the current user.
     * Optionally the relationship metadata (threshold, for example) can be filled.
     * @param current user
     * @param list of user details
     * @param fillFollowingMetaData
     */
    void updateCurrentUserFollowings(String userId, List<UserDetails> result, boolean fillFollowingMetaData);

    void setTreshold(String userId, String targetUserId, int value, boolean hideReplies);

    Following findFollowing(User follower, User followed);

    void save(User user);

    List<User> getFollowing(String followedId);

    List<User> getCloseFriends(String id);
}
