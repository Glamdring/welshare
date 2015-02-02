package com.welshare.util.cache;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.util.ClassUtils;
/**
 * Copied from DefaultKeyGenerator, but using the class method names in the key as well
 * @author bozho
 *
 */
public class CacheKeyGenerator implements KeyGenerator {

    public static final int NO_PARAM_KEY = -1;
    public static final String NULL_PARAM_KEY = "null";

    private static final Logger logger = LoggerFactory.getLogger(CacheKeyGenerator.class);

    @Override
    public Object generate(Object target, Method method, Object... params) {
        StringBuilder key = new StringBuilder();
        key.append(target.getClass().getSimpleName()).append(".").append(method.getName()).append(":");

        if (params.length == 0) {
            key.append(NO_PARAM_KEY).toString();
        }

        for (Object param : params) {
            if (param == null) {
                key.append(NULL_PARAM_KEY);
            } else if (ClassUtils.isPrimitiveOrWrapper(param.getClass()) || param instanceof String) {
                key.append(param);
            } else if (param instanceof CacheKey) {
                key.append(((CacheKey) param).getCacheKey());
            } else {
                logger.warn("Using object " + param + " as cache key. Either use key='..' or implement CacheKey. Method is " + target.getClass() + "#" + method.getName());
                key.append(param.hashCode());
            }
        }

        return  key.toString();
    }

}
