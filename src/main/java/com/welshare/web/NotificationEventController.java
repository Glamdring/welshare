package com.welshare.web;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.welshare.model.NotificationEvent;
import com.welshare.service.NotificationEventService;
import com.welshare.web.util.WebConstants;
import com.welshare.web.util.SessionAttribute;

@Controller
@RequestMapping("/notifications")
public class NotificationEventController {

    private static final String NEW_NOTIFICATIONS_VIEW = "results/newNotifications";

    private static final String NOTIFICATIONS_KEY = "notifications";

    @Inject
    private NotificationEventService service;

    @RequestMapping("/unread")
    public String getUnread(@SessionAttribute String userId, Model model, HttpSession session, HttpServletResponse response) {
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return NEW_NOTIFICATIONS_VIEW;
        }
        List<NotificationEvent> result = service.getUnread(userId);

        model.addAttribute(NOTIFICATIONS_KEY, result);
        model.addAttribute("unreadNotifications", Boolean.TRUE);
        return NEW_NOTIFICATIONS_VIEW;
    }

    @RequestMapping("/lastRead")
    public String getLastRead(@SessionAttribute String userId, @RequestParam int count, Model model) {
        if (userId == null) {
            return NEW_NOTIFICATIONS_VIEW;
        }
        List<NotificationEvent> result = service.getLastRead(userId, count);
        model.addAttribute(NOTIFICATIONS_KEY, result);
        return NEW_NOTIFICATIONS_VIEW;
    }

    @RequestMapping("/all")
    public String getAll(@SessionAttribute String userId, @RequestParam(defaultValue="0") int start, Model model) {
        if (userId == null) {
            return WebConstants.REDIRECT_LOGIN;
        }

        List<NotificationEvent> result = service.getAll(userId, null);
        model.addAttribute(NOTIFICATIONS_KEY, result);

        return "allNotifications";
    }

    @RequestMapping("/markAsRead")
    @ResponseBody
    public void markAsRead(@SessionAttribute String userId) {
        if (userId != null) {
            service.markAllAsRead(userId);
        }
    }
}
