package com.welshare.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.annotations.Index;

import com.google.common.primitives.Ints;

@Entity
@NamedQueries(
        @NamedQuery(name="ActiveReadersEntry.get", query = "SELECT are FROM ActiveReadersEntry are WHERE are.user=:user AND are.socialNetwork=:socialNetwork ORDER BY minutes ASC"))
public class ActiveReadersEntry implements Comparable<ActiveReadersEntry> {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private long id;
    @ManyToOne
    @JoinColumn(name="userId", columnDefinition="CHAR(32)")
    @Index(name="userIndex")
    private User user;
    @Column
    private String socialNetwork;
    @Column(nullable=false)
    private int count;
    @Column(nullable=false)
    private int minutes;
    @Column(nullable=false)
    private int totalFollowers;
    @Column(nullable=false)
    private double activePercentage;
    @Column(nullable=false)
    private int daysRepresented;
    @Column(nullable=false)
    private boolean weekend;

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }
    public int getCount() {
        return count;
    }
    public void setCount(int count) {
        this.count = count;
    }
    public int getMinutes() {
        return minutes;
    }
    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }
    public int getTotalFollowers() {
        return totalFollowers;
    }
    public void setTotalFollowers(int totalFollowers) {
        this.totalFollowers = totalFollowers;
    }
    public double getActivePercentage() {
        return activePercentage;
    }
    public void setActivePercentage(double activePercentage) {
        this.activePercentage = activePercentage;
    }
    public String getSocialNetwork() {
        return socialNetwork;
    }
    public void setSocialNetwork(String socialNetwork) {
        this.socialNetwork = socialNetwork;
    }
    public int getDaysRepresented() {
        return daysRepresented;
    }
    public void setDaysRepresented(int daysRepresented) {
        this.daysRepresented = daysRepresented;
    }
    public boolean isWeekend() {
        return weekend;
    }
    public void setWeekend(boolean weekend) {
        this.weekend = weekend;
    }
    @Override
    public String toString() {
        return "ActiveReadersEntry [id=" + id + ", user=" + user + ", socialNetwork=" + socialNetwork
                + ", count=" + count + ", minutes=" + minutes + ", totalFollowers=" + totalFollowers
                + ", activePercentage=" + activePercentage + ", daysRepresented=" + daysRepresented
                + ", weekend=" + weekend + "]";
    }
    @Override
    public int compareTo(ActiveReadersEntry o) {
        return Ints.compare(this.minutes, o.minutes);
    }
}
