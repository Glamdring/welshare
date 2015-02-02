package com.welshare.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

@Entity
public class ShortUrl {

    @Id
    @Column(name="shortKey", columnDefinition="VARBINARY(6)")
    private String key;

    @Column(length=1000)
    private String longUrl;

    @Column(nullable=false)
    private int visits;

    @ManyToOne
    @JoinColumn(name="userId", columnDefinition="CHAR(32)")
    private User user;

    @Column
    @Type(type = "com.welshare.util.persistence.PersistentDateTime")
    private DateTime timeAdded;

    @Column(nullable=false)
    private boolean trackViral;

    @Column(nullable=false)
    private boolean showTopBar;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

    public int getVisits() {
        return visits;
    }

    public void setVisits(int visits) {
        this.visits = visits;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public DateTime getTimeAdded() {
        return timeAdded;
    }

    public void setTimeAdded(DateTime timeAdded) {
        this.timeAdded = timeAdded;
    }

    public boolean isTrackViral() {
        return trackViral;
    }

    public void setTrackViral(boolean trackViral) {
        this.trackViral = trackViral;
    }

    public boolean isShowTopBar() {
        return showTopBar;
    }

    public void setShowTopBar(boolean showTopBar) {
        this.showTopBar = showTopBar;
    }
}
