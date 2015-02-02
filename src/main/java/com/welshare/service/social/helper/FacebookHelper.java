package com.welshare.service.social.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.restfb.DefaultFacebookClient;
import com.restfb.DefaultJsonMapper;
import com.restfb.DefaultLegacyFacebookClient;
import com.restfb.Facebook;
import com.restfb.FacebookClient;
import com.restfb.JsonMapper;
import com.restfb.LegacyFacebookClient;
import com.restfb.WebRequestor;
import com.restfb.exception.FacebookException;
import com.restfb.exception.FacebookNetworkException;
import com.restfb.exception.FacebookOAuthException;
import com.restfb.exception.FacebookResponseStatusException;
import com.restfb.types.NamedFacebookType;
import com.restfb.types.Post;
import com.restfb.types.Post.Privacy;
import com.welshare.model.Message;
import com.welshare.model.Picture;
import com.welshare.model.User;
import com.welshare.model.social.FacebookSettings;
import com.welshare.service.annotations.SqlTransactional;
import com.welshare.util.SecurityUtils;

/**
 * There are two separate methods for converting posts to messages, because
 * there are two separate ways to obtain them from Facebook: the Graph API and
 * an FQL query
 *
 * @author bozho
 *
 */
@Component
public class FacebookHelper extends SocialHelper {

    public static final String PUBLIC_ID_PREFIX = "fb";

    private static final Logger logger = LoggerFactory.getLogger(FacebookHelper.class);

    private static final String FACEBOOK_ROOT = "http://facebook.com/";
    private static final String FACEBOOK_GRAPH_ROOT = "http://graph.facebook.com/";

    private static final String USER_QUERY_FIELDS = "SELECT uid, first_name, "
            + "last_name, name, profile_url, about_me, email, pic_small FROM user WHERE ";

    @Value("${facebook.app.name}")
    private String welshareAppName;

    private final JsonMapper jsonMapper = new DefaultJsonMapper();

    @Inject
    @Qualifier("default")
    private WebRequestor webRequestor;

    @Inject
    @Qualifier("background")
    private WebRequestor backgroundWebRequestor;

    public List<Message> postsToMessages(List<Post> posts, boolean fetchImages,
            String userFacebookId, FacebookClient client,
            boolean filterInternalMessages) throws FacebookException {

        List<Message> messages = new ArrayList<Message>(posts.size());

        for (Post post : posts) {
            // skip messages coming from welshare, if they don't have replies
            if (filterInternalMessages && post.getApplication() != null && post.getApplication().getName() != null
                    && post.getApplication().getName().contains(welshareAppName)
                    && (post.getComments() == null || post.getComments().getCount() == null
                    || post.getComments().getCount().longValue() == 0)) {
                continue;
            }
            try {
                Message message = postToMessage(post, fetchImages, userFacebookId, client);
                if (!StringUtils.isEmpty(message.getText())) {
                    messages.add(message);
                }
            } catch (FacebookNetworkException ex) {
                logger.warn("Problem in converting a facebook Post to a Message for postId: " + ex.getMessage() + " : " + ExceptionUtils.getRootCauseMessage(ex));
            } catch (Exception ex) {
                // report individual exception and continue with the next message
                // facebook change bits of their API too often which causes problems
                logger.warn("Problem in converting a facebook Post to a Message for postId=" + post.getId(), ex);
            }
        }

        return messages;
    }

    public List<Message> postsToMessages(List<Post> posts, boolean fetchImages,
            String userFacebookId, FacebookClient client)
            throws FacebookException {
        return postsToMessages(posts, fetchImages, userFacebookId, client, true);
    }

