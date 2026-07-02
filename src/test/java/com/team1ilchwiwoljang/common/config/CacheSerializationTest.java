package com.team1ilchwiwoljang.common.config;

import com.team1ilchwiwoljang.domain.product.dto.response.PopularProductResponse;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import com.team1ilchwiwoljang.domain.search.dto.response.SearchKeywordPopularResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CacheSerializationTest {

    @Test
    @DisplayName("CacheConfig의 Redis 직렬화 설정으로 캐시 DTO 목록을 ClassCastException 없이 복구한다.")
    void givenPopularResponses_whenSerializeAndDeserialize_thenSuccessfulRoundTrip() {
        // given
        GenericJackson2JsonRedisSerializer serializer = new CacheConfig().redisCacheValueSerializer();

        List<SearchKeywordPopularResponse> keywordList = new ArrayList<>();
        keywordList.add(new SearchKeywordPopularResponse("셔츠", 100L));
        keywordList.add(new SearchKeywordPopularResponse("청바지", 50L));

        byte[] serializedKeywords = serializer.serialize(keywordList);
        assertThat(serializedKeywords).isNotEmpty();

        // 역직렬화 수행
        Object deserializedKeywordsObj = serializer.deserialize(serializedKeywords);
        assertThat(deserializedKeywordsObj).isInstanceOf(List.class);

        @SuppressWarnings("unchecked")
        List<SearchKeywordPopularResponse> deserializedKeywords = (List<SearchKeywordPopularResponse>) deserializedKeywordsObj;
        assertThat(deserializedKeywords).hasSize(2);
        assertThat(deserializedKeywords.get(0)).isInstanceOf(SearchKeywordPopularResponse.class);
        assertThat(deserializedKeywords.get(0).keyword()).isEqualTo("셔츠");
        assertThat(deserializedKeywords.get(0).searchCount()).isEqualTo(100L);

        List<PopularProductResponse> productList = new ArrayList<>();
        productList.add(new PopularProductResponse(1L, "베이직 셔츠", 10000, 50, ProductStatus.ON_SALE, 10));

        byte[] serializedProducts = serializer.serialize(productList);
        assertThat(serializedProducts).isNotEmpty();

        // 역직렬화 수행
        Object deserializedProductsObj = serializer.deserialize(serializedProducts);
        assertThat(deserializedProductsObj).isInstanceOf(List.class);

        @SuppressWarnings("unchecked")
        List<PopularProductResponse> deserializedProducts = (List<PopularProductResponse>) deserializedProductsObj;
        assertThat(deserializedProducts).hasSize(1);
        assertThat(deserializedProducts.get(0)).isInstanceOf(PopularProductResponse.class);
        assertThat(deserializedProducts.get(0).productId()).isEqualTo(1L);
        assertThat(deserializedProducts.get(0).name()).isEqualTo("베이직 셔츠");
    }
}
