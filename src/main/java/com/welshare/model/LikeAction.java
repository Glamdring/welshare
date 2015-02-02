package com.welshare.model;


import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

@Entity
public class LikeAction {

    @EmbeddedId
    private LikeActionPK primaryKey = new LikeActionPK();

    public LikeActionPK getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(LikeActionPK primaryKey) {
        this.primaryKey = primaryKey;
    }

    public Message getMessage() {
        return primaryKey.getMessage();
    }

    public User getUser() {
        return primaryKey.getUser();
    }

    public void setMessage(Message message) {
        primaryKey.setMessage(message);
    }

    public void setUser(User user) {
        primaryKey.setUser(user);
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
        LikeAction other = (LikeAction) obj;
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