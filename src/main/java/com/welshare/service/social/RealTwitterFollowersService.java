package com.welshare.service.social;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;

import twitter4j.IDs;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterAPIConfiguration;
import twitter4j.TwitterException;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.welshare.dao.UserDao;
import com.welshare.model.User;
import com.welshare.model.social.TwitterSettings;
import com.welshare.service.annotations.SqlTransactional;
import com.welshare.service.social.helper.TwitterHelper;

@Service
public class RealTwitterFollowersService {

    private static final int REALISTIC_FRIENDS_UPPER_LIMIT = 1000;

    private static final Logger logger = LoggerFactory.getLogger(RealTwitterFollowersService.class);
    private static final String[] BUZZWORD_PHRASES = new String[] {"SEO", "optimization", "social media"};

    @Inject
    private TwitterHelper helper;

    @Inject
    private UserDao dao;

    @SqlTransactional
    public RealFollowersResult calculateRealFollowers(String userId) {
        User user = dao.getById(User.class, userId);
        RealFollowersResult result = calculateRealFollowers(user.getTwitterSettings());
        dao.lock(user);
        user.getTwitterSettings().setRealFollowers(result.getRealFollowersUsernames().size());
        dao.persist(user);
        return result;
    }

    public RealFollowersResult calculateRealFollowers(TwitterSettings settings) {

        // this method operates in constrained conditions - the limit for calls
        // to twitter is 350

        try {
            final Twitter tw = helper.getTwitter(settings);
            List<Long> followerIds = new ArrayList<Long>();
            List<Long> followedIds = new ArrayList<Long>();
            final MutableInt callsToTwitter = new MutableInt();
            long start = System.nanoTime();

            // proxy the Twitter client object to count the number of invocations
            Twitter t = (Twitter) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[] { Twitter.class }, new InvocationHandler() {
                        @Override
                        public Object invoke(Object arg0, Method arg1, Object[] arg2) throws Throwable {
                            callsToTwitter.increment();
                            return arg1.invoke(tw, arg2);
                        }
                    });

            // get all followers
            long cursor = -1;
            while (true) {
                IDs ids = t.getFollowersIDs(cursor);
                followerIds.addAll(Arrays.asList(ArrayUtils.toObject(ids.getIDs())));
                cursor = ids.getNextCursor();
                if (!ids.hasNext()) {
                    break;
                }
            }

            // get all friends
            cursor = -1;
            while (true) {
                IDs ids = t.getFriendsIDs(cursor);
                followedIds.addAll(Arrays.asList(ArrayUtils.toObject(ids.getIDs())));
                cursor = ids.getNextCursor();
                if (!ids.hasNext()) {
                    break;
                }
            }

            // get data about all current user's followers
            List<twitter4j.User> followers = new ArrayList<twitter4j.User>(followerIds.size());
            for (int i = 0; i < followerIds.size(); i += 100) {
                long[] ids = ArrayUtils.toPrimitive(followerIds.subList(i,
                        i + Math.min(100, followerIds.size() - i)).toArray(new Long[] {}));
                if (ids.length > 0) {
                    followers.addAll(t.lookupUsers(ids));
                }
            }

            // who has mentioned / replied to the current user
            List<Status> mentions = new ArrayList<Status>();
            for (int page = 1; page <= 5; page++) {
                Paging paging = new Paging(page, 100);
                mentions.addAll(t.getMentions(paging));
            }
            Multiset<Long> replyingUsers = HashMultiset.create(mentions.size());
            for (Status status : mentions) {
                replyingUsers.add(status.getUser().getId());
            }

            // get data about all current user's "friends"
            List<twitter4j.User> friends = new ArrayList<twitter4j.User>(followedIds.size());
            for (int i = 0; i < followedIds.size(); i += 100) {
                long[] ids = ArrayUtils.toPrimitive(followedIds.subList(i,
                        i + Math.min(100, followedIds.size() - i)).toArray(new Long[] {}));
                friends.addAll(t.lookupUsers(ids));
            }

            // see who are the current user's friends following
            Set<Long> followedByFollowed = new HashSet<Long>();
            for (twitter4j.User friend : friends) {
                if (calculateWeight(replyingUsers, Collections.<Long>emptySet(), friend) > 17) {
                    IDs ids = t.getFriendsIDs(friend.getId(), -1);
                    followedByFollowed.addAll(Arrays.asList(ArrayUtils.toObject(ids.getIDs())));
                }
            }

            logger.debug("Time taken before the final loop: " +  TimeUnit.SECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS));

            int close = 0;
            Paging retweetsPaging = new Paging(1, 100);
            List<String> excludedUsernames = new ArrayList<String>();
            for (Iterator<twitter4j.User> it = followers.iterator(); it.hasNext();) {
                twitter4j.User follower = it.next();
                int weight = calculateWeight(replyingUsers, followedByFollowed, follower);

                if (weight > 17 && weight < 21) {
                    close++;
                }

                if (weight < 21) {
                    it.remove();
                    excludedUsernames.add(follower.getScreenName());
                }
            }

            long nanoSeconds = System.nanoTime() - start;
            long seconds = TimeUnit.SECONDS.convert(nanoSeconds, TimeUnit.NANOSECONDS);
            logger.info("Getting real followers of twUserId: " + settings.getUserId() + " , close-to-being-human: "
                    + close + ", calls to twitter: " + callsToTwitter.intValue() + ". Time to get result: "
                    + seconds + "s");

            List<String> resultList = new ArrayList<String>(followers.size());
            for (twitter4j.User twUser : followers) {
                resultList.add(twUser.getScreenName());
            }

