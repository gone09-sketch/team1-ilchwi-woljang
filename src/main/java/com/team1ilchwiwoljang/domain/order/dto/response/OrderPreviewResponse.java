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
            String productName,
            long productPrice,
            int quantity,
            long productTotalAmount
    ) {

        public static OrderPreviewItemResponse of(
                String productName,
                long productPrice,
                int quantity,
                long productTotalAmount
        ) {
            return new OrderPreviewItemResponse(
                    productName,
                    productPrice,
                    quantity,
                    productTotalAmount
            );
        }
    }
}
