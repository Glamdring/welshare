package com.welshare.model.util;

import org.hibernate.search.bridge.StringBridge;
import org.joda.time.DateTime;

public class DateTimeBridge implements StringBridge {

    @Override
    public String objectToString(Object param) {
        if (!(param instanceof DateTime)) {
            throw new IllegalArgumentException(
                    "Passed object is not of required type DateTime but is instead of type "
                            + param.getClass().getName());
        }

        return String.valueOf(((DateTime) param).getMillis());
    }

}
