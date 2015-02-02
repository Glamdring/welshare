package com.welshare.dao.jpa;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.welshare.dao.NotificationEventDao;
import com.welshare.model.NotificationEvent;
import com.welshare.model.User;

@Repository
public class NotificationEventDaoJpa extends BaseDao implements NotificationEventDao {

    private static final String RECIPIENT_PARAM = "recipient";

    @Override
    public List<NotificationEvent> getUnreadNotificationEvents(User user, int start, int max) {
        return findByQuery(new QueryDetails().setQueryName("NotificationEvent.getUnread")
                .setParamNames(new String[] { RECIPIENT_PARAM })
                .setParamValues(new Object[] { user })
                .setStart(start).setCount(max));
    }

    @Override
    public List<NotificationEvent> getAllNotificationEvents(User user, int start, int max) {
        return findByQuery(new QueryDetails().setQueryName("NotificationEvent.getAll")
                .setParamNames(new String[] { RECIPIENT_PARAM })
                .setParamValues(new Object[] { user })
                .setStart(start).setCount(max));
    }

    @Override
    public void markAllAsRead(User user) {
        executeQuery("UPDATE NotificationEvent ne SET read=true WHERE ne.recipient=:recipient", new String[] {RECIPIENT_PARAM}, new Object[] {user});
    }

}
