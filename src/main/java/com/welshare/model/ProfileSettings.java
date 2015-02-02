package com.welshare.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import com.welshare.model.enums.Country;
import com.welshare.model.enums.Language;

@Embeddable
public class ProfileSettings implements Serializable {

    private static final long serialVersionUID = -5863504543062963723L;

    @Column(nullable=false)
    private boolean searchableByEmail = true;

    @Column(nullable=false)
    private boolean receiveMailForReplies = true;

    @Column(nullable=false)
    private boolean receiveMailForLikes = true;

    @Column(nullable=false)
    private boolean receiveDailyTopMessagesMail = true;

    @Column(length=250)
    private String bio;

    @Column
    @Enumerated(EnumType.STRING)
    private Country country;

    @Column
    private String city;

    @Column
    @Enumerated(EnumType.STRING)
    private Language language;

    @Column
    private String translateLanguage;

    @Column
    @Type(type = "com.welshare.util.persistence.PersistentDateTime")
    @DateTimeFormat(iso=ISO.DATE_TIME)
    private DateTime birthDate;

    @Column
    private String timeZoneId;

    @Column
    private String originallyFrom;

    @Column
    private String interests;

    @Column(nullable=false)
    private boolean showExternalSiteIndicator;

    @Column(nullable=false)
    private boolean getFollowNotificationsByEmail;

    @Column(nullable=false)
    private int importantMessageScoreThreshold;

    @Column(nullable=false)
    private int importantMessageScoreThresholdRatio;

    @Column(nullable=false)
    private boolean emoticonsEnabled;

    @Column
    private String externalLikeFormat;

    @Column(nullable=false)
    private int minutesOnlinePerDay = DateTimeConstants.MINUTES_PER_HOUR;

    @Column(nullable=false)
    private boolean warnOnMinutesPerDayLimit;

    //TODO enum
    @Column
    private String gender;

    public boolean isSearchableByEmail() {
        return searchableByEmail;
    }

    public void setSearchableByEmail(boolean searchableByEmail) {
        this.searchableByEmail = searchableByEmail;
    }

    public boolean isReceiveMailForReplies() {
        return receiveMailForReplies;
    }

    public void setReceiveMailForReplies(boolean receiveMailForReplies) {
        this.receiveMailForReplies = receiveMailForReplies;
    }

    public boolean isReceiveMailForLikes() {
        return receiveMailForLikes;
    }

    public void setReceiveMailForLikes(boolean receiveMailForLikes) {
        this.receiveMailForLikes = receiveMailForLikes;
    }

    public boolean isReceiveDailyTopMessagesMail() {
        return receiveDailyTopMessagesMail;
    }

    public void setReceiveDailyTopMessagesMail(boolean receiveDailyTopMessagesMail) {
        this.receiveDailyTopMessagesMail = receiveDailyTopMessagesMail;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public String getTranslateLanguage() {
        return translateLanguage;
    }

    public void setTranslateLanguage(String translateLanguage) {
        this.translateLanguage = translateLanguage;
    }

    public DateTime getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(DateTime birthDate) {
        this.birthDate = birthDate;
    }

    public String getTimeZoneId() {
        return timeZoneId;
    }

    public void setTimeZoneId(String timeZoneId) {
        this.timeZoneId = timeZoneId;
    }

    public String getOriginallyFrom() {
        return originallyFrom;
    }

    public void setOriginallyFrom(String originallyFrom) {
        this.originallyFrom = originallyFrom;
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }

    public boolean isShowExternalSiteIndicator() {
        return showExternalSiteIndicator;
    }

    public void setShowExternalSiteIndicator(boolean showExternalSiteIndicator) {
        this.showExternalSiteIndicator = showExternalSiteIndicator;
    }

    public boolean isGetFollowNotificationsByEmail() {
        return getFollowNotificationsByEmail;
    }

    public void setGetFollowNotificationsByEmail(boolean getFollowNotificationsByEmail) {
        this.getFollowNotificationsByEmail = getFollowNotificationsByEmail;
    }

    public int getImportantMessageScoreThreshold() {
        return importantMessageScoreThreshold;
    }

    public void setImportantMessageScoreThreshold(int importantMessageScoreThreshold) {
        this.importantMessageScoreThreshold = importantMessageScoreThreshold;
    }

    public int getImportantMessageScoreThresholdRatio() {
        return importantMessageScoreThresholdRatio;
    }

    public void setImportantMessageScoreThresholdRatio(int importantMessageScoreThresholdRatio) {
        this.importantMessageScoreThresholdRatio = importantMessageScoreThresholdRatio;
    }

    public boolean isEmoticonsEnabled() {
        return emoticonsEnabled;
    }

    public void setEmoticonsEnabled(boolean emoticonsEnabled) {
        this.emoticonsEnabled = emoticonsEnabled;
    }

    public String getExternalLikeFormat() {
        return externalLikeFormat;
    }

    public void setExternalLikeFormat(String externalLikeSourceFormat) {
        this.externalLikeFormat = externalLikeSourceFormat;
    }

    public int getMinutesOnlinePerDay() {
        return minutesOnlinePerDay;
    }

    public void setMinutesOnlinePerDay(int minutesOnlinePerDay) {
        this.minutesOnlinePerDay = minutesOnlinePerDay;
    }

    public boolean isWarnOnMinutesPerDayLimit() {
        return warnOnMinutesPerDayLimit;
    }

    public void setWarnOnMinutesPerDayLimit(boolean warnOnMinutesPerDayLimit) {
        this.warnOnMinutesPerDayLimit = warnOnMinutesPerDayLimit;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
