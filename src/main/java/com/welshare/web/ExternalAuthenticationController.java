package com.welshare.web;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.openid4java.OpenIDException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.welshare.model.Login;
import com.welshare.model.User;
import com.welshare.model.enums.Country;
import com.welshare.model.enums.Language;
import com.welshare.service.MessageService;
import com.welshare.service.UserService;
import com.welshare.service.exception.UserException;
import com.welshare.service.social.SocialNetworkService;
import com.welshare.util.WebUtils;
import com.welshare.web.social.FacebookController;
import com.welshare.web.social.GooglePlusController;
import com.welshare.web.social.LinkedInController;
import com.welshare.web.social.TwitterController;
import com.welshare.web.util.WebConstants;

@Controller
@RequestMapping("/externalAuth")
public class ExternalAuthenticationController {

    public static final String EXTERNAL_USER_DATA = "externalUserData";
    public static final String EXTERNAL_REGISTRATION_VIEW = "/externalRegistration";
    public static final String IS_REGISTRATION_KEY = "isRegistration";
    private static final String LAST_NAME = "lastName";
    private static final String FIRST_NAME = "firstName";
    private static final String TIMEZONE = "timezone";
    private static final String LANGUAGE = "language";
    private static final String COUNTRY = "country";
    private static final String DATE_OF_BIRTH = "dateOfBirth";
    private static final String USERNAME = "username";
    private static final String FULLNAME = "fullname";
    private static final String EMAIL = "email";
    private static final String EXTERNAL_AUTHENTICATOR = "externalAuthenticator";
    private static final String OPENID_DISCOVERY_KEY = "openid-disc";

    private static final Logger logger = LoggerFactory.getLogger(ExternalAuthenticationController.class);

    @Inject
    private ConsumerManager manager;

    @Inject
    private UserService userService;

    @Inject
    private FacebookController facebookController;
    @Inject
    private TwitterController twitterController;
    @Inject
    private LinkedInController linkedInController;
    @Inject
    private GooglePlusController googlePlusController;

    @Inject
    private MessageService messageService;

    @Inject
    private List<SocialNetworkService> socialNetworkServices;

    @Value("${base.url}/externalAuth/openid/authenticate")
    private String returnUrl;

    @Value("${base.url.secure}/externalAuth/openid/authenticate")
    private String secureReturnUrl;

    @Value("redirect:${base.url}")
    private String redirectBaseUrl;

    @Value("${use.ssl}")
    private boolean useSsl;

    @RequestMapping("/facebook/connect")
    public void authenticateWithFacebook(
            @RequestParam(defaultValue = "false") boolean login,
            @RequestParam(required=false) String timezoneId,
            HttpServletResponse response, HttpSession session)
            throws IOException {
        // TODO check if secure - i.e. no 3rd party can authenticate
        session.setAttribute(EXTERNAL_AUTHENTICATOR, "facebook");
        session.setAttribute(WebConstants.CURRENT_TIMEZONE_ID, timezoneId);
        if (!login) {
            session.setAttribute(IS_REGISTRATION_KEY, Boolean.TRUE);
        }
        facebookController.connectWithFacebook(response, true);
    }

    @RequestMapping("/twitter/connect")
    public void authenticateWithTwitter(@RequestParam(defaultValue="false") boolean login,
            @RequestParam(required=false) String timezoneId,
            HttpServletResponse response,
            HttpSession session) throws IOException {

        // TODO check if secure - i.e. no 3rd party can authenticate
        session.setAttribute(EXTERNAL_AUTHENTICATOR, "twitter");
        session.setAttribute(WebConstants.CURRENT_TIMEZONE_ID, timezoneId);
        if (!login) {
            session.setAttribute(IS_REGISTRATION_KEY, Boolean.TRUE);
        }
        twitterController.connectWithTwitter(response, session, null, false, null);
    }

    @RequestMapping("/linkedIn/connect")
    public void authenticateWithLinkedIn(@RequestParam(defaultValue="false") boolean login,
            @RequestParam(required=false) String timezoneId,
            HttpServletResponse response,
            HttpSession session) throws IOException {

        // TODO check if secure - i.e. no 3rd party can authenticate
        session.setAttribute(EXTERNAL_AUTHENTICATOR, "linkedIn");
        session.setAttribute(WebConstants.CURRENT_TIMEZONE_ID, timezoneId);
        if (!login) {
            session.setAttribute(IS_REGISTRATION_KEY, Boolean.TRUE);
        }
        linkedInController.connectWithLinkedIn(response, session, null, false);
    }

