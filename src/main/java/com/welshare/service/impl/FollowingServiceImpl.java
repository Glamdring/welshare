package com.welshare.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.welshare.dao.FollowingDao;
import com.welshare.dao.UserDao;
import com.welshare.model.Following;
import com.welshare.model.User;
import com.welshare.model.enums.NotificationType;
import com.welshare.model.social.ExternalUserThreshold;
import com.welshare.service.EmailService;
import com.welshare.service.FollowingService;
import com.welshare.service.NotificationEventService;
import com.welshare.service.UserService;
import com.welshare.service.annotations.GraphTransactional;
import com.welshare.service.exception.OperationUnsuccessfulException;
import com.welshare.service.model.UserDetails;
import com.welshare.service.social.SocialNetworkService;
import com.welshare.service.social.SocialUtils;
import com.welshare.service.social.qualifiers.Twitter;

@Service
public class FollowingServiceImpl extends BaseServiceImpl implements FollowingService {

    private static final Logger logger = LoggerFactory.getLogger(FollowingServiceImpl.class);

    @Inject
    private UserDao userDao;

    @Inject
    private UserService userService;

    @Inject
    private FollowingDao followingDao;

    @Inject
    private NotificationEventService eventService;

    @Inject
    private EmailService emailService;

    @Inject
    private List<SocialNetworkService> socialNetworkServices;

    @Inject @Twitter
    private SocialNetworkService twitterService;

    @Value("${base.url}")
    private String baseUrl;

    @Value("${information.email.sender}")
    private String infoMailSender;

    @Override
    @GraphTransactional
    public void follow(String followerId, String followedId) {

        if (followerId.equals(followedId)) {
            throw new IllegalArgumentException("Users cannot follow themselves");
        }

        User follower = userDao.getById(User.class, followerId, true);
        User followed = userDao.getById(User.class, followedId, true);

        logger.info(follower.getUsername() + " followed " + followed.getUsername());

        Following existingFollowing = findFollowing(follower, followed, true);

        logger.debug("Existing Following is: " + existingFollowing);

        // skip, if already following
        if (existingFollowing != null) {
            return;
        }
        Following following = new Following();
        following.setFollowed(followed);
        following.setFollower(follower);
        following.setDateTime(new DateTime());

        // increment the followers count only if there wasn't a previous
        // following. This means that the user has followed the other one
        // before the friend request, and the counter has been already incremented
        // (and an existing following means this is a friend request now)
        followed.incrementFollowers();
        follower.incrementFollowing();
        userService.save(follower);
        userService.save(followed);

        if (logger.isDebugEnabled()) {
            logger.debug("Successfully incremented following counters. "
                + followed.getUsername() + " has "
                + followed.getFollowers() + " followers and "
                + follower.getUsername() + " now has "
                + follower.getFollowing() + " following");
        }


        // sending email to announce the following
        if (followed.getProfile().isGetFollowNotificationsByEmail()) {
            sendFollowingNotificationEmail(follower, followed);
        }

        eventService.createUserEvent(followed, NotificationType.FOLLOW, follower);

        Following result = getDao().saveFollowing(following);
        if (result == null) {
            throw new OperationUnsuccessfulException("Following failed");
        }

        // attempt following on twitter as well
        if (result.getFollowed().getTwitterSettings() != null && result.getFollowed().getTwitterSettings().isFetchImages()
                && result.getFollowed().getTwitterSettings().getUserId() > 0) {
            twitterService.follow(twitterService.getIdPrefix() + result.getFollowed().getTwitterSettings().getUserId(), result.getFollower());
        }
    }

    @Override
    @GraphTransactional
    public List<UserDetails> getFollowersDetails(String followedId) {
        List<User> users = getDao().getFollowers(userDao.getById(User.class, followedId));

        return extractUserDetails(users);
    }

    @Override
    @GraphTransactional
    public List<User> getFollowing(String followedId) {
        return getDao().getFollowers(userDao.getById(User.class, followedId));
    }

    @Override
    @GraphTransactional
    public List<UserDetails> getFollowingDetails(String followerId) {
        List<User> users = getDao().getFollowing(userDao.getById(User.class, followerId));
        return extractUserDetails(users);
    }

    @Override
    @GraphTransactional
    public List<UserDetails> getCloseFriendsDetails(String userId) {
       User user = userDao.getById(User.class, userId);
       List<User> friends = getDao().getCloseFriends(user);
       return extractUserDetails(friends);
    }

    private List<UserDetails> extractUserDetails(List<User> users) {
        List<UserDetails> userDetailsList = new ArrayList<UserDetails>(users.size());
        for (User user : users) {
            userDetailsList.add(new UserDetails(user));
        }

        return userDetailsList;
    }

    @GraphTransactional
    @Override
    public Following findFollowing(User follower, User followed) {
        return findFollowing(follower, followed, false);
    }

    private Following findFollowing(User follower, User followed, boolean lock) {
        Following following = getDao().findFollowing(follower, followed, lock);
        return following;
    }

