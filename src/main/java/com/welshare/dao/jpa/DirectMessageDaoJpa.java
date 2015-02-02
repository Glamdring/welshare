package com.welshare.dao.jpa;

import java.util.List;

import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.springframework.stereotype.Repository;

import com.welshare.dao.DirectMessageDao;
import com.welshare.model.DirectMessage;
import com.welshare.model.User;

@Repository
public class DirectMessageDaoJpa extends BaseDao implements DirectMessageDao {

    @Override
    public List<DirectMessage> getDirectMessages(User recipient, int start,
            int pageSize) {

        TypedQuery<DirectMessage> query = getEntityManager().createNamedQuery(
                "DirectMessage.getDirectMessages", DirectMessage.class);

        query.setParameter("recipient", recipient);
        query.setFirstResult(start);
        query.setMaxResults(pageSize);
        setCacheable(query);

        return query.getResultList();
    }

    @Override
    public void markAllAsRead(User user) {
        Query q = getEntityManager().createQuery("UPDATE DirectMessageRecipient rc SET rc.read=true WHERE rc.recipient=:user");
        q.setParameter("user", user);
        q.executeUpdate();
    }

}
