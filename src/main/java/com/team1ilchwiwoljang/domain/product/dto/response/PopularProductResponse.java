package com.team1ilchwiwoljang.domain.product.dto.response;

import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;

public record PopularProductResponse(
        Long productId,
        String name,
        int price,
        int stock,
        ProductStatus status,
        int salesCount
) {
    public static PopularProductResponse from(Product product) {
        return new PopularProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                product.getSalesCount()
        );
    }
}
