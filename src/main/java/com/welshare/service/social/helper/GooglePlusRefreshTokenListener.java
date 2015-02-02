package com.welshare.service.social.helper;

import javax.inject.Inject;

import org.springframework.social.oauth2.AccessGrant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.googlecode.googleplus.core.OAuth2RefreshListener;
import com.welshare.dao.UserDao;
import com.welshare.model.User;
import com.welshare.util.SecurityUtils;

@Component
public class GooglePlusRefreshTokenListener implements OAuth2RefreshListener {

    @Inject
    private UserDao userDao;

    // invoked when the tokens are refreshed
    @Override
    @Transactional
    public void tokensRefreshed(String oldAccessToken, AccessGrant accessGrant) {
        User user = userDao.getByPropertyValue(User.class, "googlePlusSettings.token", SecurityUtils.encrypt(oldAccessToken));
        if (user != null) {
            user.getGooglePlusSettings().setToken(SecurityUtils.encrypt(accessGrant.getAccessToken()));
            if (accessGrant.getRefreshToken() != null) {
                user.getGooglePlusSettings().setRefreshToken(SecurityUtils.encrypt(accessGrant.getRefreshToken()));
            }
            userDao.persist(user);
        }
    }
}
