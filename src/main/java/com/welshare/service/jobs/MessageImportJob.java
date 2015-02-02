package com.welshare.service.jobs;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.welshare.dao.Dao;
import com.welshare.dao.UserDao;
import com.welshare.model.User;
import com.welshare.service.social.SocialNetworkService;

@Component
public class MessageImportJob {

    private static Logger logger = LoggerFactory.getLogger(MessageImportJob.class);

    @Inject
    private UserDao userDao;

    @Inject
    private List<SocialNetworkService> socialNetworks;

    @Scheduled(fixedRate=2 * DateTimeConstants.MILLIS_PER_HOUR)
    public void importMessages() {
        userDao.performBatched(User.class, 100, new Dao.PageableOperation<User>() {
            @Override
            public void execute() {
                for (User user : getData()) {
                    for (SocialNetworkService sns : socialNetworks) {
                        try {
                            sns.importMessages(user); // the method itself verifies whether the user has configured import
                        } catch (Exception ex) {
                            logger.error("Problem importing messages for user " + user, ex);
                        }
                    }
                }
            }
        });
    }
}
