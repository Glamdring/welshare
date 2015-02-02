package com.welshare.web.social;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import com.welshare.model.Message;
import com.welshare.model.User;
import com.welshare.model.enums.Language;
import com.welshare.model.social.ExternalUserThreshold;
import com.welshare.model.social.ExternalUserThreshold.ExternalUserThresholdId;
import com.welshare.model.social.TwitterSettings;
import com.welshare.service.MessageService;
import com.welshare.service.SocialReputationService;
import com.welshare.service.UserService;
import com.welshare.service.model.UserDetails;
import com.welshare.service.social.RealTwitterFollowersService;
import com.welshare.service.social.RealTwitterFollowersService.RealFollowersResult;
import com.welshare.service.social.SocialNetworkService;
import com.welshare.service.social.helper.TwitterHelper;
import com.welshare.util.SecurityUtils;
import com.welshare.util.WebUtils;
import com.welshare.web.ExternalAuthenticationController;
import com.welshare.web.util.RequestAttribute;
import com.welshare.web.util.SessionAttribute;
import com.welshare.web.util.WebConstants;

@Controller
public class TwitterController extends BaseSocialController {

    private static final String REAL_FOLLOWERS = "realFollowers";

    private static final String REQUEST_TOKEN = "twitterRequestToken";

    private static final Logger log = LoggerFactory.getLogger(TwitterController.class);

    private static final String SIMPLE_TWITTER_CONNECT = "simpleTwitterConnect";
    private static final String TWITTER_SETTINGS = "twitterSettings";

    @Inject
    private TwitterFactory factory;

    @Inject
    private UserService userService;

    @Inject
    private TwitterHelper helper;

    @Inject
    private RealTwitterFollowersService realFollowersService;

    @Inject
    @com.welshare.service.social.qualifiers.Twitter
    private SocialNetworkService service;

    @Inject
    private SocialReputationService reputationService;

    @Value("${use.ssl}")
    private boolean useSsl;

    @Value("${base.url.secure}")
    private String baseUrlSecure;

    @Value("${base.url}")
    private String baseUrl;

    @Value("redirect:${base.url}")
    private String redirectBaseUrl;

    private String authUrl;

    @PostConstruct
    public void init() {
        authUrl = (useSsl ? baseUrlSecure : baseUrl) + "/twitter/authenticate";
    }

    @RequestMapping("/twitter/authenticate")
    @CacheEvict(value=MessageService.USER_STREAM_CACHE, key="'messages-' + #loggedUser.id + '-home'")
    public String authenticate(HttpSession session,
            HttpServletRequest request, HttpServletResponse response,
            @RequestParam("oauth_token") String oauthToken, @RequestParam("oauth_verifier") String verifier,
            @RequestAttribute User loggedUser, Map<String, Object> model) {
        Twitter twitter = factory.getInstance();

        String featureUri = (String) session.getAttribute(WebConstants.FEATURE_URI);
        try {
            RequestToken reqToken = (RequestToken) session.getAttribute(REQUEST_TOKEN);
            session.removeAttribute(REQUEST_TOKEN);

            if (reqToken == null || reqToken.getToken() == null) {
                return WebConstants.REDIRECT_LOGIN;
            }

            if (!reqToken.getToken().equals(oauthToken)) {
                throw new TwitterException("Wrong oauth_token");
            }
            AccessToken token = twitter.getOAuthAccessToken(reqToken, verifier);
            TwitterSettings settings = new TwitterSettings();
            settings.setFetchMessages(true);
            settings.setShareLikes(true);
            settings.setFetchImages(true);
            settings.setShowInProfile(true);
            settings.setToken(SecurityUtils.encrypt(token.getToken()));
            settings.setTokenSecret(token.getTokenSecret());
            settings.setLastReadNotificationTimestamp(DateTimeUtils.currentTimeMillis());
            settings.setUserId(token.getUserId());

            if (loggedUser != null) {
                List<Status> timeline = twitter.getHomeTimeline();
                if (!timeline.isEmpty()) {
                    settings.setLastReadNotificationId(timeline.get(0).getId());
                }
                // populate the previous configuration values
                if (loggedUser.getTwitterSettings().getLastReadNotificationTimestamp() > 0) {
                    settings.setShareLikes(loggedUser.getTwitterSettings().isShareLikes());
                    settings.setFetchImages(loggedUser.getTwitterSettings().isFetchImages());
                    settings.setShowInProfile(loggedUser.getTwitterSettings().isShowInProfile());
                    settings.setImportMessages(loggedUser.getTwitterSettings().isImportMessages());
                }
                service.storeSettings(settings, loggedUser.getId());
                reputationService.calculateSocialReputation(loggedUser);

                if (Boolean.TRUE.equals(session.getAttribute(WebConstants.SEND_INITIAL_MESSAGE_KEY))) {
                    service.publishInitialMessage(loggedUser);
                }
                session.removeAttribute(WebConstants.SEND_INITIAL_MESSAGE_KEY);
            } else {
                // if this is an existing user, login. Otherwise register.
                // If this is simple auth - store settings in session and redirect to the special feature uri
                if (loginExistingUser(session, request, response, String.valueOf(settings.getUserId()))) {
                    return WebUtils.redirectAfterAuthentication(redirectBaseUrl, session);
                } else if (Boolean.TRUE.equals(session.getAttribute(SIMPLE_TWITTER_CONNECT))) {
                    session.setAttribute(TWITTER_SETTINGS, settings);
                    session.removeAttribute(SIMPLE_TWITTER_CONNECT);
                    return redirectBaseUrl + featureUri;
                } else {
                    List<Status> statuses = twitter.getHomeTimeline();
                    if (!statuses.isEmpty()) {
                        settings.setLastReadNotificationId(statuses.get(0).getId());
                    }
                    return registerUser(session, model, settings);
                }
            }
        } catch (TwitterException e) {
            log.error("Twitter problem", e);
            WebUtils.addError(request, "externalAuthProblem");
            if (Boolean.TRUE.equals(session.getAttribute(ExternalAuthenticationController.IS_REGISTRATION_KEY))) {
                return WebConstants.REDIRECT_SIGNUP;
            } else {
                return WebConstants.REDIRECT_LOGIN;
            }

        }
        if (featureUri != null) {
            session.removeAttribute(WebConstants.FEATURE_URI);
            return redirectBaseUrl + featureUri;
        }
        return "redirect:/settings/social";
    }

