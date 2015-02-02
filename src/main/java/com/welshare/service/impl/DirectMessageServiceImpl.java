package com.welshare.service.impl;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.welshare.dao.DirectMessageDao;
import com.welshare.model.DirectMessage;
import com.welshare.model.DirectMessageRecipient;
import com.welshare.model.User;
import com.welshare.model.enums.NotificationType;
import com.welshare.service.DirectMessageService;
import com.welshare.service.NotificationEventService;
import com.welshare.service.annotations.SqlReadonlyTransactional;
import com.welshare.service.annotations.SqlTransactional;

@Service
public class DirectMessageServiceImpl extends BaseServiceImpl implements DirectMessageService {

    @Inject
    private DirectMessageDao dao;

    @Inject
    private NotificationEventService eventService;

    @Value("${common.page.size}")
    private int pageSize;

    @Override
    @SqlTransactional
    public void sendDirectMessage(String text, String originalId, User sender,
            List<String> recipientIds) {
        DirectMessage directMessage = new DirectMessage();
        directMessage.setSender(sender);

        for (String recipientId : recipientIds) {
            DirectMessageRecipient recipient = new DirectMessageRecipient();
            recipient.setRecipient(get(User.class, recipientId));
            directMessage.getRecipients().add(recipient);

            eventService.createEvent(NotificationType.DIRECT_MESSAGE, sender,
                    recipient.getRecipient(), null);
        }

        directMessage.setDateTime(new DateTime());
        directMessage.setText(text);

        if (StringUtils.isNotEmpty(originalId)) {
            directMessage.setOriginalMessage(get(DirectMessage.class, originalId));
        }

        save(directMessage);
    }

    @Override
    @SqlReadonlyTransactional
    public List<DirectMessage> getIncomingDirectMessages(User user, int from) {
        return dao.getDirectMessages(user, from, pageSize);
    }

    @Override
    @SqlTransactional
    public boolean delete(String messageId, User user) {
        DirectMessage msg = get(DirectMessage.class, messageId);
        if (msg == null || !msg.getSender().equals(user)) {
            return false;
        }
        msg.setDeleted(true);
        save(msg);

        return true;
    }

    @Override
    @SqlTransactional
    public void markAllAsRead(User user) {
        dao.markAllAsRead(user);
    }
}
