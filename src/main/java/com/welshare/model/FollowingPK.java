package com.welshare.model;

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Embeddable
public class FollowingPK implements Serializable {

    @ManyToOne
    @JoinColumn(name="followerId", columnDefinition="CHAR(32)")
    private User follower;

    @ManyToOne
    @JoinColumn(name="followedId", columnDefinition="CHAR(32)")
    private User followed;

    public FollowingPK() {
        // default constructor
    }

    public FollowingPK(User follower, User followed) {
        super();
        this.follower = follower;
        this.followed = followed;
    }

    public User getFollower() {
        return follower;
    }

    public void setFollower(User follower) {
        this.follower = follower;
    }

    public User getFollowed() {
        return followed;
    }

    public void setFollowed(User followed) {
        this.followed = followed;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((followed == null) ? 0 : followed.hashCode());
        result = prime * result
                + ((follower == null) ? 0 : follower.hashCode());
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
        FollowingPK other = (FollowingPK) obj;
        if (followed == null) {
            if (other.followed != null) {
                return false;
            }
        } else if (!followed.equals(other.followed)) {
            return false;
        }
        if (follower == null) {
            if (other.follower != null) {
                return false;
            }
        } else if (!follower.equals(other.follower)) {
            return false;
        }
        return true;
    }
}