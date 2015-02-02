package com.welshare.service.social;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.welshare.dao.FollowersRecordDao;
import com.welshare.model.User;
import com.welshare.model.social.FollowersRecord;
import com.welshare.service.annotations.SqlReadonlyTransactional;
import com.welshare.service.model.UserDetails;

@Service
public class FollowersTrackingService {
    @Inject
    private List<SocialNetworkService> socialNetworks;
    @Inject
    private FollowersRecordDao dao;

    private final Splitter splitter = Splitter.on(',');

    @Cacheable("followersTrackingCache")
    @SqlReadonlyTransactional
    public Map<String, List<UserDetails>> getLostFollowers(String userId) {

        Map<String, List<UserDetails>> result = Maps.newHashMap();
        User user = dao.getById(User.class, userId);
        for (SocialNetworkService sn : socialNetworks) {
            List<FollowersRecord> lastEightRecords = dao.getLastRecords(user, sn.getIdPrefix(), 8);
            if (lastEightRecords.size() < 2) {
                continue;
            }
            List<String> lostFollowerIds = Lists.newArrayList();
            for (int i = 0; i < lastEightRecords.size() - 1; i++) {
                List<String> currentIds = Lists.newArrayList(splitter.split(lastEightRecords.get(i).getFollowerIds()));
                List<String> previousIds = Lists.newArrayList(splitter.split(lastEightRecords.get(i + 1).getFollowerIds()));
                previousIds.removeAll(currentIds);
                lostFollowerIds.addAll(previousIds);
            }

            result.put(sn.getIdPrefix(), sn.getUserDetails(lostFollowerIds, user));
        }

        return result;
    }

    @SqlReadonlyTransactional
    public Map<String, List<UserDetails>> getGainedFollowers(String userId) {

        Map<String, List<UserDetails>> result = Maps.newHashMap();
        User user = dao.getById(User.class, userId);
        for (SocialNetworkService sn : socialNetworks) {
            List<FollowersRecord> lastEightRecords = dao.getLastRecords(user, sn.getIdPrefix(), 8);
            if (lastEightRecords.size() < 2) {
                continue;
            }
            List<String> gainedFollowerIds = Lists.newArrayList();
            for (int i = lastEightRecords.size() - 1; i > 0; i--) {
                List<String> currentIds = Lists.newArrayList(splitter.split(lastEightRecords.get(i - 1).getFollowerIds()));
                List<String> previousIds = Lists.newArrayList(splitter.split(lastEightRecords.get(i).getFollowerIds()));
                currentIds.removeAll(previousIds);
                gainedFollowerIds.addAll(currentIds);
            }

            result.put(sn.getIdPrefix(), sn.getUserDetails(gainedFollowerIds, user));
        }

        return result;
    }
}
