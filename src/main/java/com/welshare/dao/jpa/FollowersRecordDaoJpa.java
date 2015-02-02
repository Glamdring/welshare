package com.welshare.dao.jpa;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.welshare.dao.FollowersRecordDao;
import com.welshare.model.User;
import com.welshare.model.social.FollowersRecord;

@Repository
public class FollowersRecordDaoJpa extends BaseDao implements FollowersRecordDao {
    @Override
    public List<FollowersRecord> getLastRecords(User user, String socialNetwork, int count) {
        QueryDetails details = new QueryDetails()
            .setQuery("SELECT fr FROM FollowersRecord fr WHERE fr.user=:user AND fr.socialNetwork=:sn ORDER BY date DESC")
            .setCount(count).setParamNames(new String[] {"user", "sn"}).setParamValues(new Object[] {user, socialNetwork});

        return findByQuery(details);
    }
}
