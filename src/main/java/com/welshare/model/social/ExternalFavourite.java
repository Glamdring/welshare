package com.welshare.model.social;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.joda.time.DateTime;

import com.welshare.model.User;
import com.welshare.model.social.ExternalFavourite.ExternalFavouriteId;

@Entity
@IdClass(ExternalFavouriteId.class)
public class ExternalFavourite {
    @Id
    private String externalMessageId;

    @Id
    @ManyToOne
    @JoinColumn(name="authorId", columnDefinition="CHAR(32)")
    private User user;

    @Column(name="favouriteTime")
    private DateTime dateTime;

    public String getExternalMessageId() {
        return externalMessageId;
    }

    public void setExternalMessageId(String externalMessageId) {
        this.externalMessageId = externalMessageId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }

    public static final class ExternalFavouriteId implements Serializable {
        private String externalMessageId;
        private User user;

        public ExternalFavouriteId() {
        }
        public ExternalFavouriteId(String externalMessageId, User user) {
            super();
            this.externalMessageId = externalMessageId;
            this.user = user;
        }
        public String getExternalMessageId() {
            return externalMessageId;
        }
        public void setExternalMessageId(String externalMessageId) {
            this.externalMessageId = externalMessageId;
        }
        public User getUser() {
            return user;
        }
        public void setUser(User user) {
            this.user = user;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((externalMessageId == null) ? 0 : externalMessageId.hashCode());
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
            ExternalFavouriteId other = (ExternalFavouriteId) obj;
            if (externalMessageId == null) {
                if (other.externalMessageId != null) {
                    return false;
                }
            } else if (!externalMessageId.equals(other.externalMessageId)) {
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
