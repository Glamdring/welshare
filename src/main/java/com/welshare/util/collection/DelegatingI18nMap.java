package com.welshare.util.collection;

import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

import com.google.common.collect.ForwardingMap;

public class DelegatingI18nMap extends ForwardingMap<String, String> {
    private static final Logger logger = LoggerFactory.getLogger(DelegatingI18nMap.class);

    private final MessageSource messageSource;
    private final Locale locale;

    public DelegatingI18nMap(MessageSource messageSource, Locale locale) {
        this.messageSource = messageSource;
        this.locale = locale;
    }

    @Override
    public String get(Object key) {
        try {
            return messageSource.getMessage((String) key, null, locale);
        } catch (NoSuchMessageException ex) {
            if (!locale.equals(Locale.getDefault())) {
                try {
                    return messageSource.getMessage((String) key, null, Locale.getDefault());
                } catch (NoSuchMessageException e) {
                    logger.warn(e.getMessage());
                    return (String) key;
                }
            } else {
                logger.warn(ex.getMessage());
                return (String) key;
            }
        }
    }

    /**
     * method used for parsing messages with parameter placeholders
     * @param key
     * @param args
     * @return
     */
    public String get(String key, String... args) {
        try {
            return messageSource.getMessage(key, args, locale);
        } catch (NoSuchMessageException ex) {
            if (!locale.equals(Locale.getDefault())) {
                try {
                    return messageSource.getMessage((String) key, args, Locale.getDefault());
                } catch (NoSuchMessageException e) {
                    logger.warn(e.getMessage());
                    return key;
                }
            } else {
                logger.warn(ex.getMessage());
                return key;
            }
        }
    }

    @Override
    protected Map<String, String> delegate() {
        // no need to implement this, the forwarding map is used
        // as a simple adapter with empty implementations
        // calling them would result in NPE, but they are never called
        // because this class is used only in JSTL expressions like
        // ${mapAttr.key}
        return null;
    }

}