    public List<Message> streamPostsToMessages(List<StreamPost> posts, boolean fetchImages, String userFacebookId,
            FacebookClient client) {
        List<Message> messages = new ArrayList<Message>(posts.size());

        for (StreamPost post : posts) {
            // skip messages coming from welshare, if they don't have replies
            if (post.getAttribution() != null
                    && post.getAttribution().toLowerCase().contains(welshareAppName)
                    && (post.getComments() == null || post.getComments().getCount() == null
                    || post.getComments().getCount().longValue() == 0)) {
                continue;
            }
            try {
                Message message = streamPostToMessage(post, fetchImages, userFacebookId, client);
                // don't add this message if it only has description ("X commented on a link") or it doesn't have text at all
                if (message.getText().equals(post.getDescription()) || StringUtils.isEmpty(message.getText())) {
                    continue;
                }
                messages.add(message);
            } catch (FacebookNetworkException ex) {
                logger.warn("Problem in converting a facebook StreamPost to a Message for postId: " + ex.getMessage() + " : " + ExceptionUtils.getRootCauseMessage(ex));
            } catch (FacebookOAuthException ex) {
                logger.warn("OAuth Problem in converting a facebook StreamPost to a Message for postId: " + ex.getMessage() + " : " + ExceptionUtils.getRootCauseMessage(ex));
            } catch (FacebookResponseStatusException ex) {
                logger.warn("Response problem in converting a facebook StreamPost to a Message for postId: " + ex.getMessage() + " : " + ExceptionUtils.getRootCauseMessage(ex));
            } catch (Exception ex) {
                // report individual exception and continue with the next message
                // facebook change bits of their API too often which causes problems
                logger.warn("Problem in converting a facebook StreamPost to a Message for postId=" + post.getPostId(), ex);
            }
        }
        return messages;
    }

    public Map<String, User> getUsersData(FacebookClient client, Set<String> userIds)
            throws FacebookException {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String queryCriteria = Joiner.on(" or ").join(userIds);
        String query = USER_QUERY_FIELDS + queryCriteria + " LIMIT " + userIds.size();
        List<FqlUser> fbUsers = client.executeQuery(query, FqlUser.class);

        Map<String, User> users = new HashMap<String, User>(fbUsers.size());
        for (com.restfb.types.User fbUser : fbUsers) {
            User externalUser = new User();
            fillUserData(externalUser, fbUser);
            users.put(fbUser.getId(), externalUser);
        }

        return users;
    }

