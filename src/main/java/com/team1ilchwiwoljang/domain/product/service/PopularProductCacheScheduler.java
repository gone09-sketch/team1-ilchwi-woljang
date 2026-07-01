package com.team1ilchwiwoljang.domain.product.service;

import com.team1ilchwiwoljang.domain.product.dto.response.PopularProductResponse;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import com.team1ilchwiwoljang.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PopularProductCacheScheduler {

    private final ProductRepository productRepository;
    private final CacheManager cacheManager;

    /**
     * 9분 30초 주기로 캐시 데이터를 강제 갱신(Cache Warm-up)하여 캐시 스탬피드 및 만료 지연을 예방합니다.
     */
    @Scheduled(fixedDelay = 570000)
    public void warmUpPopularProducts() {
        log.info("인기 상품 캐시 웜업(Cache Warm-up) 백그라운드 스케줄러 작동 시작...");
        
        int limit = 10;
        Pageable pageable = PageRequest.of(0, limit);
        
        // 1. DB 직접 조회 (최신 인기 상품 데이터 획득)
        List<PopularProductResponse> popularProducts = productRepository
                .findByStatusOrderBySalesCountDesc(ProductStatus.ON_SALE, pageable)
                .stream()
                .map(PopularProductResponse::from)
                .toList();

        // 2. 캐시 메모리에 강제 덮어쓰기 (Cache Overwrite)
        Cache cache = cacheManager.getCache("popularProducts");
        if (cache != null) {
            cache.put(limit, popularProducts);
            log.info("인기 상품 캐시 웜업 완료. (Key: {}, Item Count: {})", limit, popularProducts.size());
        }
    }
}
