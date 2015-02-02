package com.welshare.service.model;

import java.io.Serializable;

import com.welshare.model.NotificationEvent;

public class ExternalNotificationEvent extends NotificationEvent implements Serializable {

    private String externalNotificationId;
    private String textMessage;
    private String href;
    private boolean showInternally;
    private String externalSiteName;
    private int count = 1; //used if one object is used to group multiple events

    public String getTextMessage() {
        return textMessage;
    }
    public void setTextMessage(String textMessage) {
        this.textMessage = textMessage;
    }
    public String getHref() {
        return href;
    }
    public void setHref(String href) {
        this.href = href;
    }
    public String getExternalSiteName() {
        return externalSiteName;
    }
    public void setExternalSiteName(String externalSiteName) {
        this.externalSiteName = externalSiteName;
    }
    public boolean isShowInternally() {
        return showInternally;
    }
    public void setShowInternally(boolean showInternally) {
        this.showInternally = showInternally;
    }
    public String getExternalNotificationId() {
        return externalNotificationId;
    }
    public void setExternalNotificationId(String externalNotificationId) {
        this.externalNotificationId = externalNotificationId;
    }
    public int getCount() {
        return count;
    }
    public void setCount(int count) {
        this.count = count;
    }
}
