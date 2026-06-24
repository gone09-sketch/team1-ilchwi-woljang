package com.team1ilchwiwoljang.domain.cart.dto.response;

import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;

public record CartItemResponse(
        Long cartItemId,
        Long productId,
        String productName,
        int productPrice,
        int quantity,
        long itemTotalPrice,
        int stock,
        ProductStatus productStatus,
        boolean orderable
) {
}
