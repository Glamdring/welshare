package com.welshare.service.social.helper;

import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.code.linkedinapi.client.LinkedInApiClient;
import com.google.code.linkedinapi.client.LinkedInApiClientFactory;
import com.google.code.linkedinapi.client.enumeration.NetworkUpdateType;
import com.google.code.linkedinapi.client.enumeration.ProfileField;
import com.google.code.linkedinapi.client.oauth.LinkedInAccessToken;
import com.google.code.linkedinapi.schema.Person;
import com.google.common.collect.Sets;
import com.welshare.model.User;
import com.welshare.model.social.LinkedInSettings;
import com.welshare.service.annotations.SqlTransactional;
import com.welshare.util.SecurityUtils;

@Component
public class LinkedInHelper extends SocialHelper {

    public static final String PUBLIC_ID_PREFIX = "li";

    public static final Set<ProfileField> PROFILE_FIELDS = Sets.newHashSet(ProfileField.FIRST_NAME, ProfileField.LAST_NAME,
            ProfileField.HEADLINE, ProfileField.PICTURE_URL, ProfileField.ID, ProfileField.NUM_CONNECTIONS, ProfileField.PUBLIC_PROFILE_URL);

    public static final Set<NetworkUpdateType> NETWORK_UPDATE_TYPES = Sets.newHashSet(
            NetworkUpdateType.SHARED_ITEM, NetworkUpdateType.JOB_UPDATE);

    @Value("${linkedin.connect.timeout}")
    private int connectTimeout;
    @Value("${linkedin.read.timeout}")
    private int readTimeout;

    @Inject
    private LinkedInApiClientFactory factory;

    public ExtendedLinkedInApiClient getClient(User user) {
        return getClient(user.getLinkedInSettings());
    }

    public ExtendedLinkedInApiClient getClient(LinkedInSettings settings) {
        ExtendedLinkedInApiClient client = (ExtendedLinkedInApiClient) factory.createLinkedInApiClient(ExtendedLinkedInApiClient.class, new LinkedInAccessToken(SecurityUtils.decrypt(settings.getToken()), settings.getTokenSecret()));
        client.setReadTimeout(readTimeout);
        client.setConnectTimeout(connectTimeout);
        return client;
    }

    public void fillUserData(User user, Person person) {
        user.getProfile().setBio(person.getHeadline());
        user.setProfilePictureURI(person.getPictureUrl());
        if (StringUtils.isEmpty(user.getProfilePictureURI())) {
            user.setProfilePictureURI("http://static01.linkedin.com/scds/common/u/img/icon/icon_no_photo_no_border_60x60.png");
        }
        user.setNames(person.getFirstName() + " " + person.getLastName());
        user.setExternalUrl(person.getPublicProfileUrl());
        if (person.getNumConnections() != null) {
            user.setFollowing(person.getNumConnections().intValue());
        }
        user.setExternalId(PUBLIC_ID_PREFIX + person.getId());
    }

    @Override
    protected String getPrefix() {
        return PUBLIC_ID_PREFIX;
    }

    @SqlTransactional
    @Override
    public void setLastImportedTime(String userId, long time) {
        User user = getUserDao().getById(User.class, userId); // needed, because the entity is detached
        user.getLinkedInSettings().setLastImportedMessageTime(time);
        getUserDao().persist(user);
    }
}
