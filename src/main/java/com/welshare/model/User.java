package com.welshare.model;

import java.io.Serializable;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import com.welshare.model.enums.AccountType;
import com.welshare.model.social.BitlySettings;
import com.welshare.model.social.FacebookSettings;
import com.welshare.model.social.GooglePlusSettings;
import com.welshare.model.social.LinkedInSettings;
import com.welshare.model.social.TwitterSettings;

@Entity
@NamedQueries({
    @NamedQuery(
        name = "User.login",
        query = "select u from User u where (u.username=:username OR u.email=:username) AND u.password=:password"
    ),
    @NamedQuery(
        name = "User.getByEmail",
        query = "SELECT u FROM User u WHERE u.email=:email"
    ),
    @NamedQuery(
        name = "User.getByUsername",
        query = "SELECT u FROM User u WHERE LOWER(u.username)=LOWER(:username)"
    ),
    @NamedQuery(
        name = "User.getFollowers",
        query = "SELECT following.primaryKey.follower FROM Following following WHERE following.primaryKey.followed=:followed ORDER BY following.dateTime DESC"),

    @NamedQuery(
        name = "User.getFollowing",
        query = "SELECT following.primaryKey.followed FROM Following following WHERE following.primaryKey.follower=:follower ORDER BY following.dateTime DESC"),

    @NamedQuery(
        name = "User.getFriends",
        query = "SELECT following.primaryKey.followed FROM Following following WHERE following.primaryKey.follower=:user AND following.closeFriend=true ORDER BY following.dateTime DESC"),

    @NamedQuery(
        name = "User.getFollowingMetaData",
        query = "SELECT following FROM Following following WHERE following.primaryKey.follower=:user"),

    @NamedQuery(
        name = "User.findByName",
        query = "SELECT user FROM User user WHERE user.names LIKE :name OR user.username LIKE :name or user.email LIKE :name"),
    @NamedQuery(
            name = "User.getTopUsers",
            query = "SELECT user FROM User user ORDER BY score+externalScore DESC"),
    @NamedQuery(
            name = "User.getTopUsersByCountry",
            query = "SELECT user FROM User user WHERE user.profile.country = :country ORDER BY score+externalScore DESC"),
    @NamedQuery(
            name = "User.getTopUsersByCity",
            query = "SELECT user FROM User user WHERE user.profile.city = :city ORDER BY score+externalScore DESC")
})
@Indexed
@Cacheable(true)
@DynamicUpdate // update only changed fields
public class User implements Serializable {

    private static final long serialVersionUID = -3081100632040573825L;

    @Id
    @org.springframework.data.annotation.Id
    @Column(columnDefinition="CHAR(32)")
    @GeneratedValue(generator="hibernate-uuid")
    @GenericGenerator(name = "hibernate-uuid", strategy = "uuid")
    @DocumentId
    private String id;

    @Transient
    private String externalId;

    @Column(unique = true)
    @Size(min=4, max=30)
    @NotNull
    @Pattern(regexp="[\\p{L}0-9_\\.]*[\\p{L}0-9_]{1}")
    @Field(store=Store.YES)
    private String username;

    @Embedded
    private ProfileSettings profile = new ProfileSettings();

    @Column(unique = true)
    @Email
    @Field(store=Store.YES)
    private String email;

    @Column
    @Size(min=6, max=60)
    @NotNull
    private String password;

    @Column
    private String passwordResetToken;

    @Column(nullable=false)
    private boolean allowUnverifiedPasswordReset;

    @Column
    private String salt;

    @Column
    @NotEmpty
    @Field(store=Store.YES)
    private String names;

    @Column(nullable=false)
    private boolean active;

    @Column(nullable=false)
    private boolean changePasswordAfterLogin;

    @Column
    private String activationCode;

    @Column(nullable=false)
    private long registrationTimestamp;

    @Column(nullable=false)
    private long lastLogin;

    @Column(nullable=false)
    private long lastLogout;

    @Column
    private String gravatarHash;

    @Column
    private String profilePictureURI;

    @Column
    private String smallProfilePictureURI;

    @Column
    private String largeProfilePictureURI;

    @Column(nullable=false)
    private int score;

    @Column(nullable=false)
    private int externalScore;

    @Column(nullable=false)
    private int messages;

