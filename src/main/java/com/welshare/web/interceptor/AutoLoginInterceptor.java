package com.welshare.web.interceptor;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.welshare.model.Login;
import com.welshare.service.MessageService;
import com.welshare.service.UserService;
import com.welshare.web.UserSession;
import com.welshare.web.util.WebConstants;
import com.welshare.web.util.ResourceController;

@Component
public class AutoLoginInterceptor extends HandlerInterceptorAdapter {

    @Inject
    private UserService userService;

    @Inject
    private MessageService messageService;

    @Value("${use.ssl}")
    private boolean useSsl;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws Exception {

        // requests for static resources should not be handled
        if (handler instanceof ResourceController) {
            return true;
        }

        // don't handle ajax requests
        String requestedWith = request.getHeader("X-Requested-With");
        if (requestedWith != null && requestedWith.equals("XMLHttpRequest")) {
            return true;
        }

        HttpSession session = request.getSession();

        if (UserSession.getUserId(session) == null && request.getCookies() != null) {
            Cookie[] cookies = request.getCookies();

            String authToken = null;
            String series = null;

            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(WebConstants.AUTH_TOKEN_COOKIE_NAME)) {
                    authToken = cookie.getValue();
                } else if (cookie.getName().equals(WebConstants.AUTH_TOKEN_SERIES_COOKIE_NAME)) {
                    series = cookie.getValue();
                }
            }

            if (authToken != null && series != null) {
                Login login = userService.rememberMeLogin(authToken, series, request.getRemoteAddr());
                if (login != null) {
                    UserSession.initializeUserSession(session, request, response, login,
                            true, false, messageService, userService);
                    session.setAttribute(WebConstants.REMEMBER_ME_LOGIN, Boolean.TRUE);
                }
            }
        }
        return true;
    }
}
