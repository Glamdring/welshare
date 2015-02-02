package com.welshare.web.social;

import java.io.IOException;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.social.oauth2.AccessGrant;
import org.springframework.social.oauth2.GrantType;
import org.springframework.social.oauth2.OAuth2Parameters;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.googlecode.googleplus.GooglePlusFactory;
import com.googlecode.googleplus.Plus;
import com.googlecode.googleplus.model.person.Person;
import com.welshare.model.User;
import com.welshare.model.enums.Language;
import com.welshare.model.social.GooglePlusSettings;
import com.welshare.service.MessageService;
import com.welshare.service.SocialReputationService;
import com.welshare.service.social.SocialNetworkService;
import com.welshare.service.social.helper.GooglePlusHelper;
import com.welshare.service.social.qualifiers.GooglePlus;
import com.welshare.util.SecurityUtils;
import com.welshare.util.WebUtils;
import com.welshare.web.ExternalAuthenticationController;
import com.welshare.web.util.WebConstants;
import com.welshare.web.util.RequestAttribute;
import com.welshare.web.util.SessionAttribute;

@Controller
public class GooglePlusController extends BaseSocialController {

    private static final Logger log = LoggerFactory.getLogger(GooglePlusController.class);

    @Inject
    @GooglePlus
    private SocialNetworkService service;

    @Inject
    private GooglePlusFactory oAuthProvider;

    @Inject
    private GooglePlusHelper helper;

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

    private OAuth2Parameters oAuthParams;

    @PostConstruct
    public void init() {
        authUrl = (useSsl ? baseUrlSecure : baseUrl) + "/googleplus/authenticate";
        oAuthParams = new OAuth2Parameters();
        oAuthParams.setRedirectUri(authUrl);
        oAuthParams.setScope("https://www.googleapis.com/auth/plus.me https://www.googleapis.com/auth/plus.moments.write");
    }

    @RequestMapping("/googleplus/authenticate")
    @CacheEvict(value = MessageService.USER_STREAM_CACHE, key = "'messages-' + #loggedUser.id + '-home'")
    public String authenticate(HttpSession session, HttpServletRequest request, HttpServletResponse response,
            @RequestParam("code") String code, @RequestAttribute User loggedUser, Map<String, Object> model) {

        try {
            AccessGrant accessGrant = oAuthProvider.getOAuthOperations().exchangeForAccess(code, oAuthParams.getRedirectUri(), null);

            if (accessGrant == null || accessGrant.getAccessToken() == null) {
                return WebConstants.REDIRECT_LOGIN;
            }

            GooglePlusSettings settings = new GooglePlusSettings();
            settings.setFetchMessages(true);
            settings.setShareLikes(true);
            settings.setFetchImages(true);
            settings.setActive(true);
            settings.setShowInProfile(true);
            settings.setToken(SecurityUtils.encrypt(accessGrant.getAccessToken()));
            settings.setRefreshToken(SecurityUtils.encrypt(accessGrant.getRefreshToken()));
            settings.setLastReadNotificationTimestamp(DateTimeUtils.currentTimeMillis());

            // no need to pass a token refresh listener here - the token has just been obtained, so it is valid
            Plus plus = oAuthProvider.getApi(accessGrant.getAccessToken(), accessGrant.getRefreshToken(), null);
            settings.setUserId(plus.getPeopleOperations().get("me").getId());

            if (loggedUser != null) {
                // populate the previous configuration values
                if (loggedUser.getGooglePlusSettings().getLastReadNotificationTimestamp() > 0) {
                    settings.setShareLikes(loggedUser.getGooglePlusSettings().isShareLikes());
                    settings.setFetchImages(loggedUser.getGooglePlusSettings().isFetchImages());
                    settings.setShowInProfile(loggedUser.getGooglePlusSettings().isShowInProfile());
                    settings.setImportMessages(loggedUser.getGooglePlusSettings().isImportMessages());
                }
                service.storeSettings(settings, loggedUser.getId());
                reputationService.calculateSocialReputation(loggedUser);

                if (Boolean.TRUE.equals(session.getAttribute(WebConstants.SEND_INITIAL_MESSAGE_KEY))) {
                    service.publishInitialMessage(loggedUser);
                }
                session.removeAttribute(WebConstants.SEND_INITIAL_MESSAGE_KEY);
            } else {
                // if this is an existing user, login. Otherwise register.
                // If this is simple auth - store settings in session and
                // redirect to the special feature uri
                if (loginExistingUser(session, request, response, settings.getUserId())) {
                    return WebUtils.redirectAfterAuthentication(redirectBaseUrl, session);
                } else {
                    return registerUser(session, model, settings);
                }
            }
        } catch (Exception e) {
            return logAndRedirect("GooglePlus problem", session, request, e);
        }

        return "redirect:/settings/social";
    }

    private String registerUser(HttpSession session, Map<String, Object> model, GooglePlusSettings settings) {
        Plus plus = oAuthProvider.getApi(SecurityUtils.decrypt(settings.getToken()), SecurityUtils.decrypt(settings.getRefreshToken()), null);

        User user = new User();
        user.setGooglePlusSettings(settings);

        Person person = plus.getPeopleOperations().get("me");
        helper.fillUserData(user, person);

        user.setUsername(person.getNickname() != null ? person.getNickname().toLowerCase().replace(' ', '.')
                : user.getNames().toLowerCase().replace(' ', '.'));
        user.setExternalAuthId(person.getId());
        user.getProfile().setLanguage(Language.EN);

        session.setAttribute(ExternalAuthenticationController.EXTERNAL_USER_DATA, user);
        model.put("user", user);
        return ExternalAuthenticationController.EXTERNAL_REGISTRATION_VIEW;
    }

    @RequestMapping("/googleplus/connect")
    public void connectWithGooglePlus(HttpServletResponse response, HttpSession session,
            @RequestAttribute User loggedUser,
            @RequestParam(required = false, defaultValue = "false") boolean sendInitialMessage)
            throws IOException {

        // if already connected, do nothing
        if (loggedUser != null && loggedUser.getGooglePlusSettings() != null
                && loggedUser.getGooglePlusSettings().isFetchMessages()) {
            return;
        }

        session.setAttribute(WebConstants.SEND_INITIAL_MESSAGE_KEY, sendInitialMessage);
        String url = oAuthProvider.getOAuthOperations().buildAuthenticateUrl(GrantType.AUTHORIZATION_CODE,
                oAuthParams);
        response.sendRedirect(url);
    }

    @RequestMapping("/googleplus/disconnect")
    public String disconnect(@SessionAttribute String userId) {
        service.disconnect(userId);

        return WebConstants.REDIRECT_SETTINGS_SOCIAL;
    }

    @RequestMapping("/user/external/gp{id}")
    public String getUser(@PathVariable String id, @RequestAttribute User loggedUser, Model model) {
        return "";
    }

    @RequestMapping("/googleplus/import")
    public void importMessages(@RequestAttribute User loggedUser) {
        service.importMessages(loggedUser);
    }
}
