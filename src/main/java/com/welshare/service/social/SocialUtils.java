package com.welshare.service.social;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import org.springframework.scheduling.annotation.AsyncResult;

import com.welshare.model.Message;

public final class SocialUtils {

    private SocialUtils() { }

    public static SocialNetworkService getSocialNetworkService(
            List<SocialNetworkService> socialNetworkServices,
            String externalId) {

        for (SocialNetworkService sns : socialNetworkServices) {
            if (sns.shouldHandle(externalId)) {
                return sns;
            }
        }
        return null;
    }

    public static boolean isExternal(String id, List<SocialNetworkService> services) {
        for (SocialNetworkService sns : services) {
            if (sns.shouldHandle(id)) {
                return true;
            }
        }
        return false;
    }

    public static <T> Future<List<T>> emptyFutureList() {
        return new AsyncResult<List<T>>(Collections.<T>emptyList());
    }

    public static Future<List<Message>> wrapMessageList(final List<Message> messages) {
        return new AsyncResult<List<Message>>(messages);
    }

    public static String trimSpecialSymbolElements(String message) {
        message = message.replace(" @", " ");
        message = message.replace(" #", " ");
        message = message.replace(" RT:", " ");
        message = message.replace(" RT ", " ");
        return message;
    }
}
