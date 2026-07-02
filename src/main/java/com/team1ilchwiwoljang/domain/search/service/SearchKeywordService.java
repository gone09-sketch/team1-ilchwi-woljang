package com.team1ilchwiwoljang.domain.search.service;

import com.team1ilchwiwoljang.domain.search.dto.response.SearchKeywordPopularResponse;
import com.team1ilchwiwoljang.domain.search.dto.response.PopularKeywordsCacheDto;
import com.team1ilchwiwoljang.domain.search.repository.SearchKeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchKeywordService {

    private final SearchKeywordTransactionHelper searchKeywordTransactionHelper;
    private final PopularKeywordCacheService popularKeywordCacheService;

    /**
     * 검색어 횟수를 1 증가시킵니다.
     * 외부 트랜잭션의 rollback-only 전파 문제를 원천 차단하기 위해, 실제 DB CUD 작업은 별도의 빈(SearchKeywordTransactionHelper)에서 REQUIRES_NEW 트랜잭션으로 격리하여 수행합니다.
     * Lost Update 방지를 위해 DB 레벨의 원자적 UPDATE 쿼리를 우선 사용하며,
     * 존재하지 않는 검색어인 경우 save를 시도하고 unique 제약 충돌 발생 시 다시 원자적 UPDATE로 재시도(UPSERT 패턴)합니다.
     */
    public void incrementSearchCount(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }

        // 100자를 초과하는 키워드는 100자로 잘라내어 DB 제약조건 예외가 유발되는 것을 방어합니다.
        String truncatedKeyword = keyword.length() > 100
                ? keyword.substring(0, 100)
                : keyword;

        try {
            int updatedRows = searchKeywordTransactionHelper.incrementCount(truncatedKeyword);
            if (updatedRows == 0) {
                searchKeywordTransactionHelper.saveNewKeyword(truncatedKeyword);
            }
        } catch (Exception e) {
            // 다른 쓰레드와 동시에 등록되어 중복 키 제약 충돌 등이 발생한 경우 -> 다시 원자적 카운트 증가 수행
            log.debug("인기 검색어 등록 중 중복 충돌 감지, 카운트 증가 재시도. keyword={}", truncatedKeyword);
            searchKeywordTransactionHelper.incrementCount(truncatedKeyword);
        }
    }

    /**
     * 누적 검색 횟수 기준 인기 검색어 TOP N 조회
     * 캐시에서 최대 100개를 단일 키 'all'로 조회한 후, 요청된 limit 만큼 메모리에서 잘라 반환하여 100% 캐시 히트율을 확보합니다.
     */
    public List<SearchKeywordPopularResponse> getPopularKeywords(int limit) {
        PopularKeywordsCacheDto cachedDto = popularKeywordCacheService.getCachedPopularKeywords();
        return cachedDto.keywords().stream()
                .limit(limit)
                .toList();
    }
}
