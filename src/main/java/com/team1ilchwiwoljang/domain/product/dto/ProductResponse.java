package com.team1ilchwiwoljang.domain.product.dto;

import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;

public record ProductResponse(
        Long productId,
        String name,
        int price,
        int stock,
        ProductStatus status,
        String description
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                product.getDescription()
        );
    }
}
