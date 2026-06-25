package com.team1ilchwiwoljang.domain.order.dto.response;

import java.util.List;

/**
 * 주문 생성 전 확인 화면에 필요한 상품 목록과 총 주문 금액 응답입니다.
 */
public record OrderPreviewResponse(
        List<OrderItemResponse> orderItems,
        long totalAmount
) {

    public static OrderPreviewResponse of(
            List<OrderItemResponse> orderItems,
            long totalAmount
    ) {
        return new OrderPreviewResponse(orderItems, totalAmount);
    }
}
