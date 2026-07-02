package com.team1ilchwiwoljang.domain.product.service;

import com.team1ilchwiwoljang.domain.product.dto.response.PopularProductResponse;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import com.team1ilchwiwoljang.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PopularProductCacheService {

    private final ProductRepository productRepository;

    /**
     * 누적 판매량 기준 인기 상품 목록 조회 (최대 100개 캐싱)
     * 캐시 키를 고정값 'all'로 단일화하여 모든 limit 요청에 대해 100% 캐시 히트를 보장합니다.
     */
    @Cacheable(value = "popularProducts", key = "'all'")
    public List<PopularProductResponse> getCachedPopularProducts() {
        Pageable pageable = PageRequest.of(0, 100);
        return productRepository.findByStatusOrderBySalesCountDesc(ProductStatus.ON_SALE, pageable)
                .stream()
                .map(PopularProductResponse::from)
                .toList();
    }

    /**
     * 캐시 웜업(Warm-up) 전용 메서드.
     * 캐시 만료와 무관하게 항상 DB를 조회해 캐시를 갱신('all' 키)합니다.
     */
    @CachePut(value = "popularProducts", key = "'all'")
    public List<PopularProductResponse> warmUpPopularProductsCache() {
        Pageable pageable = PageRequest.of(0, 100);
        return productRepository.findByStatusOrderBySalesCountDesc(ProductStatus.ON_SALE, pageable)
                .stream()
                .map(PopularProductResponse::from)
                .toList();
    }
}
