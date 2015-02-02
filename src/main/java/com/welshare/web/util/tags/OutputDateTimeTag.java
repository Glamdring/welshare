package com.welshare.web.util.tags;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

public class OutputDateTimeTag extends SimpleTagSupport {

    private String timeZone;
    private DateTime dateTime;
    private String pattern;


    @Override
    public void doTag() throws JspException, IOException {
        if (StringUtils.isEmpty(timeZone)) {
            timeZone = DateTimeZone.UTC.getID();
        }
        if (dateTime != null) {
            DateTime zonedDateTime = dateTime.withZone(DateTimeZone.forID(timeZone));
            if (pattern == null) {
                getJspContext().getOut().print(zonedDateTime.toString());
            } else {
                getJspContext().getOut().print(DateTimeFormat.forPattern(pattern).print(dateTime)); //TODO cache
            }
        }
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String format) {
        this.pattern = format;
    }
}
