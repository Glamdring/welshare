package com.welshare.service.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.hibernate.StaleStateException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeUtils;
import org.joda.time.Minutes;
import org.joda.time.Period;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.welshare.dao.UserDao;
import com.welshare.dao.enums.SearchType;
import com.welshare.model.Login;
import com.welshare.model.SocialNetworkScore;
import com.welshare.model.User;
import com.welshare.model.WaitingUser;
import com.welshare.model.enums.Country;
import com.welshare.service.EmailService;
import com.welshare.service.FollowingService;
import com.welshare.service.SocialReputationService;
import com.welshare.service.UserService;
import com.welshare.service.annotations.SqlReadonlyTransactional;
import com.welshare.service.annotations.SqlTransactional;
import com.welshare.service.exception.DisallowedLoginException;
import com.welshare.service.exception.UserException;
import com.welshare.service.model.UserDetails;
import com.welshare.service.social.SocialNetworkService;
import com.welshare.service.social.SocialUtils;

@Service
public class UserServiceImpl extends BaseServiceImpl implements UserService {

    private static final String TOP_USERS_CACHE = "topUsersCache";
    private static final int BCRYPT_HASH_LENGTH = 60;
    private static final int BCRYPT_SALT_ROUNDS = 10;
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String[] RESERVED_NAMES =
        new String[] { "login", "logout", "signup", "home", "share", "welshare", "about", "important" };

    @Autowired
    private UserDao userDao;

    @Inject
    private EmailService emailService;

    @Inject
    private List<SocialNetworkService> socialNetworkServices;

    @Inject
    private FollowingService followingService;

    @Inject
    private SocialReputationService reputationService;

    @Value("${confirmation.email.sender}")
    private String confirmationEmailSender;

    @Value("${forgotten.password.email.sender}")
    private String tempPasswordEmailSender;

    @Value("${information.email.sender}")
    private String infoEmailSender;


    @Value("${externalLikeFormat}")
    private String externalLikeFormat;

    @Value("${base.url}")
    private String baseUrl;

    @Value("${base.url.secure}")
    private String baseUrlSecure;

    @Value("${use.ssl}")
    private boolean useSsl;

    @SqlTransactional
    public Login login(String username, String password, boolean remember, String ip) throws UserException {

        // if the user has supplied email, fetch the user by email
        // and use the fetched username to perform authentication
        // this is required because the salt is computed with the username
        User user = null;
        if (username.contains("@")) {
            user = getDao().getByEmail(username);
            if (user != null) {
                username = user.getUsername();
            }
        } else {
            user = getDao().getByUsername(username);
        }

        if (user == null) {
            throw UserException.INCORRECT_LOGIN_DATA;
        }

        if (user.getLastFailedLoginAttempt() != null && user.getSubsequentFailedLoginAttempts() > 2) {
            handleBlockedLogins(user);
        }

        User loggedUser = null;

        if (user.getPassword().length() == BCRYPT_HASH_LENGTH) {
            if (BCrypt.checkpw(password, user.getPassword())) {
                // if the password is correct, this is our proper user
                loggedUser = user;
            }
        } else {
            // legacy SHA1 passwords
            String passParam = salt(password, user.getSalt().toCharArray());
            passParam = sha1Hash(passParam);
            loggedUser = getDao().login(username, passParam);
        }



        if (loggedUser == null) {
            getDao().lock(user);
            user.setSubsequentFailedLoginAttempts(user.getSubsequentFailedLoginAttempts() + 1);
            user.setLastFailedLoginAttempt(new DateTime());
            getDao().persist(user);
            throw UserException.INCORRECT_LOGIN_DATA;
        } else {
            getDao().lock(loggedUser);
            loggedUser.setChangePasswordAfterLogin(false);
            loggedUser.setLastFailedLoginAttempt(null);
            loggedUser.setSubsequentFailedLoginAttempts(0);
            return createLoginAndUpdateUserData(loggedUser, null, remember, ip); // also saves the user
        }
    }

