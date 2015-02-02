package com.welshare.service;

import java.util.List;

import com.welshare.model.Message;
import com.welshare.model.NotificationEvent;
import com.welshare.model.User;
import com.welshare.model.enums.NotificationType;

/**
 * Persists notifications
 *
 * @author Bozhidar Bozhanov
 *
 */
public interface NotificationEventService {

    void createEvent(Message originalMessage, NotificationType notificationType, User sender);

    void createUserEvent(User followed, NotificationType follow, User follower);

    void createMentionEvent(Message message, User mentionedUser);

    void createEvent(NotificationType notificationType, User sender, User recipient, String url);

    List<NotificationEvent> getUnread(String userId);

    List<NotificationEvent> getAll(String userId, String lastNotificationId);

    List<NotificationEvent> getLastRead(String userId, int count);

    void markAllAsRead(String userId);



}
