package com.welshare.web;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.impl.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.welshare.model.Login;
import com.welshare.model.User;
import com.welshare.model.enums.Language;
import com.welshare.service.UserAccountService;
import com.welshare.service.UserService;
import com.welshare.service.exception.DisallowedLoginException;
import com.welshare.service.exception.UserException;
import com.welshare.util.WebUtils;
import com.welshare.web.util.WebConstants;
import com.welshare.web.util.RequestAttribute;
import com.welshare.web.util.SessionAttribute;

@Controller
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    private static final String SIGNUP_URL = "/signup";

    private EmailValidator emailValidator = new EmailValidator();

    @Inject
    private UserService userService;

    @Inject
    private UserAccountService userAccountService;

    @Inject
    private LoginHelper loginHelper;

    @Inject
    private SettingsController settingsController;

    @Value("${signup.invite.only}")
    private boolean signupInviteOnly;

    @Value("redirect:${base.url}")
    private String redirectBaseUrl;

    @Value("${base.url}")
    private String baseUrl;

    @RequestMapping(SIGNUP_URL)
    public User signupPage(@SessionAttribute String userId, HttpSession session,
            @RequestParam(required=false) String invitationCode, HttpServletResponse response,
            HttpServletRequest request)
            throws IOException {

        // redirect to homepage if there is a currently logged user,
        // or the non-invite signup are closed
        if (userId != null || (signupInviteOnly && invitationCode == null)) {
            response.sendRedirect(baseUrl);
            return null;
        }

        if (invitationCode == null) {
            return new User();
        } else {
            User newUser = userService.createUserForInvitationCode(invitationCode);
            if (newUser == null) {
                WebUtils.addError(request, "invitationCodeInvalid");
                response.sendRedirect("/");
                return null;
            }
            // putting this in the session in order to support both internal
            // and external registration (tiwtter, fb, openid).
            session.setAttribute(WebConstants.WAITING_USER_KEY, newUser.getWaitingUserId());
            return newUser;
        }
    }

    @RequestMapping("/login")
    public String loginPage(@SessionAttribute String userId) {
        if (userId != null) { // if there is a currently logged user
            return redirectBaseUrl;
        }
        return "login";
    }

    @RequestMapping("/account/login")
    public String login(@RequestParam String username,
            @RequestParam String password,
            @RequestParam(required=false, defaultValue="0") boolean remember,
            @RequestParam(required=false) String timezoneId,
            Map<String, Object> model, HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            Login login = userService.login(username, password, remember, request.getRemoteAddr());
            loginHelper.initializeUserSession(remember, timezoneId, session, request,
                    response, login);
        } catch (DisallowedLoginException ex) {
            model.put("error", ex.getMessage() + ex.getMinutes()); //TODO show minutes
            return "login";
        } catch (UserException e) {
            model.put("error", e.getMessage());
            return "login";
        }

        return WebUtils.redirectAfterAuthentication(redirectBaseUrl, session);
    }

    @RequestMapping("/account/register")
    public String register(@Valid User user, BindingResult binding,
            @RequestParam(required=false, defaultValue="none") String timezoneId, HttpSession session,
            @RequestParam(required=false) String featureUri) {
        if (binding.hasErrors()) {
            return SIGNUP_URL;
        }

        if (StringUtils.isNotEmpty(featureUri)) {
            session.setAttribute(WebConstants.FEATURE_URI, featureUri);
        }
        try {
            // setting the user that registers with an invitation if this is the case
            Integer waitingUserId = (Integer) session.getAttribute(WebConstants.WAITING_USER_KEY);
            if (waitingUserId != null) {
                user.setWaitingUserId(waitingUserId);
                session.removeAttribute(WebConstants.WAITING_USER_KEY);
            }
            user.getProfile().setTimeZoneId(timezoneId);
            user.setCurrentTimeZoneId(timezoneId);
            user.getProfile().setLanguage(Language.EN); //TODO;
            User registeredUser = userService.register(user);
            UserSession.setUser(session, registeredUser.getId());
        } catch (UserException e) {
            binding.addError(new ObjectError("user", e.getMessage()));
            return SIGNUP_URL;
        }
        return WebUtils.redirectAfterAuthentication(redirectBaseUrl, session);
    }

    /**
     *
     * @param username
     * @return true if the username is free, false otherwise
     */
    @RequestMapping("/account/checkUsername/{username}")
    @ResponseBody
    public boolean checkUsername(@PathVariable String username) {
        return userService.checkUsername(username);
    }

    /**
    *
    * @param email
    * @return true if the email is free, false otherwise
    */
   @RequestMapping("/account/checkEmail")
   @ResponseBody
   public boolean checkEmail(@RequestParam String email) {
       return userService.checkEmail(email);
   }

    @RequestMapping("/account/activate/{code}")
    public String activate(@PathVariable("code") String code, HttpServletRequest request) {
        try {
            userService.activateUserWithCode(code);
            WebUtils.addMessage(request, "userActivated");
        } catch (UserException e) {
            WebUtils.addError(request, e.getMessage());
        }
        return "home";
    }

    @RequestMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
        session.invalidate();
        for (Cookie cookie : request.getCookies()) {
            // invalidate all cookies, except google analytics ones
            if (!cookie.getName().contains("_utm")) {
                cookie.setMaxAge(0);
                // needed, otherwise browsers don't recognize it.
                cookie.setPath("/");
                cookie.setDomain(".welshare.com");
                response.addCookie(cookie);
            }
        }
        return redirectBaseUrl;
    }

    @RequestMapping("/account/requestInvitationCode")
    public String requestInvitationCode(@RequestParam String email, Map<String, Object> model, HttpServletRequest request) {
        if (StringUtils.isBlank(email) || !emailValidator.isValid(email, null)) {
            WebUtils.addError(request, "invalidEmail");
            return WebConstants.UNREGISTERED_HOME;
        }

        boolean result = userService.registerWaitingUser(email);
        if (result) {
            WebUtils.addMessage(request, "invitationSuccess");
        } else {
            WebUtils.addError(request, "invitationDuplicateEmail");
        }
        return WebConstants.UNREGISTERED_HOME;
    }

    @RequestMapping("/account/markViewedStartingHints")
    @ResponseBody
    public boolean markViewedStartingHints(@SessionAttribute String userId) {
        if (userId == null) {
            return false;
        }
        userAccountService.setViewedStartingHints(userId);

        return true;
    }

    @RequestMapping("/account/markClosedHomepageConnectLinks")
    @ResponseBody
    public boolean markClosedHomepageConnectLinks(@SessionAttribute String userId) {
        if (userId == null) {
            return false;
        }
        userAccountService.setClosedHomepageConnectLinks(userId);

        return true;
    }

    @RequestMapping("/account/forgottenPassword")
    public String forgottenPassword() {
        return "passwords/forgottenPassword";
    }

    @RequestMapping(value="/account/remindPassword", method=RequestMethod.POST)
    public String remindPassword(@RequestParam("username") String username,
            HttpServletRequest request) {
        try {
            userService.resetPassword(username);
            WebUtils.addMessage(request, "forgottenPasswordInstructionsSent");
            return "unregisteredHome";
        } catch (UserException e) {
            WebUtils.addError(request, e.getMessage());
            return "passwords/forgottenPassword";
        }
    }

    @RequestMapping(value="/account/resetPassword", method=RequestMethod.GET)
    public String resetPassword(@RequestParam("username") String username,
            @RequestParam("token") String token,
            HttpServletRequest request, Model model) {
        User user = userService.getUserFromPasswordResetToken(username, token);
        if (user != null) {
            model.addAttribute("user", user);
            return "passwords/resetPassword";
        } else {
            WebUtils.addError(request, "invalidData");
            return WebConstants.REDIRECT_HOME;
        }
    }

    @RequestMapping(value="/account/doResetPassword", method=RequestMethod.POST)
    public String resetPassword(@RequestParam("username") String username,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("newPasswordRepeat") String newPasswordRepeat,
            @RequestParam("token") String token,
            HttpServletRequest request) {

        if (newPassword.equals(newPasswordRepeat)) {
            // check the token again. If only userId is passed, anyone
            // will be able to change all passwords
            User user = userService.getUserFromPasswordResetToken(username, token);
            if (user != null) {
                userService.changePassword(user.getId(), newPassword);
                WebUtils.addMessage(request, "passwordChanged");
            }
            return "/login";
        } else {
            WebUtils.addError(request, "passwordsMismatch");
            return "passwords/resetPassword";
        }
    }

    @RequestMapping("/account/changePassword")
    public String changePassword() {
        return "passwords/changePassword";
    }

    @RequestMapping("/account/doChangePassword")
    public String changePassword(@RequestAttribute User loggedUser,
            @RequestParam(value="currentPassword", required=false) String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("newPasswordRepeat") String newPasswordRepeat,
            HttpServletRequest request, HttpSession session, Model model) {

        if (!newPassword.equals(newPasswordRepeat)) {
            WebUtils.addError(request, "passwordsMismatch");
            return "passwords/changePassword";
        } else if (loggedUser.isAllowUnverifiedPasswordReset() && Boolean.TRUE.equals(session.getAttribute(WebConstants.REMEMBER_ME_LOGIN))) {
            WebUtils.addError(request, "passwordRequiredRememberMe");
            return "passwords/changePassword";
        } else if (!loggedUser.isAllowUnverifiedPasswordReset() && !loggedUser.getPassword().equals(
                userService.saltAndHashPassword(StringUtils.trimToEmpty(currentPassword)))) {
            WebUtils.addError(request, "incorrectPassword");
            return "passwords/changePassword";
        } else {
            userService.changePassword(loggedUser.getId(), newPassword);
            WebUtils.addMessage(request, "passwordChanged");
            return settingsController.viewAccountSettings(loggedUser, model);
        }
    }
}
