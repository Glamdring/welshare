package com.welshare.dao.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.TypedQuery;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.hibernate.search.jpa.FullTextQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.google.inject.internal.Maps;
import com.welshare.dao.UserDao;
import com.welshare.dao.enums.SearchType;
import com.welshare.model.Login;
import com.welshare.model.SocialNetworkScore;
import com.welshare.model.User;
import com.welshare.model.enums.Country;
import com.welshare.util.Constants;

@Repository
public class UserDaoJpa extends BaseDao implements UserDao {

    private static final Logger log = LoggerFactory.getLogger(UserDaoJpa.class);

    private static final String EMAIL_FIELD = "email";
    private static final String PASSWORD_FIELD = "password";
    private static final String USERNAME_FIELD = "username";

    @Value("${rankings.page.size}")
    private int rankingsPageSize;

    @Value("${common.page.size}")
    private int commonPageSize;

    @Override
    public User login(String username, String password) {
        List<User> result = findByQuery(new QueryDetails()
            .setQueryName("User.login")
            .setParamNames(new String[] { USERNAME_FIELD, PASSWORD_FIELD })
            .setParamValues(new Object[] { username, password }));

        return getResult(result);
    }

    @Override
    public User getUserWithCode(String code) {
        List<User> result = findByQuery(new QueryDetails()
            .setQuery("select u from User u where u.activationCode=:code")
            .setParamNames(new String[] { "code" })
            .setParamValues(new Object[] { code }));

        return getResult(result);
    }

    @Override
    public int cleanNonActiveUsers(long treshold) {
        int result = executeQuery("delete from User u "
                + "WHERE u.registeredTimestamp < :treshold AND active=false",
                new String[] { "treshold" }, new Object[] { treshold });

        return result;
    }

    @Override
    public User getByEmail(String email) {
        List<User> result = findByQuery(new QueryDetails()
            .setQueryName("User.getByEmail")
            .setParamNames(new String[] { EMAIL_FIELD })
            .setParamValues(new Object[] { email }));

        User user = getResult(result);
        return user;
    }

    @Override
    public User getByUsername(String username) {
        List<User> result = findByQuery(new QueryDetails()
            .setQueryName("User.getByUsername")
            .setParamNames(new String[] { USERNAME_FIELD })
            .setParamValues(new Object[] { username }));

        User user = getResult(result);
        return user;
    }

    @Override
    public Login getLoginFromAuthToken(String token, String series) {
        List<Login> result = findByQuery(new QueryDetails()
            .setQueryName("Login.getByToken")
            .setParamNames(new String[] { "token", "series" })
            .setParamValues(new Object[] {token, series }));

        return getResult(result);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<User> findByKeywords(String keywords, SearchType searchType) {
        MultiFieldQueryParser parser = new MultiFieldQueryParser(
                Version.LUCENE_31,
                new String[] { "names", USERNAME_FIELD, EMAIL_FIELD },
                new StandardAnalyzer(Version.LUCENE_31));

        keywords = escapeKeywords(keywords);

        if (searchType == SearchType.START) {
            keywords += "*";
        }
        if (searchType == SearchType.FUZZY) {
            keywords += "~";
        }
        if (searchType == SearchType.FULL) {
            String[] parts = keywords.split(" ");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                sb.append(part + "* ");
            }
            keywords = sb.toString();
        }

        Query query;

        try {
            query = parser.parse(keywords);
        } catch (ParseException e) {
            log.warn("Problem parsing query", e);
            return Collections.emptyList();
        }

        FullTextQuery ftq = getFullTextEntityManager().createFullTextQuery(
                query, User.class);
        ftq.setMaxResults(commonPageSize);

        return ftq.getResultList();
    }

    @Override
    public List<User> getTopUsers(int page) {
        TypedQuery<User> query = getEntityManager().createNamedQuery("User.getTopUsers", User.class);
        query.setFirstResult(page * rankingsPageSize);
        query.setMaxResults(rankingsPageSize);
        setCacheable(query);

        return query.getResultList();
    }

    @Override
    public List<User> getTopUsers(Country country, int page) {
         TypedQuery<User> query = getEntityManager().createNamedQuery("User.getTopUsersByCountry", User.class);
            query.setFirstResult(page * rankingsPageSize);
            query.setMaxResults(rankingsPageSize);
            query.setParameter("country", country);
            setCacheable(query);
            return query.getResultList();
    }

