package com.team1ilchwiwoljang.domain.search.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PopularKeywordsCacheDto {

    @JsonDeserialize(contentAs = SearchKeywordPopularResponse.class)
    private List<SearchKeywordPopularResponse> keywords;

    public static PopularKeywordsCacheDto from(List<SearchKeywordPopularResponse> keywords) {
        return new PopularKeywordsCacheDto(keywords);
    }

    public List<SearchKeywordPopularResponse> keywords() {
        return keywords;
    }
}
