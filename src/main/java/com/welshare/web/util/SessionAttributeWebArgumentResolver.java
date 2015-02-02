package com.welshare.web.util;

import java.lang.annotation.Annotation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.context.request.NativeWebRequest;

@Component
public class SessionAttributeWebArgumentResolver implements WebArgumentResolver {

    public Object resolveArgument(MethodParameter param,
            NativeWebRequest request) throws Exception {

        Annotation[] paramAnns = param.getParameterAnnotations();
        Class<?> paramType = param.getParameterType();

        for (Annotation paramAnn : paramAnns) {
            if (SessionAttribute.class.isInstance(paramAnn)) {
                SessionAttribute SessionAttribute = (SessionAttribute) paramAnn;
                String paramName = SessionAttribute.name();
                boolean required = SessionAttribute.required();

                HttpServletRequest httprequest = (HttpServletRequest) request
                        .getNativeRequest();
                HttpSession session = httprequest.getSession(false);

                if (paramName.isEmpty()) {
                    paramName = param.getParameterName();
                }

                Object result = null;
                if (session != null) {
                    result = session.getAttribute(paramName);
                }

                if (result == null && required && session == null) {
                    raiseSessionRequiredException(paramName, paramType);
                }
                if (result == null && required) {
                    raiseMissingParameterException(paramName, paramType);
                }

                return result;
            }
        }

        return WebArgumentResolver.UNRESOLVED;

    }

    protected void raiseMissingParameterException(String paramName,
            Class<?> paramType) {
        throw new IllegalStateException("Missing parameter '" + paramName
                + "' of type [" + paramType.getName() + "]");
    }

    protected void raiseSessionRequiredException(String paramName,
            Class<?> paramType) throws HttpSessionRequiredException {
        throw new HttpSessionRequiredException(
                "No HttpSession found for resolving parameter '" + paramName
                        + "' of type [" + paramType.getName() + "]");
    }

}
