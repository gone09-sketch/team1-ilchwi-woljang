package com.team1ilchwiwoljang.domain.search.service;

import com.team1ilchwiwoljang.domain.search.dto.response.PopularKeywordResponse;
import com.team1ilchwiwoljang.domain.search.entity.SearchKeyword;
import com.team1ilchwiwoljang.domain.search.repository.SearchKeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchKeywordService {

    private final SearchKeywordRepository searchKeywordRepository;

    /**
     * 검색어 횟수를 1 증가시킵니다.
     * 키워드가 없으면 신규 생성, 있으면 카운트를 증가합니다.
     * DataIntegrityViolationException(unique 제약 충돌) 발생 시 재조회 후 카운트를 증가하여 동시성을 방어합니다.
     */
    @Transactional
    public void incrementSearchCount(String keyword) {
        try {
            searchKeywordRepository.findByKeyword(keyword)
                    .ifPresentOrElse(
                            SearchKeyword::incrementCount,
                            () -> searchKeywordRepository.save(SearchKeyword.create(keyword))
                    );
        } catch (DataIntegrityViolationException e) {
            log.warn("인기 검색어 카운트 증가 중 unique 충돌 감지, 재시도합니다. keyword={}", keyword);
            searchKeywordRepository.findByKeyword(keyword)
                    .ifPresent(SearchKeyword::incrementCount);
        }
    }

    /**
     * 누적 검색 횟수 기준 인기 검색어 TOP N 조회 (캐싱 적용)
     */
    @Cacheable(value = "popularKeywords", key = "#limit")
    public List<PopularKeywordResponse> getPopularKeywords(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return searchKeywordRepository.findAllByOrderBySearchCountDesc(pageable)
                .stream()
                .map(PopularKeywordResponse::from)
                .toList();
    }

    /**
     * 캐시 웜업(Warm-up) 전용 메서드.
     * @CachePut은 캐시 유무와 관계없이 항상 DB를 조회하고 캐시를 새로 덮어씌워(TTL 리셋) 스탬피드를 방어합니다.
     * 스케줄러(PopularKeywordCacheScheduler)에서만 호출됩니다.
     */
    @CachePut(value = "popularKeywords", key = "#limit")
    public List<PopularKeywordResponse> warmUpPopularKeywordsCache(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return searchKeywordRepository.findAllByOrderBySearchCountDesc(pageable)
                .stream()
                .map(PopularKeywordResponse::from)
                .toList();
    }
}
