package com.welshare.web.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.springframework.format.annotation.DateTimeFormat;

import com.welshare.model.enums.Country;
import com.welshare.model.enums.Language;

public class ProfileDetails {

    @Size(min=4, max=50)
    @NotNull
    @Pattern(regexp="[\\p{L}0-9_\\.]*[\\p{L}0-9_]{1}")
    private String username;

    @Email
    @NotEmpty
    private String email;

    @NotEmpty
    private String names;
    private boolean searchableByEmail;
    @Size(max=250)
    private String bio;
    private Country country;
    private String city;
    private Language language;
    private String profilePictureURI;
    private String smallProfilePictureURI;
    private String largeProfilePictureURI;
    private String gravatarHash;
    private String translateLanguage;
    private String interests;
    private String originallyFrom;
    private boolean showExternalSiteIndicator;
    private boolean getFollowNotificationsByEmail;
    @DateTimeFormat(pattern="dd.MM.yyyy")
    private DateTime birthDate;
    private boolean receiveMailForReplies = true;
    private boolean receiveMailForLikes = true;
    private boolean emoticonsEnabled;
    private boolean receiveDailyTopMessagesMail = true;
    private int minutesOnlinePerDay = DateTimeConstants.MINUTES_PER_HOUR;
    private boolean warnOnMinutesPerDayLimit;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNames() {
        return names;
    }

    public void setNames(String names) {
        this.names = names;
    }

    public boolean isSearchableByEmail() {
        return searchableByEmail;
    }

    public void setSearchableByEmail(boolean searchableByEmail) {
        this.searchableByEmail = searchableByEmail;
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

    public String getProfilePictureURI() {
        return profilePictureURI;
    }

    public void setProfilePictureURI(String profilePictureURI) {
        this.profilePictureURI = profilePictureURI;
    }

    public String getGravatarHash() {
        return gravatarHash;
    }

    public void setGravatarHash(String gravatarHash) {
        this.gravatarHash = gravatarHash;
    }

    public String getTranslateLanguage() {
        return translateLanguage;
    }

    public void setTranslateLanguage(String translateLanguage) {
        this.translateLanguage = translateLanguage;
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }

    public String getOriginallyFrom() {
        return originallyFrom;
    }

    public void setOriginallyFrom(String originallyFrom) {
        this.originallyFrom = originallyFrom;
    }

    public DateTime getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(DateTime birthDate) {
        this.birthDate = birthDate;
    }

    public boolean isShowExternalSiteIndicator() {
        return showExternalSiteIndicator;
    }

    public void setShowExternalSiteIndicator(boolean showExternalSiteIndicator) {
        this.showExternalSiteIndicator = showExternalSiteIndicator;
    }

    public String getSmallProfilePictureURI() {
        return smallProfilePictureURI;
    }

    public void setSmallProfilePictureURI(String smallProfilePictureURI) {
        this.smallProfilePictureURI = smallProfilePictureURI;
    }

    public String getLargeProfilePictureURI() {
        return largeProfilePictureURI;
    }

    public void setLargeProfilePictureURI(String largeProfilePictureURI) {
        this.largeProfilePictureURI = largeProfilePictureURI;
    }

    public boolean isGetFollowNotificationsByEmail() {
        return getFollowNotificationsByEmail;
    }

    public void setGetFollowNotificationsByEmail(
            boolean getFollowNotificationsByEmail) {
        this.getFollowNotificationsByEmail = getFollowNotificationsByEmail;
    }

    public boolean isReceiveMailForReplies() {
        return receiveMailForReplies;
    }

    public void setReceiveMailForReplies(boolean sendMailForReplies) {
        this.receiveMailForReplies = sendMailForReplies;
    }

    public boolean isReceiveMailForLikes() {
        return receiveMailForLikes;
    }

    public void setReceiveMailForLikes(boolean sendMailForLikes) {
        this.receiveMailForLikes = sendMailForLikes;
    }

    public boolean isEmoticonsEnabled() {
        return emoticonsEnabled;
    }

    public void setEmoticonsEnabled(boolean emoticonsEnabled) {
        this.emoticonsEnabled = emoticonsEnabled;
    }

    public boolean isReceiveDailyTopMessagesMail() {
        return receiveDailyTopMessagesMail;
    }

    public void setReceiveDailyTopMessagesMail(boolean receiveDailyTopMessagesMail) {
        this.receiveDailyTopMessagesMail = receiveDailyTopMessagesMail;
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
}
