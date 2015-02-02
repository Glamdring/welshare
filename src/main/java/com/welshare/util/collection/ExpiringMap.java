package com.welshare.util.collection;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class ExpiringMap<K, V> extends ConcurrentHashMap<K, V> {

    private static final Logger logger = LoggerFactory.getLogger(ExpiringMap.class);

    private static final long serialVersionUID = -9173855046154270977L;

    private final Map<K, ExpiryInfo<K, V>> expiryInformation = new HashMap<K, ExpiryInfo<K, V>>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final long timeToIdleNanos;
    private ExpirationListener<K, V> expirationListener;

    public ExpiringMap(long timeToIdle, ExpirationListener<K, V> expirationListenerParam) {
        this.expirationListener = expirationListenerParam;
        this.timeToIdleNanos = TimeUnit.MILLISECONDS.toNanos(timeToIdle);
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    if (expiryInformation.isEmpty()) {
                        return;
                    }
                    long now = System.nanoTime();
                    Set<Entry<K, ExpiryInfo<K, V>>> entrySetCopy = Sets.newHashSet(expiryInformation.entrySet());
                    for (Entry<K, ExpiryInfo<K, V>> mapEntry : entrySetCopy) {
                        ExpiryInfo<K, V> entry = mapEntry.getValue();
                        if (entry.getLastAccessed() + timeToIdleNanos < now) {
                            expiryInformation.remove(entry.getKey());
                            ExpiringMap.this.remove(entry.getKey());
                            expirationListener.onExpiry(entry.getKey(), entry.getValue());
                        }
                    }
                } catch (Exception ex) {
                    logger.warn("Problem when expiring entires", ex);
                }
            }
              //+2000 so that the expiring job runs on bigger interval than the idle time
        }, 0, timeToIdle + 2000, TimeUnit.MILLISECONDS);
    }

    @Override
    public V put(K key, V value) {
        V result = super.put(key, value);
        if (result == null) {
            putExpiryEntry(key, value);
        }
        return result;
    }

    @Override
    public V putIfAbsent(K key, V value) {
        V result = super.putIfAbsent(key, value);
        if (result == null) {
            putExpiryEntry(key, value);
        }
        return result;
    }

    @Override
    public V get(Object key) {
        V result = super.get(key);
        if (result != null) {
            refreshAccess(key);
        }
        return result;
    }

    private void putExpiryEntry(K key, V value) {
        ExpiryInfo<K, V> entry = new ExpiryInfo<K, V>();
        entry.setKey(key);
        entry.setLastAccessed(System.nanoTime());
        entry.setValue(value);
        expiryInformation.put(key, entry);
    }

    private void refreshAccess(Object key) {
        ExpiryInfo<K, V> entry = expiryInformation.get(key);
        entry.setLastAccessed(System.nanoTime());
    }

    private static class ExpiryInfo<K, V> {
        private long lastAccessed;
        private K key;
        private V value;

        public long getLastAccessed() {
            return lastAccessed;
        }

        public void setLastAccessed(long lastAccessed) {
            this.lastAccessed = lastAccessed;
        }

        public K getKey() {
            return key;
        }

        public void setKey(K key) {
            this.key = key;
        }

        public V getValue() {
            return value;
        }

        public void setValue(V value) {
            this.value = value;
        }
    }

    public void setExpirationListener(ExpirationListener<K, V> expirationListener) {
        this.expirationListener = expirationListener;
    }

    public void destroy() {
        executor.shutdown();
    }
}