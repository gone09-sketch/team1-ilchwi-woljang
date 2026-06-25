package com.team1ilchwiwoljang.domain.order.dto.response;

public record OrderItemHistoryResponse(
        Long productId,
        String productName,
        Long productPrice,
        Long quantity,
        Long totalPrice
) {
    public static OrderItemHistoryResponse of(
            Long productId,
            String productName,
            Long productPrice,
            Long quantity,
            Long totalPrice
    ) {
        return new OrderItemHistoryResponse(
                productId,
                productName,
                productPrice,
                quantity,
                totalPrice
        );
    }
}