    @RequestMapping("/googlePlus/connect")
    public void authenticateWithGooglePlus(@RequestParam(defaultValue="false") boolean login,
            @RequestParam(required=false) String timezoneId,
            HttpServletResponse response,
            HttpSession session) throws IOException {

        // TODO check if secure - i.e. no 3rd party can authenticate
        session.setAttribute(EXTERNAL_AUTHENTICATOR, "googlePlus");
        session.setAttribute(WebConstants.CURRENT_TIMEZONE_ID, timezoneId);
        if (!login) {
            session.setAttribute(IS_REGISTRATION_KEY, Boolean.TRUE);
        }
        googlePlusController.connectWithGooglePlus(response, session, null, false);
    }

    @SuppressWarnings("unchecked")
    @RequestMapping("/openid/connect")
    public String connectWithOpenId(
            @RequestParam(defaultValue="false") boolean login,
            @RequestParam("openid_identifier") String openidIdentifier,
            @RequestParam(required=false) String timezoneId,
            HttpSession session, HttpServletRequest request, Map<String, Object> model) {

        try {
            List<?> discoveries = manager.discover(openidIdentifier);

            // attempt to associate with the OpenID provider
            // and retrieve one service endpoint for authentication
            DiscoveryInformation discovered = manager.associate(discoveries);

            // store the discovery information in the user's session
            session.setAttribute(OPENID_DISCOVERY_KEY, discovered);

            // obtain a AuthRequest message to be sent to the OpenID provider
            AuthRequest authReq = manager.authenticate(discovered, useSsl ? secureReturnUrl : returnUrl);

            // Attribute Exchange example: fetching the 'email' attribute
            FetchRequest fetch = FetchRequest.createFetchRequest();
            fetch.addAttribute(EMAIL, "http://axschema.org/contact/email", true);

            fetch.addAttribute(FULLNAME, "http://axschema.org/namePerson", true);
            fetch.addAttribute(FIRST_NAME,
                    "http://axschema.org/namePerson/first", true);
            fetch.addAttribute(LAST_NAME,
                    "http://axschema.org/namePerson/last", true);
            fetch.addAttribute(USERNAME,
                    "http://axschema.org/namePerson/friendly", true);
            fetch.addAttribute(DATE_OF_BIRTH, "http://axschema.org/birthDate",
                    false);
            fetch.addAttribute(COUNTRY,
                    "http://axschema.org/contact/country/home", false);
            fetch.addAttribute(LANGUAGE, "http://axschema.org/pref/language",
                    false);
            fetch.addAttribute(TIMEZONE, "http://axschema.org/pref/timezone",
                    false);

            // attach the extension to the authentication request
            authReq.addExtension(fetch);

            String url = authReq.getDestinationUrl(false);
            model.putAll(authReq.getParameterMap());

            if (!login) {
                session.setAttribute(IS_REGISTRATION_KEY, Boolean.TRUE);
            }
            session.setAttribute(WebConstants.CURRENT_TIMEZONE_ID, timezoneId);

            return "redirect:" + url;
        } catch (Exception ex) {
            logger.warn("Problem with OpenID initiation", ex);
            WebUtils.addError(request, "openIdProblem");
            return "login";
        }
    }

    @RequestMapping("/openid/authenticate")
    public String verifyResponse(HttpServletRequest request,
            HttpServletResponse httpResponse,
            HttpSession session, Map<String, Object> model) {

        // extract the receiving URL from the HTTP request
        StringBuffer receivingURL = request.getRequestURL();
        String queryString = request.getQueryString();
        if (StringUtils.isNotEmpty(queryString)) {
            receivingURL.append('?').append(request.getQueryString());
        }
        try {
            // extract the parameters from the authentication response
            // (which comes in as a HTTP request from the OpenID provider)
            ParameterList response = new ParameterList(
                    request.getParameterMap());

            // retrieve the previously stored discovery information
            DiscoveryInformation discovered = (DiscoveryInformation) request
                    .getSession().getAttribute(OPENID_DISCOVERY_KEY);

            // verify the response; ConsumerManager needs to be the same
            // (static) instance used to place the authentication request
            VerificationResult verification = manager.verify(
                    receivingURL.toString(), response, discovered);

            // examine the verification result and extract the verified
            // identifier
            Identifier verified = verification.getVerifiedId();
            if (verified != null) {
                AuthSuccess authSuccess = (AuthSuccess) verification
                        .getAuthResponse();

                String externalAuthId = verified.getIdentifier();

                if (loginWithExternalAuthId(session, request, httpResponse, externalAuthId)) {
                    return WebUtils.redirectAfterAuthentication(redirectBaseUrl, session);
                } else {
                    User user = new User();
                    user.setExternalAuthId(externalAuthId);

                    if (authSuccess.hasExtension(AxMessage.OPENID_NS_AX)) {
                        FetchResponse fetchResp = (FetchResponse) authSuccess
                                .getExtension(AxMessage.OPENID_NS_AX);

                        user.setEmail(fetchResp.getAttributeValue(EMAIL));
                        user.setUsername(fetchResp.getAttributeValue(USERNAME));

                        user.setNames(fetchResp.getAttributeValue(FULLNAME));
                        if (user.getNames() == null) {
                            String firstName = fetchResp
                                    .getAttributeValue(FIRST_NAME);
                            String lastName = fetchResp
                                    .getAttributeValue(LAST_NAME);
                            firstName = StringUtils.trimToEmpty(firstName);
                            lastName = StringUtils.trimToEmpty(lastName);
                            user.setNames(firstName + " " + lastName);
                        }

                        String dateOfBirth = fetchResp
                                .getAttributeValue(DATE_OF_BIRTH);
                        if (dateOfBirth != null) {
                            user.getProfile().setBirthDate(DateTimeFormat.forPattern(
                                    "dd/mm/yyyy").parseDateTime(dateOfBirth));
                        }
                        String tz = fetchResp.getAttributeValue(TIMEZONE);
                        if (tz != null) {
                            user.getProfile().setTimeZoneId(tz);
                        }
                        user.getProfile().setCountry(Country.getByName(fetchResp
                                .getAttributeValue(COUNTRY)));
                    }

                    if (user.getProfile().getLanguage() == null) {
                        user.getProfile().setLanguage(Language.EN); // TODO
                    }

                    model.put("user", user);
                    session.setAttribute(EXTERNAL_USER_DATA, user);
                    return EXTERNAL_REGISTRATION_VIEW;
                }
            }
        } catch (OpenIDException ex) {
            logger.warn("Problem verifying OpenID response for receivingURL: " + receivingURL + " : " + ex.getMessage());
        }

        if (Boolean.TRUE.equals(session.getAttribute(IS_REGISTRATION_KEY))) {
            return WebConstants.REDIRECT_SIGNUP;
        } else {
            return WebConstants.REDIRECT_LOGIN;
        }
    }

