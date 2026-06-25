package com.team1ilchwiwoljang.domain.order.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record OrderHistoryResponse(
        Long orderId,
        String orderNumber,
        Long totalAmount,
        String orderStatus,
        LocalDateTime createdAt,
        List<OrderItemHistoryResponse> orderItems
) {
    public static OrderHistoryResponse of(
            Long orderId,
            String orderNumber,
            Long totalAmount,
            String orderStatus,
            LocalDateTime createdAt,
            List<OrderItemHistoryResponse> orderItems
    ) {
        return new OrderHistoryResponse(
                orderId,
                orderNumber,
                totalAmount,
                orderStatus,
                createdAt,
                orderItems
        );
    }
}
