package com.welshare.web.util;

import java.lang.annotation.Annotation;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.context.request.NativeWebRequest;

@Component
public class RequestAttributeWebArgumentResolver implements WebArgumentResolver {

    public Object resolveArgument(MethodParameter param,
            NativeWebRequest request) throws Exception {

        Annotation[] paramAnns = param.getParameterAnnotations();
        Class<?> paramType = param.getParameterType();

        for (Annotation paramAnn : paramAnns) {
            if (RequestAttribute.class.isInstance(paramAnn)) {
                RequestAttribute RequestAttribute = (RequestAttribute) paramAnn;
                String paramName = RequestAttribute.value();
                boolean required = RequestAttribute.required();

                if (paramName.isEmpty()) {
                    paramName = param.getParameterName();
                }

                Object result = request.getAttribute(paramName, NativeWebRequest.SCOPE_REQUEST);

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
}
