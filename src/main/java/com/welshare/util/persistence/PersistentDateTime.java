package com.welshare.util.persistence;

import java.util.Properties;

import org.joda.time.DateTimeZone;


/**
 * Class simply used to support switching to multiple types without changing mapping.
 * Current type implementation: jadira.usertype
 * @author bozho
 */
public class PersistentDateTime extends org.jadira.usertype.dateandtime.joda.PersistentDateTime {

    private static final long serialVersionUID = 6517203034160316166L;

    @Override
    public void setParameterValues(Properties parameters) {
        // the type doesn't use the default joda-time timezone, but uses the JVM one instead.
        if (parameters == null) {
            parameters = new Properties();
        }
        parameters.setProperty("databaseZone", DateTimeZone.getDefault().getID());
        super.setParameterValues(parameters);
    }
}
