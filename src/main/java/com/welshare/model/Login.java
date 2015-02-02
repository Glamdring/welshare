package com.welshare.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

@Entity
@NamedQuery(
        name = "Login.getByToken",
        query = "SELECT login FROM Login login WHERE login.token=:token AND login.series=:series"
)
public class Login {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private long id;

    @Column
    @Index(name="mainIndex")
    private String token;

    @Column
    @Index(name="mainIndex")
    private String series;

    @ManyToOne
    @JoinColumn(name="userId", columnDefinition="CHAR(32)")
    private User user;

    @Column(name = "lastLoginTime")
    @Type(type = "com.welshare.util.persistence.PersistentDateTime")
    private DateTime lastLoginTime;

    @Column
    private String ip;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public DateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(DateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
