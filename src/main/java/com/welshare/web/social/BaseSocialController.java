package com.welshare.web.social;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.welshare.model.Login;
import com.welshare.service.UserService;
import com.welshare.util.WebUtils;
import com.welshare.web.ExternalAuthenticationController;
import com.welshare.web.LoginHelper;
import com.welshare.web.util.WebConstants;

public class BaseSocialController {

    private static final Logger log = LoggerFactory.getLogger(BaseSocialController.class);

    @Inject
    private LoginHelper loginHelper;

    @Inject
    private UserService userService;

    protected boolean loginExistingUser(HttpSession session,
            HttpServletRequest request, HttpServletResponse response,
            String externalUserId) {
       Login login = userService.externalLogin(externalUserId, request.getRemoteAddr());

       if (login == null) {
           return false;
       }

       String timezoneId = (String) session.getAttribute(WebConstants.CURRENT_TIMEZONE_ID);

       loginHelper.initializeUserSession(true, timezoneId, session, request, response, login);

       return true;
    }

    protected String logAndRedirect(String message, HttpSession session, HttpServletRequest request, Exception e) {
        log.error(message, e);
        WebUtils.addError(request, "externalAuthProblem");
        if (Boolean.TRUE.equals(session.getAttribute(ExternalAuthenticationController.IS_REGISTRATION_KEY))) {
            return WebConstants.REDIRECT_SIGNUP;
        } else {
            return WebConstants.REDIRECT_LOGIN;
        }
    }
}