    public Message postToMessage(Post post, boolean fetchImages,
            final String userFacebookId, FacebookClient client) {
        if (post == null) {
            return null;
        }
        Message message = new Message();
        message.getData().setExternalSiteName("facebook");
        message.getData().setExternalId(PUBLIC_ID_PREFIX + post.getId());
        message.setDateTime(new DateTime(post.getCreatedTime()));
        int replies = 0;
        if (post.getComments() != null && post.getComments().getCount() != null) {
            replies = post.getComments().getCount().intValue();
        }
        message.setReplies(replies);

        // may the facebook crap begin
        if (post.getLikesCount() != null) {
            message.setScore(post.getLikesCount().intValue());
        } else if (post.getLikes() != null && post.getLikes().getCount() != null) {
            message.setScore(post.getLikes().getCount().intValue());
        }

        if (userFacebookId != null) {
            List<? extends NamedFacebookType> data = Collections.emptyList();
            // multiple null checks, because one never knows with fb and this api...
            if (post.getLikes() != null && post.getLikes().getData() != null) {
                data = post.getLikes().getData();
            } else if (client != null) {
                if (!post.getId().startsWith("0_")) {
                    data = client.fetchConnection(post.getId() + "/likes", com.restfb.types.User.class).getData();
                }
            }

            if (message.getScore() == 0 && data != null) {
                message.setScore(data.size());
            }

            message.getData().setLikedByCurrentUser(Iterables.any(data, new Predicate<NamedFacebookType>() {
                @Override
                public boolean apply(NamedFacebookType named) {
                    return named.getId().equals(userFacebookId);
                }
            }));
        }

        String txt = post.getMessage();

        String link = StringUtils.trimToEmpty(post.getLink());

        // facebook sux here..
        if (link.isEmpty() && !StringUtils.isEmpty(post.getName()) && post.getName().contains("http")) {
            link = post.getName();
        }

        if (post.getType() != null && post.getType().equals("link")) {
            link = post.getLink();
            String postMsg = StringUtils.trimToEmpty(post.getMessage());
            if (txt != null && !txt.contains(post.getName()) && !link.contains(post.getName())) {
                txt = (postMsg.length() > 0 ? postMsg + " | " : "")
                    + StringUtils.trimToEmpty(post.getName());
            }
        }

        if (post.getType() != null && post.getType().equals("video")) {
            link = post.getLink();
            if (StringUtils.isEmpty(link)) {
                link = post.getSource();
            }
            txt = StringUtils.trimToEmpty(post.getMessage());
            if (!StringUtils.isEmpty(link)) {
                txt = txt.replace(link, "");
            }
            txt = (txt.length() > 0 ? txt + " | " : "") + post.getName();
        }

        if (StringUtils.isEmpty(txt)) {
            txt = post.getCaption();
            if (StringUtils.isEmpty(txt)) {
                txt = post.getDescription();
            }
        }

        txt = StringUtils.trimToEmpty(txt);
        link = StringUtils.trimToEmpty(link);

        // not duplicating link. Facebook is not consistent with this behaviour
        if (txt.contains(link.replace("&", "&amp;")) || txt.contains(link)) {
            message.setText(txt);
        } else {
            String delim = !txt.isEmpty() && !link.isEmpty() ? " " : "";
            message.setText(txt + delim + link);
            message.setShortText((StringUtils.trimToEmpty(post.getMessage()) + " " + link).trim());
            String postMessage = StringUtils.trimToEmpty(post.getMessage());
            if (postMessage.contains(link.replace("&", "&amp;")) || postMessage.contains(link)) {
                message.setShortText(postMessage);
            }
        }

        // if there is a recipient, and the recipient is not the sender himself,
        // then add "-> recipient" at the end.
        List<NamedFacebookType> to = post.getTo();
        if (to != null && !to.isEmpty() && to.get(0) != null
                && !message.getText().contains(to.get(0).getName())
                && !ObjectUtils.equals(post.getFrom().getId(), to.get(0).getId())) {
            message.setText(message.getText() + " ► " + to.get(0).getName());
        }

        if (fetchImages && post.getPicture() != null) {
            Picture picture = new Picture();
            picture.setExternal(true);
            picture.setPath(post.getPicture());
            picture.setExternalUrl(link);
            message.setPictures(Collections.singletonList(picture));
            message.setPictureCount(1);
        }

        User externalAuthor = new User();
        externalAuthor.setNames(post.getFrom().getName());
        externalAuthor.setProfilePictureURI(FACEBOOK_GRAPH_ROOT
                + post.getFrom().getId() + "/picture");
        externalAuthor.setExternalId(PUBLIC_ID_PREFIX + post.getFrom().getId());

        externalAuthor.setExternalUrl(FACEBOOK_ROOT + "profile.php?id=" + post.getFrom().getId());

        message.setAuthor(externalAuthor);

        //currently username is always null
        if (post.getActions() != null && post.getActions().size() > 0) {
            message.getData().setExternalUrl(post.getActions().get(0).getLink());
        } else  if (externalAuthor.getUsername() != null) {
            message.getData().setExternalUrl(FACEBOOK_ROOT
                + externalAuthor.getUsername()
                + "/posts/" + post.getId().replace(externalAuthor.getExternalId() + "_", ""));
        } else {
            // crap...
            String postId = post.getId().replace(externalAuthor.getExternalId() + "_", "");
            String userId = externalAuthor.getExternalId();
            // a comment... isn't it? Ugly, I know..
            // TODO distinguish comments and get their original message id

            message.getData().setExternalUrl(FACEBOOK_ROOT
                + "permalink.php?id=" + userId
                + "&v=wall&story_fbid=" + postId);
        }

        return message;
    }


