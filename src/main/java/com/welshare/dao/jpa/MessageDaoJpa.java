package com.welshare.dao.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.Version;
import org.hibernate.Hibernate;
import org.hibernate.search.jpa.FullTextQuery;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.google.common.base.Joiner;
import com.welshare.dao.MessageDao;
import com.welshare.dao.Paging;
import com.welshare.dao.enums.MessageSearchType;
import com.welshare.model.ActiveReadersEntry;
import com.welshare.model.Message;
import com.welshare.model.NotificationEvent;
import com.welshare.model.Picture;
import com.welshare.model.ScheduledMessage;
import com.welshare.model.Tag;
import com.welshare.model.User;

@Repository
public class MessageDaoJpa extends BaseDao implements MessageDao {

    private static final String LAST_MESSAGE_TIME_PARAM = "lastMessageTime";
    private static final String FOLLOWED_USERS_PARAM = "followedUsers";
    private static final Logger log = LoggerFactory.getLogger(MessageDaoJpa.class);
    private static final String ORIGINAL_MESSAGE_ID_PARAM = "originаlMessageId";

    private static final int MAX_AUTOCOMPLETE_RESULTS = 7;

    @Override
    public List<Message> getMessages(User user, List<User> followedUsers, Message lastMessage, int maxResults) {
        return getMessages(user, followedUsers, lastMessage, maxResults, -1);
    }

    private List<Message> getMessages(User user, List<User> followedUsers, Message lastMessage,
            int maxResults, int page) {
        followedUsers.add(user);

        if (followedUsers.isEmpty()) {
            return Collections.emptyList();
        }

        String queryName = "Message.getMessages";
        if (lastMessage != null) {
            queryName += "AfterLast";
        }
        TypedQuery<Message> query = getEntityManager().createNamedQuery(
                queryName, Message.class);
        query.setParameter(FOLLOWED_USERS_PARAM, followedUsers);
        if (lastMessage != null) {
            query.setParameter(LAST_MESSAGE_TIME_PARAM, lastMessage.getDateTime());
        }
        query.setMaxResults(maxResults);
        if (page > 0) {
            query.setFirstResult((page - 1) * maxResults);
        }

        setCacheable(query);

        return query.getResultList();
    }

    @Override
    public List<Message> getUserMessages(User author, int maxResults, int startIdx) {
        TypedQuery<Message> query = getEntityManager().createNamedQuery(
                "Message.getUserMessages", Message.class);
        query.setParameter("user", author);
        query.setFirstResult(startIdx);
        query.setMaxResults(maxResults);

        return query.getResultList();
    }

    @Override
    public List<Message> getAnalytics(User author, int maxResults, int startIdx) {
        TypedQuery<Message> query = getEntityManager().createNamedQuery(
                "Message.getAnalytics", Message.class);
        query.setParameter("user", author);
        query.setFirstResult(startIdx);
        query.setMaxResults(maxResults);

        return query.getResultList();
    }

    @Override
    public List<Tag> getTagsByStart(String tagPart) {

        String param = tagPart + "%";

        TypedQuery<Tag> query = getEntityManager()
            .createNamedQuery("Tag.findSuggestions", Tag.class);

        query.setParameter("tagPart", param);

        query.setMaxResults(MAX_AUTOCOMPLETE_RESULTS);

        return query.getResultList();
    }


    @Override
    public List<Message> getReplies(String originalMessageId) {
         TypedQuery<Message> query = getEntityManager().createNamedQuery("Message.getReplies", Message.class);
         query.setParameter(ORIGINAL_MESSAGE_ID_PARAM, originalMessageId);
         setCacheable(query);
         return query.getResultList();
    }

    @Override
    public void deleteReplies(String messageId) {
        TypedQuery<Message> query = getEntityManager().createNamedQuery("Message.getReplies", Message.class);
        query.setParameter(ORIGINAL_MESSAGE_ID_PARAM, messageId);

        List<Message> result = query.getResultList();
        for (Message msg : result) {
            delete(msg);
        }
    }

    @Override
    public void deleteNotifications(String messageId) {
        TypedQuery<NotificationEvent> query = getEntityManager().createNamedQuery("NotificationEvent.getByMessageId", NotificationEvent.class);
        query.setParameter("messageId", messageId);

        List<NotificationEvent> result = query.getResultList();
        for (NotificationEvent event : result) {
            delete(event);
        }
    }

    @Override
    public List<Message> getMessages(User loggedUser, List<User> followedUsers, int messagesPerFetch, int page) {
        return getMessages(loggedUser, followedUsers, null, messagesPerFetch, page);
    }

