package com.welshare.web.social;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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

import com.google.code.linkedinapi.client.LinkedInApiClient;
import com.google.code.linkedinapi.client.LinkedInApiClientException;
import com.google.code.linkedinapi.client.LinkedInApiClientFactory;
import com.google.code.linkedinapi.client.enumeration.ProfileField;
import com.google.code.linkedinapi.client.oauth.LinkedInAccessToken;
import com.google.code.linkedinapi.client.oauth.LinkedInOAuthService;
import com.google.code.linkedinapi.client.oauth.LinkedInOAuthServiceException;
import com.google.code.linkedinapi.client.oauth.LinkedInRequestToken;
import com.google.code.linkedinapi.schema.Person;
import com.google.common.collect.Sets;
import com.welshare.model.Message;
import com.welshare.model.User;
import com.welshare.model.enums.Language;
import com.welshare.model.social.ExternalUserThreshold;
import com.welshare.model.social.ExternalUserThreshold.ExternalUserThresholdId;
import com.welshare.model.social.LinkedInSettings;
import com.welshare.service.MessageService;
import com.welshare.service.SocialReputationService;
import com.welshare.service.UserService;
import com.welshare.service.model.UserDetails;
import com.welshare.service.social.SocialNetworkService;
import com.welshare.service.social.helper.LinkedInHelper;
import com.welshare.service.social.qualifiers.LinkedIn;
import com.welshare.util.SecurityUtils;
import com.welshare.util.WebUtils;
import com.welshare.web.ExternalAuthenticationController;
import com.welshare.web.util.WebConstants;
import com.welshare.web.util.RequestAttribute;
import com.welshare.web.util.SessionAttribute;

@Controller
public class LinkedInController extends BaseSocialController {

    private static final String REQUEST_TOKEN = "linkedInRequestToken";
    private static final Logger log = LoggerFactory.getLogger(LinkedInController.class);

    @Inject
    private LinkedInOAuthService oAuthService;

    @Inject
    private LinkedInApiClientFactory factory;

    @Inject
    private LinkedInHelper helper;

    @Inject
    private UserService userService;

