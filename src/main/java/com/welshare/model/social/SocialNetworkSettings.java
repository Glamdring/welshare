package com.welshare.model.social;

import java.io.Serializable;

public interface SocialNetworkSettings extends Serializable {
    /**
     * TODO use a better name
     * @return whether the user has connected his account to this network
     */
    boolean isFetchMessages();

    /**
     * @return whether the network is active by default (when sharing or resharing)
     */
    boolean isActive();

    /**
     * Whether a link to the profile on the external network should be displayed on welshare
     * @return
     */
    boolean isShowInProfile();

    /**
     * @return the time (in millis) of the last message imported from the external network
     */
    long getLastImportedMessageTime();

    /**
     * @return whether messages should be imported from the external network
     */
    boolean isImportMessages();

    /**
     * @return the reason for disconnecting the account. Null if the account hasn't been disconnected.
     */
    String getDisconnectReason();
}