    public Message streamPostToMessage(StreamPost post, boolean fetchImages,
            final String userFacebookId, FacebookClient client) {
        if (post == null) {
            return null;
        }
        Message message = new Message();
        message.getData().setExternalSiteName("facebook");
        message.getData().setExternalId(PUBLIC_ID_PREFIX + post.getPostId());
        message.setDateTime(new DateTime(post.getCreatedTime() * 1000));
        int replies = 0;
        if (post.getComments() != null && post.getComments().getCount() != null) {
            replies = post.getComments().getCount().intValue();
        }
        message.setReplies(replies);

        if (post.getLikes() != null && post.getLikes().getCount() != null) {
            message.setScore(post.getLikes().getCount().intValue());
        }

        message.getData().setLikedByCurrentUser(post.getLikes().isUserLikes());

        String txt = StringUtils.trimToEmpty(post.getMessage());
        Attachment attachment = post.getAttachment();

        String link = "";
        if (attachment != null) {
            link = StringUtils.trimToEmpty(attachment.getHref());
        }
        if (link.startsWith("http://www.facebook.com/profile.php?id=")) {
            link = post.getPermalink();
        } else  if (fetchImages && attachment != null && CollectionUtils.isNotEmpty(attachment.getStreamMedia())) {
            // if the post is a photo, set the permalink as link (=> text), because otherwise the album is linked
            if (attachment.getStreamMedia().get(0).getType().equals("photo")) {
                link = post.getPermalink();
            }
            Picture picture = new Picture();
            picture.setExternal(true);
            picture.setPath(attachment.getStreamMedia().get(0).getSrc());
            picture.setExternalUrl(link);
            message.setPictures(Collections.singletonList(picture));
            message.setPictureCount(1);
        }

        if (txt.isEmpty() && link.isEmpty()) {
            txt = StringUtils.trimToEmpty(post.getDescription());
        }

        // not duplicating link. Facebook is not consistent with this behaviour
        if (txt.contains(StringUtils.replace(link, "&", "&amp;")) || txt.contains(link)) {
            message.setText(txt);
        } else {
            String delim = !txt.isEmpty() && !link.isEmpty() ? " " : "";
            message.setText(txt + delim + link);
            message.setShortText((StringUtils.trimToEmpty(post.getMessage()) + " " + link).trim());
            String postMessage = StringUtils.trimToEmpty(post.getMessage());
            if (postMessage.contains(link.replace("&", "&amp;")) || postMessage.contains(link)) {
                message.setShortText(postMessage);
            }
        }

        // if there is a recipient, and the recipient is not the sender himself,
        // then add "-> recipient" at the end.

//        if (client != null) {
//        List<NamedFacebookType> to = post.getTo();
//        if (to != null && !to.isEmpty() && to.get(0) != null
//                && !message.getText().contains(to.get(0).getName())
//                && !ObjectUtils.equals(post.getFrom().getId(), to.get(0).getId())) {
//            message.setText(message.getText() + " ► " + to.get(0).getName());
//        }

        User externalAuthor = new User();
        com.restfb.types.User fbUser = client.fetchObject(post.getActorId(), com.restfb.types.User.class);
        if (fbUser != null) {
            fillUserData(externalAuthor, fbUser);
        }

        message.setAuthor(externalAuthor);
        message.getData().setExternalUrl(post.getPermalink());

        return message;
    }

    public void fillUserData(User user, com.restfb.types.User fbUser) {
        user.getProfile().setBio(fbUser.getAbout());
        user.setProfilePictureURI(FACEBOOK_GRAPH_ROOT + fbUser.getId() + "/picture");

        // do not set username, it is not relevant
        user.setNames(StringUtils.trimToEmpty(fbUser.getName()));

        if (user.getNames().trim().isEmpty()) {
            user.setNames(user.getUsername());
        }
        user.getProfile().setGender(fbUser.getGender());
        if (fbUser.getBirthday() != null) {
            try {
                user.getProfile().setBirthDate(DateTimeFormat.forPattern("MM/dd/yyyy")
                    .parseDateTime(fbUser.getBirthday()));
            } catch (Exception ex) {
                logger.warn("Facebook has given us a wrongly formatted date:" + fbUser.getBirthday()
                        + " for user with id=fb" + fbUser.getId());
            }
        }
        user.setExternalId(PUBLIC_ID_PREFIX + fbUser.getId());
        user.setExternalUrl(FACEBOOK_ROOT + "profile.php?id=" + fbUser.getId());
    }

