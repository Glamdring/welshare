package com.welshare.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;

@Entity
@NamedQueries(@NamedQuery(
        name = "Picture.getMessage",
        query = "SELECT msg from Message msg JOIN msg.pictures pic WHERE pic=:picture"))
public class Picture implements Serializable {

    private static final long serialVersionUID = 4401583917626357127L;

    @Id
    @Column(columnDefinition = "CHAR(32)")
    @GeneratedValue(generator = "hibernate-uuid")
    @GenericGenerator(name = "hibernate-uuid", strategy = "uuid")
    private String id;

    @Column
    private String path;

    @Column
    private String publicUrl;

    @Column
    private String shortKey;

    @ManyToOne
    @JoinColumn(name="uploaderId", columnDefinition="CHAR(32)")
    private User uploader;

    @Transient
    private boolean external;

    @Transient
    private String externalUrl;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isExternal() {
        return external;
    }

    public void setExternal(boolean external) {
        this.external = external;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public String getShortKey() {
        return shortKey;
    }

    public void setShortKey(String shortKey) {
        this.shortKey = shortKey;
    }

    public User getUploader() {
        return uploader;
    }

    public void setUploader(User uploader) {
        this.uploader = uploader;
    }
}
