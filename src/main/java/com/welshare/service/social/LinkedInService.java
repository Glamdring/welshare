package com.welshare.service.social;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.xml.bind.UnmarshalException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.code.linkedinapi.client.LinkedInApiClient;
import com.google.code.linkedinapi.client.LinkedInApiClientException;
import com.google.code.linkedinapi.client.constant.LinkedInApiUrls;
import com.google.code.linkedinapi.client.constant.LinkedInApiUrls.LinkedInApiUrlBuilder;
import com.google.code.linkedinapi.schema.Connections;
import com.google.code.linkedinapi.schema.Like;
import com.google.code.linkedinapi.schema.Likes;
import com.google.code.linkedinapi.schema.Network;
import com.google.code.linkedinapi.schema.Person;
import com.google.code.linkedinapi.schema.Update;
import com.google.code.linkedinapi.schema.UpdateComment;
import com.google.code.linkedinapi.schema.UpdateComments;
import com.google.code.linkedinapi.schema.Updates;
import com.google.code.linkedinapi.schema.VisibilityType;
import com.google.common.collect.Lists;
import com.restfb.exception.FacebookException;
import com.welshare.dao.UserDao;
import com.welshare.model.Message;
import com.welshare.model.NotificationEvent;
import com.welshare.model.User;
import com.welshare.model.social.LinkedInSettings;
import com.welshare.model.social.SocialNetworkSettings;
import com.welshare.model.social.VideoData;
import com.welshare.service.MessageService.EvictHomeStreamCacheStringParam;
import com.welshare.service.ShareService.ResharingDetails;
import com.welshare.service.annotations.SqlReadonlyTransactional;
import com.welshare.service.annotations.SqlTransactional;
import com.welshare.service.enums.PictureSize;
import com.welshare.service.model.UserDetails;
import com.welshare.service.social.helper.ExtendedLinkedInApiClient;
import com.welshare.service.social.helper.LinkedInHelper;
import com.welshare.service.social.qualifiers.LinkedIn;
import com.welshare.util.Constants;
import com.welshare.util.WebUtils;

@Service
@LinkedIn
@Order(3)
public class LinkedInService implements SocialNetworkService {

    private static final Logger logger = LoggerFactory.getLogger(LinkedInService.class);

    @Inject
    private LinkedInHelper helper;

    @Inject
    private UserDao dao;

    @Value("${messages.per.fetch}")
    private int messagesPerFetch;

    @Override
    @Async
    @SqlTransactional
    public void share(Message message, User user) {
        if (!isServiceEnabled(user)) {
            return;
        }
        try {
            LinkedInApiClient client = helper.getClient(user);

            List<String> urls = WebUtils.extractUrls(message.getText());
            String messageText = getMessageText(message, user);
            // if there are urls, but no picture
            if (!urls.isEmpty() || message.getPictureCount() > 0) {
                String url = null;
                String imageUrl = null;
                if (!urls.isEmpty()) {
                    url = urls.get(0);
                    VideoData vd = helper.getVideoData(url);
                    if (vd != null) {
                        imageUrl = vd.getPicture();
                    }
                } else {
                    url = message.getPictures().get(0).getPublicUrl();
                    imageUrl = WebUtils.addSuffix(message.getPictures().get(0).getPath(),
                            PictureSize.LARGE.getSuffix());
                }
                client.postShare(messageText, null, url, imageUrl, VisibilityType.ANYONE);
            } else {
                client.postNetworkUpdate(messageText);
            }
        } catch (LinkedInApiClientException ex) {
            handleException("Problem sharing message", ex, user);
        }
    }

    @Override
    @Async
    public void like(String originalMessageId, ResharingDetails details, User user) {
        if (!isServiceEnabled(user)) {
            return;
        }
        LinkedInApiClient client = helper.getClient(user);
        String linkedInId = getLinkedInId(originalMessageId);
        try {
            client.likePost(linkedInId);
        } catch (LinkedInApiClientException ex) {
            handleException("Problem liking a LinkedIn update", ex, user);
        }
    }

