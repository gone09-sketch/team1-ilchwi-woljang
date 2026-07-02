package com.team1ilchwiwoljang.domain.search.service;

import com.team1ilchwiwoljang.domain.search.entity.SearchKeyword;
import com.team1ilchwiwoljang.domain.search.repository.SearchKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SearchKeywordTransactionHelper {

    private final SearchKeywordRepository searchKeywordRepository;

    /**
     * 원자적 카운트 증가 쿼리를 REQUIRES_NEW 트랜잭션으로 수행합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int incrementCount(String keyword) {
        return searchKeywordRepository.incrementSearchCount(keyword);
    }

    /**
     * 신규 검색어를 REQUIRES_NEW 트랜잭션으로 저장합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveNewKeyword(String keyword) {
        searchKeywordRepository.save(SearchKeyword.create(keyword));
    }
}
