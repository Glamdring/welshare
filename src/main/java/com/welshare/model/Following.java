package com.welshare.model;


import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

@Entity
public class Following {

    @EmbeddedId
    private FollowingPK primaryKey = new FollowingPK();

    @Column(nullable=false)
    private boolean closeFriend;

    @Column(nullable=false)
    private int likesThreshold;

    @Column(nullable=false)
    private boolean hideReplies;

    @Column(name = "followingDateTime")
    @Type(type = "com.welshare.util.persistence.PersistentDateTime")
    @DateTimeFormat(iso=ISO.DATE_TIME)
    private DateTime dateTime;

    public FollowingPK getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(FollowingPK primaryKey) {
        this.primaryKey = primaryKey;
    }

    public User getFollowed() {
        return primaryKey.getFollowed();
    }

    public User getFollower() {
        return primaryKey.getFollower();
    }

    public void setFollowed(User followed) {
        primaryKey.setFollowed(followed);
    }

    public void setFollower(User follower) {
        primaryKey.setFollower(follower);
    }

    public boolean isCloseFriend() {
        return closeFriend;
    }

    public void setCloseFriend(boolean friend) {
        this.closeFriend = friend;
    }

    public int getLikesThreshold() {
        return likesThreshold;
    }

    public void setLikesThreshold(int threshold) {
        this.likesThreshold = threshold;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }

    public boolean isHideReplies() {
        return hideReplies;
    }

    public void setHideReplies(boolean hideReplies) {
        this.hideReplies = hideReplies;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((primaryKey == null) ? 0 : primaryKey.hashCode());
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
        Following other = (Following) obj;
        if (primaryKey == null) {
            if (other.primaryKey != null) {
                return false;
            }
        } else if (!primaryKey.equals(other.primaryKey)) {
            return false;
        }
        return true;
    }
}
