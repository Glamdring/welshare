package com.welshare.service;

import java.util.List;

import com.welshare.model.User;
import com.welshare.service.model.UserDetails;
import com.welshare.web.model.ProfileDetails;

public interface UserAccountService {

    /**
     * Stores the uploaded user picture
     *
     * @param user
     * @param originalFilename
     * @param bytes
     *
     */
    void saveProfilePicture(String userId, String originalFilename, byte[] bytes);

    /**
     * Stores 2 cropped profiles pictures - one used for profile page and the
     * other for streams
     * @param x
     * @param y
     * @param size size of the cropped area
     * @param user
     */
    void storeCroppedProfilePictures(int x, int y, int size, String userId);

    User saveSettings(ProfileDetails profileDetails, String userId);

    User setClosedHomepageConnectLinks(String userId);

    User setViewedStartingHints(String userId);

    void setLastLogout(String id);

    void setImportantMessageThresholds(int threshold, int thresholdRatio, String userId);

    List<UserDetails> getLimitedUsers(String userId);

    void saveSocialSettings(String likeFormat, String userId);

    void unsubscribeFromDailyEmail(String userId);

}
