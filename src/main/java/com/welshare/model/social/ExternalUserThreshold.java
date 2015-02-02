package com.welshare.model.social;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.welshare.model.User;
import com.welshare.model.social.ExternalUserThreshold.ExternalUserThresholdId;

@Entity
@IdClass(ExternalUserThresholdId.class)
public class ExternalUserThreshold {

    @Id
    @ManyToOne
    @JoinColumn(name="userId", columnDefinition="CHAR(32)")
    private User user;

    @Column
    @Id
    private String externalUserId;

    @Column(nullable=false)
    private int threshold;

    @Column(nullable=false)
    private boolean hideReplies;

    public String getExternalUserId() {
        return externalUserId;
    }

    public void setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public boolean isHideReplies() {
        return hideReplies;
    }

    public void setHideReplies(boolean hideReplies) {
        this.hideReplies = hideReplies;
    }

    public static class ExternalUserThresholdId implements Serializable {
        private User user;
        private String externalUserId;

        public ExternalUserThresholdId() {
        }
        public ExternalUserThresholdId(User user, String externalUserId) {
            super();
            this.user = user;
            this.externalUserId = externalUserId;
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public String getExternalUserId() {
            return externalUserId;
        }

        public void setExternalUserId(String externalUserId) {
            this.externalUserId = externalUserId;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((externalUserId == null) ? 0 : externalUserId.hashCode());
            result = prime * result + ((user == null) ? 0 : user.hashCode());
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
            ExternalUserThresholdId other = (ExternalUserThresholdId) obj;
            if (externalUserId == null) {
                if (other.externalUserId != null) {
                    return false;
                }
            } else if (!externalUserId.equals(other.externalUserId)) {
                return false;
            }
            if (user == null) {
                if (other.user != null) {
                    return false;
                }
            } else if (!user.equals(other.user)) {
                return false;
            }
            return true;
        }
    }
}
