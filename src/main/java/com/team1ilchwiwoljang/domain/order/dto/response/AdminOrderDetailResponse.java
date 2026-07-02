package com.team1ilchwiwoljang.domain.order.dto.response;

import com.team1ilchwiwoljang.domain.order.entity.Order;
import java.time.LocalDateTime;
import java.util.List;

public record AdminOrderDetailResponse(
        Long orderId,
        String orderNumber,
        Long memberId,
        Long totalAmount,
        Long pgAmount,
        String orderStatus,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime canceledAt,
        List<OrderItemResponse> orderItems
) {
    public static AdminOrderDetailResponse from(Order order, List<OrderItemResponse> orderItems) {
        return new AdminOrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getMember().getId(),
                order.getTotalAmount(),
                order.getPgAmount(),
                order.getOrderStatus().name(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getCanceledAt(),
                orderItems
        );
    }
}
