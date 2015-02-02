package com.welshare.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

@Entity
@NamedQueries(@NamedQuery(name = "ScheduledMessage.getMessages", query = "SELECT sm FROM ScheduledMessage sm WHERE sm.scheduledTime < :toTime"))
public class ScheduledMessage {

    @GeneratedValue(strategy=GenerationType.AUTO)
    @Id
    private long id;

    @Column
    @Type(type = "com.welshare.util.persistence.PersistentDateTime")
    private DateTime scheduledTime;

    @Column
    @Type(type = "com.welshare.util.persistence.PersistentDateTime")
    private DateTime timeOfScheduling;

    @Column(length=300)
    private String text;

    @Column
    private String userId;

    @Column
    private String pictureUrls;
    @Column
    private String externalSites;
    @Column
    private String hideFromUsernames;
    @Column
    private boolean hideFromCloseFriends;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public DateTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(DateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPictureUrls() {
        return pictureUrls;
    }

    public void setPictureUrls(String pictureUrls) {
        this.pictureUrls = pictureUrls;
    }

    public String getExternalSites() {
        return externalSites;
    }

    public void setExternalSites(String externalSite) {
        this.externalSites = externalSite;
    }

    public String getHideFromUsernames() {
        return hideFromUsernames;
    }

    public void setHideFromUsernames(String hideFromUsernames) {
        this.hideFromUsernames = hideFromUsernames;
    }

    public boolean isHideFromCloseFriends() {
        return hideFromCloseFriends;
    }

    public void setHideFromCloseFriends(boolean hideFromCloseFriends) {
        this.hideFromCloseFriends = hideFromCloseFriends;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public DateTime getTimeOfScheduling() {
        return timeOfScheduling;
    }

    public void setTimeOfScheduling(DateTime timeOfScheduling) {
        this.timeOfScheduling = timeOfScheduling;
    }

    // overriding hascode and equals based on id because objects of this class
    // are stored in a collection that relies on them

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
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
        ScheduledMessage other = (ScheduledMessage) obj;
        if (id != other.id) {
            return false;
        }
        return true;
    }

}
