package com.team1ilchwiwoljang.domain.product.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PopularProductsCacheDto {
    
    @JsonDeserialize(contentAs = PopularProductResponse.class)
    private List<PopularProductResponse> products;

    public static PopularProductsCacheDto from(List<PopularProductResponse> products) {
        return new PopularProductsCacheDto(products);
    }

    public List<PopularProductResponse> products() {
        return products;
    }
}
