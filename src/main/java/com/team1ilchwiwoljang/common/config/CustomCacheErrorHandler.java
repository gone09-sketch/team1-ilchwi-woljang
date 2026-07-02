package com.team1ilchwiwoljang.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

@Slf4j
public class CustomCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Redis 캐시 조회 실패 (DB로 Fallback 진행) - Cache: {}, Key: {}, Error: {}", 
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Redis 캐시 저장 실패 - Cache: {}, Key: {}, Error: {}", 
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Redis 캐시 삭제 실패 - Cache: {}, Key: {}, Error: {}", 
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Redis 캐시 초기화 실패 - Cache: {}, Error: {}", 
                cache.getName(), exception.getMessage());
    }
}
