package com.team1ilchwiwoljang.domain.search.service;

import com.team1ilchwiwoljang.domain.search.dto.response.PopularKeywordResponse;
import com.team1ilchwiwoljang.domain.search.entity.SearchKeyword;
import com.team1ilchwiwoljang.domain.search.repository.SearchKeywordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class SearchKeywordServiceIntegrationTest {

    @Autowired
    private SearchKeywordService searchKeywordService;

    @Autowired
    private SearchKeywordRepository searchKeywordRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        Cache cache = cacheManager.getCache("popularKeywords");
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    @DisplayName("검색어를 검색하면 DB에 카운트가 누적된다.")
    void givenKeyword_whenIncrementSearchCount_thenCountAccumulated() {
        // when
        searchKeywordService.incrementSearchCount("셔츠");
        searchKeywordService.incrementSearchCount("셔츠");
        searchKeywordService.incrementSearchCount("청바지");

        // then
        SearchKeyword shirt = searchKeywordRepository.findByKeyword("셔츠").orElseThrow();
        SearchKeyword jeans = searchKeywordRepository.findByKeyword("청바지").orElseThrow();

        assertThat(shirt.getSearchCount()).isEqualTo(2L);
        assertThat(jeans.getSearchCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("인기 검색어 조회 시 처음에는 DB를 조회하고 캐시에 저장되며, 두 번째부터는 캐시에서 반환한다.")
    void givenKeywords_whenGetPopularKeywords_thenCachingBehaviorWorks() {
        // given
        searchKeywordService.incrementSearchCount("셔츠");
        searchKeywordService.incrementSearchCount("셔츠");
        searchKeywordService.incrementSearchCount("청바지");

        Cache cache = cacheManager.getCache("popularKeywords");
        assertThat(cache).isNotNull();
        assertThat(cache.get(10)).isNull(); // 캐시 비어있음 확인

        // when: 첫 번째 호출 (DB 조회)
        List<PopularKeywordResponse> firstCall = searchKeywordService.getPopularKeywords(10);

        // then: 캐시에 저장됨
        assertThat(cache.get(10)).isNotNull();
        assertThat(firstCall).hasSize(2);
        assertThat(firstCall.get(0).keyword()).isEqualTo("셔츠");   // 카운트 높은 순
        assertThat(firstCall.get(0).searchCount()).isEqualTo(2L);
        assertThat(firstCall.get(1).keyword()).isEqualTo("청바지");

        // when: DB 데이터 변경 (캐시에는 반영 안 됨)
        searchKeywordService.incrementSearchCount("원피스");
        searchKeywordService.incrementSearchCount("원피스");
        searchKeywordService.incrementSearchCount("원피스");

        // then: 두 번째 호출 시 캐시 데이터 반환 (원피스 미포함)
        List<PopularKeywordResponse> secondCall = searchKeywordService.getPopularKeywords(10);
        assertThat(secondCall).isEqualTo(firstCall);
    }

    @Test
    @DisplayName("인기 검색어 조회 결과는 검색 횟수 내림차순으로 정렬된다.")
    void givenMultipleKeywords_whenGetPopularKeywords_thenReturnOrderedByCount() {
        // given
        searchKeywordService.incrementSearchCount("청바지");
        searchKeywordService.incrementSearchCount("셔츠");
        searchKeywordService.incrementSearchCount("셔츠");
        searchKeywordService.incrementSearchCount("원피스");
        searchKeywordService.incrementSearchCount("원피스");
        searchKeywordService.incrementSearchCount("원피스");

        // when
        List<PopularKeywordResponse> result = searchKeywordService.getPopularKeywords(10);

        // then: 원피스(3) > 셔츠(2) > 청바지(1) 순
        assertThat(result).hasSize(3);
        assertThat(result.get(0).keyword()).isEqualTo("원피스");
        assertThat(result.get(1).keyword()).isEqualTo("셔츠");
        assertThat(result.get(2).keyword()).isEqualTo("청바지");
    }
}
