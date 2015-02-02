package com.welshare.service.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.welshare.dao.NotificationEventDao;
import com.welshare.model.Message;
import com.welshare.model.NotificationEvent;
import com.welshare.model.User;
import com.welshare.model.enums.NotificationType;
import com.welshare.service.EmailService;
import com.welshare.service.MessageService;
import com.welshare.service.NotificationEventService;
import com.welshare.service.annotations.SqlReadonlyTransactional;
import com.welshare.service.annotations.SqlTransactional;
import com.welshare.service.social.SocialNetworkService;

@Service
public class NotificationEventServiceImpl implements NotificationEventService {

    private static final EventTimeComparator EVENT_TIME_COMPARATOR
        = new EventTimeComparator();

    @Inject
    private NotificationEventDao dao;

    @Inject
    private List<SocialNetworkService> socialNetworkServices;

    @Inject
    private EmailService emailService;

    @Value("${common.page.size}")
    private int pageSize;

    @Value("${base.url}")
    private String baseUrl;

    @Value("${information.email.sender}")
    private String emailSender;

    @Inject
    private CacheManager cacheManager;

    @Override
    public void createEvent(Message message, NotificationType notificationType,
            User sender) {

        NotificationEvent event = new NotificationEvent();
        event.setNotificationType(notificationType);
        if (message.isReply()) {
            event.setTargetMessage(message.getOriginalMessage());
        } else {
            event.setTargetMessage(message);
        }
        event.setSender(sender);
        event.setRecipient(message.getAuthor());
        event.setDateTime(new DateTime());

        sendEmailNotification(message, notificationType, message.getAuthor(), sender);
        dao.persist(event);
    }

    @Override
    public void createMentionEvent(Message message, User mentionedUser) {
        NotificationEvent event = new NotificationEvent();
        event.setNotificationType(NotificationType.MENTION);
        if (message.isReply()) {
            event.setTargetMessage(message.getOriginalMessage());
        } else {
            event.setTargetMessage(message);
        }
        event.setSender(message.getAuthor());
        event.setRecipient(mentionedUser);
        event.setDateTime(new DateTime());

        sendEmailNotification(message, NotificationType.MENTION, mentionedUser, message.getAuthor());
        dao.persist(event);

    }

    @Override
    @Cacheable(value = MessageService.USER_STREAM_CACHE, key="'unreadNotifications-' + #userId")
    public List<NotificationEvent> getUnread(String userId) {
        User user = dao.getById(User.class, userId);
        List<NotificationEvent> unread = dao.getUnreadNotificationEvents(user, 0, pageSize);

        for (SocialNetworkService snService : socialNetworkServices) {
            unread.addAll(snService.getUnreadNotifications(user));
        }

        Collections.sort(unread, EVENT_TIME_COMPARATOR);

        return unread;
    }

    @Override
    @Cacheable(value = MessageService.USER_STREAM_CACHE, key="'allNotifications-' + #userId + '-' + #lastNotificationId")
    @SqlReadonlyTransactional
    public List<NotificationEvent> getAll(String userId, String lastNotificationId) {
        User user = dao.getById(User.class, userId);
        List<NotificationEvent> events = dao.getAllNotificationEvents(user, 0, pageSize); //TODO change 0
        //TODO use paging
        //TODO remove null
        for (SocialNetworkService snService : socialNetworkServices) {
            events.addAll(snService.getNotifications(null, 100, user));
        }

        Collections.sort(events, EVENT_TIME_COMPARATOR);

        return events;
    }

    @Override
    @EvictUnread
    @EvictLastRead
    @SqlTransactional
    public void markAllAsRead(String userId) {
        User user = dao.getById(User.class, userId, true);
        dao.markAllAsRead(user);
        for (SocialNetworkService snService : socialNetworkServices) {
            snService.markNotificationsAsRead(user);
        }

        // multiple evict annotations are not working with spring yet, so we manually remove the entry
        cacheManager.getCache(MessageService.USER_STREAM_CACHE).evict("lastReadNotifications-" + userId);

    }

    @Override
    @SqlTransactional
    public void createUserEvent(User followed, NotificationType type,
            User follower) {
        NotificationEvent event = new NotificationEvent();
        event.setNotificationType(type);
        event.setSender(follower);
        event.setRecipient(followed);
        event.setDateTime(new DateTime());

        dao.persist(event);
    }

    private static class EventTimeComparator implements Comparator<NotificationEvent> {
        @Override
        public int compare(NotificationEvent o1, NotificationEvent o2) {
            return -1 * o1.getDateTime().compareTo(o2.getDateTime()); //desc
        }
    }

    @Override
    @Cacheable(value = MessageService.USER_STREAM_CACHE, key="'lastReadNotifications-' + #userId")
    @SqlReadonlyTransactional
    public List<NotificationEvent> getLastRead(String userId, int count) {
        User user = dao.getById(User.class, userId);
        List<NotificationEvent> events = dao.getAllNotificationEvents(user, 0, count);

        //TODO change null to something meaningful (as argument)
        for (SocialNetworkService snService : socialNetworkServices) {
            events.addAll(snService.getNotifications(null, 100, user));
        }

        Collections.sort(events, EVENT_TIME_COMPARATOR);

        // sublist starts at 5-count, because if count is 4, this means there
        // is 1 unread notification in the list
        if (events.size() >= 5 - count) {
            return new ArrayList<NotificationEvent>(events.subList(5 - count, Math.min(events.size(), 5)));
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    @SqlTransactional
    public void createEvent(NotificationType notificationType, User sender,
            User recipient, String url) {

        NotificationEvent event = new NotificationEvent();
        event.setNotificationType(notificationType);
        event.setSender(sender);
        event.setRecipient(recipient);
        event.setDateTime(new DateTime());
        // TODO URL

        dao.persist(event);
    }

    private void sendEmailNotification(Message targetMessage,
            NotificationType type, User recipient, User sender) {

        if (type != NotificationType.REPLY && type != NotificationType.LIKE && type != NotificationType.MENTION) {
            return;
        }

        String url = baseUrl + "/message/" + targetMessage.getId();

        EmailService.EmailDetails details = new EmailService.EmailDetails();
        details.setTo(recipient.getEmail())
            .setLocale(recipient.getProfile().getLanguage().toLocale())
            .setFrom(emailSender)
            .setSubjectParams(new String[] {sender.getNames()})
            .setMessageParams(new String[] {sender.getNames(), url});

        if (type == NotificationType.REPLY && recipient.getProfile().isReceiveMailForReplies()) {
            details.setSubjectKey("likeNotificationSubject")
                .setMessageKey("likeNotificationMessage");
            emailService.send(details);
        }

        if (type == NotificationType.LIKE && recipient.getProfile().isReceiveMailForLikes()) {
            details.setSubjectKey("replyNotificationSubject")
                .setMessageKey("replyNotificationMessage");
            emailService.send(details);
        }

        if (type == NotificationType.MENTION && recipient.getProfile().isReceiveMailForReplies()) {
            details.setSubjectKey("mentionNotificationSubject")
                .setMessageKey("mentionNotificationMessage");
            emailService.send(details);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @CacheEvict(value = MessageService.USER_STREAM_CACHE, key="'lastReadNotifications-' + #userId")
    public @interface EvictLastRead { }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @CacheEvict(value = MessageService.USER_STREAM_CACHE, key="'unreadNotifications-' + #userId")
    public @interface EvictUnread { }
}
