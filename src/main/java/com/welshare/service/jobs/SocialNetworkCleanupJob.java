package com.welshare.service.jobs;

import java.util.List;

import javax.inject.Inject;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.welshare.dao.Dao.PageableOperation;
import com.welshare.dao.UserDao;
import com.welshare.model.User;
import com.welshare.service.social.SocialNetworkService;

/**
 * Disconnects users whose connected accounts do not exist anymore on the external network
 * @author bozho
 *
 */
@Component
public class SocialNetworkCleanupJob {

    @Inject
    private List<SocialNetworkService> services;
    @Inject
    private UserDao userDao;

    @Scheduled(cron="0 0 0 ? * *")
    public void cleanup() {
        userDao.performBatched(User.class, 200, new PageableOperation<User>() {
            @Override
            public void execute() {
                for (SocialNetworkService service : services) {
                    service.disconnectDeletedUsers(getData());
                }
            }
        });
    }
}
