package com.galileo.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.cache.annotation.EnableCaching;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    RedisCacheConfiguration redisCacheConfiguration() {
        var typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.galileo.api.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.util.")
                .build();
        var serializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .build();

        return RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> "galileo::" + cacheName + "::")
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }

    @Bean
    RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(
            CacheProperties properties,
            RedisCacheConfiguration defaults) {
        return builder -> builder
                .transactionAware()
                .withCacheConfiguration(
                        CacheNames.DEPARTMENTS,
                        defaults.entryTtl(properties.departmentTtl()))
                .withCacheConfiguration(
                        CacheNames.TEAMS,
                        defaults.entryTtl(properties.teamTtl()))
                .withCacheConfiguration(
                        CacheNames.USERS,
                        defaults.entryTtl(properties.userTtl()))
                .withCacheConfiguration(
                        CacheNames.ROLES,
                        defaults.entryTtl(properties.roleTtl()));
    }

    /**
     * Redis improves latency but is not a system of record. Cache failures must not
     * make PostgreSQL-backed business operations unavailable.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                logFailure("read", cache, exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                logFailure("write", cache, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                logFailure("eviction", cache, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                logFailure("clear", cache, exception);
            }

            private void logFailure(String operation, Cache cache, RuntimeException exception) {
                LOGGER.warn(
                        "Cache {} failed for '{}'; continuing with PostgreSQL as the source of truth: {}",
                        operation,
                        cache.getName(),
                        exception.getClass().getSimpleName());
            }
        };
    }
}
