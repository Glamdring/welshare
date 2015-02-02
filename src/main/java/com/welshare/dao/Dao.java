package com.welshare.dao;

import java.util.List;

public interface Dao {

    /**
     * Deletes an object identified by the specified Id
     *
     * @param <T>
     * @param clazz
     * @param id
     */
    <T> void delete(Class<?> clazz, T id);

    /**
     * Deletes the specified object
     * @param e
     */
    void delete(Object obj);

    /**
     * Loads the object from the persistent store
     *
     * @param <T>
     * @param clazz
     * @param id
     * @return the entity, if found. Null otherwise.
     */
    <T> T getById(Class<T> clazz, Object id);

    <T> T getById(Class<T> clazz, Object id, boolean lock);

    /**
     * Makes the entity persistent.
     * If it is transient, saves it.
     * If it is detatched, attaches it.
     *
     * @param entity
     * @return the persistent entity
     */
    <T> T persist(T e);


    /**
     * Gets an object by the given criteria
     * @param <T>
     * @param clazz
     * @param propertyName
     * @param propertyValue
     * @return entity
     */
    <T> T getByPropertyValue(Class<T> clazz, String propertyName,
            Object propertyValue);

    <T> List<T> listOrdered(Class<T> clazz, String orderField);

    <T> List<T> list(Class<T> clazz);

    <T> List<T> listPaged(Class<T> clazz, int start, int pageSize);

    /**
     * re-creates the search indices
     */
    void reindexSearch();

    <T> List<T> getListByPropertyValue(Class<T> clazz, String propertyName,
            Object propertyValue);

    <T> List<T> getOrderedListByPropertyValue(Class<T> clazz, String propertyName,
            Object propertyValue, String orderField);

    <T> List<T> getPagedListByPropertyValue(Class<T> clazz, String propertyName,
            Object propertyValue, int page, int pageSize);


    /**
     * Lock the given entity.
     * @param enttiy
     */
    void lock(Object entity);

    <T> void performBatched(Class<T> clazz, int pageSize, PageableOperation<T> operation);

    abstract class PageableOperation<T> {
        private List<T> data;

        public abstract void execute();

        public List<T> getData() {
            return data;
        }

        public void setData(List<T> data) {
            this.data = data;
        }
    }
}
