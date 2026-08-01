package com.ebusiness.product.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

final class RedisCacheSupport {

    static final String PRODUCTS_ALL = "productsAll";
    static final String PRODUCTS_BY_CATEGORY = "productsByCategory";

    private RedisCacheSupport() {
    }

    static RedisCacheConfiguration baseConfig(Duration ttl) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.ebusiness.product.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.math.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.lang.")
                .build(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );

        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(ttl)
            .disableCachingNullValues()
            .prefixCacheNameWith("product-service:")
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer(mapper)));
    }

    static Map<String, RedisCacheConfiguration> namedCaches(RedisCacheConfiguration defaults) {
        Map<String, RedisCacheConfiguration> caches = new HashMap<>();
        caches.put(PRODUCTS_ALL, defaults.entryTtl(Duration.ofMinutes(30)));
        caches.put(PRODUCTS_BY_CATEGORY, defaults.entryTtl(Duration.ofMinutes(30)));
        return caches;
    }
}
