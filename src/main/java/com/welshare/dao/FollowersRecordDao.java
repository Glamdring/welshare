package com.welshare.dao;

import java.util.List;

import com.welshare.model.User;
import com.welshare.model.social.FollowersRecord;

public interface FollowersRecordDao extends Dao {
    List<FollowersRecord> getLastRecords(User user, String socialNetwork, int count);
}
