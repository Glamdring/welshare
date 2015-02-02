package com.welshare.model.enums;

public enum NotificationType {

    MENTION, REPLY, LIKE, DIRECT_MESSAGE, PICTURE_TAG,
    FOLLOW, UNFOLLOW;

    public String getName() {
        return name();
    }
}
