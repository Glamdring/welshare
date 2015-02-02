package com.welshare.util.cache;

/**
 * To be implemented by objects passed as method arguments to @Cacheable
 * methods. Prefer the key=".." attribute or just pass primitives. Use this
 * interface rarely, e.g. to prevent duplications
 *
 * @author bozho
 *
 */
public interface CacheKey {

    String getCacheKey();
}