    private String registerUser(HttpSession session, Map<String, Object> model,
        TwitterSettings settings) throws TwitterException {
        Twitter client = helper.getTwitter(settings);

        User user = new User();
        user.setTwitterSettings(settings);

        twitter4j.User twUser = client.showUser(settings.getUserId());
        helper.fillUserData(user, twUser);
        user.setMessages(0);
        user.setFollowers(0);
        user.setFollowing(0);

        user.setExternalAuthId(String.valueOf(twUser.getId()));

        user.getProfile().setLanguage(Language.EN); //TODO new Locale(twUser.getLang());

        if (twUser.getTimeZone() != null) {
            //user.getProfile().setTimeZoneId(twUser.getTimeZone()); //TODO extract proper TZ id (twitter gives some non-standard text)
        }

        session.setAttribute(ExternalAuthenticationController.EXTERNAL_USER_DATA, user);
        model.put("user", user);
        return ExternalAuthenticationController.EXTERNAL_REGISTRATION_VIEW;
    }

    @RequestMapping("/twitter/connect")
    public void connectWithTwitter(HttpServletResponse response,
            HttpSession session, @RequestAttribute User loggedUser,
            @RequestParam(required=false, defaultValue="false") boolean sendInitialMessage,
            @RequestParam(required=false) String featureUri)
            throws IOException {

        // if already connected, do nothing
        if (loggedUser != null && loggedUser.getTwitterSettings() != null && loggedUser.getTwitterSettings().isFetchMessages()) {
            return;
        }
        if (StringUtils.isNotEmpty(featureUri)) {
            session.setAttribute(WebConstants.FEATURE_URI, featureUri);
        }
        Twitter twitter = factory.getInstance();
        try {
            RequestToken requestToken = twitter.getOAuthRequestToken(authUrl);
            String url = requestToken.getAuthenticationURL();

            session.setAttribute(REQUEST_TOKEN, requestToken);
            session.setAttribute(WebConstants.SEND_INITIAL_MESSAGE_KEY, sendInitialMessage);

            response.sendRedirect(url);
        } catch (TwitterException e) {
            log.error("Twitter problem", e);
        }
    }

    /**
     * Used whenever the authentication won't be used for registration/login/account association.
     * The result will be stored temporarily in the session
     *
     * @param response
     * @param session
     * @param loggedUser
     * @param sendInitialMessage
     * @throws IOException
     */
    @RequestMapping("/twitter/simpleConnect")
    public void simpleConnectWithTwitter(HttpServletResponse response,
            HttpSession session, @RequestAttribute User loggedUser,
            @RequestParam(required=false, defaultValue="false") boolean sendInitialMessage,
            @RequestParam String featureUri)
            throws IOException {

        session.setAttribute(SIMPLE_TWITTER_CONNECT, Boolean.TRUE);
        connectWithTwitter(response, session, loggedUser, sendInitialMessage, featureUri);
    }