    @Override
    protected FollowingDao getDao() {
        return followingDao;
    }

    @Override
    @GraphTransactional
    public Set<UserDetails> getFriendSuggestions(String userId) {

        User user = userDao.getById(User.class, userId);

        Set<UserDetails> details = new HashSet<UserDetails>();
        for (SocialNetworkService sns : socialNetworkServices) {
            details.addAll(sns.getFriends(user));
        }

        // if the friends are already followed by the current user on welshare,
        // remove them from the suggestion list
        List<UserDetails> following = getFollowingDetails(userId);
        for (Iterator<UserDetails> it = details.iterator(); it.hasNext();) {
            UserDetails eligibleFriend = it.next();
            for (UserDetails followingUser : following) {
                if (eligibleFriend.getId().equals(followingUser.getId())) {
                    it.remove();
                }
            }
        }

        return details;
    }

    @Override
    @GraphTransactional
    public void unfollow(String followerId, String followedId) {
        User follower = userDao.getById(User.class, followerId, true);
        User followed = userDao.getById(User.class, followedId, true);
        Following f = findFollowing(follower, followed, true);
        if (f == null) {
            return;
        }

        followingDao.deleteFollowing(f);

        follower.decrementFollowing();
        followed.decrementFollowers();
        userService.save(follower);
        userService.save(followed);
    }

    @Override
    @GraphTransactional
    public void updateCurrentUserFollowings(String userId, List<UserDetails> result, boolean fillMetaData) {
        if (userId == null) {
            return;
        }
        User user = userDao.getById(User.class, userId);

        if (user == null || result == null || result.isEmpty() || result.get(0) == null) {
            return;
        }
        Set<UserDetails> followed = new HashSet<UserDetails>(getFollowingDetails(user.getId()));
        Set<UserDetails> followers = new HashSet<UserDetails>(getFollowersDetails(user.getId()));
        Set<UserDetails> friends = new HashSet<UserDetails>(getCloseFriendsDetails(user.getId()));

        Map<String, Following> followingMetaData = Collections.emptyMap();
        if (fillMetaData) {
            followingMetaData = getDao().getFollowingMetaData(user);
        }

        for (UserDetails usr : result) {

            if (fillMetaData) {
                Following meta = followingMetaData.get(usr.getId());
                if (meta != null) {
                    usr.setLikesThreshold(meta.getLikesThreshold());
                }
            }

            if (friends.contains(usr)) {
                usr.setCloseFriendOfCurrentUser(true);
                usr.setFollowedByCurrentUser(true);
                usr.setFollowingCurrentUser(true);
                continue;
            }

            if (followed.contains(usr)) {
                usr.setFollowedByCurrentUser(true);
            }

            if (followers.contains(usr)) {
                usr.setFollowingCurrentUser(true);
            }
        }
    }

    @Override
    @GraphTransactional
    public void setTreshold(String userId, String targetUserId, int value, boolean hideReplies) {
        SocialNetworkService sns = SocialUtils.getSocialNetworkService(socialNetworkServices, targetUserId);
        User user = userDao.getById(User.class, userId);
        if (sns == null) {
            User targetUser = userDao.getById(User.class, targetUserId);
            Following following = findFollowing(user, targetUser);
            following.setLikesThreshold(value);
            following.setHideReplies(hideReplies);
            getDao().updateFollowing(following);
        } else {
            ExternalUserThreshold threshold = new ExternalUserThreshold();
            threshold.setUser(user);
            threshold.setExternalUserId(targetUserId);
            threshold.setThreshold(value);
            threshold.setHideReplies(hideReplies);
            userService.save(threshold); // this will either save new or update existing
        }
    }

    @Override
    @GraphTransactional
    public boolean toggleCloseFriend(String followerId, String followedId) {
        User user = userDao.getById(User.class, followerId);
        User targetUser = userDao.getById(User.class, followedId);
        Following following = findFollowing(user, targetUser);
        following.setCloseFriend(!following.isCloseFriend());
        getDao().updateFollowing(following);

        return following.isCloseFriend();
    }

    @Override
    @GraphTransactional
    public void save(User user) {
        followingDao.persist(user);
    }

    private void sendFollowingNotificationEmail(User follower,
            User followed) {

        Map<String, Object> model = Maps.newHashMap();
        model.put("follower", follower);
        model.put("followed", followed);

        EmailService.EmailDetails details = new EmailService.EmailDetails();
        details.setMessageTemplate("following.vm")
            .setMessageTemplateModel(model)
            .setHtml(true)
            .setSubjectKey("followingEmailSubject")
            .setSubjectParams(new String[] {follower.getNames()})
            .setFrom(infoMailSender).setTo(followed.getEmail())
            .setLocale(followed.getProfile().getLanguage().toLocale());

        emailService.send(details);
    }

    @Override
    public List<User> getCloseFriends(String followerId) {
        return getDao().getCloseFriends(userDao.getById(User.class, followerId));
    }
}
