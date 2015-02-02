package com.welshare.service;

import com.welshare.model.User;

public interface MessageFilterService extends BaseService {

    void createMessageFilter(String text, User loggedUser);

    void deleteMessageFilter(long filterId, User loggedUser);

    void createInterestedInKeyword(String keywords, User loggedUser);

    void deleteInterestedInKeyword(long interestedInId, User loggedUser);
}