    @Override
    public List<User> getTopUsers(String city, int page) {
         TypedQuery<User> query = getEntityManager().createNamedQuery("User.getTopUsersByCity", User.class);
            query.setFirstResult(page * rankingsPageSize);
            query.setMaxResults(rankingsPageSize);
            query.setParameter("city", city);
            setCacheable(query);
            return query.getResultList();
    }

    @Override
    public int calculateScore(User user) {
        int score = 0;
        javax.persistence.Query query = getEntityManager().createQuery("SELECT COUNT(la.primaryKey.message) FROM LikeAction la JOIN la.primaryKey.message WHERE la.primaryKey.message.author=:user");
        query.setParameter("user", user);
        int likesCount = ((Long) query.getSingleResult()).intValue();
        score += likesCount * Constants.LIKE_SCORE;

        javax.persistence.Query repliesQuery = getEntityManager().createQuery("SELECT COUNT(message) FROM Message message WHERE message.originalMessage.author=:user AND message.author != :user");
        repliesQuery.setParameter("user", user);
        int repliesCount = ((Long) query.getSingleResult()).intValue();

        score += repliesCount * Constants.REPLY_SCORE;

        return score;
    }

    @Override
    public void deleteLogins(String userId) {
        executeQuery("DELETE FROM Login WHERE user.id=:userId",
                new String[] { "userId" }, new Object[] { userId });
    }

    @Override
    public List<User> getUsers(List<String> userIds) {
        if (userIds.isEmpty()) {
            return new ArrayList<User>(); //not an empty list, because it may be later modified
        }

        //TODO IN queries depend on max_allowed_packet mysql setting. At some point it should be increased?
        QueryDetails details = new QueryDetails()
            .setQuery("SELECT u FROM User u WHERE u.id IN(:userIds))")
            .setParamNames(new String[] {"userIds"})
            .setParamValues(new Object[] {userIds})
            .setCacheable(true);

        return findByQuery(details);
    }

    @Override
    public Map<String, SocialNetworkScore> getReputationScores(String userId) {
        QueryDetails details = new QueryDetails()
            .setQuery("SELECT snScore FROM SocialNetworkScore snScore where snScore.user.id = :userId")
            .setParamNames(new String[] {"userId"})
            .setParamValues(new Object[] {userId});

        List<SocialNetworkScore> scores = findByQuery(details);
        Map<String, SocialNetworkScore> map = Maps.newLinkedHashMap();
        for (SocialNetworkScore score : scores) {
            map.put(score.getSocialNetwork(), score);
        }
        return map;
    }

    @Override
    public void deleteReputationScores() {
        executeQuery("DELETE FROM SocialNetworkScore", null, null);
    }

    @Override
    public int getMessageCount(User user) {
        String query = "SELECT count(m) From Message m WHERE m.author=:author AND m.deleted=false";
        TypedQuery<Long> q = getEntityManager().createQuery(query, Long.class);
        q.setParameter("author", user);
        return q.getSingleResult().intValue();
    }

    @Override
    public void deleteUser(User user) {
        deleteLogins(user.getId());
        String query = "UPDATE Message m SET m.deleted=true WHERE m.author=:user AND m.score = 0 AND m.replies = 0";
        executeUserQuery(user, query);

        query = "UPDATE Message m SET m.author=null WHERE m.author=:user";
        executeUserQuery(user, query);

        query = "UPDATE Message m SET m.addressee=null WHERE m.addressee=:user";
        executeUserQuery(user, query);

        query = "DELETE FROM LikeAction la WHERE la.primaryKey.user=:user";
        executeUserQuery(user, query);

        query = "UPDATE DirectMessageRecipient dmr SET dmr.recipient=null WHERE dmr.recipient=:user";
        executeUserQuery(user, query);

        query = "UPDATE DirectMessage dm SET dm.sender=null WHERE dm.sender=:user";
        executeUserQuery(user, query);

        query = "DELETE FROM NotificationEvent ne WHERE ne.sender=:user OR ne.recipient=:user";
        executeUserQuery(user, query);

        // legacy. Now everything is on Neo4j, and we are not removing it from there yet
        query = "DELETE FROM Following f WHERE f.primaryKey.follower=:user OR f.primaryKey.followed=:user";
        executeUserQuery(user, query);

        query = "UPDATE ShortUrl su SET su.user=null WHERE su.user=:user";
        executeUserQuery(user, query);

        delete(user);
    }

    private void executeUserQuery(User user, String query) {
        javax.persistence.Query q = getEntityManager().createQuery(query);
        q.setParameter("user", user);
        q.executeUpdate();
    }
}
