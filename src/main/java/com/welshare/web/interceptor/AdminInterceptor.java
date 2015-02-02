package com.welshare.web.interceptor;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.welshare.dao.UserDao;
import com.welshare.model.User;
import com.welshare.web.UserSession;

@Component
public class AdminInterceptor extends HandlerInterceptorAdapter {

    @Inject
    private UserDao dao;

    @Override
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response, Object handler) throws Exception {

        if (request.getRequestURI().startsWith("/admin/")) {
            String userId = UserSession.getUserId(request.getSession());
            if (userId == null) {
                return false;
            }

            User user = dao.getById(User.class, userId);
            if (user != null && user.isAdmin()) {
                return true;
            }
            response.sendRedirect("/login");
            return false;
        }

        return true;
    }
}
