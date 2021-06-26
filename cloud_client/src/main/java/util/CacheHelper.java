package util;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

public class CacheHelper {

    private static final CacheManager cacheManager;

    static {
        cacheManager = CacheManagerBuilder
                .newCacheManagerBuilder()
                .build();
        cacheManager.init();
    }

    public static <K, V> Cache<K, V> createCache(String alias, Class<K> keyType, Class<V> valueType, long cacheSize) {
        return cacheManager.createCache(alias, CacheConfigurationBuilder
                .newCacheConfigurationBuilder(keyType, valueType, ResourcePoolsBuilder.heap(cacheSize)));
    }

    public static <K, V> Cache<K, V> getCache(String alias, Class<K> keyType, Class<V> valueType) {
        return cacheManager.getCache(alias, keyType, valueType);
    }
}
