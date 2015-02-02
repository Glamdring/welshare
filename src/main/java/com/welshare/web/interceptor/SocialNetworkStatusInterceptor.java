package com.welshare.web.interceptor;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.welshare.service.MessageService;
import com.welshare.service.social.SocialNetworkStatusHolder;
import com.welshare.web.UserSession;

@Component
public class SocialNetworkStatusInterceptor extends HandlerInterceptorAdapter {

    @Inject
    private CacheManager cacheManager;

    @Inject
    @Qualifier("messageSource")
    private MessageSource messages;

    @Override
    public void postHandle(HttpServletRequest request,
            HttpServletResponse response, Object handler,
            ModelAndView mv) throws Exception {
        if (mv != null) {
            mv.addObject("socialNetworkStatuses", SocialNetworkStatusHolder.getStatuses());

            // also set as special response header, so that it can be read by ajax requests
            String requestedWith = request.getHeader("X-Requested-With");
            if (SocialNetworkStatusHolder.getStatuses() != null && requestedWith != null && requestedWith.equals("XMLHttpRequest")) {
                List<String> resolvedMessages = Lists.newArrayListWithCapacity(SocialNetworkStatusHolder.getStatuses().size());
                for (String status : SocialNetworkStatusHolder.getStatuses()) {
                    // resolve special messages for the ajax response. They are suffixed with "Ajax"
                    resolvedMessages.add(messages.getMessage(status + "Ajax", new Object[]{}, request.getLocale()));
                }
                response.setHeader("External-Network-Statuses", Joiner.on(", ").join(resolvedMessages));
            }

            // clear the cache in case there was an error in some of the networks, so that it is re-fetched next time
            if (!CollectionUtils.isEmpty(SocialNetworkStatusHolder.getStatuses())) {
                cacheManager.getCache(MessageService.USER_STREAM_CACHE).evict("messages-" + UserSession.getUserId(request.getSession()) + "-home");
            }
        }
        SocialNetworkStatusHolder.clear();
    }
}
