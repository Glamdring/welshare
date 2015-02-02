package com.welshare.service;

import java.util.List;

public interface BaseService {

    <T> T save(T entity);

    <T> void delete(T e);

    @Deprecated
    <T> List<T> list(Class<T> clazz);

    <T> List<T> listOrdered(Class<T> clazz, String orderColumn);

    <T> List<T> listOrdered(Class<T> clazz, String propertyName,
            Object propertyValue, String orderField);

    <T> T get(Class<T> clazz, Object id);

    <T> T get(Class<T> clazz, String propertyName, Object propertyValue);
}
