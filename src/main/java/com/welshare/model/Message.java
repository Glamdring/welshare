package com.welshare.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
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

import com.welshare.model.util.DateTimeBridge;
import com.welshare.model.util.UserBridge;

/**
 * The message object is used for both storing in the DB and sending to clients
 * Also, external messages (from twitter, facebook, etc) are represented as
 * a Message object, for the sake of unification
 *
 * @author Bozhidar Bozhanov
 *
 */
@Entity
@Embeddable
@NamedQueries({
        @NamedQuery(name = "Message.getMessages", query = "SELECT DISTINCT m FROM Message m LEFT OUTER JOIN m.originalMessage om WHERE m.author IN (:followedUsers) AND (m.liking = true OR m.originalMessage.id IS NULL OR om.author NOT IN (:followedUsers)) AND m.deleted=false ORDER BY m.dateTime DESC"),
        @NamedQuery(name = "Message.getMessagesAfterLast", query = "SELECT DISTINCT m FROM Message m LEFT OUTER JOIN m.originalMessage om WHERE m.dateTime < :lastMessageTime AND m.author IN (:followedUsers) AND (m.liking = true OR m.originalMessage.id IS NULL OR om.author NOT IN (:followedUsers)) AND m.deleted=false ORDER BY m.dateTime DESC"),
        @NamedQuery(name = "Message.getMessagesSinceLast", query = "SELECT DISTINCT m FROM Message m LEFT OUTER JOIN m.originalMessage om WHERE m.dateTime > :lastMessageTime AND m.author IN (:followedUsers) AND (m.liking = true OR m.originalMessage.id IS NULL OR om.author NOT IN (:followedUsers)) AND m.deleted=false ORDER BY m.dateTime DESC"),
        @NamedQuery(name = "Message.getUserMessages", query = "SELECT m FROM Message m WHERE (m.author = :user OR m.addressee = :user) AND m.deleted=false ORDER BY m.dateTime DESC"),
        @NamedQuery(name = "Message.getTaggedMessages", query = "SELECT m FROM Message m, Tag tag WHERE tag.name=:tag AND tag IN elements(m.tags) AND m.deleted=false ORDER BY m.dateTime DESC"),
        @NamedQuery(name = "Message.getReplies", query = "SELECT m FROM Message m WHERE m.originalMessage.id = :originаlMessageId AND m.liking = false AND m.deleted=false ORDER BY m.dateTime ASC"),
        @NamedQuery(name = "Message.getAnalytics", query = "SELECT m FROM Message m WHERE m.author=:user AND m.liking = false AND m.deleted=false AND m.imported=false ORDER BY m.dateTime DESC"),
        @NamedQuery(name = "Message.getOldMessage", query = "SELECT m FROM Message m WHERE m.author=:user AND DAY(m.dateTime) = DAY(NOW()) AND m.dateTime < :threshold AND m.originalMessage IS NULL and m.externalOriginalMessageId IS NULL ORDER BY RAND() DESC") })
@JsonIgnoreProperties({ "tags", "reply" })
@Indexed
public class Message implements Serializable {

    private static final long serialVersionUID = 1633475697995565589L;

    @Id
    @Column(columnDefinition="CHAR(32)")
    @GeneratedValue(generator="hibernate-uuid")
    @GenericGenerator(name = "hibernate-uuid", strategy = "uuid")
    @DocumentId
    private String id;

    @Column(length = 300)
    @Field
    private String text;

    @Column(nullable=false)
    private int score;

    @ManyToOne
    private Message originalMessage;

    @Column
    private String externalOriginalMessageId;

    @Column(nullable=false)
    private boolean externalLike;

