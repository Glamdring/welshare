package com.welshare.util.collection;

public interface ExpirationListener<K, V> {

    void onExpiry(K key, V value);
}
