package com.team1ilchwiwoljang.domain.order.dto.response;

import com.team1ilchwiwoljang.domain.order.entity.Order;
import java.util.List;

public record OrderResponse(
        Long orderId,
        String orderNumber,
        String orderStatus,
        long totalAmount,
        List<OrderItemResponse> orderItems
) {

    public static OrderResponse from(Order order, List<OrderItemResponse> orderItems) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getOrderStatus().name(),
                order.getTotalAmount(),
                orderItems
        );
    }
}
