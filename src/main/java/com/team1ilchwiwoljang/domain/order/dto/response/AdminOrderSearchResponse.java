package com.team1ilchwiwoljang.domain.order.dto.response;

import com.team1ilchwiwoljang.domain.order.entity.Order;

import java.time.LocalDateTime;

public record AdminOrderSearchResponse(
        Long orderId,
        String orderNumber,
        Long memberId,
        Long totalAmount,
        Long pgAmount,
        String orderStatus,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime canceledAt
) {
    public static AdminOrderSearchResponse from(Order order) {
        return new AdminOrderSearchResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getMember().getId(),
                order.getTotalAmount(),
                order.getPgAmount(),
                order.getOrderStatus().name(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getCanceledAt()
        );
    }
}
