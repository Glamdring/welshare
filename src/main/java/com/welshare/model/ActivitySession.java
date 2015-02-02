package com.welshare.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

@Entity
public class ActivitySession {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private long id;

    @ManyToOne
    @JoinColumn(name="userId", columnDefinition="CHAR(32)")
    private User user;

    @Column
    @Type(type = "com.welshare.util.persistence.PersistentDateTime")
    private DateTime start;

    @Column
    @Type(type = "com.welshare.util.persistence.PersistentDateTime")
    private DateTime end;

    // a bit of redundancy here - storing both end time and duration
    @Column(nullable=false)
    private int seconds;

    @Transient
    private boolean userWarned;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public DateTime getStart() {
        return start;
    }

    public void setStart(DateTime start) {
        this.start = start;
    }

    public DateTime getEnd() {
        return end;
    }

    public void setEnd(DateTime end) {
        this.end = end;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public boolean isUserWarned() {
        return userWarned;
    }

    public void setUserWarned(boolean userWarned) {
        this.userWarned = userWarned;
    }
}
