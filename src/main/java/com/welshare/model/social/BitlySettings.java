package com.welshare.model.social;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class BitlySettings implements Serializable {

    private static final long serialVersionUID = 890309185789413563L;

    @Column(name="bitlyUser")
    private String user;

    @Column(name="bitlyApiKey")
    private String apiKey;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
