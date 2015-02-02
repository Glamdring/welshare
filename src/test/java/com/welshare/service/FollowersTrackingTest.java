package com.welshare.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.welshare.dao.FollowersRecordDao;
import com.welshare.model.User;
import com.welshare.model.social.FollowersRecord;
import com.welshare.service.model.UserDetails;
import com.welshare.service.social.FollowersTrackingService;
import com.welshare.service.social.SocialNetworkService;

public class FollowersTrackingTest {

    @Test
    public void gainedFollowersTest() {
        FollowersTrackingService service = new FollowersTrackingService();
        FollowersRecordDao dao = Mockito.mock(FollowersRecordDao.class);
        List<FollowersRecord> list = new ArrayList<FollowersRecord>();
        FollowersRecord rec = new FollowersRecord();
        rec.setFollowerIds("1,2,3");
        FollowersRecord rec2 = new FollowersRecord();
        rec2.setFollowerIds("1,2,3,4");
        FollowersRecord rec3 = new FollowersRecord();
        rec3.setFollowerIds("1,2,3,4,5");
        // add in desc order, since they are expected that way
        list.add(rec3);
        list.add(rec2);
        list.add(rec);
        Mockito.when(dao.getLastRecords(Mockito.<User>any(), Mockito.anyString(), Mockito.anyInt())).thenReturn(list);
        ReflectionTestUtils.setField(service, "dao", dao);

        SocialNetworkService sn = Mockito.mock(SocialNetworkService.class);
        Mockito.when(sn.getIdPrefix()).thenReturn("tw");
        Mockito.when(sn.getUserDetails(Mockito.<List<String>>any(), Mockito.<User>any())).thenAnswer(new Answer<List<UserDetails>>() {

            @Override
            public List<UserDetails> answer(InvocationOnMock invocation) throws Throwable {
                List<String> ids = (List<String>) invocation.getArguments()[0];
                List<UserDetails> result = new ArrayList<UserDetails>();
                for (String id : ids) {
                    UserDetails details = new UserDetails();
                    details.setId(id);
                    result.add(details);
                }
                return result;
            }
        });
        ReflectionTestUtils.setField(service, "socialNetworks", Lists.newArrayList(sn));

        Map<String, List<UserDetails>> result = service.getGainedFollowers("any");
        Assert.assertEquals(2, result.get("tw").size());
    }
}
