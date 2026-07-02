package com.team1ilchwiwoljang.domain.search.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SearchKeywordServiceTest {

    @InjectMocks
    private SearchKeywordService searchKeywordService;

    @Mock
    private SearchKeywordTransactionHelper searchKeywordTransactionHelper;

    @Mock
    private PopularKeywordCacheService popularKeywordCacheService;

    @Test
    @DisplayName("신규 검색어를 처음 검색하면 원자적 업데이트가 0을 반환하고, 새 SearchKeyword 엔티티를 생성하여 저장한다.")
    void givenNewKeyword_whenIncrementSearchCount_thenSaveNewEntity() {
        // given
        given(searchKeywordTransactionHelper.incrementCount("셔츠")).willReturn(0);

        // when
        searchKeywordService.incrementSearchCount("셔츠");

        // then
        verify(searchKeywordTransactionHelper, times(1)).incrementCount("셔츠");
        verify(searchKeywordTransactionHelper, times(1)).saveNewKeyword("셔츠");
    }

    @Test
    @DisplayName("이미 존재하는 검색어를 다시 검색하면 원자적 업데이트가 1을 반환하며 엔티티를 별도로 저장하지 않는다.")
    void givenExistingKeyword_whenIncrementSearchCount_thenIncrementCount() {
        // given
        given(searchKeywordTransactionHelper.incrementCount("셔츠")).willReturn(1);

        // when
        searchKeywordService.incrementSearchCount("셔츠");

        // then
        verify(searchKeywordTransactionHelper, times(1)).incrementCount("셔츠");
        verify(searchKeywordTransactionHelper, never()).saveNewKeyword(anyString());
    }

    @Test
    @DisplayName("신규 검색어 저장 도중 unique 제약 충돌 발생 시, 다시 원자적 업데이트를 호출한다.")
    void givenUniqueConstraintViolation_whenIncrementSearchCount_thenRetryAndIncrement() {
        // given: 첫 원자적 업데이트 0 반환 -> save 호출 시 unique 충돌 -> 재시도에서 incrementSearchCount 재호출
        given(searchKeywordTransactionHelper.incrementCount("셔츠"))
                .willReturn(0) // 첫 번째 시도
                .willReturn(1); // 재시도

        doThrow(new DataIntegrityViolationException("unique constraint violation"))
                .when(searchKeywordTransactionHelper).saveNewKeyword("셔츠");

        // when
        searchKeywordService.incrementSearchCount("셔츠");

        // then: 업데이트는 2번 호출, save는 1번 호출됨
        verify(searchKeywordTransactionHelper, times(2)).incrementCount("셔츠");
        verify(searchKeywordTransactionHelper, times(1)).saveNewKeyword("셔츠");
    }
}
