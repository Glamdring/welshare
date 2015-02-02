package com.welshare.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

@Entity
public class ShortenedLinkVisitData {

    public static final ShortenedLinkVisitData EMPTY = new ShortenedLinkVisitData();

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private long id;

    @ManyToOne
    private ShortUrl shortUrl;

    @Column(length=255)
    private String referer;

    @Column
    private String language;

    @Column
    private String ip;

    @Column
    private String sessionId;

    @Column(name="visitTime")
    @Type(type = "com.welshare.util.persistence.PersistentDateTime")
    @DateTimeFormat(iso=ISO.DATE_TIME)
    private DateTime dateTime;

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public ShortUrl getShortUrl() {
        return shortUrl;
    }
    public void setShortUrl(ShortUrl shortUrl) {
        this.shortUrl = shortUrl;
    }
    public String getReferer() {
        return referer;
    }
    public void setReferer(String referer) {
        this.referer = referer;
    }
    public String getLanguage() {
        return language;
    }
    public void setLanguage(String language) {
        this.language = language;
    }
    public String getIp() {
        return ip;
    }
    public void setIp(String ip) {
        this.ip = ip;
    }
    public String getSessionId() {
        return sessionId;
    }
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    public DateTime getDateTime() {
        return dateTime;
    }
    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }
    @Override
    public String toString() {
        return "ShortenedLinkVisitData [id=" + id + ", shortUrl=" + shortUrl
                + ", referer=" + referer + ", language=" + language + ", ip="
                + ip + ", sessionId=" + sessionId + "]";
    }
}
