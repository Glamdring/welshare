package com.welshare.web.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.math.NumberRange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.welshare.util.Constants;

@Component
public class ShortDomainInterceptor extends HandlerInterceptorAdapter {

    private static final String SHORT_URI_PREFIX = "/short/";

    @Value("${url.shortener.domain}")
    private String shortenerDomain;

    @Value("${base.url}")
    private String baseUrl;

    @Override
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response, Object handler) throws Exception {

        if (request.getRequestURL().toString().contains(shortenerDomain)
                && !request.getRequestURI().startsWith(SHORT_URI_PREFIX)) {

            // if this is the root, redirect to the real domain
            if (request.getRequestURI().replace(request.getContextPath(), "").equals("/")) {
                response.sendRedirect(baseUrl);
                return false;
            }

            // if the length of the request URI is not a short key, redirect to base url

            String key = request.getRequestURI();

            // if ending in a punctuation mark, try stripping it before checking further
            // that's because some clients may not properly display urls followed by punctuation marks
            if (key.endsWith(")") || key.endsWith(".") || key.endsWith(",")) {
                key = key.substring(0, key.length() - 1);
            }

            if (new NumberRange(Constants.SHORT_URL_KEY_LENGTH,
                    Constants.SHORT_URL_KEY_LENGTH + Constants.VIRAL_URL_ADDITION_LENGTH).containsInteger(key.length() - 1)) { //-1 for the leading slash
                request.getRequestDispatcher(SHORT_URI_PREFIX + request.getRequestURI())
                        .forward(request, response);
                return false;
            } else {
                response.sendRedirect(baseUrl);
                return false;
            }
        } else {
            return true;
        }
    }
}
