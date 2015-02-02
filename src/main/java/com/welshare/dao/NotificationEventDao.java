package com.welshare.dao;

import java.util.List;

import com.welshare.model.NotificationEvent;
import com.welshare.model.User;

public interface NotificationEventDao extends Dao {

    List<NotificationEvent> getUnreadNotificationEvents(User user, int start, int max);

    List<NotificationEvent> getAllNotificationEvents(User user, int start, int max);

    void markAllAsRead(User user);
}
