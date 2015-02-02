package com.welshare.web.util;

import javax.inject.Inject;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.welshare.service.UserAccountService;
import com.welshare.web.UserSession;

@WebListener
public class SessionListener implements HttpSessionListener {

    @Inject
    private volatile UserAccountService userAccountService;

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        //do nothing
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        initializeDependencies(event);

        event.getSession().removeAttribute(WebConstants.NEWEST_MESSAGES_RETRIEVED);
        event.getSession().removeAttribute(WebConstants.OLDEST_MESSAGES_RETRIEVED);

        // persist the time when the user is logged out (timeout, logout)
        String userId = UserSession.getUserId(event.getSession());
        if (userId != null) {
            userAccountService.setLastLogout(userId);
        }

    }

    private void initializeDependencies(HttpSessionEvent event) {
        if (userAccountService == null) {
            synchronized (this) {
                if (userAccountService == null) {
                    WebApplicationContext ctx = WebApplicationContextUtils
                            .getRequiredWebApplicationContext(event.getSession()
                                    .getServletContext());

                    ctx.getAutowireCapableBeanFactory().autowireBean(this);
                }
            }
        }
    }
}
