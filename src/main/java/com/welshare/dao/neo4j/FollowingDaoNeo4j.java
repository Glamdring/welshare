package com.welshare.dao.neo4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.stereotype.Repository;

import com.welshare.dao.FollowingDao;
import com.welshare.dao.UserDao;
import com.welshare.model.Following;
import com.welshare.model.User;

@Repository
public class FollowingDaoNeo4j extends BaseNeo4jDao implements FollowingDao {

    private static final String ID_PROPERTY = User.class.getName() + ID_SUFFIX;
    private static final String DATE_TIME_PROPERTY = "dateTime";
    private static final String LIKES_THRESHOLD_PROPERTY = "likesThreshold";
    private static final String CLOSE_FRIEND_PROPERTY = "closeFriend";

    @Inject
    private UserDao userDao;

    @Override
    public List<User> getFollowers(User followed) {
        List<String> userIds = new ArrayList<String>();
        Node node = getUserNode(followed);
        for (Relationship r : node.getRelationships(Direction.INCOMING,
                UserRelationship.FOLLOWS)) {
            userIds.add((String) r.getStartNode().getProperty(ID_PROPERTY));
        }
        return userDao.getUsers(userIds);
    }

    @Override
    public List<User> getFollowing(User follower) {
        List<String> userIds = new ArrayList<String>();
        Node node = getUserNode(follower);
        for (Relationship r : node.getRelationships(Direction.OUTGOING,
                UserRelationship.FOLLOWS)) {
            userIds.add((String) r.getEndNode().getProperty(ID_PROPERTY));
        }
        return userDao.getUsers(userIds);
    }

    @Override
    public List<User> getCloseFriends(User user) {
        List<String> userIds = new ArrayList<String>();
        Node node = getUserNode(user);
        for (Relationship r : node.getRelationships(Direction.OUTGOING,
                UserRelationship.FOLLOWS)) {
            if (r.getProperty(CLOSE_FRIEND_PROPERTY).equals(Boolean.TRUE)) {
                userIds.add((String) r.getEndNode().getProperty(ID_PROPERTY));
            }
        }
        return userDao.getUsers(userIds);
    }

    @Override
    public Map<String, Following> getFollowingMetaData(User user) {
        Node node = getUserNode(user);

        Map<String, Following> map = new HashMap<String, Following>();
        for (Relationship r : node.getRelationships(Direction.OUTGOING, UserRelationship.FOLLOWS)) {
            User followed = getBean(User.class, r.getEndNode());
            map.put(followed.getId(), getFollowing(user, followed, r));
        }

        return map;
    }

    @Override
    public Following findFollowing(User follower, User followed, boolean lock) {
        Node node = getUserNode(follower);
        Iterable<Relationship> relationships = node.getRelationships(
                Direction.OUTGOING, UserRelationship.FOLLOWS);
        for (Relationship r : relationships) {
            if (r.getEndNode().getProperty(ID_PROPERTY)
                    .equals(followed.getId())) {
                return getFollowing(follower, followed, r);
            }
        }
        return null;
    }

    private Following getFollowing(User follower, User followed, Relationship r) {
        Following following = new Following();
        following.setFollower(follower);
        following.setFollowed(followed);
        following.setDateTime(new DateTime(r.getProperty(DATE_TIME_PROPERTY)));
        following.setLikesThreshold((Integer) r.getProperty(LIKES_THRESHOLD_PROPERTY));
        following.setCloseFriend((Boolean) r.getProperty(CLOSE_FRIEND_PROPERTY));
        return following;
    }

    public Following saveFollowing(Following following) {
        Node startNode = getUserNode(following.getFollower());
        Node endNode = getUserNode(following.getFollowed());

        Relationship r = startNode.createRelationshipTo(endNode,
                UserRelationship.FOLLOWS);
        fillFollowingProperties(following, r);

        return following;
    }

    @Override
    public void deleteFollowing(Following following) {
        Node node = getUserNode(following.getFollower());
        for (Relationship r : node.getRelationships(Direction.OUTGOING,
                UserRelationship.FOLLOWS)) {
            if (r.getEndNode().getProperty(ID_PROPERTY)
                    .equals(following.getFollowed().getId())) {
                r.delete();
                return;
            }
        }
    }

    @Override
    public void updateFollowing(Following following) {
        Node node = getUserNode(following.getFollower());
        for (Relationship r : node.getRelationships(Direction.OUTGOING,
                UserRelationship.FOLLOWS)) {
            if (r.getEndNode().getProperty(ID_PROPERTY)
                    .equals(following.getFollowed().getId())) {
                fillFollowingProperties(following, r);
                return;
            }
        }
    }

    private Node getUserNode(User user) {
        Node node = getIndex().getSingleNode(ID_PROPERTY, user.getId());
        // if user node does not yet exist in the graph db - create it
        if (node == null) {
            persist(user);
            node = getIndex().getSingleNode(ID_PROPERTY, user.getId());
        }
        return node;
    }

    private void fillFollowingProperties(Following following, Relationship r) {
        r.setProperty(DATE_TIME_PROPERTY, following.getDateTime().getMillis());
        r.setProperty(LIKES_THRESHOLD_PROPERTY, following.getLikesThreshold());
        r.setProperty(CLOSE_FRIEND_PROPERTY, following.isCloseFriend());
    }

    public static enum UserRelationship implements RelationshipType {
        FOLLOWS
    }
}