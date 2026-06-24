package com.team1ilchwiwoljang.domain.order.dto.response;

import com.team1ilchwiwoljang.domain.order.entity.OrderItem;

public record OrderItemResponse(
        String productName,
        Long productPrice,
        Long quantity,
        Long totalPrice
) {

    public static OrderItemResponse from(OrderItem orderItem) {
        return new OrderItemResponse(
                orderItem.getProductNameSnapshot(),
                orderItem.getProductPriceSnapshot(),
                orderItem.getQuantity(),
                orderItem.getTotalPrice()
        );
    }
}
