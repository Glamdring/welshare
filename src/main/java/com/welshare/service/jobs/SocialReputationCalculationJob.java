package com.welshare.service.jobs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.welshare.dao.Dao;
import com.welshare.dao.UserDao;
import com.welshare.model.User;
import com.welshare.service.SocialReputationService;

@Component
public class SocialReputationCalculationJob {

    @Inject
    private SocialReputationService service;

    @Inject
    private UserDao userDao;

    @Value("${social.reputation.thread.pool.size}")
    private int poolSize;

    @Scheduled(cron = "0 0 1 * * ?") //every night at 1
    public void recalculateSocialReputation() {
        // not using @Async on the method, because it will use up the thread
        // pool (only one thread pool for all @Asyncs. TODO: https://jira.springsource.org/browse/SPR-9318
        final ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        userDao.performBatched(User.class, 200, new Dao.PageableOperation<User>() {
            @Override
            public void execute() {
                for (final User user : getData()) {
                    executor.submit(new Runnable() {
                        @Override
                        public void run() {
                            service.sequentiallyCalculateSocialReputation(user);
                        }
                    });
                }
            }
        });
        executor.shutdown();
    }
}
