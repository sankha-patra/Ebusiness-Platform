package com.ebusiness.platform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

@Slf4j
@Configuration
@Profile("!local & !freetier")
public class RedisConfig implements CachingConfigurer {

    @Bean
    public RedisCacheConfiguration defaultCacheConfig() {
        return RedisCacheSupport.baseConfig(Duration.ofMinutes(5));
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = defaultCacheConfig();

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(RedisCacheSupport.namedCaches(defaultConfig))
            .build();
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException ex, Cache cache, Object key) {
                log.error("Redis GET failed for cache={} key={} — falling through to DB",
                    cache.getName(), key, ex);
            }

            @Override
            public void handleCachePutError(RuntimeException ex, Cache cache, Object key, Object value) {
                log.error("Redis PUT failed for cache={} key={} — data returned, cache not updated",
                    cache.getName(), key, ex);
            }

            @Override
            public void handleCacheEvictError(RuntimeException ex, Cache cache, Object key) {
                log.error("Redis EVICT failed for cache={} key={} — stale data possible, monitor TTL",
                    cache.getName(), key, ex);
            }

            @Override
            public void handleCacheClearError(RuntimeException ex, Cache cache) {
                log.error("Redis CLEAR failed for cache={} — stale data possible, monitor TTL",
                    cache.getName(), ex);
            }
        };
    }
}
