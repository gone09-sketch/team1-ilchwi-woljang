package com.team1ilchwiwoljang.domain.search.service;

import com.team1ilchwiwoljang.domain.search.entity.SearchKeyword;
import com.team1ilchwiwoljang.domain.search.repository.SearchKeywordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SearchKeywordServiceTest {

    @InjectMocks
    private SearchKeywordService searchKeywordService;

    @Mock
    private SearchKeywordRepository searchKeywordRepository;

    @Test
    @DisplayName("신규 검색어를 처음 검색하면 SearchKeyword 엔티티를 생성하여 저장한다.")
    void givenNewKeyword_whenIncrementSearchCount_thenSaveNewEntity() {
        // given
        given(searchKeywordRepository.findByKeyword("셔츠")).willReturn(Optional.empty());

        // when
        searchKeywordService.incrementSearchCount("셔츠");

        // then
        verify(searchKeywordRepository, times(1)).save(any(SearchKeyword.class));
    }

    @Test
    @DisplayName("이미 존재하는 검색어를 다시 검색하면 카운트를 1 증가시킨다.")
    void givenExistingKeyword_whenIncrementSearchCount_thenIncrementCount() {
        // given
        SearchKeyword existing = SearchKeyword.create("셔츠"); // searchCount = 1
        given(searchKeywordRepository.findByKeyword("셔츠")).willReturn(Optional.of(existing));

        // when
        searchKeywordService.incrementSearchCount("셔츠");

        // then
        assertThat(existing.getSearchCount()).isEqualTo(2L);
        verify(searchKeywordRepository, never()).save(any());
    }

    @Test
    @DisplayName("unique 제약 충돌 발생 시 재조회하여 카운트를 증가시킨다.")
    void givenUniqueConstraintViolation_whenIncrementSearchCount_thenRetryAndIncrement() {
        // given: 첫 조회에서는 없음, save 시 unique 충돌, 재조회에서는 존재
        SearchKeyword existing = SearchKeyword.create("셔츠");
        given(searchKeywordRepository.findByKeyword("셔츠"))
                .willReturn(Optional.empty())         // 첫 번째 조회: 없음
                .willReturn(Optional.of(existing));   // 재조회: 존재

        given(searchKeywordRepository.save(any(SearchKeyword.class)))
                .willThrow(new DataIntegrityViolationException("unique constraint violation"));

        // when
        searchKeywordService.incrementSearchCount("셔츠");

        // then: 재조회 후 카운트 증가
        assertThat(existing.getSearchCount()).isEqualTo(2L);
        verify(searchKeywordRepository, times(2)).findByKeyword(anyString());
    }
}
