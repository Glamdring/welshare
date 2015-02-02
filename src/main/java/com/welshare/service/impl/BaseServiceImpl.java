package com.welshare.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.welshare.dao.Dao;
import com.welshare.service.BaseService;
import com.welshare.service.annotations.SqlReadonlyTransactional;
import com.welshare.service.annotations.SqlTransactional;

/**
 * Base class for all services.
 *
 * @author Bozhidar Bozhanov
 */
@Service("baseService")
public class BaseServiceImpl implements BaseService {

    @Resource(name="dao")
    private Dao dao;

    public void setDao(Dao dao) {
        this.dao = dao;
    }

    protected Dao getDao() {
        return dao;
    }

    @SqlReadonlyTransactional
    public <T> T get(Class<T> clazz, Object id) {
        T result = getDao().getById(clazz, id);
        return result;
    }

    @SqlTransactional
    public <T> void delete(T e) {
        getDao().delete(e);
    }

    @SqlTransactional
    public void delete(Class<?> clazz, int id) {
        getDao().delete(clazz, id);
    }

    @SqlReadonlyTransactional
    public <T> List<T> list(Class<T> clazz) {
        return getDao().list(clazz);
    }

    @SqlReadonlyTransactional
    public <T> List<T> listOrdered(Class<T> clazz, String orderColumn) {
        return getDao().listOrdered(clazz, orderColumn);
    }

    @SqlTransactional
    public <T> T save(T e) {
        return getDao().persist(e);
    }

    @Override
    @SqlTransactional
    public <T> T get(Class<T> clazz, String propertyName, Object propertyValue) {
        T result = getDao().getByPropertyValue(clazz, propertyName, propertyValue);

        return result;
    }

    @Override
    @SqlReadonlyTransactional
    public <T> List<T> listOrdered(Class<T> clazz, String propertyName,
            Object propertyValue, String orderField) {
        return getDao().getOrderedListByPropertyValue(clazz, propertyName, propertyValue, orderField);
    }
}
