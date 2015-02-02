package com.welshare.web;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.welshare.model.Login;
import com.welshare.service.MessageService;
import com.welshare.service.UserService;

@Component
public class LoginHelper {

    @Inject
    private MessageService messageService;

    @Inject
    private UserService userService;

    public void initializeUserSession(boolean remember, String timezoneId,
            HttpSession session, HttpServletRequest request,
            HttpServletResponse response, Login login) {
        if (StringUtils.isNotEmpty(timezoneId) && !timezoneId.equals(login.getUser().getCurrentTimeZoneId())) {
            login.getUser().setCurrentTimeZoneId(timezoneId);
            userService.save(login.getUser());
        }
        UserSession.initializeUserSession(session, request, response, login,
                remember, false, messageService, userService);
    }
}
