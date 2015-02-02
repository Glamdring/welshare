package com.welshare.model;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

@Entity
public class Favourite {

    @EmbeddedId
    private FavouritePK primaryKey = new FavouritePK();

    public FavouritePK getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(FavouritePK primaryKey) {
        this.primaryKey = primaryKey;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((primaryKey == null) ? 0 : primaryKey.hashCode());
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
        Favourite other = (Favourite) obj;
        if (primaryKey == null) {
            if (other.primaryKey != null) {
                return false;
            }
        } else if (!primaryKey.equals(other.primaryKey)) {
            return false;
        }
        return true;
    }
}
