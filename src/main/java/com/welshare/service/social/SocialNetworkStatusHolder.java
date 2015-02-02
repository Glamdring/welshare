package com.welshare.service.social;

import java.util.HashSet;
import java.util.Set;
/**
 * Class to hold status messages from all social network services
 * Theses status messages can later be shown to the user
 *
 * @author Bozhidar Bozhanov
 *
 */
public final class SocialNetworkStatusHolder {

    private static ThreadLocal<Set<String>> statuses = new ThreadLocal<Set<String>>();

    private SocialNetworkStatusHolder() { }

    public static void addStatus(String messageKey) {
        Set<String> list = statuses.get();
        if (list == null) {
            list = new HashSet<String>();
            statuses.set(list);
        }

        list.add(messageKey);
    }

    public static Set<String> getStatuses() {
        return statuses.get();
    }

    public static void clear() {
        statuses.remove();
    }
}
