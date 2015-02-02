package com.welshare.dao.jpa;

public class QueryDetails {

    private String query;
    private String queryName;
    private String[] paramNames = new String[0];
    private Object[] paramValues = new Object[0];
    private int start = -1;
    private int count = -1;
    private boolean cacheable;

    public String getQuery() {
        return query;
    }
    public QueryDetails setQuery(String query) {
        this.query = query;
        return this;
    }
    public String[] getParamNames() {
        return paramNames;
    }
    public QueryDetails setParamNames(String[] paramNames) {
        this.paramNames = paramNames.clone();
        return this;
    }
    public Object[] getParamValues() {
        return paramValues;
    }
    public QueryDetails setParamValues(Object[] paramValues) {
        this.paramValues = paramValues.clone();
        return this;
    }
    public int getStart() {
        return start;
    }
    public QueryDetails setStart(int start) {
        this.start = start;
        return this;
    }
    public int getCount() {
        return count;
    }
    public QueryDetails setCount(int count) {
        this.count = count;
        return this;
    }
    public boolean isCacheable() {
        return cacheable;
    }
    public QueryDetails setCacheable(boolean cacheable) {
        this.cacheable = cacheable;
        return this;
    }
    public String getQueryName() {
        return queryName;
    }
    public QueryDetails setQueryName(String queryName) {
        this.queryName = queryName;
        return this;
    }

}