    @Inject
    @LinkedIn
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
        authUrl = (useSsl ? baseUrlSecure : baseUrl) + "/linkedIn/authenticate";
    }

    @RequestMapping("/linkedIn/authenticate")
    @CacheEvict(value=MessageService.USER_STREAM_CACHE, key="'messages-' + #loggedUser.id + '-home'")
    public String authenticate(HttpSession session,
            HttpServletRequest request, HttpServletResponse response,
            @RequestParam("oauth_token") String oauthToken,
            @RequestParam("oauth_verifier") String oauthVerifier,
            @RequestAttribute User loggedUser, Map<String, Object> model) {

        try {
            LinkedInRequestToken reqToken = (LinkedInRequestToken) session.getAttribute(REQUEST_TOKEN);
            session.removeAttribute(REQUEST_TOKEN);

            if (reqToken == null || reqToken.getToken() == null) {
                return WebConstants.REDIRECT_LOGIN;
            }
            if (!reqToken.getToken().equals(oauthToken)) {
                throw new LinkedInOAuthServiceException("Wrong oauth_token");
            }

            LinkedInAccessToken accessToken = oAuthService.getOAuthAccessToken(reqToken, oauthVerifier);

            LinkedInSettings settings = new LinkedInSettings();
            settings.setFetchMessages(true);
            settings.setShareLikes(true);
            settings.setFetchImages(true);
            settings.setActive(false);
            settings.setShowInProfile(true);
            settings.setToken(SecurityUtils.encrypt(accessToken.getToken()));
            settings.setTokenSecret(accessToken.getTokenSecret());
            settings.setLastReadNotificationTimestamp(DateTimeUtils.currentTimeMillis());

            LinkedInApiClient client = factory.createLinkedInApiClient(accessToken);
            settings.setUserId(client.getProfileForCurrentUser(Sets.newHashSet(ProfileField.ID)).getId());

            if (loggedUser != null) {
                // populate the previous configuration values
                if (loggedUser.getLinkedInSettings().getLastReadNotificationTimestamp() > 0) {
                    settings.setShareLikes(loggedUser.getLinkedInSettings().isShareLikes());
                    settings.setFetchImages(loggedUser.getLinkedInSettings().isFetchImages());
                    settings.setShowInProfile(loggedUser.getLinkedInSettings().isShowInProfile());
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
                if (loginExistingUser(session, request, response, settings.getUserId())) {
                    return WebUtils.redirectAfterAuthentication(redirectBaseUrl, session);
                } else {
                    return registerUser(session, model, settings);
                }
            }
        } catch (LinkedInApiClientException e) {
            return logAndRedirect("LinkedIn problem", session, request, e);
        } catch (LinkedInOAuthServiceException e) {
            return logAndRedirect("LinkedIn oAuth problem", session, request, e);
        }
        return "redirect:/settings/social";
    }

    private String registerUser(HttpSession session, Map<String, Object> model, LinkedInSettings settings) {
        LinkedInApiClient client = helper.getClient(settings);

        User user = new User();
        user.setLinkedInSettings(settings);

        Person person = client.getProfileForCurrentUser(LinkedInHelper.PROFILE_FIELDS);
        helper.fillUserData(user, person);

        user.setUsername(user.getNames().toLowerCase().replace(' ', '.'));
        user.setExternalAuthId(person.getId());
        user.getProfile().setLanguage(Language.EN);

        session.setAttribute(ExternalAuthenticationController.EXTERNAL_USER_DATA, user);
        model.put("user", user);
        return ExternalAuthenticationController.EXTERNAL_REGISTRATION_VIEW;
    }

    @RequestMapping("/linkedIn/connect")
    public void connectWithLinkedIn(HttpServletResponse response,
            HttpSession session, @RequestAttribute User loggedUser,
            @RequestParam(required=false, defaultValue="false") boolean sendInitialMessage) throws IOException {

        // if already connected, do nothing
        if (loggedUser != null && loggedUser.getLinkedInSettings() != null && loggedUser.getLinkedInSettings().isFetchMessages()) {
            return;
        }

        LinkedInRequestToken requestToken = oAuthService.getOAuthRequestToken(authUrl);
        String url = requestToken.getAuthorizationUrl();

        session.setAttribute(REQUEST_TOKEN, requestToken);
        session.setAttribute(WebConstants.SEND_INITIAL_MESSAGE_KEY, sendInitialMessage);

        response.sendRedirect(url);
    }

    @RequestMapping("/linkedIn/disconnect")
    public String disconnect(@SessionAttribute String userId) {
        service.disconnect(userId);

        return WebConstants.REDIRECT_SETTINGS_SOCIAL;
    }

    @RequestMapping("/user/external/li{id}")
    public String getUser(@PathVariable String id, @RequestAttribute User loggedUser, Model model) {
        String externalId = "tw" + id;
        UserDetails externalUser = service.getUserDetails(externalId, loggedUser);

        // check if there is any threshold for this user
        ExternalUserThresholdId thresholdId = new ExternalUserThresholdId(loggedUser, externalId);
        ExternalUserThreshold threshold = userService.get(ExternalUserThreshold.class, thresholdId);
        if (threshold != null) {
            externalUser.setLikesThreshold(threshold.getThreshold());
        }

        service.getMessagesOfUser(externalId, loggedUser);
        model.addAttribute("followedByCurrent", service.isFriendWithCurrentUser(externalId, loggedUser));
        model.addAttribute("user", externalUser);

        List<Message> messages = service.getMessagesOfUser(externalId, loggedUser);
        WebUtils.prepareForOutput(messages);
        model.addAttribute("messages", messages);
        return "external/linkedIn/user";
    }

    @RequestMapping("/linkedIn/follow/{externalUserId}")
    @ResponseBody
    public boolean follow(@PathVariable String externalUserId, @RequestAttribute User loggedUser) {
        return false;
    }

    @RequestMapping("/linkedIn/unfollow/{externalUserId}")
    @ResponseBody
    public boolean unfollow(@PathVariable String externalUserId, @RequestAttribute User loggedUser) {
        return false;
    }

    @RequestMapping("/linkedIn/import")
    public void importUpdates(@RequestAttribute User loggedUser) {
        service.importMessages(loggedUser);
    }
}
