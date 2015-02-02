package com.welshare.service.jobs;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.welshare.dao.Dao;
import com.welshare.dao.UserDao;
import com.welshare.model.NotificationEvent;
import com.welshare.model.User;
import com.welshare.model.enums.NotificationType;
import com.welshare.service.ShareService;
import com.welshare.service.UserService;
import com.welshare.service.model.ExternalNotificationEvent;
import com.welshare.service.social.FollowersTrackingService;
import com.welshare.service.social.SocialNetworkService;
import com.welshare.service.social.qualifiers.Twitter;

@Service
public class WeeklyTwitterSummaryJob {
    private static final Logger logger = LoggerFactory.getLogger(WeeklyTwitterSummaryJob.class);

    @Inject
    private FollowersTrackingService followersTrackingService;
    @Inject
    private ShareService shareService;
    @Inject
    private UserService userService;
    @Inject
    private UserDao userDao;
    @Inject @Twitter
    private SocialNetworkService twitterService;

    @Scheduled(cron="0 0 1 * * MON")
    public void tweetWeeklySummaries() {
        userDao.performBatched(User.class, 200, new Dao.PageableOperation<User>() {
            @Override
            public void execute() {
                for (User user : getData()) {
                    scheduleWeeklyTwitterSummary(user);
                }
            }
        });
    }

    public void scheduleWeeklyTwitterSummary(User user) {
        try {
            if (!user.getTwitterSettings().isTweetWeeklySummary()) {
                return;
            }
            NotificationEvent maxEvent = null;
            if (user.getTwitterSettings().getLastSummaryNotificationTimestamp() > 0) {
                maxEvent = new NotificationEvent();
                maxEvent.setDateTime(new DateTime(user.getTwitterSettings().getLastSummaryNotificationTimestamp()));
            }
            List<NotificationEvent> events = twitterService.getNotifications(maxEvent, 400, user);
            //for the first run, cut the notifications on the one week mark
            if (user.getTwitterSettings().getLastSummaryNotificationTimestamp() == 0) {
                DateTime lastWeek = new DateTime().minusWeeks(1);
                for (Iterator<NotificationEvent> it = events.iterator(); it.hasNext();) {
                    NotificationEvent event = it.next();
                    if (event.getDateTime().isBefore(lastWeek)) {
                        it.remove();
                    }
                }
            }

            // now store the last notification time
            user.getTwitterSettings().setLastSummaryNotificationTimestamp(events.get(events.size() - 1).getDateTime().getMillis());
            // a new transaction
            userService.save(user);

            // proceed to calculations
            int retweets = 0;
            int mentions = 0;
            int newFollowers = 0;
            for (NotificationEvent event : events) {
                if (event.getNotificationType() == null) { //that's a retweet
                    retweets += ((ExternalNotificationEvent) event).getCount();
                }
                if (event.getNotificationType() == NotificationType.MENTION || event.getNotificationType() == NotificationType.REPLY) {
                    mentions++;
                }
            }
            newFollowers = followersTrackingService.getGainedFollowers(user.getId()).size();

            StringBuilder messageText = new StringBuilder("My weekly twitter summary: I got ");
            if (retweets > 0) {
                messageText.append(retweets + " retweets, ");
            }
            if (mentions > 0) {
                messageText.append(mentions + " mentions, ");
            }
            if (newFollowers > 0) {
                messageText.append(newFollowers + " new follower" + (newFollowers > 1 ? "s" : ""));
            }
            messageText.append(". via http://welshare.com");

            // send at 11 am in the user's time zone
            DateTimeZone tz = null;
            try {
                tz = DateTimeZone.forID(user.getProfile().getTimeZoneId());
            } catch (IllegalArgumentException ex) {
                tz = DateTimeZone.UTC;
            }
            DateTime scheduledTime = new DateTime().withZone(tz).withHourOfDay(11);
            if (scheduledTime.isBeforeNow()) {
                scheduledTime = scheduledTime.plusDays(1);
            }

            shareService.schedule(messageText.toString(), user.getId(), Collections.<String>emptyList(),
                    Lists.newArrayList("tw"), Collections.<String>emptyList(), false, scheduledTime);
        } catch (Exception ex) {
            logger.error("Problem sending daily emails", ex);
        }
    }
}
