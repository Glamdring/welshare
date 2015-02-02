package com.welshare.web;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.joda.time.DateTimeConstants;

import com.welshare.model.Login;
import com.welshare.model.Message;
import com.welshare.model.User;
import com.welshare.service.MessageService;
import com.welshare.service.UserService;
import com.welshare.util.WebUtils;
import com.welshare.web.util.WebConstants;

public final class UserSession {

    public static final String USER_KEY = "userId";
    public static final String EXTERNAL_USERNAMES_KEY = "externalUsernames";
    private static final int COOKIE_AGE = DateTimeConstants.SECONDS_PER_WEEK;

    private UserSession() {
    }

    public static String getUserId(HttpSession session) {
        return (String) session.getAttribute(USER_KEY);
    }

    public static void initializeUserSession(HttpSession sessionParam,
            HttpServletRequest request, HttpServletResponse response,
            final Login login, boolean remember, boolean useSsl,
            final MessageService messageService, UserService userService) {
        final HttpSession session = UserSession.resetSessionId(sessionParam, request, response);
        UserSession.setUser(session, login.getUser().getId());
        if (remember) {
            UserSession.addPermanentCookies(response, login, useSsl);
        }
        UserSession.setExternalUsernames(session, userService.getExternalUsernames(login.getUser()));

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                UserSession.setMissedImportantMessages(session, messageService, login.getUser());
            }
        });
        executor.shutdown();
    }

    public static void setUser(HttpSession session, String userId) {
        session.setAttribute(USER_KEY, userId);
    }

    public static void setExternalUsernames(HttpSession session, List<String> externalUsernames) {
        session.setAttribute(EXTERNAL_USERNAMES_KEY, externalUsernames.toArray(new String[externalUsernames.size()]));
    }

    public static HttpSession resetSessionId(HttpSession session,
            HttpServletRequest request, HttpServletResponse response) {
        //do nothing
        // TODO make this work if the whole site is going to go through https

        return session;
    }

    public static void addPermanentCookies(HttpServletResponse response, Login login, boolean useSsl) {
        Cookie authTokenCookie = new Cookie(WebConstants.AUTH_TOKEN_COOKIE_NAME, login.getToken());
        authTokenCookie.setMaxAge(COOKIE_AGE);
        //cookie.setSecure(useSsl);
        authTokenCookie.setPath("/");
        authTokenCookie.setDomain(".welshare.com");
        response.addCookie(authTokenCookie);

        Cookie seriesCookie = new Cookie(WebConstants.AUTH_TOKEN_SERIES_COOKIE_NAME, login.getSeries());
        seriesCookie.setMaxAge(COOKIE_AGE);
        //userIdCookie.setSecure(useSsl);
        seriesCookie.setPath("/");
        seriesCookie.setDomain(".welshare.com");
        response.addCookie(seriesCookie);
    }

    public static void setMissedImportantMessages(
            HttpSession session, MessageService messageService, User user) {

        Collection<Message> messages = messageService.getMissedImportantMessages(user);
        WebUtils.prepareForOutput(messages);
        session.setAttribute(WebConstants.MISSED_IMPORTANT_MESSAGES, messages);
        session.setAttribute(WebConstants.MISSED_IMPORTANT_MESSAGES_UNREAD_COUNT, messages.size());
    }
}
