package com.welshare.dao;

import java.util.List;
import java.util.Map;

import com.welshare.model.Following;
import com.welshare.model.User;

public interface FollowingDao extends Dao {

    List<User> getFollowers(User followed);

    List<User> getFollowing(User follower);

    List<User> getCloseFriends(User user);

    Map<String, Following> getFollowingMetaData(User user);

    Following findFollowing(User follower, User followed, boolean lock);

    Following saveFollowing(Following following);

    void deleteFollowing(Following f);

    void updateFollowing(Following following);
}
