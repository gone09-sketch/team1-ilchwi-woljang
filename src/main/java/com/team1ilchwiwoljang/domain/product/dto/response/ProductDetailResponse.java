package com.team1ilchwiwoljang.domain.product.dto.response;

import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;

public record ProductDetailResponse(
        Long productId,
        String name,
        String description,
        int price,
        int stock,
        ProductStatus status,
        Long categoryId,
        String categoryName
) {

    public static ProductDetailResponse from(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null
        );
    }
}
