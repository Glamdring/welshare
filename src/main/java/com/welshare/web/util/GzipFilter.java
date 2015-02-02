package com.welshare.web.util;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class GzipFilter extends org.mortbay.servlet.GzipFilter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res,
            FilterChain chain) throws IOException, ServletException {
        String requestUri = ((HttpServletRequest) req).getRequestURI();
        // don't gzip images
        if (requestUri.contains("images/") || requestUri.contains(".jpg")
                || requestUri.contains(".png") || requestUri.contains(".gif")) {
            chain.doFilter(req, res);
        } else {
            super.doFilter(req, res, chain);
        }
    }
}
