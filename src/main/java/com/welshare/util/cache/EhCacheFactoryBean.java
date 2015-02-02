package com.welshare.util.cache;

public class EhCacheFactoryBean extends org.springframework.cache.ehcache.EhCacheFactoryBean {

    private int statisticsAccuracy;
    private boolean statisticsEnabled;

    @Override
    public void afterPropertiesSet() throws net.sf.ehcache.CacheException, java.io.IOException {
        super.afterPropertiesSet();
        getObject().setStatisticsAccuracy(statisticsAccuracy);
        getObject().setStatisticsEnabled(statisticsEnabled);
    }

    public int getStatisticsAccuracy() {
        return statisticsAccuracy;
    }

    public void setStatisticsAccuracy(int statisticsAccuracy) {
        this.statisticsAccuracy = statisticsAccuracy;
    }
    public boolean isStatisticsEnabled() {
        return statisticsEnabled;
    }
    public void setStatisticsEnabled(boolean statisticsEnabled) {
        this.statisticsEnabled = statisticsEnabled;
    }

}