    private void handleBlockedLogins(User user) throws DisallowedLoginException {
        int minutes = Minutes.minutesBetween(user.getLastFailedLoginAttempt(), new DateTime()).getMinutes();
        if (minutes  < user.getSubsequentFailedLoginAttempts() - 2) {
            throw new DisallowedLoginException("disallowedLogin", user.getSubsequentFailedLoginAttempts() - 2);
        }
    }

    private Login createLoginAndUpdateUserData(User user, String series, boolean remember, String ip) {
        Login login = new Login();
        login.setUser(user);
        login.setIp(ip);

        if (remember) {
            login.setToken(UUID.randomUUID().toString());
            login.setSeries(series != null ? series : UUID.randomUUID().toString());
            login.setLastLoginTime(new DateTime());

            save(login);
        }

        if (user.getProfile().isWarnOnMinutesPerDayLimit() && ActivitySessionService.shouldResetOnlineSecondsToday(user)) {
            user.setOnlineSecondsToday(0);
        }

        user.setLastLogin(DateTimeUtils.currentTimeMillis());
        save(user);

        return login;
    }

    @Override
    @SqlTransactional
    public Login externalLogin(String externalAuthId, String ip) {
        User user = getDao().getByPropertyValue(User.class, "externalAuthId", externalAuthId);
        // if not found (i.e. this is not the primary login), try the networks
        // to which the user has connected his account
        if (user == null) {
            for (SocialNetworkService sns : socialNetworkServices) {
                user = sns.getInternalUserByExternalId(externalAuthId);
                if (user != null) {
                    break;
                }
            }
            if (user == null) {
                return null;
            }
        }
        return createLoginAndUpdateUserData(user, null, true, ip);
    }

    @Override
    @SqlTransactional
    public User register(User user) throws UserException {
        validateUser(user);

        user.setActivationCode(sha1Hash(generateRandomString()));

        user = finalizeRegistration(user);
        return user;
    }

    @Override
    @SqlTransactional
    public User externalRegister(User user) throws UserException {
        validateUser(user);
        //set a password
        user.setPassword(generateRandomString());
        user.setActive(true); // active by default
        user.setAllowUnverifiedPasswordReset(true);

        return finalizeRegistration(user);
    }

    private User finalizeRegistration(User user) {
        user.setRegistrationTimestamp(DateTimeUtils.currentTimeMillis());
        user.getProfile().setShowExternalSiteIndicator(true);
        user.getProfile().setGetFollowNotificationsByEmail(true);
        user.getProfile().setImportantMessageScoreThreshold(3);
        user.getProfile().setImportantMessageScoreThresholdRatio(10);
        user.getProfile().setExternalLikeFormat(externalLikeFormat);
        user.setGravatarHash(sha1Hash(user.getUsername()));
        user.setPassword(saltAndHashPassword(user.getPassword()));
        user.setLastLogout(user.getRegistrationTimestamp());
        user.getProfile().setTranslateLanguage(user.getProfile().getLanguage().getCode());

        user = getDao().persist(user);

        // initial reputation calculation
        reputationService.calculateSocialReputation(user);

        if (user.getWaitingUserId() > 0) {
            WaitingUser wuser = get(WaitingUser.class, user.getWaitingUserId());
            wuser.setRegistered(true);
            save(wuser);
        }

        // store the user node in the relationship graph
        followingService.save(user);

        sendRegistrationEmail(user);
        return user;
    }

    private void validateUser(User user) throws UserException {
        if (user == null) {
            throw new IllegalArgumentException("Passed user cannot be null");
        }

        if (!checkUsername(user.getUsername())) {
            throw UserException.USER_ALREADY_EXISTS;
        }

        if (!checkEmail(user.getEmail())) {
            throw UserException.EMAIL_ALREADY_EXISTS;
        }
    }

    // TODO this should be public and invoked from the controller, so that no email is sent if the transaction fails
    private void sendRegistrationEmail(User user) {
        Locale locale = user.getProfile().getLanguage().toLocale();

        EmailService.EmailDetails details = new EmailService.EmailDetails();
        details.setTo(user.getEmail())
            .setFrom(confirmationEmailSender)
            .setLocale(locale)
            .setSubjectKey("confirmationEmailSubject")
            .setMessageKey("confirmationEmailMessage")
            .setMessageParams(new String[] { user.getUsername() });


        if (!user.isActive()) {
            details.setExtraMessageKey("activationEmailMessage")
                .setExtraMessageParams(new String[] {baseUrl + "/account/activate/" + user.getActivationCode()});
        }

        emailService.send(details);
    }