    @Override
    @Async
    public void unlike(String originalMessageId, User user) {
        if (!isServiceEnabled(user)) {
            return;
        }
        LinkedInApiClient client = helper.getClient(user);
        String linkedInId = getLinkedInId(originalMessageId);
        try {
            client.unlikePost(linkedInId);
        } catch (LinkedInApiClientException ex) {
            handleException("Problme unliking a LinkedIn", ex, user);
        }

    }

    @Override
    @Async
    @SqlTransactional
    public void reply(String originalMessageId, Message message) {
        if (!isServiceEnabled(message.getAuthor())) {
            return;
        }
        try {
            LinkedInApiClient client = helper.getClient(message.getAuthor());
            client.postComment(getLinkedInId(originalMessageId), message.getTextWithPictureUrls());
        } catch (LinkedInApiClientException ex) {
            handleException("Problem replying to a linkedIn message", ex, message.getAuthor());
        }
    }

    @Override
    @Async
    public Future<List<Message>> getMessages(Message lastMessage, User user) {
        if (!isServiceEnabled(user)) {
            return SocialUtils.emptyFutureList();
        }

        try {
            ExtendedLinkedInApiClient client = helper.getClient(user);
            LinkedInApiUrlBuilder builder = client.createLinkedInApiUrlBuilder(LinkedInApiUrls.NETWORK_UPDATES);
            builder.withParameter("start", String.valueOf(0))
                .withParameter("count", String.valueOf(messagesPerFetch))
                .withParameterEnumSet("type", LinkedInHelper.NETWORK_UPDATE_TYPES);
            if (lastMessage != null) {
                builder.withParameter("before", String.valueOf(lastMessage.getDateTime().getMillis() - 1));
            }
            Updates updates = client.getNetworkUpdates(builder).getUpdates();
            if (updates == null) {
                return SocialUtils.emptyFutureList();
            }
            List<Message> messages = updatesToMessages(updates.getUpdateList());
            return SocialUtils.wrapMessageList(messages);
        } catch (LinkedInApiClientException ex) {
            handleException("Problem getting linkedIn messages", ex, user);
            return SocialUtils.emptyFutureList();
        }
    }

    private List<Message> updatesToMessages(List<Update> updateList) {
        List<Message> result = new ArrayList<Message>(updateList.size());
        for (Update update : updateList) {
            try {
                result.add(updateToMessage(update));
            } catch (Exception ex) {
                handleException(
                        "Problem converting individual linkedIn update with id=" + update.getUpdateKey(), ex,
                        null);
            }
        }
        return result;
    }

    private Message updateToMessage(Update update) {
        Message message = new Message();
        message.getData().setExternalId(LinkedInHelper.PUBLIC_ID_PREFIX + update.getUpdateKey());
        message.setDateTime(new DateTime(update.getTimestamp()));
        message.setScore(update.getNumLikes().intValue());
        if (update.getUpdateComments() != null && update.getUpdateComments().getTotal() != null) {
            message.setReplies(update.getUpdateComments().getTotal().intValue());
        }
        message.getData().setExternalSiteName("linkedIn");

        User author = new User();
        if (update.getUpdateContent() != null && update.getUpdateContent().getPerson() != null &&
                update.getUpdateContent().getPerson().getCurrentShare() != null) {
            message.setText(StringUtils.trimToEmpty(update.getUpdateContent().getPerson().getCurrentShare()
                .getComment()));
            if (update.getUpdateContent().getPerson().getCurrentShare().getContent() != null) {
                String url = update.getUpdateContent().getPerson().getCurrentShare().getContent()
                        .getSubmittedUrl();
                if (StringUtils.isNotEmpty(url)) {
                    message.setText((message.getText() + " " + url).trim());
                }
            }
        }

        // http://www.linkedin.com/updates?discuss=&scope=1584038&stype=M&topic=5518032634191544320&type=U&a=4Yau
        message.getData().setExternalUrl(null); // TODO

        message.getData().setExternalSiteName("linkedIn");
        author.setExternalId(update.getUpdateContent().getPerson().getId());
        message.setAuthor(author);
        helper.fillUserData(author, update.getUpdateContent().getPerson());

        return message;
    }

