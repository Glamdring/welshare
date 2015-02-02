package com.welshare.model;

import java.io.Serializable;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import com.welshare.model.enums.NotificationType;

@Entity
@NamedQueries({
    @NamedQuery(name = "NotificationEvent.getUnread",
            query = "SELECT ne FROM NotificationEvent ne WHERE ne.recipient=:recipient AND read=false ORDER BY ne.dateTime DESC"),

    @NamedQuery(name = "NotificationEvent.getAll",
            query = "SELECT ne FROM NotificationEvent ne WHERE ne.recipient=:recipient ORDER BY ne.dateTime DESC"),

    @NamedQuery(name = "NotificationEvent.getByMessageId",
            query = "SELECT ne FROM NotificationEvent ne WHERE ne.targetMessage.id=:messageId")
})
@Cacheable
public class NotificationEvent implements Serializable {

    private static final long serialVersionUID = -3691212438838975016L;

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private long id;

    @Column
    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    @ManyToOne
    private Message targetMessage;

    @ManyToOne
    private User recipient;

    @ManyToOne
    private User sender;

    @Column(name="isRead", nullable=false)
    private boolean read;

    @Column(name = "eventTime")
    @Type(type = "com.welshare.util.persistence.PersistentDateTime")
    @DateTimeFormat(iso=ISO.DATE_TIME)
    private DateTime dateTime;

    // Not placed in the subclass becaues access to it is needed in
    // general-purpose methods
    @Transient
    private String externalMessageId;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public Message getTargetMessage() {
        return targetMessage;
    }

    public void setTargetMessage(Message targetMessage) {
        this.targetMessage = targetMessage;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getExternalMessageId() {
        return externalMessageId;
    }

    public void setExternalMessageId(String externalMessageId) {
        this.externalMessageId = externalMessageId;
    }
}
