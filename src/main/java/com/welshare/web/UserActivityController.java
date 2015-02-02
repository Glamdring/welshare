package com.welshare.web;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Seconds;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.welshare.model.ActivitySession;
import com.welshare.model.User;
import com.welshare.util.collection.ExpirationListener;
import com.welshare.util.collection.ExpiringMap;
import com.welshare.web.util.RequestAttribute;

@Controller
@RequestMapping("/userActivity")
public class UserActivityController {

    private ExpiringMap<String, ActivitySession> map;

    @Inject
    private ExpirationListener<String, ActivitySession> activitySessionExpirationListener;

    @PostConstruct
    public void init() {
        map = new ExpiringMap<String, ActivitySession>(30 * DateTimeConstants.MILLIS_PER_SECOND, activitySessionExpirationListener);
    }

    @PreDestroy
    public void destroy() {
        map.destroy();
    }

    @RequestMapping("/poll")
    @ResponseBody
    public boolean pollIfLimitReached(@RequestAttribute User loggedUser) {
        if (loggedUser == null) {
            return false;
        }
        ActivitySession session = map.get(loggedUser.getId());
        // no atomicity required, hence getting & putting sequentially
        if (session == null) {
            session = new ActivitySession();
            session.setStart(new DateTime());
            map.put(loggedUser.getId(), session);
        }

        // warn the user only once per session
        if (loggedUser.getProfile().isWarnOnMinutesPerDayLimit() && !session.isUserWarned()) {
            // the time today = the time save in the DB + the time of the current session so far
            int onlineSecondsToday = loggedUser.getOnlineSecondsToday() + Seconds.secondsBetween(session.getStart(), new DateTime()).getSeconds();
            if (loggedUser.getProfile().getMinutesOnlinePerDay() * 60 < onlineSecondsToday) {
                session.setUserWarned(true);
                return true;
            }
        }

        return false;
    }
}
