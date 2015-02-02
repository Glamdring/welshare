package com.welshare.dao.jpa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.welshare.dao.FollowingDao;
import com.welshare.model.Following;
import com.welshare.model.FollowingPK;
import com.welshare.model.User;

//@Repository
@Deprecated
public class FollowingDaoJpa extends BaseDao implements FollowingDao {

    private static final String USER_PARAM = "user";

    @Override
    public List<User> getFollowers(User followed) {
        return findByQuery(new QueryDetails()
            .setQueryName("User.getFollowers")
            .setParamNames(new String[] { "followed" })
            .setParamValues(new Object[] { followed }));
    }

    @Override
    public List<User> getFollowing(User follower) {
        return findByQuery(new QueryDetails()
            .setQueryName("User.getFollowing")
            .setParamNames(new String[] { "follower" })
            .setParamValues(new Object[] { follower }));
    }

    @Override
    public List<User> getCloseFriends(User user) {
        return findByQuery(new QueryDetails()
            .setQueryName("User.getFriends")
            .setParamNames(new String[] { USER_PARAM })
            .setParamValues(new Object[] { user }));
    }

    @Override
    public Map<String, Following> getFollowingMetaData(User user) {
        List<Following> list = findByQuery(new QueryDetails()
            .setQueryName("User.getFollowingMetaData")
            .setParamNames(new String[] { USER_PARAM })
            .setParamValues(new Object[] { user }));

        Map<String, Following> map = new HashMap<String, Following>(list.size());
        for (Following following : list) {
            map.put(following.getFollowed().getId(), following);
        }
        return map;
    }

    @Override
    public Following findFollowing(User follower, User followed, boolean lock) {
        return getById(Following.class, new FollowingPK(follower, followed), lock);
    }

    @Override
    public Following saveFollowing(Following following) {
        return persist(following);
    }

    @Override
    public void deleteFollowing(Following f) {
        delete(f);
    }

    @Override
    public void updateFollowing(Following following) {
        persist(following);
    }
}