    @SqlTransactional
    public User activateUserWithCode(String code) throws UserException {
        User user = getDao().getUserWithCode(code);

        if (user != null) {
            if (!user.isActive()) {
                user.setActive(true);
                getDao().persist(user);
                return user;
            }
            throw UserException.USER_ALREADY_ACTIVE;
        }
        throw UserException.INVALID_ACTIVATION_CODE;
    }

    @SqlTransactional
    public void clearNonActivatedUsers() {
        long treshold = System.currentTimeMillis()
                - DateTimeConstants.MILLIS_PER_DAY;
        int result = getDao().cleanNonActiveUsers(treshold);

        log.info("Cleaning inactive users : " + result);
    }

    private void sendPasswordResetEmail(User user) throws UserException {
        Locale locale = user.getProfile().getLanguage().toLocale();
        String resetUrl = (useSsl ? baseUrlSecure : baseUrl) + "/account/resetPassword?username="
            + user.getUsername() + "&token="
            + user.getPasswordResetToken();

        EmailService.EmailDetails details = new EmailService.EmailDetails();
        details.setFrom(tempPasswordEmailSender)
            .setTo(user.getEmail())
            .setSubjectKey("forgottenPasswordEmailSubject")
            .setMessageKey("forgottenPasswordEmailMessage")
            .setMessageParams(new String[] {resetUrl})
            .setLocale(locale);

        emailService.send(details);
    }

    @Override
    @SqlTransactional
    public void changePassword(String userId, String newPassword) {
        User user = getDao().getById(User.class, userId, true);
        if (user != null && newPassword != null && newPassword.length() >= 4) {
            user.setPassword(saltAndHashPassword(newPassword));
            user.setPasswordResetToken("");
            user.setAllowUnverifiedPasswordReset(false);
            getDao().persist(user);
        }
    }

