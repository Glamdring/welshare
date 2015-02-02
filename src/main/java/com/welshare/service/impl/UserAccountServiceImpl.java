package com.welshare.service.impl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;
import javax.inject.Inject;

import org.apache.commons.io.FilenameUtils;
import org.joda.time.DateTimeUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.mortennobel.imagescaling.AdvancedResizeOp;
import com.mortennobel.imagescaling.ResampleOp;
import com.welshare.dao.UserDao;
import com.welshare.model.User;
import com.welshare.model.social.ExternalUserThreshold;
import com.welshare.service.PictureService;
import com.welshare.service.UserAccountService;
import com.welshare.service.annotations.SqlReadonlyTransactional;
import com.welshare.service.annotations.SqlTransactional;
import com.welshare.service.model.UserDetails;
import com.welshare.service.social.SocialNetworkService;
import com.welshare.web.model.ProfileDetails;

@Service
public class UserAccountServiceImpl extends BaseServiceImpl implements UserAccountService {

    @Inject
    private PictureService pictureService;

    @Inject
    private UserDao userDao;

    @Inject
    private List<SocialNetworkService> socialNetworkServices;

    @Override
    @SqlTransactional
    public void saveProfilePicture(String userId, String originalFilename, byte[] bytes) {
        User user = getDao().getById(User.class, userId, true);
        String url = pictureService.uploadProfilePicture(bytes, originalFilename, userId);
        user.setProfilePictureURI(url);
        getDao().persist(user);
    }

    @Override
    @SqlTransactional
    public void storeCroppedProfilePictures(int x, int y, int size, String userId) {
        try {
            User user = getDao().getById(User.class, userId, true);
            String url = user.getProfilePictureURI();
            if (!url.startsWith("http")) {
                url = "http:" + url;
            }
            BufferedImage original = ImageIO.read(new URL(url));

            String format = FilenameUtils.getExtension(user.getProfilePictureURI());

            BufferedImage cropped = original.getSubimage(x, y, size, size);

            ResampleOp smallOp = new ResampleOp(48, 48);
            smallOp.setUnsharpenMask(AdvancedResizeOp.UnsharpenMask.Normal);
            BufferedImage small = smallOp.filter(cropped, null);

            ResampleOp largeOp = new ResampleOp(72, 72);
            largeOp.setUnsharpenMask(AdvancedResizeOp.UnsharpenMask.Normal);
            BufferedImage large = largeOp.filter(cropped, null);

            user.setSmallProfilePictureURI(pictureService.storedCroppedProfilePicture(small, "_small", user.getId(), format));
            user.setLargeProfilePictureURI(pictureService.storedCroppedProfilePicture(large, "_large", user.getId(), format));

            save(user);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    @SqlTransactional
    public User saveSettings(ProfileDetails profileDetails, String userId) {
        User user = getDao().getById(User.class, userId, true);
        BeanUtils.copyProperties(profileDetails, user.getProfile());
        user.setNames(profileDetails.getNames());
        user.setEmail(profileDetails.getEmail());
        user.setUsername(profileDetails.getUsername());

        user = save(user);

        return user;
    }

    @Override
    @SqlTransactional
    public User setClosedHomepageConnectLinks(String userId) {
        User user = getDao().getById(User.class, userId, true);
        user.setClosedHomepageConnectLinks(true);
        return save(user);
    }

    @Override
    @SqlTransactional
    public User setViewedStartingHints(String userId) {
        User user = getDao().getById(User.class, userId, true);
        user.setViewedStartingHints(true);
        return save(user);
    }

    @Override
    @SqlTransactional
    public void setLastLogout(String id) {
        User user = getDao().getById(User.class, id, true);
        user.setLastLogout(DateTimeUtils.currentTimeMillis());
        save(user);
    }

    @Override
    @SqlTransactional
    public void setImportantMessageThresholds(int threshold, int thresholdRatio, String userId) {
        User user = getDao().getById(User.class, userId, true);
        if (threshold > 0) {
            user.getProfile().setImportantMessageScoreThreshold(threshold);
        }
        user.getProfile().setImportantMessageScoreThresholdRatio(thresholdRatio);
        save(user);
    }

    @Override
    @SqlReadonlyTransactional
    public List<UserDetails> getLimitedUsers(String userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        User user = userDao.getById(User.class, userId);
        List<UserDetails> result = new ArrayList<UserDetails>();

        List<ExternalUserThreshold> thresholds = getDao().getListByPropertyValue(ExternalUserThreshold.class, "user.id", userId);
        for (ExternalUserThreshold threshold : thresholds) {
            if (threshold.getThreshold() > 0 || threshold.isHideReplies()) {
                for (SocialNetworkService sns : socialNetworkServices) {
                    if (sns.shouldHandle(threshold.getExternalUserId())) {
                        UserDetails details = sns.getUserDetails(threshold.getExternalUserId(), user);
                        if (details != null) {
                            result.add(details);
                        }
                    }
                }
            }
        }

        return result;
    }

    @Override
    @SqlTransactional
    public void saveSocialSettings(String likeFormat, String userId) {
        User user = userDao.getById(User.class, userId);
        user.getProfile().setExternalLikeFormat(likeFormat);
        userDao.persist(user);
    }

    @Override
    @SqlTransactional
    public void unsubscribeFromDailyEmail(String userId) {
        //TODO use a generated token rather than the userId
        User user = userDao.getById(User.class, userId, true);
        if (user == null) {
            return;
        }
        user.getProfile().setReceiveDailyTopMessagesMail(false);
        userDao.persist(user);
    }
}
