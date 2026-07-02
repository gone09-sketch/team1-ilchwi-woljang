package com.team1ilchwiwoljang.domain.order.dto.response;

import java.util.List;

/**
 * 상품 상세 페이지에서 바로 주문하기 전 보여줄 주문서 미리보기 응답입니다.
 * 장바구니 기반 미리보기와 달리 cartId가 없으므로 별도 DTO로 분리했습니다.
 */
public record DirectOrderPreviewResponse(
        List<DirectOrderPreviewItemResponse> orderItems,
        long totalOrderAmount
) {

    public static DirectOrderPreviewResponse of(
            List<DirectOrderPreviewItemResponse> orderItems,
            long totalOrderAmount
    ) {
        return new DirectOrderPreviewResponse(orderItems, totalOrderAmount);
    }

    public record DirectOrderPreviewItemResponse(
            Long productId,
            String productName,
            long productPrice,
            int quantity,
            long productTotalAmount
    ) {

        public static DirectOrderPreviewItemResponse of(
                Long productId,
                String productName,
                long productPrice,
                int quantity,
                long productTotalAmount
        ) {
            return new DirectOrderPreviewItemResponse(
                    productId,
                    productName,
                    productPrice,
                    quantity,
                    productTotalAmount
            );
        }
    }
}