    private String generateRandomString() {
        int length = 8 + (int) (Math.random() * 4);
        byte[] chars = new byte[length];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (byte) (48 + (Math.random() * 89));
        }
        try {
            MessageDigest digester = MessageDigest.getInstance("SHA-1");
            digester.update(new String(chars).getBytes());
            return getHexString(digester.digest()).substring(0, length);
        } catch (NoSuchAlgorithmException ex) {
            return new String(chars).trim();
        }
    }

    private String salt(String password, char[] salt) {
        StringBuffer sb = new StringBuffer();
        char[] chars = password.toCharArray();
        int crystal = 0;
        for (int i = 0; i < chars.length; i++) {
            sb.append(chars[i]);
            if (i % 2 == 0 && crystal < salt.length) {
                sb.append(salt[crystal++]);
            }
        }
        return sb.toString();
    }

    public String saltAndHashPassword(String password) {
        // no need to pass the salt, and no need to store it in a separate field - it is stored as part of the hash
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_SALT_ROUNDS));
    }


    private String sha1Hash(String str) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA");
            digest.update(str .getBytes());

            return getHexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            log.error("Hashing algorithm not found");
            return str;
        }
    }

    //TODO use Hex.encode
    private String getHexString(byte[] array) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < array.length; i++) {
            hexString.append(Integer.toHexString(0xFF & array[i]));
        }

        return hexString.toString();
    }

    @SqlReadonlyTransactional
    public List<User> list() {
        return list(User.class);
    }

    @SqlReadonlyTransactional
    public List<User> listOrdered(String orderColumn) {
        return listOrdered(User.class, orderColumn);
    }

    @SqlReadonlyTransactional
    public boolean checkUsername(String username) {
        if (Arrays.binarySearch(RESERVED_NAMES, username) >= 0) {
            return false;
        }
        return getDao().getByUsername(username) == null;
    }

    @SqlReadonlyTransactional
    public boolean checkEmail(String email) {
        return getDao().getByEmail(email) == null;
    }

    @Override
    @SqlTransactional
    public void createInitialUser() {
        List<User> users = list(User.class);
        if (users.isEmpty()) {
            User user = new User();
            user.setUsername("admin");
            user.setActive(true);
            user.setPassword(saltAndHashPassword("adminpass"));
            user.setRegistrationTimestamp(DateTimeUtils.currentTimeMillis());
            // user.setAccessLevel(AccessLevel.ADMINISTRATOR);
            user.setEmail("admin@welshare.com");
            user.setNames("");

            getDao().persist(user);
        }
    }

    public EmailService getEmailService() {
        return emailService;
    }

    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    @SqlReadonlyTransactional
    public User getUserFromAuthToken(String authToken, String series) {
        Login login = getDao().getLoginFromAuthToken(authToken, series);
        if (login == null) {
            return null;
        }
        return login.getUser();
    }

    @Override
    protected UserDao getDao() {
        return userDao;
    }

    @Override
    @SqlReadonlyTransactional
    public UserDetails getUserDetails(String username) {
        User user = getDao().getByUsername(username);
        if (user == null) {
            return null;
        }
        return new UserDetails(user);
    }

    @Override
    @SqlReadonlyTransactional
    public List<UserDetails> findUsers(String keywords) {
        if (StringUtils.isEmpty(keywords)) {
            return Collections.emptyList();
        }

        List<User> byName = getDao().findByKeywords(keywords.trim(), SearchType.FULL);
        return extractUserDetails(byName);
    }

    @Override
    @SqlReadonlyTransactional
    public List<UserDetails> suggestUsers(String keywords) {
        if (StringUtils.isEmpty(keywords)) {
            return Collections.emptyList();
        }
        List<User> byName = getDao().findByKeywords(keywords.trim(), SearchType.START);
        return extractUserDetails(byName);
    }

    private List<UserDetails> extractUserDetails(List<User> byName) {
        List<UserDetails> result = new ArrayList<UserDetails>(byName.size());
        for (User userByName : byName) {
            result.add(new UserDetails(userByName));
        }

        return result;
    }

    @Override
    @SqlReadonlyTransactional
    public List<UserDetails> suggestUsers(String keywords, User currentUser) {
        if (StringUtils.isEmpty(keywords)) {
            return Collections.emptyList();
        }

        List<User> byName = getDao().findByKeywords(keywords.trim(), SearchType.START);
        return extractUserDetails(byName);
    }

    @Override
    @SqlTransactional
    public boolean registerWaitingUser(String email) {

        if (getDao().getByEmail(email) != null
                || getDao().getByPropertyValue(WaitingUser.class, "email", email) != null) {
            return false;
        }

        WaitingUser user = new WaitingUser();
        user.setInvitationCode(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setRegistered(false);
        user.setRegistrationTimestamp(DateTimeUtils.currentTimeMillis());

        getDao().persist(user);

        return true;
    }

    @Override
    @SqlTransactional
    public User createUserForInvitationCode(String code) {
        WaitingUser wuser = getDao().getByPropertyValue(WaitingUser.class, "invitationCode", code);
        if (wuser.isRegistered()) {
            return null;
        }
        User user = new User();
        user.setEmail(wuser.getEmail());
        user.setWaitingUserId(wuser.getWaitingUserId());

        return user;
    }

    @Override
    @SqlTransactional
    public void resetPassword(String username) throws UserException {
        User user = null;
        if (username.contains("@")) {
            user = getDao().getByEmail(username);
        } else {
            user = getDao().getByUsername(username);
        }
        if (user == null) {
            throw UserException.INCORRECT_LOGIN_DATA;
        }

        getDao().lock(user);
        user.setPasswordResetToken(generateRandomString());
        user.setAllowUnverifiedPasswordReset(false);
        save(user);

        sendPasswordResetEmail(user);
    }

    /**
     * http://jaspan.com/improved_persistent_login_cookie_best_practice
     */
    @Override
    @Transactional(isolation=Isolation.READ_UNCOMMITTED, rollbackFor=StaleStateException.class)
    public Login rememberMeLogin(String token, String series, String ip) {
        Login existingLogin = getDao().getLoginFromAuthToken(token, series);
        if (existingLogin == null) {
            Login loginBySeries = getDao().getByPropertyValue(Login.class, "series", series);
            // if a login series exists, assume the previous token was stolen, so deleting all persistent logins
            // an exception is a request made within a few seconds from the last login time
            // which may mean request from the same browser that is not yet aware of the renewed cookie
            if (loginBySeries != null && new Period(loginBySeries.getLastLoginTime(), new DateTime()).getSeconds() < 5) {
                log.info("Assuming login cookies theft; deleting all sessions for user " + loginBySeries.getUser());
                getDao().deleteLogins(loginBySeries.getUser().getId());
            } else if (log.isDebugEnabled()) {
                log.debug("No existing login found for token=" + token + ", series=" + series);
            }
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("Existing login found for token=" + token + " and series=" + series);
        }

        Login newLogin = createLoginAndUpdateUserData(existingLogin.getUser(), existingLogin.getSeries(), true, ip);
        delete(existingLogin);
        return newLogin;
    }

    @Override
    @SqlReadonlyTransactional
    public User getUserFromPasswordResetToken(String username, String token) {
        User user = getDao().getByUsername(username);
        if (user != null
            && StringUtils.equals(user.getPasswordResetToken(), token)) {
                return user;
        }
        return null;
    }

    @Override
    @Cacheable(value = TOP_USERS_CACHE, key="#page")
    @SqlReadonlyTransactional
    public List<UserDetails> getTopUsers(int page) {
        List<User> users = getDao().getTopUsers(page);
        return extractUserDetails(users);
    }

    @Override
    @Cacheable(value = TOP_USERS_CACHE, key="'city-' + #city + '-' + #page")
    @SqlReadonlyTransactional
    public List<UserDetails> getTopUsers(String city, int page) {
        List<User> users = getDao().getTopUsers(city, page);
        return extractUserDetails(users);
    }

    @Override
    @Cacheable(value = TOP_USERS_CACHE, key="'country-' + #country.code + '-' + #page")
    @SqlReadonlyTransactional
    public List<UserDetails> getTopUsers(Country country, int page) {
        List<User> users = getDao().getTopUsers(country, page);
        return extractUserDetails(users);
    }

    @Override
    @SqlReadonlyTransactional
    public List<String> getExternalUsernames(User user) {
        List<String> usernames = new ArrayList<String>();
        for (SocialNetworkService sns : socialNetworkServices) {
            String username = sns.getExternalUsername(user);
            if (username != null) {
                usernames.add(username);
            }
        }
        return usernames;
    }

    @Override
    @SqlReadonlyTransactional
    public UserDetails getByPublicId(String id, String currentUserId) {
        User currentUser = get(User.class, currentUserId);
        SocialNetworkService sns = SocialUtils.getSocialNetworkService(socialNetworkServices, id);
        if (sns == null) {
            User user = get(User.class, id);
            if (user == null) {
                return null;
            }
            return new UserDetails(user);
        } else {
            return sns.getUserDetails(id, currentUser);
        }
    }

    @Override
    @SqlReadonlyTransactional
    public User getByUsername(String username) {
        return getDao().getByUsername(username);
    }

    @Override
    public Map<String, SocialNetworkScore> getReputationScores(String userId) {
        return getDao().getReputationScores(userId);
    }

    @Override
    public void notifyAdminUsers(String message) {
        List<User> admins = userDao.getListByPropertyValue(User.class, "admin", Boolean.TRUE);
        EmailService.EmailDetails email = new EmailService.EmailDetails();
        email.setMessage(message).setSubject(message).setFrom(infoEmailSender);
        for (User admin : admins) {
            email.addTo(admin.getEmail());
        }
        emailService.send(email);
    }

    @Override
    @SqlTransactional
    public void deleteUser(String userId) {
        User user = get(User.class, userId);
        userDao.deleteUser(user);
    }

    @Override
    @SqlTransactional
    public void clearDisconnectReasons(String userId) {
        User user = getDao().getById(User.class, userId);
        user.getFacebookSettings().setDisconnectReason(null);
        user.getTwitterSettings().setDisconnectReason(null);
        user.getLinkedInSettings().setDisconnectReason(null);
        user.getGooglePlusSettings().setDisconnectReason(null);

        getDao().persist(user);
    }
}