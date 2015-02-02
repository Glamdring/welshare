package com.welshare.service.infrastructure;

import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.joda.time.DateTime;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.restfb.FacebookClient;
import com.restfb.exception.FacebookException;
import com.welshare.dao.FollowingDao;
import com.welshare.dao.UserDao;
import com.welshare.dao.jpa.FollowingDaoJpa;
import com.welshare.model.Following;
import com.welshare.model.Message;
import com.welshare.model.User;
import com.welshare.service.SocialReputationService;
import com.welshare.service.annotations.SqlTransactional;
import com.welshare.service.jobs.BackupJob;
import com.welshare.service.jobs.BestTimeToShareJob;
import com.welshare.service.jobs.DailyEmailsJob;
import com.welshare.service.jobs.FollowersTrackingJob;
import com.welshare.service.jobs.SocialReputationCalculationJob;
import com.welshare.service.jobs.WeeklyTwitterSummaryJob;
import com.welshare.service.social.helper.FacebookHelper;

@Service
public class InternalService {

    private static final Logger logger = LoggerFactory.getLogger(InternalService.class);

    @Inject
    private BackupJob backupJob;

    @Inject
    private BestTimeToShareJob bestTimeToShareJob;

    @Inject
    private FollowingDao followingDao;

    @Inject
    private UserDao userDao;

    @Inject
    private GraphDatabaseService graphDb;

    @Inject
    private DailyEmailsJob dailyEmailJob;

    @Inject
    private FollowersTrackingJob followerTrackingJob;

    @Inject
    private SocialReputationCalculationJob socialReputationJob;

    @Inject
    private SocialReputationService socialReputationService;

    @Inject
    private FacebookHelper facebookHelper;

    @Inject
    private ApplicationContext ctx;

    @Inject
    private WeeklyTwitterSummaryJob weeklyTwitterSummaryJob;

    @PersistenceContext
    private EntityManager entityManager;

    @Async
    public void reindexSearch() {
        userDao.reindexSearch();
    }

    @Async
    public void backup() {
        backupJob.run();
    }

    @Async
    @SqlTransactional
    public void recalculateFollowers() {
        // TODO this needs to be way more optimized - query only for count
        List<User> users = userDao.list(User.class);
        for (User user : users) {
            user.setFollowers(followingDao.getFollowers(user).size());
            user.setFollowing(followingDao.getFollowing(user).size());
            user.setCloseFriends(followingDao.getCloseFriends(user).size());
            userDao.persist(user);
        }
    }

    @Async
    @SqlTransactional
    public void recalculateScores() {
        // TODO this needs to be way more optimized - query only for count
        List<User> users = userDao.list(User.class);
        for (User user : users) {
            userDao.lock(user);
            user.setScore(userDao.calculateScore(user));
            userDao.persist(user);
        }
    }

    @Async
    @SqlTransactional
    public void recalculateMessageCount() {
        List<User> users = userDao.list(User.class);
        for (User user : users) {
            userDao.lock(user);
            int count = userDao.getMessageCount(user);
            user.setMessages(count);
            userDao.persist(user);
        }
    }

    @Async
    public void shutdownNeo4j() {
        graphDb.shutdown();
    }

    @Async
    public void migrateFollowingGraph() {
        // the jpa dao only needed here, no need to live in the context
        FollowingDaoJpa dao = new FollowingDaoJpa();
        ctx.getAutowireCapableBeanFactory().autowireBean(dao);

        List<User> users = userDao.list(User.class);

        Transaction tx = graphDb.beginTx();
        try {
            for (User user : users) {
                List<User> followers = dao.getFollowers(user);
                for (User follower : followers) {
                    if (follower.equals(user)) {
                        continue;
                    }
                    Following following = dao.findFollowing(follower, user, false);
                    if (following.getDateTime() == null) {
                        following.setDateTime(new DateTime().minusMonths(2));
                    }
                    if (following != null && followingDao.findFollowing(follower, user, false) == null) {
                        followingDao.saveFollowing(following);
                    }
                }
            }
            tx.success();
        } catch (RuntimeException ex) {
            tx.failure();
            logger.error("GraphDB transaction problem", ex);
            throw ex;
        } finally {
            tx.finish();
        }
    }

    @Async
    public void sendDailyMessages() {
        dailyEmailJob.sendTopDailyMessagesEmail();
    }

    @Async
    public void recalculateReputationScores() {
        socialReputationJob.recalculateSocialReputation();
    }

    @Async
    public void cleanSocialReputation() {
        socialReputationService.cleanSocialReputation();
    }

    @Async
    @SqlTransactional
    public void fillNullFacebookUserIds() {
        List<User> users = userDao.list(User.class);
        int totalNullIds = 0;
        int failedOperations = 0;
        for (User user : users) {
            if (user.getFacebookSettings() != null && user.getFacebookSettings().getToken() != null && user.getFacebookSettings().getUserId() == null) {
                FacebookClient client = facebookHelper.getFacebookClient(user);
                try {
                    String id = client.fetchObject("me", com.restfb.types.User.class).getId();
                    if (id != null) {
                        user.getFacebookSettings().setUserId(id);
                        userDao.persist(user);
                        totalNullIds++;
                    } else {
                        logger.warn("Got null id from facebook for user " + user);
                    }
                } catch (FacebookException ex) {
                    failedOperations++;
                }
            }
        }

        logger.info("Total facebook user ids that were null: " + totalNullIds);
        logger.info("Failed operations: " + failedOperations);
    }

    @Async
    public void triggerBestTimeToShareCalculations() {
        bestTimeToShareJob.storeFollowersActivity();
    }

    @Async
    public void triggerFollowerTracking() {
        followerTrackingJob.storeFollowerIds();
    }

    @Async
    public void sendWeeklySummaryTweets(String email) {
        User user = userDao.getByEmail(email);
        weeklyTwitterSummaryJob.scheduleWeeklyTwitterSummary(user);
    }

    public String getStatus() {
        try {
            List<User> list = userDao.listPaged(User.class, 1, 1);
            if (list == null || list.isEmpty()) {
                return "failed: db returned no result";
            }
        } catch (Exception ex) {
            return "failed: " + ex.getMessage();
        }
        return "success";
    }

    @Async
    @SqlTransactional
    public void setProperEncoding() throws UnsupportedEncodingException {
        TypedQuery<List> q = entityManager.createQuery("SELECT list(id, text) FROM Message m where m.dateTime < :dateTime", List.class);
        q.setParameter("dateTime", new DateTime().withMonthOfYear(6).withDayOfMonth(15));
        List<List> list = q.getResultList();
        for (List msg : list) {
            String text = new String(((String) msg.get(1)).getBytes("cp1252"), "utf-8");
            Query up = entityManager.createQuery("UPDATE Message SET text=:text WHERE id=:id");
            up.setParameter("id", msg.get(0));
            up.setParameter("text", text);
            up.executeUpdate();
        }
    }
}
