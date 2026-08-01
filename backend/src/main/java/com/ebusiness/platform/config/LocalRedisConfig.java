package com.ebusiness.platform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.IOException;
import java.net.Socket;

@Slf4j
@Configuration
@Profile("local")
public class LocalRedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Try to connect to external Redis first
        if (isRedisRunning("localhost", 6379)) {
            log.info("Connecting to external Redis at localhost:6379");
            return new LettuceConnectionFactory("localhost", 6379);
        }
        
        // If Redis is not available, log warning but still create connection factory
        // The CacheErrorHandler will handle failures gracefully
        log.warn("Redis not available at localhost:6379. Application will run without caching.");
        log.warn("To enable caching, either start Redis server or remove caching annotations.");
        
        return new LettuceConnectionFactory("localhost", 6379);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    private boolean isRedisRunning(String host, int port) {
        try (Socket socket = new Socket(host, port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
