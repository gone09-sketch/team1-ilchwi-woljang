package com.team1ilchwiwoljang.domain.search.dto.response;

import com.team1ilchwiwoljang.domain.search.entity.SearchKeyword;

public record SearchKeywordPopularResponse(
        String keyword,
        long searchCount
) {
    public static SearchKeywordPopularResponse from(SearchKeyword searchKeyword) {
        return new SearchKeywordPopularResponse(
                searchKeyword.getKeyword(),
                searchKeyword.getSearchCount()
        );
    }
}
