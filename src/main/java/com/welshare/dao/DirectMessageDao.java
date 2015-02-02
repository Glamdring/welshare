package com.welshare.dao;

import java.util.List;

import com.welshare.model.DirectMessage;
import com.welshare.model.User;

public interface DirectMessageDao extends Dao {

    List<DirectMessage> getDirectMessages(User recipient, int start, int pageSize);

    void markAllAsRead(User user);
}
