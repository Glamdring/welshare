package com.welshare.service.jobs;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.google.common.base.Joiner;
import com.welshare.dao.Dao.PageableOperation;
import com.welshare.dao.UserDao;
import com.welshare.model.User;
import com.welshare.model.social.FollowersRecord;
import com.welshare.service.annotations.SqlTransactional;
import com.welshare.service.social.SocialNetworkService;

@Component
public class FollowersTrackingJob {

    @Inject
    private List<SocialNetworkService> socialNetworks;

    @Inject
    private UserDao userDao;

    @Scheduled(cron="0 0 11 ? * *")
    @SqlTransactional
    public void storeFollowerIds() {
        final DateTime now = new DateTime();
        final Joiner joiner = Joiner.on(',');
        userDao.performBatched(User.class, 200, new PageableOperation<User>() {
            @Override
            public void execute() {
                for (User user : getData()) {
                    for (SocialNetworkService sns : socialNetworks) {
                        List<String> ids = sns.getFollowerIds(user);
                        if (!ids.isEmpty()) {
                            FollowersRecord record = new FollowersRecord();
                            record.setDate(now);
                            record.setSocialNetwork(sns.getIdPrefix());
                            record.setFollowerIds(joiner.join(ids));
                            record.setUser(user);
                            userDao.persist(record);
                        }
                    }
                }
            }
        });
    }
}
