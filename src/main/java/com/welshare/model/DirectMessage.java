package com.welshare.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.joda.time.DateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import com.welshare.model.util.UserBridge;

@Entity
@Indexed
@NamedQueries({
    @NamedQuery(
        name = "DirectMessage.getDirectMessages",
        query = "SELECT dm FROM DirectMessage dm JOIN dm.recipients recipient WHERE dm.deleted = false AND recipient.recipient=:recipient"
    )
})
@Cacheable
public class DirectMessage {

    @Id
    @Column(columnDefinition="CHAR(32)")
    @GeneratedValue(generator="hibernate-uuid")
    @GenericGenerator(name = "hibernate-uuid", strategy = "uuid")
    @DocumentId
    private String id;

    @Column(length=300)
    @Field
    private String text;

    @ManyToOne
    @JoinColumn(name="senderId", columnDefinition="CHAR(32)")
    @FieldBridge(impl=UserBridge.class)
    @Field(store=Store.YES)
    private User sender;

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER, orphanRemoval=true)
    private List<DirectMessageRecipient> recipients = new ArrayList<DirectMessageRecipient>();

    @Column(name = "shareTime")
    @Type(type = "com.welshare.util.persistence.PersistentDateTime")
    @DateTimeFormat(iso=ISO.DATE_TIME)
    private DateTime dateTime;

    @ManyToOne
    private DirectMessage originalMessage;

    @Column(nullable=false)
    private boolean deleted;

    @Transient
    private String formattedText;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public List<DirectMessageRecipient> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<DirectMessageRecipient> recipients) {
        this.recipients = recipients;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getFormattedText() {
        return formattedText;
    }

    public void setFormattedText(String formattedText) {
        this.formattedText = formattedText;
    }

    public DirectMessage getOriginalMessage() {
        return originalMessage;
    }

    public void setOriginalMessage(DirectMessage originalMessage) {
        this.originalMessage = originalMessage;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
