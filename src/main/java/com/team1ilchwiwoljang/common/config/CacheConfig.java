package com.team1ilchwiwoljang.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig implements CachingConfigurer {

    private final ObjectMapper objectMapper;

    public CacheConfig() {
        // 다형성 역직렬화 대상을 프로젝트 패키지 및 기본 Collection 계열로 제한하여 보안 취약점을 방어합니다.
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.team1ilchwiwoljang")
                .allowIfSubType("java.util")
                .allowIfSubType("java.lang")
                .build();

        // 직접 인스턴스를 생성하고 레코드/날짜 지원 모듈을 등록합니다.
        this.objectMapper = new ObjectMapper()
                .registerModule(new com.fasterxml.jackson.module.paramnames.ParameterNamesModule())
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .activateDefaultTyping(
                        ptv,
                        ObjectMapper.DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY
                );
    }

    @Bean
    @Profile("!test")
    @SuppressWarnings("removal")
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // record 역직렬화를 완벽하게 지원하고 사람이 읽을 수 있는 JSON으로 캐시를 관리하도록 직렬화기 지정
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .entryTtl(Duration.ofMinutes(30));

        // 인기 상품 캐시 개별 설정: Enum 값에 따라 TTL 설정
        Map<String, RedisCacheConfiguration> customConfigs = new HashMap<>();
        customConfigs.put(
                CacheType.POPULAR_PRODUCTS.getCacheName(),
                defaultConfig.entryTtl(Duration.ofMinutes(CacheType.POPULAR_PRODUCTS.getExpiredAfterWriteMin()))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(customConfigs)
                .build();
    }

    @Bean
    @Profile("test")
    public CacheManager testCacheManager() {
        // 테스트 환경에서는 Redis 의존성 없이 가볍게 ConcurrentMapCacheManager를 사용합니다.
        return new ConcurrentMapCacheManager(CacheType.POPULAR_PRODUCTS.getCacheName());
    }

    @Override
    public CacheErrorHandler errorHandler() {
        // Redis 장애 발생 시 캐시 조회 에러를 무시하고 DB 조회 Fallback을 보장하기 위해 커스텀 에러 핸들러를 등록합니다.
        return new CustomCacheErrorHandler();
    }
}