    @Override
    @Async
    public Future<List<Message>> getMessages(User user) {
        return getMessages(null, user);
    }

    @Override
    public List<Message> getUserMessages(Message lastMessage, User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }

        try {
            LinkedInApiClient client = helper.getClient(user);
            Updates updates = null;
            if (lastMessage == null) {
                updates = client.getUserUpdates(LinkedInHelper.NETWORK_UPDATE_TYPES, 0, messagesPerFetch)
                        .getUpdates();
            } else {
                updates = client.getUserUpdates(LinkedInHelper.NETWORK_UPDATE_TYPES, 0, messagesPerFetch,
                        null, lastMessage.getDateTime().toDate()).getUpdates();
            }
            List<Message> messages = updatesToMessages(updates.getUpdateList());
            return messages;
        } catch (LinkedInApiClientException ex) {
            handleException("Problem getting linkedIn user messages", ex, user);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Message> getReplies(String originalMessageId, User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }

        LinkedInApiClient client = helper.getClient(user);
        try {
            UpdateComments updateComments = client.getNetworkUpdateComments(getLinkedInId(originalMessageId));
            List<UpdateComment> comments = updateComments.getUpdateCommentList();
            List<Message> result = new ArrayList<Message>(comments.size());
            for (UpdateComment comment : comments) {
                Message msg = new Message();
                msg.setText(comment.getComment());
                msg.setDateTime(new DateTime(comment.getTimestamp()));
                msg.getData().setExternalId(LinkedInHelper.PUBLIC_ID_PREFIX + comment.getId());
                User author = new User();
                helper.fillUserData(user, comment.getPerson());
                msg.setAuthor(author);
                result.add(msg);
            }
            return result;
        } catch (LinkedInApiClientException ex) {
            handleException("Problem getting linkedIn replies", ex, user);
            return Collections.emptyList();
        }
    }

    @Override
    @SqlTransactional
    @EvictHomeStreamCacheStringParam
    public User storeSettings(SocialNetworkSettings settings, String userId) {
        if (settings instanceof LinkedInSettings) {
            User user = dao.getById(User.class, userId, true);
            user.setLinkedInSettings((LinkedInSettings) settings);
            dao.persist(user);
            return user;
        }
        return null;
    }

    @Override
    public boolean shouldHandle(String messageId) {
        if (messageId != null && messageId.startsWith(LinkedInHelper.PUBLIC_ID_PREFIX)) {
            return true;
        }

        return false;
    }

    @Override
    @SqlReadonlyTransactional
    public User getInternalUserByExternalId(String externalUserId) {
        return dao.getByPropertyValue(User.class, "linkedInSettings.userId", externalUserId);
    }

    @Override
    public UserDetails getUserDetails(String externalUserId, User currentUser) {
        if (!isServiceEnabled(currentUser)) {
            return null;
        }

        LinkedInApiClient client = helper.getClient(currentUser);
        try {
            Person person = client.getProfileById(getLinkedInId(externalUserId), LinkedInHelper.PROFILE_FIELDS);
            User user = new User();
            helper.fillUserData(user, person);
            return new UserDetails(user);
        } catch (LinkedInApiClientException ex) {
            handleException("Problem getting linkedIn user", ex, currentUser);
            return null;
        }
    }

