package com.team1ilchwiwoljang.domain.search.dto.response;

import com.team1ilchwiwoljang.domain.search.entity.SearchKeyword;

public record PopularKeywordResponse(
        String keyword,
        long searchCount
) {
    public static PopularKeywordResponse from(SearchKeyword searchKeyword) {
        return new PopularKeywordResponse(
                searchKeyword.getKeyword(),
                searchKeyword.getSearchCount()
        );
    }
}
