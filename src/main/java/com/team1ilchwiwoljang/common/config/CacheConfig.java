package com.team1ilchwiwoljang.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        for (CacheType type : CacheType.values()) {
            cacheManager.registerCustomCache(type.getCacheName(),
                Caffeine.newBuilder()
                    .expireAfterWrite(type.getExpiredAfterWriteMin(), TimeUnit.MINUTES)
                    .maximumSize(type.getMaximumSize())
                    .build()
            );
        }
        
        return cacheManager;
    }
}
