package com.welshare.service.jobs;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.welshare.dao.Dao;
import com.welshare.dao.UserDao;
import com.welshare.model.Message;
import com.welshare.model.User;
import com.welshare.service.EmailService;
import com.welshare.service.MessageService;
import com.welshare.service.annotations.SqlReadonlyTransactional;
import com.welshare.util.WebUtils;

@Service
public class DailyEmailsJob {
    private static final Logger logger = LoggerFactory.getLogger(DailyEmailsJob.class);

    @Inject
    private MessageService messageService;
    @Inject
    private UserDao userDao;
    @Inject
    private EmailService emailService;

    @Value("${dailyMessageThreshold}")
    private int threshold;
    @Value("${dailyMessageThresholdRatio}")
    private int thresholdRatio;
    @Value("${information.email.sender}")
    private String senderEmail;

    @Scheduled(cron="0 0 9 ? * *")
    @SqlReadonlyTransactional
    public void sendTopDailyMessagesEmail() {
        userDao.performBatched(User.class, 200, new Dao.PageableOperation<User>() {
            @Override
            public void execute() {
                for (User user : getData()) {
                    try {
                        if (!user.getProfile().isReceiveDailyTopMessagesMail()) {
                            continue;
                        }
                        List<Message> topDailyMessages = messageService.getTopDailyMessages(threshold, thresholdRatio, user);
                        if (topDailyMessages.isEmpty()) {
                            continue;
                        }

                        WebUtils.prepareForOutput(topDailyMessages);

                        EmailService.EmailDetails details = new EmailService.EmailDetails();
                        details.setTo(user.getEmail())
                            .setMessageTemplate("dailyMessages.vm")
                            .setMessageTemplateModel(Collections.<String, Object>singletonMap("messages", topDailyMessages))
                            .setHtml(true)
                            .setFrom(senderEmail)
                            .setCurrentUser(user)
                            .setLocale(user.getProfile().getLanguage().toLocale())
                            .setSubjectKey("dailyTopMessagesSubject");
                        emailService.send(details);
                    } catch (Exception ex) {
                        logger.error("Problem sending daily emails", ex);
                    }
                }
            }
        });
    }
}
