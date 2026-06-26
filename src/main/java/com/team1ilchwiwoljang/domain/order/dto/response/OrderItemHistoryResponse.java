package com.team1ilchwiwoljang.domain.order.dto.response;

public record OrderItemHistoryResponse(
        Long productId,
        String productName,
        Long productPrice,
        Long quantity,
        Long totalPrice,
        Long categoryId,
        String categoryName
) {
    public static OrderItemHistoryResponse of(
            Long productId,
            String productName,
            Long productPrice,
            Long quantity,
            Long totalPrice,
            Long categoryId,
            String categoryName
    ) {
        return new OrderItemHistoryResponse(
                productId,
                productName,
                productPrice,
                quantity,
                totalPrice,
                categoryId,
                categoryName
        );
    }
}