    private boolean loginWithExternalAuthId(HttpSession session,
            HttpServletRequest request, HttpServletResponse response,
            String externalAuthId) {
        Login login = userService.externalLogin(externalAuthId, request.getRemoteAddr());
        if (login != null) {
            String timezoneId = (String) session.getAttribute(WebConstants.CURRENT_TIMEZONE_ID);
            if (StringUtils.isNotEmpty(timezoneId) && !timezoneId.equals(login.getUser().getCurrentTimeZoneId())) {
                login.getUser().setCurrentTimeZoneId(timezoneId);
                userService.save(login.getUser());
            }
            UserSession.initializeUserSession(session, request, response, login,
                    true, false, messageService, userService);
            return true;
        } else {
            return false;
        }
    }

    @RequestMapping("/register")
    public String register(
            @Valid User user, BindingResult binding,
            @RequestParam String timezoneId, HttpSession session) {
        // password is invalid because it is not passed. If anything else is,
        // then return
        if (binding.hasErrors() && binding.getAllErrors().size() > 1) {
            return EXTERNAL_REGISTRATION_VIEW;
        } else {
            try {
                if (!userService.checkUsername(user.getUsername())) {
                    binding.addError(new FieldError("user", "username", "userAlreadyExists"));
                    return EXTERNAL_REGISTRATION_VIEW;
                }
                User fullUser = (User) session.getAttribute(EXTERNAL_USER_DATA);
                if (fullUser == null) {
                    return WebConstants.REDIRECT_SIGNUP;
                }
                fullUser.setUsername(user.getUsername());
                fullUser.setNames(user.getNames());
                fullUser.setEmail(user.getEmail());
                fullUser.setCurrentTimeZoneId(timezoneId);
                // if not sent by the auth provider
                if (fullUser.getProfile().getTimeZoneId() == null) {
                    fullUser.getProfile().setTimeZoneId(fullUser.getCurrentTimeZoneId());
                }
                // setting the user that registers with an invitation if this is the case
                Integer waitingUserId = (Integer) session.getAttribute(WebConstants.WAITING_USER_KEY);
                if (waitingUserId != null) {
                    fullUser.setWaitingUserId(waitingUserId);
                    session.removeAttribute(WebConstants.WAITING_USER_KEY);
                }
                User registeredUser = userService.externalRegister(fullUser);
                UserSession.setUser(session, registeredUser.getId());
                UserSession.setExternalUsernames(session, userService.getExternalUsernames(user));

                // cleanup the session values:
                session.removeAttribute(IS_REGISTRATION_KEY);
                session.removeAttribute(EXTERNAL_AUTHENTICATOR);
            } catch (UserException e) {
                binding.addError(new FieldError("user", "email", e.getMessage()));
                return EXTERNAL_REGISTRATION_VIEW;
            }
            return WebUtils.redirectAfterAuthentication(redirectBaseUrl, session);
        }
    }

    @InitBinder
    public void initBinder(WebDataBinder binder, HttpServletRequest request) {
        if (request.getRequestURI().equals("/externalAuth/register")) {
            binder.setDisallowedFields("password");
        }
    }
}
