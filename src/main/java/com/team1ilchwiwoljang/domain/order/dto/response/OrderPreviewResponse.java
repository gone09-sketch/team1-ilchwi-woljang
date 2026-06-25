package com.team1ilchwiwoljang.domain.order.dto.response;

import java.util.List;

public record OrderPreviewResponse(
        List<OrderPreviewItemResponse> orderItems,
        long totalOrderAmount
) {

    public static OrderPreviewResponse of(
            List<OrderPreviewItemResponse> orderItems,
            long totalOrderAmount
    ) {
        return new OrderPreviewResponse(orderItems, totalOrderAmount);
    }

    public record OrderPreviewItemResponse(
            Long cartId,
            Long productId,
            String productName,
            long productPrice,
            int quantity,
            long productTotalAmount
    ) {

        public static OrderPreviewItemResponse of(
                Long cartId,
                Long productId,
                String productName,
                long productPrice,
                int quantity,
                long productTotalAmount
        ) {
            return new OrderPreviewItemResponse(
                    cartId,
                    productId,
                    productName,
                    productPrice,
                    quantity,
                    productTotalAmount
            );
        }
    }
}
