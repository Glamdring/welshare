package com.welshare.web.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Sets the cache headers for non-static resources.
 *
 * Note that this can't be done via the AnnotationMethodHandlerAdapter, because
 * it sets the values for static resources as well
 *
 * @author Bozhidar Bozhanov
 *
 */
@Component
public class CacheInterceptor extends HandlerInterceptorAdapter {

    private static final String HEADER_PRAGMA = "Pragma";
    private static final String HEADER_EXPIRES = "Expires";
    private static final String HEADER_CACHE_CONTROL = "Cache-Control";

    @Override
    public void postHandle(HttpServletRequest request,
            HttpServletResponse response, Object handler, ModelAndView mv) throws Exception {

        // if this is static resource and if no headers were set by the controller
        if (!request.getRequestURI().contains("/static/") && response.getHeader(HEADER_EXPIRES) == null) {
            response.setHeader(HEADER_PRAGMA, "no-cache");
            // HTTP 1.0 header
            response.setDateHeader(HEADER_EXPIRES, 1L);
            // HTTP 1.1 header: "no-cache" is the standard value,
            // "no-store" is necessary to prevent caching on FireFox.
            response.setHeader(HEADER_CACHE_CONTROL, "no-cache");
            response.addHeader(HEADER_CACHE_CONTROL, "no-store");
        }
    }
}
