package com.welshare.service.impl;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import com.welshare.model.InterestedInKeyword;
import com.welshare.model.MessageFilter;
import com.welshare.model.User;
import com.welshare.service.MessageFilterService;
import com.welshare.service.MessageService;
import com.welshare.service.annotations.SqlTransactional;

@Service
public class MessageFilterServiceImpl extends BaseServiceImpl implements
        MessageFilterService {

    @Override
    @CacheEvict(value = MessageService.USER_STREAM_CACHE, key = "'messages-' + #loggedUser.id + '-home'")
    @SqlTransactional
    public void createMessageFilter(String text, User loggedUser) {
        MessageFilter filter = new MessageFilter();
        filter.setUser(loggedUser);
        filter.setFilterText(text);
        save(filter);
    }

    @Override
    @CacheEvict(value = MessageService.USER_STREAM_CACHE, key = "'messages-' + #loggedUser.id + '-home'")
    @SqlTransactional
    public void deleteMessageFilter(long filterId, User loggedUser) {
        MessageFilter filter = get(MessageFilter.class, filterId);
        if (filter != null && filter.getUser().equals(loggedUser)) {
            delete(filter);
        }
    }

    @Override
    @SqlTransactional
    public void createInterestedInKeyword(String keywords, User loggedUser) {
        InterestedInKeyword entity = new InterestedInKeyword();
        entity.setUser(loggedUser);
        entity.setKeywords(keywords);
        save(entity);
    }

    @Override
    @SqlTransactional
    public void deleteInterestedInKeyword(long interestedInId, User loggedUser) {
        InterestedInKeyword entity = get(InterestedInKeyword.class, interestedInId);
        if (entity != null && entity.getUser().equals(loggedUser)) {
            delete(entity);
        }
    }


}