    @Cacheable(value="socialClientsCache", key="#user.facebookSettings.token")
    public FacebookClient getFacebookClient(User user) {
        return getFacebookClient(user.getFacebookSettings());
    }

    @Cacheable(value="socialClientsCache", key="#settings.token")
    public FacebookClient getFacebookClient(FacebookSettings settings) {
        if (settings == null) {
            return null;
        }
        String decryptedToken = SecurityUtils.decrypt(settings.getToken());
        FacebookClient client = new DefaultFacebookClient(
                decryptedToken, webRequestor, jsonMapper);
        return client;
    }

    public FacebookClient getBackgroundFacebookClient(User user) {
        // not caching these
        String decryptedToken = SecurityUtils.decrypt(user.getFacebookSettings().getToken());
        FacebookClient client = new DefaultFacebookClient(decryptedToken, backgroundWebRequestor, jsonMapper);
        return client;
    }

    @Cacheable(value="socialClientsCache", key="#token + '_legacy'")
    public LegacyFacebookClient getLegacyClient(String token) {
        String decyrptedToken = SecurityUtils.decrypt(token);
        LegacyFacebookClient client = new DefaultLegacyFacebookClient(
                decyrptedToken, webRequestor, jsonMapper);
        return client;
    }

    public static class StreamPost {
        @Facebook("post_id")
        private String postId;

        @Facebook("app_id")
        private Integer appId;

        @Facebook("source_id")
        private String sourceId;

        @Facebook("updated_time")
        private long updatedTime;

        @Facebook("created_time")
        private long createdTime;

        @Facebook("filter_key")
        private String filterKey;

        @Facebook("attribution")
        private String attribution;

        @Facebook("actor_id")
        private String actorId;

        @Facebook("target_id")
        private String targetId;

        @Facebook("message")
        private String message;

        @Facebook("attachment")
        private Attachment attachment;

        @Facebook("comments")
        private Comments comments;

        @Facebook("likes")
        private Likes likes;

        @Facebook("privacy")
        private Privacy privacy;

        @Facebook("permalink")
        private String permalink;

//        @Facebook("target_ids")
//        @Facebook("message_tags") TODO handle these

        @Facebook("description")
        private String description;

        public String getPostId() {
            return postId;
        }

        public void setPostId(String postId) {
            this.postId = postId;
        }

        public Integer getAppId() {
            return appId;
        }

        public void setAppId(Integer appId) {
            this.appId = appId;
        }

        public String getSourceId() {
            return sourceId;
        }

        public void setSourceId(String sourceId) {
            this.sourceId = sourceId;
        }

        public long getUpdatedTime() {
            return updatedTime;
        }

        public void setUpdatedTime(long updatedTime) {
            this.updatedTime = updatedTime;
        }

        public long getCreatedTime() {
            return createdTime;
        }

        public void setCreatedTime(long createdTime) {
            this.createdTime = createdTime;
        }

        public String getFilterKey() {
            return filterKey;
        }

        public void setFilterKey(String filterKey) {
            this.filterKey = filterKey;
        }

        public String getAttribution() {
            return attribution;
        }

        public void setAttribution(String attribution) {
            this.attribution = attribution;
        }

        public String getActorId() {
            return actorId;
        }

        public void setActorId(String actorId) {
            this.actorId = actorId;
        }

        public String getTargetId() {
            return targetId;
        }

