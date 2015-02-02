package com.welshare.web.interceptor;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class SslInterceptor extends HandlerInterceptorAdapter {

    // no need to inject it for now..
    private final PathMatcher pathMatcher = new AntPathMatcher();

    @Value("${base.url.secure}")
    private String secureRoot;

    @Resource(name="secureLocations")
    private List<String> secureLocations;

    @Value("${use.ssl}")
    private boolean useSsl;


    @Override
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response, Object handler) throws Exception {

        if (useSsl && !request.isSecure() && shouldForceSecure(request.getRequestURI())) {

            String redirectUrl = secureRoot + request.getRequestURI();
            if (request.getQueryString() != null) {
                redirectUrl += "?" + request.getQueryString();
            }
            // force session creation - thus it will be accessible to both the
            // secure and the insecure contexts
            request.getSession(true);
            response.sendRedirect(redirectUrl);
            return false;
        }

        return true;
    }

    private boolean shouldForceSecure(String path) {
        for (String pattern : secureLocations) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}
