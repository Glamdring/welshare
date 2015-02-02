package com.welshare.service.jobs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.welshare.dao.Dao;
import com.welshare.dao.UserDao;
import com.welshare.model.ActiveReadersEntry;
import com.welshare.model.User;
import com.welshare.service.EmailService;
import com.welshare.service.MessageService;
import com.welshare.service.model.UserDetails;
import com.welshare.service.social.SocialNetworkService;

@Component
public class BestTimeToShareJob {

    private static final Logger logger = LoggerFactory.getLogger(BestTimeToShareJob.class);

    @Inject
    private List<SocialNetworkService> services;

    @Inject
    private UserDao userDao;
    @Inject
    private MessageService messageService;
    @Inject
    private EmailService emailService;

    @Value("${best.time.to.share.thread.pool.size}")
    private int poolSize;
    @Value("${information.email.sender}")
    private String infoEmail;

    @Scheduled(cron = "0 0/30 * * * *") //every half an hour
    public void storeFollowersActivity() {
        // not using @Async on the method, because it will use up the thread
        // pool (only one thread pool for all @Asyncs. TODO: https://jira.springsource.org/browse/SPR-9318)
        final ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        userDao.performBatched(User.class, 200, new Dao.PageableOperation<User>() {
            public void execute() {
                for (final User user : getData()) {
                    DateTimeZone timezone = DateTimeZone.UTC;
                    if (StringUtils.isNotEmpty(user.getActualTimeZoneId())) {
                        timezone = DateTimeZone.forID(user.getActualTimeZoneId());
                    }
                    DateTime now = new DateTime(timezone);
                    int minutes = now.getMinuteOfDay();
                    final int roundMinutes = minutes - (minutes % 30);
                    final int dayOfYear = now.getDayOfYear();
                    final boolean weekend = now.getDayOfWeek() == DateTimeConstants.SATURDAY || now.getDayOfWeek() == DateTimeConstants.SUNDAY;
                    final List<ActiveReadersEntry> existing = userDao.getOrderedListByPropertyValue(ActiveReadersEntry.class, "user", user, "minutes");
                    executor.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                for (SocialNetworkService service : services) {
                                    if (!service.isServiceEnabled(user)) {
                                        continue;
                                    }
                                    List<ActiveReadersEntry> entriesList = new ArrayList<ActiveReadersEntry>(existing);
                                    for (Iterator<ActiveReadersEntry> it = entriesList.iterator(); it.hasNext();) {
                                        if (!it.next().getSocialNetwork().equals(service.getIdPrefix())) {
                                            it.remove();
                                        }
                                    }

                                    int count = service.getCurrentlyActiveReaders(user);
                                    UserDetails details = service.getCurrentUserDetails(user);
                                    if (details == null) {
                                        continue; //there's been an exception
                                    }
                                    int followers = details.getFollowers();
                                    if (followers == 0) {
                                        continue; // skip
                                    }
                                    ActiveReadersEntry entry = null;
                                    double currentActivityPercentage = ((double) count / followers) * 100;
                                    int weekendEntries = 0;
                                    int weekdayEntries = 0;
                                    for (ActiveReadersEntry tempEntry : entriesList) {
                                        if (tempEntry.getMinutes() == roundMinutes && tempEntry.isWeekend() == weekend) {
                                            entry = tempEntry;
                                        }
                                        if (tempEntry.isWeekend()) {
                                            weekendEntries++;
                                        } else {
                                            weekdayEntries++;
                                        }
                                    }

                                    if (entry != null) {
                                        // if there is data for more than 4 days, only perform the calculation once every 5 days
                                        if (entry.getDaysRepresented() > 4 && dayOfYear % 5 != 0) {
                                            continue;
                                        }
                                        currentActivityPercentage = (entry.getActivePercentage()
                                                * entry.getDaysRepresented() + currentActivityPercentage)
                                                / (double) (entry.getDaysRepresented() + 1);
                                        entry.setActivePercentage(currentActivityPercentage);
                                        entry.setDaysRepresented(entry.getDaysRepresented() + 1);
                                    } else {
                                        entry = new ActiveReadersEntry();
                                        entry.setMinutes(roundMinutes);
                                        entry.setSocialNetwork(service.getIdPrefix());
                                        entry.setDaysRepresented(1);
                                        entry.setActivePercentage(currentActivityPercentage);
                                        entry.setUser(user);
                                        entry.setWeekend(weekend);
                                    }
                                    if (currentActivityPercentage == Double.NaN) {
                                        logger.warn("NaN for entry: " + entry);
                                    }

                                    // store the latest values, though they are not necessarily needed
                                    entry.setTotalFollowers(followers);
                                    entry.setCount(count);
                                    // transaction needed, but a global one for the whole process would be too long-running
                                    messageService.save(entry);

                                    // send notification when there's enough data
                                    if (!user.isReceivedActivityStatsEmail() && (weekdayEntries == 48 || (weekendEntries == 48 && weekdayEntries < 48))) {
                                        sendNotification(user);
                                    }
                                }
                            } catch (Exception ex) {
                                logger.error("Problem adding followers activity entry", ex);
                            }
                        }
                    });
                }
            }
        });
        executor.shutdown();
    }

    private void sendNotification(User user) {
        EmailService.EmailDetails email = new EmailService.EmailDetails();
        email.addTo(user.getEmail()).setFrom(infoEmail)
            .setMessageKey("bestTimeToShareMessage").setSubjectKey("bestTimeToShareSubject")
            .setLocale(user.getProfile().getLanguage().toLocale());

        emailService.send(email);

        user.setReceivedActivityStatsEmail(true);
        messageService.save(user);
    }
}
