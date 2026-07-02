package com.team1ilchwiwoljang.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {

    private final ObjectMapper objectMapper;

    public CacheConfig() {
        // 직접 인스턴스를 생성하고 record/날짜 지원 모듈을 등록합니다.
        this.objectMapper = new ObjectMapper()
                .registerModule(new com.fasterxml.jackson.module.paramnames.ParameterNamesModule())
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .activateDefaultTyping(
                        LaissezFaireSubTypeValidator.instance,
                        ObjectMapper.DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY
                );
    }

    @Bean
    @Profile("!test")
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // record 역직렬화를 완벽하게 지원하고 사람이 읽을 수 있는 JSON으로 캐시를 관리하도록 직렬화기 지정
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .entryTtl(Duration.ofMinutes(30));

        Map<String, RedisCacheConfiguration> customConfigs = new HashMap<>();
        // 인기 상품 캐시: TTL 10분
        customConfigs.put(CacheType.POPULAR_PRODUCTS.getCacheName(),
                defaultConfig.entryTtl(Duration.ofMinutes(CacheType.POPULAR_PRODUCTS.getExpiredAfterWriteMin())));
        // 인기 검색어 캐시: TTL 10분
        customConfigs.put(CacheType.POPULAR_KEYWORDS.getCacheName(),
                defaultConfig.entryTtl(Duration.ofMinutes(CacheType.POPULAR_KEYWORDS.getExpiredAfterWriteMin())));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(customConfigs)
                .build();
    }

    @Bean
    @Profile("test")
    public CacheManager testCacheManager() {
        // 테스트 환경에서는 Redis 의존성 없이 가볍게 ConcurrentMapCacheManager를 사용합니다.
        return new ConcurrentMapCacheManager(
                CacheType.POPULAR_PRODUCTS.getCacheName(),
                CacheType.POPULAR_KEYWORDS.getCacheName()
        );
    }
}
