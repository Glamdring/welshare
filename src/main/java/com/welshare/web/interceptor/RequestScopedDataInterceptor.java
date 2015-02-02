package com.welshare.web.interceptor;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.welshare.dao.UserDao;
import com.welshare.model.InterestedInKeyword;
import com.welshare.model.MessageFilter;
import com.welshare.model.User;
import com.welshare.web.UserSession;
import com.welshare.web.util.ResourceController;

/**
 * On each request the User object is fetched from the database/cache.
 * This ensures that fresh data is shown each time.
 *
 * It will hit the cache in almost all cases, so this is mostly in-memory
 * query towards the cache (O(1))
 *
 * @author Bozhidar Bozhanov
 *
 */
@Component
public class RequestScopedDataInterceptor extends HandlerInterceptorAdapter {

    @Inject
    private UserDao userDao;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws Exception {

        // requests for static resources should not be handled
        if (handler instanceof ResourceController) {
            return true;
        }

        HttpSession session = request.getSession();

        String userId = UserSession.getUserId(session);
        if (userId != null) {
            // no transaction management - read-only transaction
            User loggedUser = userDao.getById(User.class, userId);
            request.setAttribute("loggedUser", loggedUser);
            request.setAttribute("messageFilters", userDao.getListByPropertyValue(MessageFilter.class, "user", loggedUser));
            request.setAttribute("interestedInKeywords", userDao.getListByPropertyValue(InterestedInKeyword.class, "user", loggedUser));
        }

        return true;
    }
}
