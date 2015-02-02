package com.welshare.service;

import java.util.List;

import com.welshare.model.DirectMessage;
import com.welshare.model.User;

public interface DirectMessageService extends BaseService {
    /**
     * Sends a direct message from one user to a group of other users
     * @param text
     * @param originalId
     * @param sender
     * @param recipientId
     */
    void sendDirectMessage(String text, String originalId, User sender, List<String> recipientIds);

    /**
     * Gets pages list
     * @param the recipient of the messages
     * @return paged list
     */
    List<DirectMessage> getIncomingDirectMessages(User user, int from);

    boolean delete(String messageId, User user);

    void markAllAsRead(User user);
}
