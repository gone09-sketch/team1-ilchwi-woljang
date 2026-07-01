package com.team1ilchwiwoljang.domain.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PopularProductCacheScheduler {

    private final ProductService productService;

    /**
     * 9분 30초 주기로 @CachePut 웜업 메서드를 호출하여 Redis 캐시 TTL을 강제 리셋합니다.
     * (캐시 만료 시간 10분보다 짧은 주기로 실행하여 캐시 스탬피드를 방어합니다.)
     */
    @Scheduled(fixedDelay = 570000)
    public void warmUpPopularProducts() {
        log.info("인기 상품 캐시 웜업(Cache Warm-up) 백그라운드 스케줄러 작동 시작...");
        
        int limit = 10;
        // @CachePut이 붙은 웜업 전용 메서드를 호출 → 캐시 유무 관계없이 항상 TTL 리셋
        productService.warmUpPopularProductsCache(limit);
        
        log.info("인기 상품 캐시 웜업 완료. (Key: {})", limit);
    }
}