        public void setTargetId(String targetId) {
            this.targetId = targetId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Attachment getAttachment() {
            return attachment;
        }

        public void setAttachment(Attachment attachment) {
            this.attachment = attachment;
        }

        public Comments getComments() {
            return comments;
        }

        public void setComments(Comments comments) {
            this.comments = comments;
        }

        public Likes getLikes() {
            return likes;
        }

        public void setLikes(Likes likes) {
            this.likes = likes;
        }

        public Privacy getPrivacy() {
            return privacy;
        }

        public void setPrivacy(Privacy privacy) {
            this.privacy = privacy;
        }

        public String getPermalink() {
            return permalink;
        }

        public void setPermalink(String permalink) {
            this.permalink = permalink;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class Likes {
        @Facebook("href")
        private String href;
        @Facebook("count")
        private Integer count;
        @Facebook("user_likes")
        private boolean userLikes;

        public String getHref() {
            return href;
        }
        public void setHref(String href) {
            this.href = href;
        }
        public Integer getCount() {
            return count;
        }
        public void setCount(Integer count) {
            this.count = count;
        }
        public boolean isUserLikes() {
            return userLikes;
        }
        public void setUserLikes(boolean userLikes) {
            this.userLikes = userLikes;
        }
    }

    public static class Comments {
        @Facebook("count")
        private Integer count;
        @Facebook("comment_list")
        private List<Comment> comments;
        public Integer getCount() {
            return count;
        }
        public void setCount(Integer count) {
            this.count = count;
        }
        public List<Comment> getComments() {
            return comments;
        }
        public void setComments(List<Comment> comments) {
            this.comments = comments;
        }
    }

    public static class Comment {
        @Facebook("fromid")
        private String fromId;
        @Facebook("time")
        private long time;
        @Facebook("text")
        private String text;
        @Facebook("likes")
        private Integer likes;
        @Facebook("user_likes")
        private boolean userLikes;
        public String getFromId() {
            return fromId;
        }
        public void setFromId(String fromId) {
            this.fromId = fromId;
        }
        public long getTime() {
            return time;
        }
        public void setTime(long time) {
            this.time = time;
        }
        public String getText() {
            return text;
        }
        public void setText(String text) {
            this.text = text;
        }
        public Integer getLikes() {
            return likes;
        }
        public void setLikes(Integer likes) {
            this.likes = likes;
        }
        public boolean isUserLikes() {
            return userLikes;
        }
        public void setUserLikes(boolean userLikes) {
            this.userLikes = userLikes;
        }
    }

    public static class Attachment {
        @Facebook("media")
        private List<StreamMedia> streamMedia;
        @Facebook("name")
        private String name;
        @Facebook("href")
        private String href;
        @Facebook("caption")
        private String caption;
        @Facebook("description")
        private String description;
        public List<StreamMedia> getStreamMedia() {
            return streamMedia;
        }
        public void setStreamMedia(List<StreamMedia> streamMedia) {
            this.streamMedia = streamMedia;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getHref() {
            return href;
        }
        public void setHref(String href) {
            this.href = href;
        }
        public String getCaption() {
            return caption;
        }
        public void setCaption(String caption) {
            this.caption = caption;
        }
        public String getDescription() {
            return description;
        }
        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class StreamMedia {
        @Facebook("href")
        private String href;
        @Facebook("type")
        private String type;
        @Facebook("src")
        private String src;
        public String getHref() {
            return href;
        }
        public void setHref(String href) {
            this.href = href;
        }
        public String getType() {
            return type;
        }
        public void setType(String type) {
            this.type = type;
        }
        public String getSrc() {
            return src;
        }
        public void setSrc(String src) {
            this.src = src;
        }
    }
    public static class FqlUser extends com.restfb.types.User {
        @Facebook("uid")
        private String id;

        @Facebook("profile_url")
        private String link;

        @Facebook("about_me")
        private String about;

        @Facebook("pic_square")
        private String picture;

        @Facebook("friend_count")
        private int friendCount;

        @Override
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @Override
        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }

        @Override
        public String getAbout() {
            return about;
        }

        public void setAbout(String about) {
            this.about = about;
        }

        public String getPicture() {
            return picture;
        }

        public void setPicture(String picture) {
            this.picture = picture;
        }

        public int getFriendCount() {
            return friendCount;
        }

        public void setFriendCount(int friendCount) {
            this.friendCount = friendCount;
        }
    }

    @Override
    protected String getPrefix() {
        return PUBLIC_ID_PREFIX;
    }

    @SqlTransactional
    @Override
    public void setLastImportedTime(String userId, long time) {
        User user = getUserDao().getById(User.class, userId); // needed, because the entity is detached
        user.getFacebookSettings().setLastImportedMessageTime(time);
        getUserDao().persist(user);
    }
}
