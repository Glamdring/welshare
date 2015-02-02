package com.welshare.service.model;

import java.io.Serializable;

import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;

import com.welshare.model.User;
import com.welshare.model.enums.Country;

public class UserDetails implements Serializable {

    private String id;
    private String externalId;
    private String externalUrl;
    private String username;
    private String names;
    private String gravatarHash;
    private int score;
    private int externalScore;
    private int messages;
    private String profilePictureURI;
    private String smallProfilePictureURI;
    private String largeProfilePictureURI;
    private boolean followingCurrentUser;
    private boolean followedByCurrentUser;
    private boolean closeFriendOfCurrentUser;
    private int likesThreshold;
    private boolean hideReplies;
    private int followers;
    private int following;
    private int closeFriends;
    private String city;
    private Country country;
    private String bio;
    private String interests;
    private String originallyFrom;
    private DateTime birthDate;

    public UserDetails(User user) {
        BeanUtils.copyProperties(user.getProfile(), this);
        BeanUtils.copyProperties(user, this);
    }

    public UserDetails() {
        // default constructor
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNames() {
        return names;
    }

    public void setNames(String names) {
        this.names = names;
    }

    public String getGravatarHash() {
        return gravatarHash;
    }

    public void setGravatarHash(String gravatarHash) {
        this.gravatarHash = gravatarHash;
    }


    public int getScore() {
        return score;
    }


    public void setScore(int score) {
        this.score = score;
    }

    public String getProfilePictureURI() {
        return profilePictureURI;
    }

    public void setProfilePictureURI(String profilePictureURI) {
        this.profilePictureURI = profilePictureURI;
    }

    public boolean isFollowingCurrentUser() {
        return followingCurrentUser;
    }

    public void setFollowingCurrentUser(boolean isFollowingCurrentUser) {
        this.followingCurrentUser = isFollowingCurrentUser;
    }

    public boolean isFollowedByCurrentUser() {
        return followedByCurrentUser;
    }

    public void setFollowedByCurrentUser(boolean followedByCurrentUser) {
        this.followedByCurrentUser = followedByCurrentUser;
    }

    public boolean isCloseFriendOfCurrentUser() {
        return closeFriendOfCurrentUser;
    }

    public void setCloseFriendOfCurrentUser(boolean friendWithCurrentUser) {
        this.closeFriendOfCurrentUser = friendWithCurrentUser;
    }

    public int getFollowers() {
        return followers;
    }

    public void setFollowers(int followers) {
        this.followers = followers;
    }

    public int getFollowing() {
        return following;
    }

    public void setFollowing(int following) {
        this.following = following;
    }

    public int getCloseFriends() {
        return closeFriends;
    }

    public void setCloseFriends(int friends) {
        this.closeFriends = friends;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }

    public String getOriginallyFrom() {
        return originallyFrom;
    }

    public void setOriginallyFrom(String originallyFrom) {
        this.originallyFrom = originallyFrom;
    }

    public DateTime getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(DateTime birthDate) {
        this.birthDate = birthDate;
    }

    public int getLikesThreshold() {
        return likesThreshold;
    }

    public void setLikesThreshold(int threshold) {
        this.likesThreshold = threshold;
    }

    public String getSmallProfilePictureURI() {
        return smallProfilePictureURI;
    }

    public void setSmallProfilePictureURI(String smallProfilePictureURI) {
        this.smallProfilePictureURI = smallProfilePictureURI;
    }

    public String getLargeProfilePictureURI() {
        return largeProfilePictureURI;
    }

    public void setLargeProfilePictureURI(String largeProfilePictureURI) {
        this.largeProfilePictureURI = largeProfilePictureURI;
    }

    public int getMessages() {
        return messages;
    }

    public void setMessages(int messageCount) {
        this.messages = messageCount;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public int getExternalScore() {
        return externalScore;
    }

    public void setExternalScore(int externalScore) {
        this.externalScore = externalScore;
    }

    public boolean isHideReplies() {
        return hideReplies;
    }

    public void setHideReplies(boolean hideReplies) {
        this.hideReplies = hideReplies;
    }

    public boolean isExternal() {
        return externalId != null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        UserDetails other = (UserDetails) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }
}