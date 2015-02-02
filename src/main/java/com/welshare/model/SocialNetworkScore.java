package com.welshare.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames = { "userId", "socialNetwork" }))
public class SocialNetworkScore {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private long scoreId;

    @ManyToOne
    @JoinColumn(name="userId", columnDefinition="CHAR(32)")
    private User user;

    @Column
    private String socialNetwork;

    @Column(nullable=false)
    private int score;

    @Column
    @Type(type = "com.welshare.util.persistence.PersistentDateTime")
    private DateTime lastCalculated;

    public long getScoreId() {
        return scoreId;
    }

    public void setScoreId(long scoreId) {
        this.scoreId = scoreId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public DateTime getLastCalculated() {
        return lastCalculated;
    }

    public void setLastCalculated(DateTime lastCalculated) {
        this.lastCalculated = lastCalculated;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getSocialNetwork() {
        return socialNetwork;
    }

    public void setSocialNetwork(String socialNetwork) {
        this.socialNetwork = socialNetwork;
    }
}
