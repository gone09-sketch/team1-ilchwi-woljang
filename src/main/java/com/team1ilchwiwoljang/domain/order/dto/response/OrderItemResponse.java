package com.team1ilchwiwoljang.domain.order.dto.response;

import com.team1ilchwiwoljang.domain.order.entity.OrderItem;

public record OrderItemResponse(
        String productName,
        long productPrice,
        long quantity,
        long productTotalAmount
) {

    public static OrderItemResponse of(
            String productName,
            long productPrice,
            long quantity,
            long productTotalAmount
    ) {
        return new OrderItemResponse(
                productName,
                productPrice,
                quantity,
                productTotalAmount
        );
    }

    public static OrderItemResponse from(OrderItem orderItem) {
        return OrderItemResponse.of(
                orderItem.getProductNameSnapshot(),
                orderItem.getProductPriceSnapshot(),
                orderItem.getQuantity(),
                orderItem.getTotalPrice()
        );
    }
}
