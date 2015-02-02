package com.welshare.dao.neo4j;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.beanutils.BeanUtils;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.index.IndexService;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.graph.core.Property;
import org.springframework.data.graph.neo4j.template.Neo4jTemplate;

import com.welshare.dao.Dao;
import com.welshare.util.ReflectionUtils;
import com.welshare.util.ReflectionUtils.BeanProperty;

public class BaseNeo4jDao implements Dao {

    protected static final String ID_SUFFIX = ".id";

    @Inject
    private Neo4jTemplate template;

    @Inject
    private IndexService index;

    @Override
    public <T> void delete(Class<?> clazz, T id) {
        Node node = index.getSingleNode(clazz.getName() + ID_SUFFIX, id);
        for (Relationship r : node.getRelationships()) {
            r.delete();
        }
        node.delete();
    }

    @Override
    public void delete(Object obj) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getById(Class<T> clazz, Object id) {
        Node node = index.getSingleNode(clazz.getName() + ID_SUFFIX, id);
        return getBean(clazz, node);
    }

    protected <T> T getBean(Class<T> clazz, Node node) {
        try {
            T instance = clazz.newInstance();
            Iterable<String> propertyKeys = node.getPropertyKeys();
            for (String key : propertyKeys) {
                if (!key.equals(clazz.getName() + ID_SUFFIX)) {
                    BeanUtils.setProperty(instance, key, node.getProperty(key));
                } else {
                    BeanProperty prop = ReflectionUtils.getAnnotatedProperty(instance, Id.class);
                    BeanUtils.setProperty(instance, prop.getName(), node.getProperty(key));
                }
            }
            return instance;
        } catch (Exception e) {
            throw new DataAccessResourceFailureException("Problem obtainig bean from node", e);
        }
    }

    @Override
    public <T> T getById(Class<T> clazz, Object id, boolean lock) {
        return getById(clazz, id);
    }

    @Override
    public <T> T persist(T e) {
        BeanProperty id = ReflectionUtils.getAnnotatedProperty(e, Id.class);
        if (id == null) {
            throw new IllegalArgumentException("The bean must have an @Id field");
        }

        List<BeanProperty> persistentProperties = ReflectionUtils
                .getAnnotatedProperties(e, Persistent.class);
        Property[] properties = new Property[persistentProperties.size() + 1];
        properties[0] = new Property(e.getClass().getName() + ID_SUFFIX, id.getValue());
        int i = 1;
        for (BeanProperty property : persistentProperties) {
            properties[i] = new Property(property.getName(), property.getValue());
            i++;
        }

        Node node = template.createNode(properties);
        index.index(node, e.getClass().getName() + ID_SUFFIX, id.getValue());
        return e;
    }

    @Override
    public <T> T getByPropertyValue(Class<T> clazz, String propertyName,
            Object propertyValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> List<T> listOrdered(Class<T> clazz, String orderField) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> List<T> list(Class<T> clazz) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reindexSearch() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> List<T> getListByPropertyValue(Class<T> clazz,
            String propertyName, Object propertyValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> List<T> getOrderedListByPropertyValue(Class<T> clazz,
            String propertyName, Object propertyValue, String orderField) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> List<T> getPagedListByPropertyValue(Class<T> clazz,
            String propertyName, Object propertyValue, int page, int pageSize) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void lock(Object entity) {
        // do nothing, locked by default
    }

    public Neo4jTemplate getTemplate() {
        return template;
    }

    public void setTemplate(Neo4jTemplate template) {
        this.template = template;
    }

    public IndexService getIndex() {
        return index;
    }

    public void setIndex(IndexService index) {
        this.index = index;
    }

    @Override
    public <T> List<T> listPaged(Class<T> clazz, int start, int pageSize) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> void performBatched(Class<T> clazz, int pageSize, PageableOperation<T> operation) {
        throw new UnsupportedOperationException();
    }
}
