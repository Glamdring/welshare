package com.welshare.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.welshare.dao.UserDao;
import com.welshare.model.SocialNetworkScore;
import com.welshare.model.User;
import com.welshare.service.SocialReputationService;
import com.welshare.service.annotations.SqlTransactional;
import com.welshare.service.social.SocialNetworkService;

@Service
public class SocialReputationServiceImpl implements SocialReputationService {

    private static final Logger logger = LoggerFactory.getLogger(SocialReputationServiceImpl.class);

    @Inject
    private List<SocialNetworkService> socialNetworkServices;

    @Inject
    private UserDao userDao;

    @PostConstruct
    public void init() {
        Collections.sort(socialNetworkServices, AnnotationAwareOrderComparator.INSTANCE);
    }

    @Override
    @SqlTransactional
    @Async
    public void calculateSocialReputation(User user) {
        try {
            int totalExternalScore = 0;
            // the user object passed is detached, so here are getting a fresh user object,
            // because the above operations are slow, and changes might have occurred
            user = userDao.getById(User.class, user.getId());
            if (user == null) {
                logger.debug("No user found in DB for user dto=" + user);
                return;
            }

            Map<String, SocialNetworkScore> scores = userDao.getReputationScores(user.getId());
            for (SocialNetworkService sns : socialNetworkServices) {
                if (sns.isServiceEnabled(user)) {
                    DateTime now = new DateTime();
                    SocialNetworkScore score = scores.get(sns.getIdPrefix());
                    if (score == null) {
                        score = new SocialNetworkScore();
                        int reputation = sns.calculateReputation(user, null);
                        score.setScore(reputation);
                    } else {
                        int reputation = sns.calculateReputation(user, score.getLastCalculated());
                        score.setScore(score.getScore() + reputation);
                    }

                    // using a separate entity rather than an @ElementCollection in User
                    // because this collection is used on just one page. It makes more sense
                    // for it to be retrieved by a separate method call
                    score.setLastCalculated(now);
                    score.setSocialNetwork(sns.getIdPrefix());
                    score.setUser(user);
                    userDao.persist(score);

                    totalExternalScore += score.getScore();
                }
            }
            user.setExternalScore(totalExternalScore);
            userDao.persist(user);
        } catch (Exception ex) {
            logger.error("Problem calculating external scores for user: " + user, ex);
        }
    }

    @Override
    @SqlTransactional
    public void cleanSocialReputation() {
        userDao.deleteReputationScores();
    }

    @Override
    public void sequentiallyCalculateSocialReputation(User user) {
        this.calculateSocialReputation(user);
    }
}
