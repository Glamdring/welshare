package com.welshare.service.jobs;

import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.welshare.dao.MessageDao;
import com.welshare.model.ScheduledMessage;
import com.welshare.service.ShareService;
import com.welshare.service.annotations.SqlTransactional;

@Component
public class ScheduledMessagesJob {
    private static final String DELIMITER = ",";

    @Inject
    private PriorityQueue<ScheduledMessage> queue;

    @Inject
    private ShareService shareService;

    @Inject
    private MessageDao messageDao;

    @Scheduled(fixedRate=DateTimeConstants.MILLIS_PER_HOUR)
    public void fillInMemoryQueue() {
        DateTime inOneHour = new DateTime().plusHours(1);
        List<ScheduledMessage> messages = messageDao.getScheduledMessages(inOneHour);
        for (ScheduledMessage msg : messages) {
            // push to the queue only if it is scheduled to be sent more than 65 minutes from the moment of scheduling
            // otherwise it would be in the queue already (see ShareService#schedule)
            if (msg.getTimeOfScheduling().plusMinutes(65).isBefore(msg.getScheduledTime())) {
                queue.offer(msg);
            }
        }
    }

    @Scheduled(fixedRate=DateTimeConstants.MILLIS_PER_MINUTE)
    @SqlTransactional
    public void shareScheduledMessages() {
        while (true) {
            if (queue.isEmpty()) {
                break;
            }
            DateTime messageTime = queue.peek().getScheduledTime();
            // send messages whose time is before the current moment. This job runs every minute,
            // so the maximum delay for a message will be 1 minute
            if (messageTime.isBeforeNow()) {
                ScheduledMessage msg = queue.poll();
                shareService.share(msg.getText(),
                        msg.getUserId(),
                        Arrays.asList(StringUtils.split(msg.getPictureUrls(), DELIMITER)),
                        Arrays.asList(StringUtils.split(msg.getExternalSites(), DELIMITER)),
                        Arrays.asList(StringUtils.split(msg.getHideFromUsernames(), DELIMITER)),
                        msg.isHideFromCloseFriends());

                shareService.delete(msg);
            } else {
                break;
            }
        }
    }
}
