package com.welshare.model.util;

import org.hibernate.search.bridge.StringBridge;

import com.welshare.model.User;

public class UserBridge implements StringBridge {

    @Override
    public String objectToString(Object param) {
        if (!(param instanceof User)) {
            throw new IllegalArgumentException(
                    "Passed object is not of required type User but is instead of type "
                            + param.getClass().getName());
        }

        return ((User) param).getId();
    }

}
