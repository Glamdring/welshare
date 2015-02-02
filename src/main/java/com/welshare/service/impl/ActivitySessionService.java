package com.welshare.service.impl;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.welshare.model.ActivitySession;
import com.welshare.model.User;
import com.welshare.service.annotations.SqlTransactional;
import com.welshare.util.collection.ExpirationListener;

@Service
public class ActivitySessionService extends BaseServiceImpl implements
        ExpirationListener<String, ActivitySession> {

    private static final Logger logger = LoggerFactory.getLogger(ActivitySessionService.class);

    @Override
    @SqlTransactional
    public void onExpiry(String userId, ActivitySession session) {

        User user = getDao().getById(User.class, userId);
        logger.debug("Expiring activity session of user: " + user);
        session.setUser(user);
        session.setEnd(new DateTime());
        session.setSeconds(Seconds.secondsBetween(session.getStart(), session.getEnd()).getSeconds());
        save(session);

        if (user.getProfile().isWarnOnMinutesPerDayLimit()) {
            int secondsToday = user.getOnlineSecondsToday();
            if (shouldResetOnlineSecondsToday(user)) {
                secondsToday = 0;
            }
            secondsToday += session.getSeconds();
            user.setOnlineSecondsToday(secondsToday);
        }
    }

    /**
     * static method for calculating the current value of the "onlineSecondsToday" field. Usually returns the same value, unless
     * @param user
     * @return
     */
    public static boolean shouldResetOnlineSecondsToday(User user) {
        DateTimeZone timeZone = null;
        try {
            timeZone = DateTimeZone.forID(user.getProfile().getTimeZoneId());
        } catch (IllegalArgumentException ex) {
            logger.warn("No time zone found for ID=" + user.getProfile().getTimeZoneId());
            // zone not found
            timeZone = DateTimeZone.UTC;
        }
        // if the last login is yesterday, set the secondsToday to zero
        if (new DateTime(user.getLastLogin(), DateTimeZone.forID("GMT")).isBefore(new DateMidnight(timeZone))) {
            return true;
        }

        return false;
    }
}