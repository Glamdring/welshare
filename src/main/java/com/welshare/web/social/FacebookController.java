package com.welshare.web.social;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.restfb.FacebookClient;
import com.restfb.exception.FacebookException;
import com.welshare.model.Message;
import com.welshare.model.User;
import com.welshare.model.enums.Language;
import com.welshare.model.social.ExternalUserThreshold;
import com.welshare.model.social.ExternalUserThreshold.ExternalUserThresholdId;
import com.welshare.model.social.FacebookSettings;
import com.welshare.service.SocialReputationService;
import com.welshare.service.UserService;
import com.welshare.service.model.UserDetails;
import com.welshare.service.social.SocialNetworkService;
import com.welshare.service.social.helper.FacebookHelper;
import com.welshare.service.social.qualifiers.Facebook;
import com.welshare.util.SecurityUtils;
import com.welshare.util.WebUtils;
import com.welshare.web.ExternalAuthenticationController;
import com.welshare.web.util.RequestAttribute;
import com.welshare.web.util.SessionAttribute;
import com.welshare.web.util.WebConstants;

@Controller
public class FacebookController extends BaseSocialController {

    private static final String PROFILE_CONNECTION = "me";

    private static final Logger log = LoggerFactory.getLogger(FacebookController.class);

    @Inject
    @Facebook
    private SocialNetworkService service;

    @Inject
    private FacebookHelper helper;

    @Inject
    private UserService userService;

    @Inject
    private SocialReputationService reputationService;

    @Value("${base.url.secure}")
    private String baseUrlSecure;

    @Value("${base.url}")
    private String baseUrl;

    @Value("${facebook.api.key}")
    private String apiKey;

    @Value("${facebook.app.secret}")
    private String apiSecret;

    @Value("${use.ssl}")
    private boolean useSsl;

    @Value("redirect:${base.url}")
    private String redirectBaseUrl;

    private String authUrl;

    @PostConstruct
    public void init() {
        authUrl = (useSsl ? baseUrlSecure : baseUrl) + "/facebook/authenticate";
    }