    @Override
    public List<Message> getIncomingMessages(User user, List<User> followedUsers, DateTime lastMessageDateTime) {
        followedUsers.add(user);

        TypedQuery<Message> query = getEntityManager().createNamedQuery(
                "Message.getMessagesSinceLast", Message.class);
        query.setParameter(FOLLOWED_USERS_PARAM, followedUsers);
        query.setParameter(LAST_MESSAGE_TIME_PARAM, lastMessageDateTime);

        query.setMaxResults(200); //TODO configurable

        setCacheable(query);

        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Message> search(String queryText, List<User> followedUsers, MessageSearchType type,
            User currentUser, Paging paging) {
        MultiFieldQueryParser parser = new MultiFieldQueryParser(
                Version.LUCENE_31,
                new String[] { "text" },
                new StandardAnalyzer(Version.LUCENE_31));

        queryText = escapeKeywords(queryText);
        String[] parts = queryText.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(part + "* ");
        }

        if (currentUser == null && type != MessageSearchType.ALL) {
            return Collections.emptyList();
        }

        if (type == MessageSearchType.STREAM) {
            followedUsers.add(currentUser); // own messages are part of the stream
            List<String> userIds = new ArrayList<String>(followedUsers.size());
            sb.append(" AND ( author:\"");
            for (User followed : followedUsers) {
                userIds.add(followed.getId());
            }
            sb.append(Joiner.on("\" OR author:\"").join(userIds));
            sb.append("\")");
        }

        if (type == MessageSearchType.OWN) {
            sb.append(" AND author:\"" + currentUser.getId() + "\"");
        }

        queryText = sb.toString();

        Query query;

        try {
            query = parser.parse(queryText);
        } catch (ParseException e) {
            log.warn("Problem parsing query", e);
            return Collections.emptyList();
        }

        FullTextQuery ftq = getFullTextEntityManager().createFullTextQuery(
                query, Message.class);

        ftq.setMaxResults(paging.getResultsPerPage());
        ftq.setFirstResult(paging.getPage() * paging.getResultsPerPage());
        ftq.setSort(new Sort(new SortField("dateTime", SortField.LONG, true)));

        return ftq.getResultList();
    }

    @Override
    public List<Message> getTaggedMessages(String tag, Paging paging) {
        TypedQuery<Message> query = getEntityManager().createNamedQuery("Message.getTaggedMessages", Message.class);
        query.setParameter("tag", tag);
        query.setFirstResult(paging.getPage() * paging.getResultsPerPage());
        query.setMaxResults(paging.getResultsPerPage());
        setCacheable(query);

        return query.getResultList();
    }

    @Override
    public void delete(Message message) {
        // attempt delete. If it was liked (score more than one), or it fails
        // (due to constraint violation) simply mark it as deleted.
        try {
            if (message.getScore() == 0) {
                super.delete(message);
                getEntityManager().flush();
            } else {
                message.setDeleted(true);
                persist(message);
            }
        } catch (PersistenceException ex) {
            message.setDeleted(true);
            persist(message);
        }
    }

    @Override
    public void initialize(Message message) {
        if (message.getPictureCount() > 0) {
            Hibernate.initialize(message.getPictures());
        }

        if (message.getOriginalMessage() != null && message.getOriginalMessage().getPictureCount() > 0) {
            Hibernate.initialize(message.getOriginalMessage().getPictures());
        }
    }

    @Override
    public List<String> getFavouritedMessageIds(List<Message> messages, User user) {
        // this method is a performance optimization, hence it looks uglier

        List<String> messageIds = new ArrayList<String>(messages.size());
        List<String> externalMessageIds = new ArrayList<String>(messages.size());
        for (Message msg : messages) {
            if (msg.getId() != null) {
                messageIds.add(msg.getId());
            } else {
                externalMessageIds.add(msg.getData().getExternalId());
            }
        }

        List<String> result = new ArrayList<String>();

        if (!messageIds.isEmpty()) {
            QueryDetails details = new QueryDetails()
                .setQuery("SELECT fav.primaryKey.message.id FROM Favourite fav "
                        + "WHERE fav.primaryKey.message.id IN (:messageIds)")
                .setParamNames(new String[] {"messageIds"})
                .setParamValues(new Object[] {messageIds});

            List<String> internalIds = findByQuery(details);
            result.addAll(internalIds);
        }

        if (!externalMessageIds.isEmpty()) {
              QueryDetails details = new QueryDetails()
                .setQuery("SELECT extFav.externalMessageId FROM ExternalFavourite extFav "
                        + "WHERE extFav.externalMessageId IN (:externalMessageIds)")
                .setParamNames(new String[] {"externalMessageIds"})
                .setParamValues(new Object[] {externalMessageIds});

              List<String> externalIds = findByQuery(details);
              result.addAll(externalIds);
        }

        return result;
    }

    @Override
    public Message getMessages(Picture picture) {
        QueryDetails details = new QueryDetails().setQueryName("Picture.getMessage")
                .setParamNames(new String[] {"picture"}).setParamValues(new Object[] {picture});
        List<Message> msgs = findByQuery(details);
        return getResult(msgs);
    }

    @Override
    public List<ScheduledMessage> getScheduledMessages(DateTime toTime) {
        QueryDetails details = new QueryDetails().setQueryName("ScheduledMessage.getMessages")
                .setParamNames(new String[] {"toTime"})
                .setParamValues(new Object[] {toTime});
        return findByQuery(details);
    }

    @Override
    public List<Message> getOldMessages(User author, int count) {
        TypedQuery<Message> query = getEntityManager().createNamedQuery("Message.getOldMessage", Message.class);
        query.setParameter("user", author);
        query.setParameter("threshold", new DateTime().minusMonths(4));
        query.setMaxResults(count);
        return query.getResultList();
    }

    @Override
    public List<ActiveReadersEntry> getActiveReaderEntries(User user, String idPrefix) {
        TypedQuery<ActiveReadersEntry> q = getEntityManager().createNamedQuery("ActiveReadersEntry.get", ActiveReadersEntry.class);
        q.setParameter("user", user);
        q.setParameter("socialNetwork", idPrefix);

        return q.getResultList();
    }
}