    /**
     * The field holds the current timezone for the user and is updated on each login
     */
    @Column
    private String currentTimeZoneId;

    @Transient
    private String externalUrl;

    // instantiating the settings object, because of non-nullable fields
    // the existence of settings does not mean the user is using the service
    @Embedded
    private TwitterSettings twitterSettings = new TwitterSettings();

    @Embedded
    private FacebookSettings facebookSettings = new FacebookSettings();

    @Embedded
    private LinkedInSettings linkedInSettings = new LinkedInSettings();

    @Embedded
    private GooglePlusSettings googlePlusSettings = new GooglePlusSettings();

    @Embedded
    private BitlySettings bitlySettings;

    @Column(nullable=false)
    private int following;

    @Column(nullable=false)
    private int followers;

    @Column(nullable=false)
    private int closeFriends;

    @Column
    private String externalAuthId;

    @Column(nullable=false)
    private long lastNotificationsReadTimestamp;

    @Column(nullable=false)
    private int waitingUserId;

    @Column(nullable=false)
    private boolean viewedStartingHints;

    @Column(nullable=false)
    private boolean closedHomepageConnectLinks;

    @Column(nullable=false)
    private boolean receivedActivityStatsEmail;

    @Column(nullable=false)
    private int subsequentFailedLoginAttempts;

    @Column
    @Type(type = "com.welshare.util.persistence.PersistentDateTime")
    @DateTimeFormat(iso=ISO.DATE_TIME)
    private DateTime lastFailedLoginAttempt;

    @Column(nullable=false)
    private boolean admin;

    @Column(nullable=false)
    private int onlineSecondsToday;

    @Column(nullable=false)
    @Enumerated(EnumType.ORDINAL)
    private AccountType accountType = AccountType.FREE;