    @RequestMapping("/facebook/authenticate")
    public String authenticate(HttpServletRequest request,
            HttpServletResponse response, @RequestAttribute User loggedUser,
            HttpSession session, Map<String, Object> model) {

        String code = request.getParameter("code");

        if (code != null) {
            String url = "https://graph.facebook.com/oauth/access_token?client_id=" + apiKey + "&redirect_uri="
                    + authUrl + "&client_secret=" + apiSecret + "&code=" + code;

            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

                InputStream is = conn.getInputStream();
                StringWriter sw = new StringWriter();
                IOUtils.copy(is, sw);

                String accessToken = WebUtils.getQueryParams("?" + sw.toString()).get("access_token").get(0);

                FacebookSettings settings = new FacebookSettings();
                settings.setFetchMessages(true);
                settings.setFetchImages(true);
                settings.setShareLikes(true);
                settings.setShowInProfile(true);
                settings.setShowProfileLinkAction(true); //TODO make configurable from UI
                settings.setToken(SecurityUtils.encrypt(accessToken));
                settings.setLastReadNotificationTimestamp(DateTimeUtils.currentTimeMillis());
                FacebookClient client = helper.getFacebookClient(settings);
                com.restfb.types.User fbUser = client.fetchObject(PROFILE_CONNECTION, com.restfb.types.User.class);
                if (fbUser.getId() == null) {
                    log.warn("Got null user ID for user: " + fbUser.getFirstName() + " " + fbUser.getLastName());
                }
                settings.setUserId(fbUser.getId());

                // already registered user is connecting his account to fb
                if (loggedUser != null && loggedUser.getFacebookSettings().getToken() == null) {
                    // populate the previous configuration values
                    if (loggedUser.getFacebookSettings().getLastReadNotificationTimestamp() > 0) {
                        settings.setShareLikes(loggedUser.getFacebookSettings().isShareLikes());
                        settings.setFetchImages(loggedUser.getFacebookSettings().isFetchImages());
                        settings.setShowInProfile(loggedUser.getFacebookSettings().isShowInProfile());
                        settings.setImportMessages(loggedUser.getFacebookSettings().isImportMessages());
                    }

                    service.storeSettings(settings, loggedUser.getId());
                    reputationService.calculateSocialReputation(loggedUser);

                    if (Boolean.TRUE.equals(session.getAttribute(WebConstants.SEND_INITIAL_MESSAGE_KEY))) {
                        service.publishInitialMessage(loggedUser);
                    }
                    session.removeAttribute(WebConstants.SEND_INITIAL_MESSAGE_KEY);
                } else {
                    // if this is an existing user, login. otherwise register
                    if (loginExistingUser(session, request, response, settings.getUserId())) {
                        return WebUtils.redirectAfterAuthentication(redirectBaseUrl, session);
                    } else {
                        return registerUser(session, model, settings);
                    }
                }

            } catch (FacebookException ex) {
                logAndRedirect("Facebook problem", session, request, ex);
            } catch (IOException ex) {
                logAndRedirect("IO Exception while retriveing facebook access_token", session, request, ex);
            }

            return "redirect:/settings/social";
        } else {
            return redirectBaseUrl;
        }
    }

    private String registerUser(HttpSession session, Map<String, Object> model,
            FacebookSettings settings) throws FacebookException {
        FacebookClient client = helper.getFacebookClient(settings);

        User user = new User();
        user.setFacebookSettings(settings);
        com.restfb.types.User fbUser = client.fetchObject(PROFILE_CONNECTION, com.restfb.types.User.class);
        helper.fillUserData(user, fbUser);
        // assume lowercase, dot-separated name
        user.setUsername(user.getNames().toLowerCase().replace(' ', '.'));
        user.setEmail(fbUser.getEmail());
        user.setExternalAuthId(fbUser.getId());
        user.getProfile().setLanguage(Language.EN); //TODO new Locale(fbUser.getLocale()).getLanguage()
        Double tzOffset = fbUser.getTimezone();
        if (tzOffset != null) {
            String[] timeZoneIds = TimeZone.getAvailableIDs((int) (tzOffset * DateTimeConstants.MILLIS_PER_HOUR));
            String tzId = "UTC";
            if (timeZoneIds.length > 0) {
                tzId = timeZoneIds[0];
            }
            user.getProfile().setTimeZoneId(tzId);
        }

        session.setAttribute(ExternalAuthenticationController.EXTERNAL_USER_DATA, user);
        model.put("user", user);

        if (Boolean.TRUE.equals(session.getAttribute(WebConstants.SEND_INITIAL_MESSAGE_KEY))) {
            service.publishInitialMessage(user);
        }
        session.removeAttribute(WebConstants.SEND_INITIAL_MESSAGE_KEY);

        return ExternalAuthenticationController.EXTERNAL_REGISTRATION_VIEW;
    }

    @RequestMapping("/facebook/connect")
    public void connectWithFacebook(HttpServletResponse response,
            HttpSession session, @RequestAttribute User loggedUser,
            @RequestParam(required = false, defaultValue="false") boolean sendInitialMessage)
            throws IOException {
        // connect only if not already connected
        if (loggedUser != null && (loggedUser.getFacebookSettings() == null || !loggedUser.getFacebookSettings().isFetchMessages())) {
            session.setAttribute(WebConstants.SEND_INITIAL_MESSAGE_KEY, sendInitialMessage);
            connectWithFacebook(response, false);
        }
    }

    public void connectWithFacebook(HttpServletResponse response, boolean needEmail) throws IOException {
        boolean needOffline = true; //TODO pass this as parameter if needed?

        String url = "https://graph.facebook.com/oauth/authorize?"
            + "client_id=" + apiKey + "&redirect_uri=" + authUrl
            + "&scope=publish_stream,user_activities,friends_activities,"
            + "read_stream,user_photos,friends_photos,user_videos,friends_videos,friends_about_me,manage_notifications";

        if (needEmail) {
            url += ",email";
        }
        if (needOffline) {
            url += ",offline_access";
        }

        response.sendRedirect(url);
    }

    @RequestMapping("/facebook/disconnect")
    public String disconnect(@SessionAttribute String userId) {
        service.disconnect(userId);

        return WebConstants.REDIRECT_SETTINGS_SOCIAL;
    }

    @RequestMapping("/user/external/fb{id}")
    public String getUser(@PathVariable String id, @RequestAttribute User loggedUser, Model model) {
        if (StringUtils.isEmpty(id)) {
            return WebConstants.REDIRECT_HOME;
        }

        String externalId = "fb" + id;
        UserDetails externalUser = service.getUserDetails(externalId, loggedUser);

        // check if there is any threshold for this user
        ExternalUserThresholdId thresholdId = new ExternalUserThresholdId(loggedUser, externalId);
        ExternalUserThreshold threshold = userService.get(ExternalUserThreshold.class, thresholdId);
        if (threshold != null) {
            externalUser.setLikesThreshold(threshold.getThreshold());
            externalUser.setHideReplies(threshold.isHideReplies());
        }

        service.getMessagesOfUser(externalId, loggedUser);
        model.addAttribute("friendOfCurrent", service.isFriendWithCurrentUser(externalId, loggedUser));
        model.addAttribute("user", externalUser);
        List<Message> messages = service.getMessagesOfUser(externalId, loggedUser);
        WebUtils.prepareForOutput(messages);
        model.addAttribute("messages", messages);
        return "external/facebook/user";
    }

    @RequestMapping("/facebook/import")
    public void importPosts(@RequestAttribute User loggedUser) {
        service.importMessages(loggedUser);
    }
}