    @Override
    @Cacheable(value="singleExternalMessageCache", key="#externalMessageId + '-' + #currentUser?.id")
    public Message getMessageByExternalId(String externalMessageId, User currentUser) {
        if (!isServiceEnabled(currentUser)) {
            return null;
        }
        // that's ugly
        ExtendedLinkedInApiClient client = helper.getClient(currentUser);
        try {
            String updateKey = getLinkedInId(externalMessageId);
            Update update = client.getUpdate(updateKey);
            String personId = update.getUpdateContent().getPerson().getId();
            Network userNetwork = client.getUserUpdates(personId, LinkedInHelper.NETWORK_UPDATE_TYPES);
            if (userNetwork == null) {
                return null;
            }
            for (Update upd : userNetwork.getUpdates().getUpdateList()) {
                if (upd.getUpdateKey().equals(updateKey)) {
                    return updateToMessage(upd);
                }
            }
            return null;
        } catch (LinkedInApiClientException ex) {
            handleException("Problem getting individual linkedIn message by id", ex, currentUser);
            return null;
        }
    }

    @Override
    public void delete(String externalMessageId, User user) {
        // not supported
    }

    @Override
    public List<Message> getIncomingMessages(Message lastMessage, User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }
        LinkedInApiClient client = helper.getClient(user);
        try {
            Updates updates = client.getNetworkUpdates(LinkedInHelper.NETWORK_UPDATE_TYPES, 0,
                    messagesPerFetch, lastMessage.getDateTime().plusSeconds(1).toDate(), new Date()).getUpdates();
            if (updates == null) {
                return Collections.emptyList();
            }

            return updatesToMessages(updates.getUpdateList());
        } catch (LinkedInApiClientException ex) {
            handleException("Problem getting incoming LinkedIn messages", ex, user);
            return Collections.emptyList();
        }
    }

    @Override
    @SqlReadonlyTransactional
    public List<UserDetails> getFriends(User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }

        LinkedInApiClient client = helper.getClient(user);
        try {
            Connections connections = client.getConnectionsForCurrentUser();
            List<Person> connectionsList = connections.getPersonList();
            List<UserDetails> userDetails = new ArrayList<UserDetails>(connectionsList.size());
            for (Person friend : connectionsList) {
                User wsUser = dao.getByPropertyValue(User.class, "linkedInSettings.userId", friend.getId());
                if (wsUser != null) {
                    UserDetails details = new UserDetails(wsUser);
                    userDetails.add(details);
                }
            }

            logger.debug("Fetched " + userDetails.size()
                    + " friends from linkedIn that are registered on welshare");
            return userDetails;

        } catch (FacebookException e) {
            handleException("Problem getting linkedIn friends", e, user);
            return Collections.emptyList();
        }
    }

    @Override
    public void publishInitialMessage(User user) {
        if (!isServiceEnabled(user)) {
            return;
        }

        LinkedInApiClient client = helper.getClient(user);
        try {
            // TODO i18nize
            client.postNetworkUpdate("I am using Welshare! http://welshare.com");
        } catch (LinkedInApiClientException e) {
            handleException("Problem sharing initial message to twitter", e, user);
        }
    }

    @Override
    public List<NotificationEvent> getUnreadNotifications(User user) {
        return Collections.emptyList();
    }

    @Override
    public List<NotificationEvent> getNotifications(NotificationEvent maxEvent, int count, User user) {
        return Collections.emptyList();
    }

    @Override
    public void markNotificationsAsRead(User user) {
        // TODO Auto-generated method stub
    }

    @Override
    public List<User> getLikers(String externalMessageId, User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }
        LinkedInApiClient client = helper.getClient(user);
        try {
            Likes likes = client.getNetworkUpdateLikes(getLinkedInId(externalMessageId));
            if (likes != null) {
                List<User> result = new ArrayList<User>(likes.getTotal().intValue());
                for (Like like : likes.getLikeList()) {
                    User liker = new User();
                    helper.fillUserData(liker, like.getPerson());
                    result.add(liker);
                }
                return result;
            } else {
                return Collections.emptyList();
            }
        } catch (LinkedInApiClientException ex) {
            handleException("Problem getting likers of message with id=" + externalMessageId, ex, user);
            return Collections.emptyList();
        }
    }

    @Override
    @SqlTransactional
    @EvictHomeStreamCacheStringParam
    public User disconnect(String userId) {
        User user = dao.getById(User.class, userId, true);
        if (user == null || user.getLinkedInSettings() == null) {
            return null;
        }

        // clear the settings
        user.getLinkedInSettings().setFetchMessages(false);
        user.getLinkedInSettings().setToken(null);
        user.getLinkedInSettings().setUserId(null);

        return dao.persist(user);
    }

    @Override
    public String getUserId(String username, User user) {
        // no such thing as "username" on linkedin
        return null;
    }

    @Override
    public String getUserDisplayName(User author) {
        String name = author.getNames();
        if (StringUtils.isEmpty(name)) {
            name = author.getUsername();
            if (StringUtils.isEmpty(name)) {
                name = "unknown";
            }
        }
        return name;
    }

    @Override
    public Object getStats(User user) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getIdPrefix() {
        return LinkedInHelper.PUBLIC_ID_PREFIX;
    }

    @Override
    public String getExternalUsername(User user) {
        return null; // no username on linkedin
    }

    @Override
    public String getShortText(Message externalMessage, User user) {
        if (!isServiceEnabled(user)) {
            return externalMessage.getText();
        }
        return helper.getShortText(externalMessage);
    }

    @Override
    public List<Message> getMissedIncomingMessages(User user) {
        return Collections.emptyList();
    }

    @Override
    public boolean shouldShareLikes(User user) {
        return user.getLinkedInSettings().isShareLikes();
    }

    @Override
    public boolean isServiceEnabled(User user) {
        return user != null && user.getLinkedInSettings() != null
                && user.getLinkedInSettings().getToken() != null
                && user.getLinkedInSettings().isFetchMessages();
    }

    @Override
    public boolean isFriendWithCurrentUser(String externalUserId, User currentUser) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<Message> getMessagesOfUser(String externalId, User user) {
        if (!isServiceEnabled(user)) {
            return Collections.emptyList();
        }

        LinkedInApiClient client = helper.getClient(user);

        try {
            Network nw = client.getUserUpdates(externalId, LinkedInHelper.NETWORK_UPDATE_TYPES, 0,
                    messagesPerFetch);
            return updatesToMessages(nw.getUpdates().getUpdateList());
        } catch (LinkedInApiClientException ex) {
            handleException("Problem getting updates of user with id=" + externalId, ex, user);
            return Collections.emptyList();
        }
    }

    @Override
    public void favourite(String messageId, User user) {
        // no notion of "favourites" on linkedIn. Consider sendin a messages to
        // self

    }

    @Override
    public void follow(String externalUserId, User user) {
        if (!isServiceEnabled(user)) {
            return;
        }

        LinkedInApiClient client = helper.getClient(user);

        try {
            Person person = client.getProfileById(externalUserId);
            client.sendInviteToPerson(person, null, null);
        } catch (LinkedInApiClientException ex) {
            handleException("Problem sending linkedIn invitation", ex, user);
        }
    }

    @Override
    public List<Message> getYesterdayMessages(User user) {
        return Collections.emptyList();
    }

    @Override
    public void edit(Message editedMessage, User user) {
        // TODO Auto-generated method stub
    }

    private String getMessageText(Message message, User user) {
        String messageText = message.getTextWithPictureUrls();
        if (message.isLiking()) {
            messageText = WebUtils.formatLike(message.getOriginalMessage().getTextWithPictureUrls(),
                    message.getTextWithPictureUrls(), message.getOriginalMessage().getAuthor().getNames(),
                    user.getProfile().getExternalLikeFormat());
        }

        messageText = SocialUtils.trimSpecialSymbolElements(messageText);
        return messageText;
    }

    private void handleException(String message, Exception ex, User user) {
        Throwable rootCause = ExceptionUtils.getRootCause(ex);
        if (rootCause instanceof SocketTimeoutException || rootCause instanceof UnmarshalException ) {
            logger.warn(message + ": " + ex.getMessage() + " : " + ExceptionUtils.getRootCauseMessage(ex));
        } else if (StringUtils.trimToEmpty(ex.getMessage()).contains("The token used in the OAuth request is not valid")) {
            disconnect(user.getId());
            logger.warn("Invalid token, disconnecting." + message, ex);
        } else {
            logger.warn(message, ex);
        }
    }

    private String getLinkedInId(String externalId) {
        return externalId.replace(LinkedInHelper.PUBLIC_ID_PREFIX, "");
    }

    @Override
    public List<Message> getTopRecentMessages(User user) {
        return Collections.emptyList();
    }

    @Override
    public int calculateReputation(User user, DateTime since) {
        if (!isServiceEnabled(user)) {
            return -1;
        }

        ExtendedLinkedInApiClient client = helper.getClient(user);

        int reputation = 0;

        if (since != null) {
            try {
                Network network = client.getUserUpdates(since.toDate(), new DateTime().toDate());
                reputation += calculateReputation(network);
            } catch (LinkedInApiClientException ex) {
                handleException("Problem calculating linkedIn reputation for user " + user, ex, user);
            }
        } else {
            for (int page = 0; page < 30; page++) {
                try {
                    Network network = client.getUserUpdates(page, 200);
                    if (network.getUpdates().getUpdateList() == null) {
                        break;
                    }
                    reputation += calculateReputation(network);
                    if (network.getUpdates().getUpdateList().isEmpty()) {
                        break;
                    }
                } catch (LinkedInApiClientException ex) {
                    handleException("Problem calculating linkedIn reputation for user " + user, ex, user);
                }
            }
        }
        return reputation;
    }

    private int calculateReputation(Network network) {
        int reputation = 0;
        for (Update update : network.getUpdates().getUpdateList()) {
            if (update.getNumLikes() != null) {
                reputation += update.getNumLikes().intValue() * (Constants.LIKE_SCORE - 2);
            }
            if (update.getUpdateComments() != null && update.getUpdateComments().getTotal() != null) {
                reputation += update.getUpdateComments().getTotal().intValue() * Constants.REPLY_SCORE;
            }
        }
        return reputation;
    }

    @Override
    public void importMessages(User user) {
        if (!isServiceEnabled(user) || !user.getLinkedInSettings().isImportMessages()) {
            return;
        }

        ExtendedLinkedInApiClient client = helper.getClient(user);

        List<Message> messages = Lists.newArrayList();
        Date since = new Date(user.getLinkedInSettings().getLastImportedMessageTime());
        try {
            Network network = client.getUserUpdates(since, new DateTime().toDate());
            for (Update update : network.getUpdates().getUpdateList()) {
                messages.add(updateToMessage(update));
            }
        } catch (LinkedInApiClientException ex) {
            handleException("Problem calculating linkedIn reputation for user " + user, ex, user);
        }

        helper.importExternalMessages(user, messages);
    }

    @Override
    public int getCurrentlyActiveReaders(User user) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void reshare(String messageId, String comment, User user) {
        if (!isServiceEnabled(user)) {
            return;
        }
        LinkedInApiClient client = helper.getClient(user);
        String linkedInId = getLinkedInId(messageId);
        try {
            client.reShare(linkedInId, comment, VisibilityType.ANYONE);
        } catch (LinkedInApiClientException ex) {
            handleException("Problem liking a LinkedIn update", ex, user);
        }

    }

    @Override
    public void fillMessageAnalyticsData(List<ExternalMessageAnalyticsData> list, User user) {
        // TODO Auto-generated method stub
    }

    @Override
    public UserDetails getCurrentUserDetails(User user) {
        if (!isServiceEnabled(user)) {
            return null;
        }
        return getUserDetails(getIdPrefix() + user.getLinkedInSettings().getUserId(), user);
    }

    @Override
    public void disconnectDeletedUsers(List<User> users) {
        // TODO Auto-generated method stub
    }

    @Override
    public List<String> getFollowerIds(User user) {
        return Collections.emptyList();
    }

    @Override
    public List<UserDetails> getUserDetails(List<String> ids, User user) {
        return Collections.emptyList();
    }
}
