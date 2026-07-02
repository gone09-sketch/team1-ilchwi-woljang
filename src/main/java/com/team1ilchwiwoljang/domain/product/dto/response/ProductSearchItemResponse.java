package com.team1ilchwiwoljang.domain.product.dto.response;

import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;

public record ProductSearchItemResponse(
        Long productId,
        String name,
        int price,
        int stock,
        ProductStatus status,
        boolean orderable,
        String imageUrl
) {

    public static ProductSearchItemResponse from(Product product) {
        return new ProductSearchItemResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                product.isOnSale(),
                product.getImageUrl()
        );
    }
}
