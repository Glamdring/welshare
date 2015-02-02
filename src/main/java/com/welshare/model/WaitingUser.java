package com.welshare.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class WaitingUser {

    @GeneratedValue
    @Id
    private int waitingUserId;

    @Column
    private String email;

    @Column
    private String invitationCode;

    @Column(nullable=false)
    private boolean invitationSent;

    @Column(nullable=false)
    private boolean registered;

    @Column(nullable=false)
    private long registrationTimestamp;

    public int getWaitingUserId() {
        return waitingUserId;
    }

    public void setWaitingUserId(int waitingUserId) {
        this.waitingUserId = waitingUserId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getInvitationCode() {
        return invitationCode;
    }

    public void setInvitationCode(String invitationCode) {
        this.invitationCode = invitationCode;
    }

    public boolean isInvitationSent() {
        return invitationSent;
    }

    public void setInvitationSent(boolean invitationSent) {
        this.invitationSent = invitationSent;
    }

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public long getRegistrationTimestamp() {
        return registrationTimestamp;
    }

    public void setRegistrationTimestamp(long registrationTimestamp) {
        this.registrationTimestamp = registrationTimestamp;
    }
}