    public String getEmail() {
        return email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNames() {
        return names;
    }

    public void setNames(String names) {
        this.names = names;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    public void setPasswordResetToken(String passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }

    public boolean isAllowUnverifiedPasswordReset() {
        return allowUnverifiedPasswordReset;
    }

    public void setAllowUnverifiedPasswordReset(boolean allowUnverifiedPasswordReset) {
        this.allowUnverifiedPasswordReset = allowUnverifiedPasswordReset;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isChangePasswordAfterLogin() {
        return changePasswordAfterLogin;
    }

    public void setChangePasswordAfterLogin(boolean changePasswordAfterLogin) {
        this.changePasswordAfterLogin = changePasswordAfterLogin;
    }

    public String getActivationCode() {
        return activationCode;
    }

    public void setActivationCode(String activationCode) {
        this.activationCode = activationCode;
    }

    public long getRegistrationTimestamp() {
        return registrationTimestamp;
    }

    public void setRegistrationTimestamp(long registrationTimestamp) {
        this.registrationTimestamp = registrationTimestamp;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public long getLastLogout() {
        return lastLogout;
    }

    public void setLastLogout(long lastLogout) {
        this.lastLogout = lastLogout;
    }

    public String getGravatarHash() {
        return gravatarHash;
    }

    public void setGravatarHash(String gravatarHash) {
        this.gravatarHash = gravatarHash;
    }

    public String getProfilePictureURI() {
        return profilePictureURI;
    }

    public void setProfilePictureURI(String profilePictureURI) {
        this.profilePictureURI = profilePictureURI;
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

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getMessages() {
        return messages;
    }

    public void setMessages(int messages) {
        this.messages = messages;
    }

    public String getCurrentTimeZoneId() {
        return currentTimeZoneId;
    }

    public void setCurrentTimeZoneId(String currentTimeZoneId) {
        this.currentTimeZoneId = currentTimeZoneId;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public TwitterSettings getTwitterSettings() {
        return twitterSettings;
    }

    public void setTwitterSettings(TwitterSettings twitterSettings) {
        this.twitterSettings = twitterSettings;
    }

    public FacebookSettings getFacebookSettings() {
        return facebookSettings;
    }

    public void setFacebookSettings(FacebookSettings facebookSettings) {
        this.facebookSettings = facebookSettings;
    }

    public BitlySettings getBitlySettings() {
        return bitlySettings;
    }

    public void setBitlySettings(BitlySettings bitlySettings) {
        this.bitlySettings = bitlySettings;
    }

    public int getFollowing() {
        return following;
    }

    public void setFollowing(int following) {
        this.following = following;
    }

    public int getFollowers() {
        return followers;
    }

    public void setFollowers(int followers) {
        this.followers = followers;
    }

    public int getCloseFriends() {
        return closeFriends;
    }

    public void setCloseFriends(int friends) {
        this.closeFriends = friends;
    }

    public String getExternalAuthId() {
        return externalAuthId;
    }

    public void setExternalAuthId(String externalAuthId) {
        this.externalAuthId = externalAuthId;
    }

    public long getLastNotificationsReadTimestamp() {
        return lastNotificationsReadTimestamp;
    }

    public void setLastNotificationsReadTimestamp(long lastNotificationsReadTimestamp) {
        this.lastNotificationsReadTimestamp = lastNotificationsReadTimestamp;
    }

    public int getWaitingUserId() {
        return waitingUserId;
    }

    public void setWaitingUserId(int waitingUserId) {
        this.waitingUserId = waitingUserId;
    }

    public boolean isViewedStartingHints() {
        return viewedStartingHints;
    }

    public void setViewedStartingHints(boolean viewedStartingHints) {
        this.viewedStartingHints = viewedStartingHints;
    }

    public boolean isClosedHomepageConnectLinks() {
        return closedHomepageConnectLinks;
    }

    public void setClosedHomepageConnectLinks(boolean closedHomepageConnectLinks) {
        this.closedHomepageConnectLinks = closedHomepageConnectLinks;
    }

    public int getSubsequentFailedLoginAttempts() {
        return subsequentFailedLoginAttempts;
    }

    public void setSubsequentFailedLoginAttempts(int subsequentFailedLoginAttempts) {
        this.subsequentFailedLoginAttempts = subsequentFailedLoginAttempts;
    }

    public DateTime getLastFailedLoginAttempt() {
        return lastFailedLoginAttempt;
    }

    public void setLastFailedLoginAttempt(DateTime lastFailedLoginAttempt) {
        this.lastFailedLoginAttempt = lastFailedLoginAttempt;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public LinkedInSettings getLinkedInSettings() {
        return linkedInSettings;
    }

    public void setLinkedInSettings(LinkedInSettings linkedInSettings) {
        this.linkedInSettings = linkedInSettings;
    }

    public GooglePlusSettings getGooglePlusSettings() {
        return googlePlusSettings;
    }

    public void setGooglePlusSettings(GooglePlusSettings googlePlusSettings) {
        this.googlePlusSettings = googlePlusSettings;
    }

    public ProfileSettings getProfile() {
        return profile;
    }

    public void setProfile(ProfileSettings profile) {
        this.profile = profile;
    }

    public int getOnlineSecondsToday() {
        return onlineSecondsToday;
    }

    public void setOnlineSecondsToday(int onlineMinutesToday) {
        this.onlineSecondsToday = onlineMinutesToday;
    }

    public int getExternalScore() {
        return externalScore;
    }

    public void setExternalScore(int externalScore) {
        this.externalScore = externalScore;
    }

    public boolean isReceivedActivityStatsEmail() {
        return receivedActivityStatsEmail;
    }

    public void setReceivedActivityStatsEmail(boolean receivedActivityStatsEmail) {
        this.receivedActivityStatsEmail = receivedActivityStatsEmail;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public String getPublicId() {
        if (id != null) {
            return id;
        } else {
            return externalId;
        }
    }

    public boolean isExternal() {
        return externalId != null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
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
        User other = (User) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    public void incrementFollowers() {
        this.followers++;
    }

    public void incrementFollowing() {
        this.following++;
    }

    public void incrementFriends() {
        this.closeFriends++;
    }

    public void decrementFollowing() {
        this.following--;
    }

    public void decrementFollowers() {
        this.followers--;
    }
    public void decrementFriends() {
        this.closeFriends--;
    }

    public void incrementMessageCount() {
        this.messages++;
    }
    public void decrementMessageCount() {
        this.messages--;
    }

    public String getActualTimeZoneId() {
        if (StringUtils.isNotEmpty(currentTimeZoneId)) {
            return currentTimeZoneId;
        } else {
            return profile.getTimeZoneId();
        }
    }

    @Override
    public String toString() {
        return "User[id=" + id + ", username=" + username + "]";
    }
}
