package com.ebusiness.product.config;

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
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.io.IOException;
import java.net.Socket;
import java.time.Duration;

@Slf4j
@Configuration
public class CacheConfig implements CachingConfigurer {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        if (isRedisReachable(redisHost, redisPort)) {
            try {
                connectionFactory.getConnection().ping();
                log.info("Redis OK at {}:{} — productsAll / productsByCategory", redisHost, redisPort);
                RedisCacheConfiguration defaults = RedisCacheSupport.baseConfig(Duration.ofMinutes(5));
                return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(defaults)
                    .withInitialCacheConfigurations(RedisCacheSupport.namedCaches(defaults))
                    .build();
            } catch (Exception e) {
                log.warn("Redis ping failed — {}", e.getMessage());
            }
        } else {
            log.warn("Redis unreachable at {}:{} — cache disabled", redisHost, redisPort);
        }
        return new NoOpCacheManager();
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException ex, Cache cache, Object key) {
                log.warn("Redis GET failed cache={} key={}", cache.getName(), key);
            }

            @Override
            public void handleCachePutError(RuntimeException ex, Cache cache, Object key, Object value) {
                log.warn("Redis PUT failed cache={} key={}", cache.getName(), key);
            }

            @Override
            public void handleCacheEvictError(RuntimeException ex, Cache cache, Object key) {
                log.warn("Redis EVICT failed cache={} key={}", cache.getName(), key);
            }

            @Override
            public void handleCacheClearError(RuntimeException ex, Cache cache) {
                log.warn("Redis CLEAR failed cache={}", cache.getName());
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
