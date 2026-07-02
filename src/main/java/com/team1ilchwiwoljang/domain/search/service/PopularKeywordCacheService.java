package com.team1ilchwiwoljang.domain.search.service;

import com.team1ilchwiwoljang.domain.search.dto.response.SearchKeywordPopularResponse;
import com.team1ilchwiwoljang.domain.search.repository.SearchKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PopularKeywordCacheService {

    private final SearchKeywordRepository searchKeywordRepository;

    /**
     * 인기 검색어 조회 (최대 100개 캐싱)
     * 캐시 키를 고정값 'all'로 단일화하여 모든 limit 요청에 대해 100% 캐시 히트를 보장합니다.
     */
    @Cacheable(value = "popularKeywords", key = "'all'")
    public List<SearchKeywordPopularResponse> getCachedPopularKeywords() {
        Pageable pageable = PageRequest.of(0, 100);
        return searchKeywordRepository.findAllByOrderBySearchCountDesc(pageable)
                .stream()
                .map(SearchKeywordPopularResponse::from)
                .toList();
    }

    /**
     * 캐시 웜업(Warm-up) 전용 메서드.
     * 캐시 만료와 무관하게 항상 DB를 조회해 캐시를 갱신('all' 키)합니다.
     */
    @CachePut(value = "popularKeywords", key = "'all'")
    public List<SearchKeywordPopularResponse> warmUpPopularKeywordsCache() {
        Pageable pageable = PageRequest.of(0, 100);
        return searchKeywordRepository.findAllByOrderBySearchCountDesc(pageable)
                .stream()
                .map(SearchKeywordPopularResponse::from)
                .toList();
    }
}
