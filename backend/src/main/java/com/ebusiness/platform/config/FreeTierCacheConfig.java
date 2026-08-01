package com.ebusiness.platform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.io.IOException;
import java.net.Socket;
import java.time.Duration;

@Slf4j
@Configuration
@Profile("freetier")
public class FreeTierCacheConfig implements CachingConfigurer {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        if (isRedisReachable(redisHost, redisPort)) {
            try {
                connectionFactory.getConnection().ping();
                log.info("Redis available at {}:{} — separate caches for productsAll and productsByCategory",
                    redisHost, redisPort);
                return buildRedisCacheManager(connectionFactory);
            } catch (Exception e) {
                log.warn("Redis ping failed at {}:{} — {}", redisHost, redisPort, e.getMessage());
            }
        } else {
            log.warn("Redis not reachable at {}:{} — caching disabled, requests go straight to DB",
                redisHost, redisPort);
        }
        return new NoOpCacheManager();
    }

    private RedisCacheManager buildRedisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheSupport.baseConfig(Duration.ofMinutes(5));

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
                log.warn("Redis GET failed for cache={} key={} — falling through to DB",
                    cache.getName(), key);
            }

            @Override
            public void handleCachePutError(RuntimeException ex, Cache cache, Object key, Object value) {
                log.warn("Redis PUT failed for cache={} key={} — response returned, cache not updated",
                    cache.getName(), key);
            }

            @Override
            public void handleCacheEvictError(RuntimeException ex, Cache cache, Object key) {
                log.warn("Redis EVICT failed for cache={} key={} — stale data possible until TTL",
                    cache.getName(), key);
            }

            @Override
            public void handleCacheClearError(RuntimeException ex, Cache cache) {
                log.warn("Redis CLEAR failed for cache={} — stale data possible until TTL",
                    cache.getName());
            }
        };
    }

    private boolean isRedisReachable(String host, int port) {
        try (Socket socket = new Socket(host, port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
