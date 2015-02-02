package com.welshare.web.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.welshare.util.collection.DelegatingI18nMap;

@Component
public class I18nShorthandInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private LocaleResolver localeResolver;

    @Override
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response, Object handler) throws Exception {

        request.setAttribute("msg", new DelegatingI18nMap(messageSource, localeResolver.resolveLocale(request)));

        return true;
    }
}