            RealFollowersResult result = new RealFollowersResult();
            result.setRealFollowersUsernames(resultList);
            result.setTotalFollowers(tw.showUser(settings.getUserId()).getFollowersCount());
            result.setExcludedFollowersUsernames(excludedUsernames);
            result.setRealFollowers(resultList.size());
            return result;
        } catch (TwitterException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private int calculateWeight(Multiset<Long> replyingUsers, Set<Long> followedByFollowed,
            twitter4j.User follower) {
        String description = StringUtils.trimToEmpty(follower.getDescription());
        boolean hasFavourites = follower.getFavouritesCount() > 0;
        boolean isListed = follower.getListedCount() > 0;
        boolean hasRealisticFriendsCount = follower.getFriendsCount() < REALISTIC_FRIENDS_UPPER_LIMIT;
        boolean isFollowedByFriend = followedByFollowed.contains(follower.getId());
        boolean noSiteInBio = !description.contains("http://") && !description.contains("https://") && !description.contains(".com");
        int mentionsCount = replyingUsers.count(follower.getId());
        boolean isProtected = follower.isProtected();
        boolean isGeoEnabled = follower.isGeoEnabled();
        boolean isTranslator = follower.isTranslator();
        boolean hasMessages = follower.getStatusesCount() > 0;
        //TODO source now contains <a href=".." rel=nofollow>appname</a> - strip the tag and retain only the name
        boolean hasOwnApp = follower.getStatus() != null && follower.getStatus().getSource() != null && !follower.getStatus().getSource().equals("web")
                && StringUtils.getLevenshteinDistance(follower.getScreenName(), follower.getStatus().getSource()) < 4;
        boolean hasBuzzwordBio = containsBuzzword(description);

        int weight = 0;
        weight += isProtected ? 21 : 0;
        weight += isTranslator ? 20 : 0;
        weight += mentionsCount * 9;
        weight += isFollowedByFriend ? 7 : 0;
        weight += noSiteInBio ? 6 : -2;
        weight += hasRealisticFriendsCount ? 6 : -1;
        weight += hasFavourites ? 4 : 0;
        weight += isGeoEnabled ? 3 : 0;
        weight += isListed ? 2 : 0;
        weight += hasOwnApp ? -15 : 0;
        weight += hasMessages ? 0 : -5;
        weight += hasBuzzwordBio ? -6 : 0;
        return weight;
    }
    private boolean containsBuzzword(String description) {
        for (String buzzwordPhrase : BUZZWORD_PHRASES) {
            if (StringUtils.containsIgnoreCase(description, buzzwordPhrase)) {
                return true;
            }
        }
        return false;
    }
    public static class RealFollowersResult {
        public static final RealFollowersResult ERROR = new RealFollowersResult();
        static {
            ERROR.setError(true);
        }

        private List<String> realFollowersUsernames;
        private List<String> excludedFollowersUsernames;
        private int totalFollowers;
        private int realFollowers;
        private boolean error;

        public List<String> getRealFollowersUsernames() {
            return realFollowersUsernames;
        }
        public void setRealFollowersUsernames(List<String> realFollowers) {
            this.realFollowersUsernames = realFollowers;
        }
        public int getTotalFollowers() {
            return totalFollowers;
        }
        public void setTotalFollowers(int totalFollowers) {
            this.totalFollowers = totalFollowers;
        }
        public List<String> getExcludedFollowersUsernames() {
            return excludedFollowersUsernames;
        }
        public void setExcludedFollowersUsernames(List<String> excludedFollowersUsernames) {
            this.excludedFollowersUsernames = excludedFollowersUsernames;
        }
        public boolean isError() {
            return error;
        }
        public void setError(boolean error) {
            this.error = error;
        }
        public int getRealFollowers() {
            return realFollowers;
        }
        public void setRealFollowers(int realFollowers) {
            this.realFollowers = realFollowers;
        }
    }

    public void foo() {
        TwitterAPIConfiguration conf;
        // t.getMemberSuggestions(categorySlug);
        // t.getSuggestedUserCategories();
        // t.getUserSuggestions(categorySlug);
    }

    public static void main(String[] args) throws Exception {
        String token = "cSHP1IV6tFCAylg02iJ/fPjXDEIs8KwEsRKV+RtJzPpoEjmEOZfaVbNuZH0khnQxHQA3UFtF7QsLgBWNSkIeWw==";
        //String token = "OFiXow5WV1PIkVjVITkZHBThHn5APMeQc3eLKcouQTA7UnpvRaVxIvzR0wUuCQDwj72NNujHE0g2b04529CPQQ==";
        String tokenSecret = "BNTR3E1bKIxyUjejHsFLbytQci39CNp191GqKkgIuI";
        //String tokenSecret = "aW9peUqUZP554IIyo9hddoZLYYR55CU2cxJVA1T9eA";

        long userId = 51876257;
        TwitterSettings settings = new TwitterSettings();
        settings.setFetchMessages(true);
        settings.setToken(token);
        settings.setTokenSecret(tokenSecret);
        settings.setUserId(userId);

        ApplicationContext ctx = new ClassPathXmlApplicationContext("classpath*:applicationContext.xml");
        TwitterHelper helper = ctx.getBean(TwitterHelper.class);
        System.out.println(helper.getTwitter(settings).showUser(226123582).getScreenName());
        RealTwitterFollowersService service = ctx.getBean(RealTwitterFollowersService.class);
        List<String> users = service.calculateRealFollowers(settings).getRealFollowersUsernames();
        for (String twuser : users) {
            System.out.println(twuser);
        }
        System.out.println(users.size());
    }
}