    @RequestMapping("/twitter/disconnect")
    public String disconnect(@SessionAttribute String userId) {
        service.disconnect(userId);

        return WebConstants.REDIRECT_SETTINGS_SOCIAL;
    }

    @RequestMapping("/user/external/tw{id}")
    public String getUser(@PathVariable String id, @RequestAttribute User loggedUser, Model model) {
        if (StringUtils.isEmpty(id)) {
            return WebConstants.REDIRECT_HOME;
        }
        String externalId = "tw" + id;
        UserDetails externalUser = service.getUserDetails(externalId, loggedUser);

        // check if there is any threshold for this user
        ExternalUserThresholdId thresholdId = new ExternalUserThresholdId(loggedUser, externalId);
        ExternalUserThreshold threshold = userService.get(ExternalUserThreshold.class, thresholdId);
        if (threshold != null) {
            externalUser.setLikesThreshold(threshold.getThreshold());
            externalUser.setHideReplies(threshold.isHideReplies());
        }

        service.getMessagesOfUser(externalId, loggedUser);
        model.addAttribute("followedByCurrent", service.isFriendWithCurrentUser(externalId, loggedUser));
        model.addAttribute("user", externalUser);

        List<Message> messages = service.getMessagesOfUser(externalId, loggedUser);
        WebUtils.prepareForOutput(messages);
        model.addAttribute("messages", messages);
        return "external/twitter/user";
    }

    @RequestMapping("/twitter/follow/{externalUserId}")
    @ResponseBody
    public boolean follow(@PathVariable String externalUserId, @RequestAttribute User loggedUser) {
        Twitter t = helper.getTwitter(loggedUser);
        try {
            t.createFriendship(helper.getTwitterId(externalUserId));
            return true;
        } catch (TwitterException e) {
            log.error("Problem following user " + externalUserId, e);
            return false;
        }
    }

    @RequestMapping("/twitter/unfollow/{externalUserId}")
    @ResponseBody
    public boolean unfollow(@PathVariable String externalUserId, @RequestAttribute User loggedUser) {
        Twitter t = helper.getTwitter(loggedUser);
        try {
            t.destroyFriendship(helper.getTwitterId(externalUserId));
            return true;
        } catch (TwitterException e) {
            log.error("Problem unfollowing user " + externalUserId, e);
            return false;
        }
    }

    /**
     * Very long request. Client sends a calculation request and then pings
     * another URL to see if result is set (in the session)
     *
     * @param userId
     * @param twitterSettings
     */
    @RequestMapping("/twitter/realFollowers/calculate")
    @ResponseBody
    public void calculateRealFollowers(@SessionAttribute final String userId,
            @SessionAttribute(name = TWITTER_SETTINGS) final TwitterSettings twitterSettings,
            final HttpSession session) {

        // can't use `@Async`, because the result has to be set in the session
        new Thread(new Runnable() {
            @Override
            public void run() {
                RealFollowersResult result = null;
                try {
                    if (userId != null) {
                        result = realFollowersService.calculateRealFollowers(userId);
                    } else if (twitterSettings != null) {
                        result = realFollowersService.calculateRealFollowers(twitterSettings);
                    }
                    session.setAttribute(REAL_FOLLOWERS, result);
                } catch (Exception e) {
                    log.warn("Twitter problem when calculating real followers: ", e);
                    session.setAttribute(REAL_FOLLOWERS, RealFollowersResult.ERROR);
                }
            }
        }).start();
    }

    @RequestMapping("/twitter/realFollowersResult")
    @ResponseBody
    public RealFollowersResult getRealFollowersResult(@SessionAttribute RealFollowersResult realFollowers, HttpSession session) {
        if (realFollowers != null) {
            session.removeAttribute(REAL_FOLLOWERS);
            return realFollowers;
        } else {
            return null;
        }
    }

    @RequestMapping("/twitter/realFollowers")
    public String viewRealFollowers(@RequestAttribute User loggedUser, Model model) {
        if (loggedUser != null) {
            UserDetails userDetails = service.getUserDetails(TwitterHelper.PUBLIC_ID_PREFIX + loggedUser.getTwitterSettings().getUserId(), loggedUser);
            model.addAttribute("userDetails", userDetails);
        }
        return "external/twitter/realFollowers";
    }

    @RequestMapping("/twitter/import")
    public void importTweets(@RequestAttribute User loggedUser) {
        service.importMessages(loggedUser);
    }

    @RequestMapping("/twitter/currentlyActiveFollowers")
    @ResponseBody
    public int getCurrentlyActiveFollowers(@RequestAttribute User loggedUser) {
        if (loggedUser == null) {
            return 0;
        }
        return service.getCurrentlyActiveReaders(loggedUser);
    }
}
