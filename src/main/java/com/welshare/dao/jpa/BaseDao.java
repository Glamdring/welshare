package com.welshare.dao.jpa;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.lucene.queryParser.QueryParser;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.springframework.stereotype.Repository;

import com.welshare.dao.Dao;

@Repository("dao")
public class BaseDao implements Dao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public <T> void delete(Class<?> clazz, T id) {
        delete(getById(clazz, id));
    }

    @Override
    public void delete(Object object) {
        object = entityManager.merge(object);
        entityManager.remove(object);

    }

    @Override
    public <T> T getById(Class<T> clazz, Object id) {
        return getById(clazz, id, false);
    }

    @Override
    public <T> T getById(Class<T> clazz, Object id, boolean lock) {
        if (lock) {
            return entityManager.find(clazz, id, LockModeType.PESSIMISTIC_WRITE);
        } else {
            return entityManager.find(clazz, id);
        }
    }

    @Override
    public <T> T persist(T e) {
        // if e is already in the persistence context (session), no action is
        // taken, except for cascades
        // if e is detached, a copy (e') is returned, which is attached
        // (managed)
        // if e is transient (new instance), it is saved and a persistent (and
        // managed) copy is returned
        e = entityManager.merge(e);

        return e;
    }

    @Override
    public <T> T getByPropertyValue(Class<T> clazz, String propertyName,
            Object propertyValue) {
        String dotlessPropertyName = propertyName.replace(".", "");
        List<T> result = findByQuery(new QueryDetails()
                .setQuery("SELECT ob FROM " + clazz.getName() + " ob WHERE " + propertyName
                        + "=:" + dotlessPropertyName)
                .setParamNames(new String[] {dotlessPropertyName})
                .setParamValues(new Object[] {propertyValue}));

        return getResult(result);

    }

    @Override
    public <T> List<T> getListByPropertyValue(Class<T> clazz, String propertyName,
            Object propertyValue) {
        String dotlessPropertyName = propertyName.replace(".", "");
        List<T> result = findByQuery(new QueryDetails().setQuery(
                "SELECT o FROM " + clazz.getName() + " o WHERE " + propertyName
                        + "=:" + dotlessPropertyName)
                .setParamNames(new String[] { dotlessPropertyName })
                .setParamValues(new Object[] { propertyValue }));

        return result;

    }

    protected int executeQuery(String query, String[] names, Object[] args) {
        if (names == null) {
            names = new String[] {};
        }

        if (args == null) {
            args = new Object[] {};
        }

        Query q = entityManager.createQuery(query);
        for (int i = 0; i < names.length; i++) {
            q.setParameter(names[i], args[i]);
        }
        return q.executeUpdate();
    }

    @SuppressWarnings("unchecked")
    protected <T> List<T> findByQuery(QueryDetails details) {
        Query q = null;
        if (details.getQueryName() != null) {
            q = entityManager.createNamedQuery(details.getQueryName());
        } else if (details.getQuery() != null) {
            q = entityManager.createQuery(details.getQuery());
        } else {
            throw new IllegalArgumentException("Either query or query name must be set");
        }

        for (int i = 0; i < details.getParamNames().length; i++) {
            q.setParameter(details.getParamNames()[i], details.getParamValues()[i]);
        }
        if (details.getStart() > -1) {
            q.setFirstResult(details.getStart());
        }
        if (details.getCount() > -1) {
            q.setMaxResults(details.getCount());
        }
        if (details.isCacheable()) {
            setCacheable(q);
        }
        return q.getResultList();
    }

    protected void setCacheable(Query query) {
        //TODO consider if every query should be cached (hibernate advises against it)
        query.setHint("org.hibernate.cacheable", Boolean.TRUE);
    }

    protected Object getDelegate() {
        return entityManager.getDelegate();
    }

    protected int executeNamedQuery(String name) {
        return entityManager.createNamedQuery(name).executeUpdate();
    }

    protected <T> T getResult(List<T> result) {
        if (!result.isEmpty()) {
             return result.get(0);
         }

         return null;
    }

    @Override
    public <T> List<T> listOrdered(Class<T> clazz, String orderField) {
        return findByQuery(new QueryDetails().setQuery("from " + clazz.getName() + " order by "
                + orderField));
    }

    @Override
    public <T> List<T> list(Class<T> clazz) {
        return findByQuery(new QueryDetails().setQuery("from " + clazz.getName()));
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    public void reindexSearch() {
        try {
            getFullTextEntityManager().createIndexer().startAndWait();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }


    /*
     * Getting the entity manager for the full-text operations
     *
     */
    protected FullTextEntityManager getFullTextEntityManager() {
        FullTextEntityManager fullTextEntityManager = Search
                .getFullTextEntityManager(getEntityManager());
        return fullTextEntityManager;
    }

    protected String escapeKeywords(String keywords) {
        keywords = keywords.trim();
        if (keywords.isEmpty()) {
            return keywords;
        }

        if (keywords.charAt(0) == '*') {
            if (keywords.length() == 1) {
                return "";
            } else {
                keywords = keywords.substring(1).trim();
            }
        }

        keywords = QueryParser.escape(keywords);

        return keywords;
    }

    @Override
    public <T> List<T> getPagedListByPropertyValue(Class<T> clazz,
            String propertyName, Object propertyValue, int page, int pageSize) {
        String queryString = "SELECT o FROM " + clazz.getName() + " o WHERE " + propertyName + "=:" + propertyName;
        TypedQuery<T> query = getEntityManager().createQuery(queryString, clazz);
        query.setFirstResult(page * pageSize);
        query.setMaxResults(pageSize);
        query.setParameter(propertyName, propertyValue);
        return query.getResultList();
    }

    @Override
    public <T> List<T> getOrderedListByPropertyValue(Class<T> clazz,
            String propertyName, Object propertyValue, String orderField) {
        List<T> result = findByQuery(new QueryDetails()
                .setQuery("SELECT o FROM " + clazz.getName()
                + " o WHERE " + propertyName + "=:" + propertyName
                + " ORDER BY " + orderField).setParamNames(new String[] { propertyName })
                .setParamValues(new Object[] { propertyValue }));

        return result;
    }

    @Override
    public void lock(Object entity) {
        if (entity != null) {
            getEntityManager().lock(entity, LockModeType.PESSIMISTIC_WRITE);
        }
    }

    @Override
    public <T> List<T> listPaged(Class<T> clazz, int start, int pageSize) {
        return findByQuery(new QueryDetails().setQuery("from " + clazz.getName() + " ORDER BY id").setStart(start).setCount(pageSize));
    }

    /**
    * Performs a given operation on all records in batches
    * @param operation
    * @param pageSize
    */
    @Override
    public <T> void performBatched(Class<T> clazz, int pageSize, PageableOperation<T> operation) {
        int page = 0;
        while (true) {
            List<T> data = listPaged(clazz, page * pageSize, pageSize);
            page++;
            operation.setData(data);
            operation.execute();
            // final batch
            if (data.size() < pageSize) {
                break;
            }
        }
    }
}