    @ManyToOne
    @JoinColumn(name="authorId", columnDefinition="CHAR(32)")
    @FieldBridge(impl=UserBridge.class)
    @Field(store=Store.YES)
    private User author;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE } )
    private Set<Tag> tags = new HashSet<Tag>();

    @Column(name = "shareTime")
    @Type(type = "com.welshare.util.persistence.PersistentDateTime")
    @DateTimeFormat(iso=ISO.DATE_TIME)
    @FieldBridge(impl=DateTimeBridge.class)
    @Field(store=Store.YES)
    private DateTime dateTime;

    @Column(nullable=false)
    private boolean liking;

    @Column(nullable=false)
    private int replies;

    @OneToMany(fetch=FetchType.LAZY, cascade=CascadeType.ALL)
    private List<Picture> pictures = new ArrayList<Picture>();

    @Column(nullable=false)
    private int pictureCount;

    @Column(nullable=false)
    private boolean deleted;

    @ElementCollection(fetch=FetchType.EAGER)
    private List<String> associatedExternalIds = new ArrayList<String>();

    @ManyToOne
    @JoinColumn(name="addresseeId", columnDefinition="CHAR(32)")
    private User addressee;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE } )
    @JoinTable(name="hiddenFromUsers")
    private List<User> hiddenFrom = new ArrayList<User>();

    @Column(nullable=false)
    private boolean imported;

    @Column
    private String importSource;

    @Column(nullable=false)
    private boolean hasShortenedUrls;

    // A couple of display-only properties (not stored in DB)

    @Transient
    private MessageData data = new MessageData();

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

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Message getOriginalMessage() {
        return originalMessage;
    }

    public void setOriginalMessage(Message originalMessage) {
        this.originalMessage = originalMessage;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }

    public boolean isLiking() {
        return liking;
    }

    public void setLiking(boolean like) {
        this.liking = like;
    }

    public int getReplies() {
        return replies;
    }

    public void setReplies(int comments) {
        this.replies = comments;
    }

    public boolean isReply() {
        return originalMessage != null && !liking;
    }

    public boolean isExternalReply() {
        return data.getExternalId() != null && externalOriginalMessageId != null;
    }

    public String getExternalOriginalMessageId() {
        return externalOriginalMessageId;
    }

    public void setExternalOriginalMessageId(String externalOriginalMessageId) {
        this.externalOriginalMessageId = externalOriginalMessageId;
    }

    public List<Picture> getPictures() {
        return pictures;
    }

    public void setPictures(List<Picture> pictures) {
        this.pictures = pictures;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public String getPublicId() {
        if (id != null) {
            return id;
        } else {
            return data.getExternalId();
        }
    }

    public List<String> getAssociatedExternalIds() {
        return associatedExternalIds;
    }

    public void setAssociatedExternalIds(List<String> associatedExternalIds) {
        this.associatedExternalIds = associatedExternalIds;
    }

    public void setAddressee(User addressee) {
        this.addressee = addressee;
    }

    public boolean isExternalLike() {
        return externalLike;
    }

    public void setExternalLike(boolean externalLike) {
        this.externalLike = externalLike;
    }

    /**
     * A non-standard getter!
     * @return the short text if set, or the regular text otherwise
     */
    public String getShortText() {
        if (data.getShortText() == null) {
            return text;
        } else {
            return data.getShortText();
        }
    }

    public void setShortText(String shortText) {
        this.data.setShortText(shortText);
    }

    /**
     * @return the original message id - internal, if the message is internal,
     * and external, if the message is external
     */
    public String getPublicOriginalMessageId() {
        if (externalOriginalMessageId != null) {
            return externalOriginalMessageId;
        } else if (originalMessage != null) {
            return originalMessage.getPublicId();
        } else {
            //throw new IllegalStateException("The method should be called only for replies");
            return null;
        }
    }

    public MessageData getData() {
        return data;
    }

    public void setData(MessageData data) {
        this.data = data;
    }

    public User getAddressee() {
        return addressee;
    }

    public List<User> getHiddenFrom() {
        return hiddenFrom;
    }

    public void setHiddenFrom(List<User> hiddenFrom) {
        this.hiddenFrom = hiddenFrom;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Message other = (Message) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Message [id=" + id + ", text=" + text + ", externalId="
                + data.getExternalId() + "]";
    }

    public String getTextWithPictureUrls() {
        StringBuilder sb = new StringBuilder(text);
        if (pictureCount > 0) {
            for (Picture pic : pictures) {
                sb.append(" ").append(pic.getPublicUrl());
            }
        }

        return sb.toString();
    }

    public int getPictureCount() {
        return pictureCount;
    }

    public void setPictureCount(int pictureCount) {
        this.pictureCount = pictureCount;
    }

    public boolean isImported() {
        return imported;
    }

    public void setImported(boolean imported) {
        this.imported = imported;
    }

    public String getImportSource() {
        return importSource;
    }

    public void setImportSource(String importSource) {
        this.importSource = importSource;
    }

    public boolean isHasShortenedUrls() {
        return hasShortenedUrls;
    }

    public void setHasShortenedUrls(boolean hasShortenedUrls) {
        this.hasShortenedUrls = hasShortenedUrls;
    }
}
