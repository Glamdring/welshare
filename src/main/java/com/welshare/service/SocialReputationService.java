package com.welshare.service;

import com.welshare.model.User;

public interface SocialReputationService {
    /**
     * The method calculates and stores the social reputation score for external
     * networks for a given user. If there is no calculated score for a given
     * service (the method is invoked for the first time), then a full
     * calculation is performed, using all messages of the user.
     *
     * For each subsequent invocation, only the messages since the last
     * recalculation are inspected and taken into account. The new score is then
     * added to the already calculated value
     *
     * @param user
     */
    void calculateSocialReputation(User user);

    /**
     * Same as above, but guaranteed to be invoked in the same thread
     * @param user
     */
    void sequentiallyCalculateSocialReputation(User user);

    /**
     * Deletes all social reputation information from the DB. That way the next calculation will be a full recalculation
     * Note: do not use this method regularly. Only if a significant formula change occurs
     */
    void cleanSocialReputation();
}
