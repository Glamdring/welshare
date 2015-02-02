package com.welshare.service.social.helper;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTimeUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

import com.googlecode.googleplus.GooglePlusFactory;
import com.googlecode.googleplus.Plus;
import com.googlecode.googleplus.core.OAuth2RefreshListener;
import com.googlecode.googleplus.model.activity.Activity;
import com.googlecode.googleplus.model.activity.ActivityActor;
import com.googlecode.googleplus.model.person.Person;
import com.welshare.model.Message;
import com.welshare.model.User;
import com.welshare.model.social.GooglePlusSettings;
import com.welshare.service.annotations.SqlTransactional;
import com.welshare.util.SecurityUtils;

@Component
public class GooglePlusHelper extends SocialHelper {

    public static final String PUBLIC_ID_PREFIX = "gp";
    private static final DateTimeFormatter FORMAT = DateTimeFormat.forPattern("dd/mm/yyyy");

    @Inject
    private OAuth2RefreshListener refreshListener;

    @Inject
    private GooglePlusFactory oAuthProvider;

    public void fillUserData(User user, Person person) {
        user.setNames(person.getDisplayName());
        if (person.getBirthday() != null) {
            user.getProfile().setBirthDate(FORMAT.parseDateTime(person.getBirthday()));
        }
        user.getProfile().setBio(person.getAboutMe());
        user.setProfilePictureURI(person.getImage().getUrl());
        user.setExternalId(person.getId());
        user.getProfile().setCity(person.getCurrentLocation());
        user.setExternalUrl(person.getUrl());
    }

    public Plus getClient(User user) {
        return getClient(user.getGooglePlusSettings());
    }

    public Plus getClient(GooglePlusSettings settings) {
        return oAuthProvider.getApi(SecurityUtils.decrypt(settings.getToken()), SecurityUtils.decrypt(settings.getRefreshToken()), refreshListener);
    }

    public List<Message> activitiesToMessages(List<Activity> activities, boolean fetchImages, boolean filterInternalMessages) {
        List<Message> messages = new ArrayList<Message>(activities.size());

        for (Activity activity : activities) {
            // skip messages that are coming from welshare
            if (filterInternalMessages
                    && false) { //TODO
                continue;
            }
            Message message = activityToMessage(activity, fetchImages);

            messages.add(message);
        }
        return messages;
    }

    public Message activityToMessage(Activity activity, boolean fetchImages) {
        Message message = new Message();
        message.setDateTime(activity.getPublished());
        message.getData().setExternalId(PUBLIC_ID_PREFIX + activity.getId());
        message.setText(activity.getTitle()); //TODO get the full content? strip tags?
        message.setScore((int) activity.getObject().getPlusoners().getTotalItems());
        message.getData().setExternalUrl(activity.getUrl());
        message.setReplies((int) activity.getObject().getReplies().getTotalItems());
        message.getData().setExternalSiteName("googlePlus");

        User author = actorToUser(activity.getActor());
        message.setAuthor(author);

        return message;
    }

    public User actorToUser(ActivityActor actor) {
        User author = new User();
        author.setExternalId(PUBLIC_ID_PREFIX + actor.getId());
        author.setNames(actor.getDisplayName());
        author.setProfilePictureURI(actor.getImage().getUrl());
        return author;
    }

    public static void main(String[] args) {
        GooglePlusFactory factory = new GooglePlusFactory("46986933912-nal9j6en72htv0b3p7ku8sfv5khj5gan.apps.googleusercontent.com", "qPzAACFpWi_BW3-YXhqUSfik");
        Plus plus = factory.getApi("ya29.AHES6ZRnXeGoKBj1OmM2zjBLENLw7cnlwgtr17-NEUJUKA", "1/f4F1jFadakQoToC9FCXXvasyOPbaws42EUdhzvIq28k", null);
        plus.getCommentOperations().list("z12kyfqx0vypvpzc523yuj2azpzkxnyfh");
    }

    @Override
    protected String getPrefix() {
        return PUBLIC_ID_PREFIX;
    }

    @SqlTransactional
    @Override
    public void setLastImportedTime(String userId, long time) {
        User user = getUserDao().getById(User.class, userId); // needed, because the entity is detached
        user.getGooglePlusSettings().setLastImportedMessageTime(time);
        getUserDao().persist(user);
    }
}
