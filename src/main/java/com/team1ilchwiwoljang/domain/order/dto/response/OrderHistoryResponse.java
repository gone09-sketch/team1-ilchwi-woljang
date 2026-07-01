package com.team1ilchwiwoljang.domain.order.dto.response;

import java.time.LocalDateTime;

public record OrderHistoryResponse(
        Long orderId,
        String orderNumber,
        Long totalAmount,
        String orderStatus,
        LocalDateTime createdAt
) {
    public static OrderHistoryResponse of(
            Long orderId,
            String orderNumber,
            Long totalAmount,
            String orderStatus,
            LocalDateTime createdAt
    ) {
        return new OrderHistoryResponse(
                orderId,
                orderNumber,
                totalAmount,
                orderStatus,
                createdAt
        );
    }
}
