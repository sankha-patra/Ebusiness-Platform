package com.ebusiness.platform.config;

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

/**
 * Shared Redis cache setup. Product "all" and "by category" use separate cache names
 * so a category response can never be served for GET /products.
 */
final class RedisCacheSupport {

    static final String PRODUCTS_ALL = "productsAll";
    static final String PRODUCTS_BY_CATEGORY = "productsByCategory";
    static final String ORDER_STATUS = "orderStatus";
    static final String PAYMENT_STATUS = "paymentStatus";
    static final String TENANT_CONFIG = "tenantConfig";

    private RedisCacheSupport() {
    }

    static RedisCacheConfiguration baseConfig(Duration ttl) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.ebusiness.platform.")
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
            .prefixCacheNameWith("ebusiness:")
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer(mapper)));
    }

    static Map<String, RedisCacheConfiguration> namedCaches(RedisCacheConfiguration defaults) {
        Map<String, RedisCacheConfiguration> caches = new HashMap<>();
        caches.put(ORDER_STATUS, defaults.entryTtl(Duration.ofMinutes(2)));
        caches.put(PAYMENT_STATUS, defaults.entryTtl(Duration.ofMinutes(1)));
        // Separate regions: category hit must never satisfy "all products"
        caches.put(PRODUCTS_ALL, defaults.entryTtl(Duration.ofMinutes(30)));
        caches.put(PRODUCTS_BY_CATEGORY, defaults.entryTtl(Duration.ofMinutes(30)));
        caches.put(TENANT_CONFIG, defaults.entryTtl(Duration.ofHours(1)));
        return caches;
    }
}
